package mb.fw.policeporms.domain.sender.scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;

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
		log.info("[{}-{}] 스케줄 작업 시작 ▶️▶️▶️ transaction-id : {}", interfaceId, spec.getInterfaceDescription(),
				transactionId);

//        ResponseMessage result = interfaceCallService.executeApiDataSend(spec, transactionId);

		// 서비스 실행 시 콜백 정의(인터페이스 로깅을 위해~)
		ResponseMessage result = interfaceCallService.executeApiDataSend(spec, transactionId, (totalCount) -> {
			interfaceLogging.ifPresent(logging -> {
				logging.asyncStartLogging(interfaceId, transactionId, "OUT", "INN", totalCount);
			});
		});

		// 최종 결과 로깅
		if (InterfaceStatus.SUCCESS == result.getProcessCd()) {
			log.info("[{}] ✅ 전송 성공 : {}건", transactionId, result.getResultCount());
		} else {
			log.error("[{}] ❌ 전송 실패 : {}", transactionId, result.getProcessMsg());
		}
		interfaceLogging.ifPresent(logging -> {
			logging.asyncEndLogging(interfaceId, transactionId, result.getResultCount(), result.getProcessCd(),
					result.getProcessMsg());
		});

		log.info("[{}] 스케줄 종료 🏁🏁🏁🏁🏁", transactionId);
	}
}
