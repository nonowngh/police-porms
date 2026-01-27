package mb.fw.policeporms.sender.scheduler;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.TransactionIdGenerator;
import mb.fw.policeporms.annotaion.SenderComponent;
import mb.fw.policeporms.constants.InterfaceStatusConstants;
import mb.fw.policeporms.dto.ResponseMessage;
import mb.fw.policeporms.sender.service.InterfaceCallService;
import mb.fw.policeporms.spec.InterfaceSpec;

@Slf4j
@SenderComponent
public class DynamicScheduler {

	private final List<InterfaceSpec> specs;
	private TaskScheduler scheduler;
	private final List<ScheduledFuture<?>> futures = new java.util.ArrayList<>();
	private final InterfaceCallService interfaceCallService;

	public DynamicScheduler(List<InterfaceSpec> specs, InterfaceCallService interfaceCallService) {
		this.specs = specs;
		this.interfaceCallService = interfaceCallService;
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
        log.info("Scheduled interfaceId={} with cron={}", spec.getInterfaceId(), spec.getBatchSchedulerCron());
    }

	private void runTask(InterfaceSpec spec) {
        if (spec == null) return;
        String interfaceId = spec.getInterfaceId();
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
		String transactionId = TransactionIdGenerator.generate(interfaceId, "", currentDate);
        log.info("[{}] 스케줄 작업 시작 -> transaction-id : {}", interfaceId, transactionId);
        
        // 서비스 실행
        ResponseMessage result = interfaceCallService.executeApiDataSend(spec, transactionId);
        
        // 최종 결과 로깅
        if (InterfaceStatusConstants.SUCCESS.equals(result.getProcessCd())) {
            log.info("[{}] 전송 성공: {}건", transactionId, result.getResultCount());
        } else {
            log.error("[{}] 전송 실패: {}", transactionId, result.getProcessMsg());
        }

       log.info("[{}] 스케줄 종료 -> transaction-id : {}", interfaceId, transactionId);
    }
}
