package mb.fw.policeporms.domain.sender.scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.TransactionIdGenerator;
import mb.fw.policeporms.common.annotation.SenderComponent;
import mb.fw.policeporms.common.constant.InterfaceStatus;
import mb.fw.policeporms.common.dto.ResponseMessage;
import mb.fw.policeporms.common.logging.InterfaceLogging;
import mb.fw.policeporms.common.spec.InterfaceSpec;
import mb.fw.policeporms.domain.sender.service.InterfaceCallService;

@Slf4j
@SenderComponent
public class DynamicScheduler {

	private final List<InterfaceSpec> specs;
	private TaskScheduler scheduler;
	private final List<ScheduledFuture<?>> futures = new java.util.ArrayList<>();
	private final InterfaceCallService interfaceCallService;
	private final Optional<InterfaceLogging> interfaceLogging;

	public DynamicScheduler(List<InterfaceSpec> specs, InterfaceCallService interfaceCallService,
			Optional<InterfaceLogging> interfaceLogging) {
		this.specs = specs;
		this.interfaceCallService = interfaceCallService;
		this.interfaceLogging = interfaceLogging;
	}

	@PostConstruct
	public void init() {
		// 스케줄러 초기화
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.setPoolSize(5);
		taskScheduler.setThreadNamePrefix("interface-scheduler-"); // 공통 접두사
		taskScheduler.initialize();
		this.scheduler = taskScheduler;

		specs.forEach(this::scheduleTask);
	}

	private void scheduleTask(InterfaceSpec spec) {
		Runnable task = () -> {
			try {
				runTask(spec);
			} catch (Exception e) {
				log.error("[{}] 스케줄 작업 실행 중 치명적 오류: {}", spec.getInterfaceId(), e.getMessage());
			}
		};

		ScheduledFuture<?> future = scheduler.schedule(task, new CronTrigger(spec.getBatchSchedulerCron()));
		futures.add(future);
		log.info("⏰ 스케줄 등록 interfaceId={} with cron={}", spec.getInterfaceId(), spec.getBatchSchedulerCron());
	}

	private void runTask(InterfaceSpec spec) {
		if (spec == null)
			return;
		String interfaceId = spec.getInterfaceId();
		String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
		String transactionId = TransactionIdGenerator.generate(interfaceId, "", currentDate);

		// 시작 시간 기록
		long startTimeMs = System.currentTimeMillis();
		String startTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));

		// 로깅용 시작 시간을 미리 캡처해 둠 (나중에 로그 쏠 때 사용)
		String esbStartTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));

		log.info("[{}-{}] 스케줄 작업 시작 ▶️▶️▶️ transaction-id : {} | 시작시간 : {}", interfaceId, spec.getInterfaceDescription(), transactionId, startTimeStr);

		AtomicInteger apiFetchedCount = new AtomicInteger(0);

		// 콜백에서는 API 조회 건수만 저장 (startLogging 은 제거)
		ResponseMessage result = interfaceCallService.executeApiDataSend(spec, transactionId, (totalCount) -> {
			apiFetchedCount.set(totalCount);
		});

		// 최종 결과 콘솔 로깅
		if (InterfaceStatus.SUCCESS == result.getProcessCd()) {
			log.info("[{}] ✅ 전송 성공 : API 조회 {}건 / 실제 DB 적재 {}건", transactionId,
					String.format("%,d", apiFetchedCount.get()), String.format("%,d", result.getResultCount()));
		} else {
			log.error("[{}] ❌ 전송 실패 : {}", transactionId, result.getProcessMsg());
		}

		// 로깅 처리
		interfaceLogging.ifPresent(logging -> {
			// 실패했을 경우에는 원본 API 조회 건수를 로깅하고, 성공했을 경우에는 실제 DB 적재 건수를 세팅
			int esbTotalCount = (InterfaceStatus.SUCCESS == result.getProcessCd()) ? result.getResultCount() : apiFetchedCount.get();
			int esbErrorCount = (InterfaceStatus.SUCCESS == result.getProcessCd()) ? 0 : apiFetchedCount.get();

			// 미리 캡처해둔 시작시간(esbStartTime)과 실제 적재 건수를 넣어 시작 로그 전송
			logging.asyncStartLogging(interfaceId, transactionId, "OUT", "INN", esbTotalCount, esbStartTime);

			// 이어서 곧바로 종료 로그 전송 (성공 시 에러 0건)
			logging.asyncEndLogging(interfaceId, transactionId, esbErrorCount, result.getProcessCd(), result.getProcessMsg());
		});

		// 종료 시간 및 소요 시간 계산
		long endTimeMs = System.currentTimeMillis();
		String endTimeStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS"));
		long durationMs = endTimeMs - startTimeMs;
		double durationSec = durationMs / 1000.0;

		log.info("[{}] 스케줄 종료 🏁🏁🏁🏁🏁 종료시간 : {} | 총 소요시간 : {}ms ({}초)", transactionId, endTimeStr, durationMs, durationSec);
	}
	
	public boolean executeManually(String interfaceId) {
	    Optional<InterfaceSpec> targetSpec = specs.stream().filter(spec -> spec.getInterfaceId().equals(interfaceId)).findFirst();

	    if (targetSpec.isPresent()) {
	        InterfaceSpec spec = targetSpec.get();
	        
	        // API 요청 쓰레드가 블로킹되지 않도록 기존 스케줄러 쓰레드 풀을 이용해 비동기 실행
	        if (this.scheduler instanceof ThreadPoolTaskScheduler) {
	            ((ThreadPoolTaskScheduler) this.scheduler).execute(() -> {
	                try {
	                    log.info("▶️ API 요청으로 인터페이스 수동 즉시 실행: {}", interfaceId);
	                    runTask(spec);
	                } catch (Exception e) {
	                    log.error("[{}] API 수동 실행 중 치명적 오류: {}", spec.getInterfaceId(), e.getMessage());
	                }
	            });
	        }
	        return true;
	    } else {
	        log.warn("❌ 수동 실행 실패: 찾을 수 없는 interfaceId ({})", interfaceId);
	        return false;
	    }
	}
}