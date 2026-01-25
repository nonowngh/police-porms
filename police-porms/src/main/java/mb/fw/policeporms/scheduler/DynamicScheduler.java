package mb.fw.policeporms.scheduler;

import java.util.List;
import java.util.concurrent.ScheduledFuture;

import javax.annotation.PostConstruct;

import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.service.InterfaceCallService;
import mb.fw.policeporms.spec.InterfaceSpec;

@Slf4j
@Component
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

		// JSON에 정의된 cron대로 스케줄 등록
		specs.forEach(this::scheduleTask);
	}

	private void scheduleTask(InterfaceSpec spec) {
		Runnable task = () -> runTask(spec);
		// Runnable toString() 오버라이드 -> 오류 로그에서 interfaceId 표시
		Runnable wrappedTask = new Runnable() {
			@Override
			public void run() {
				task.run();
			}
			@Override
			public String toString() {
				return "InterfaceTask-" + spec.getInterfaceId();
			}
		};
		ScheduledFuture<?> future = ((ThreadPoolTaskScheduler) scheduler).schedule(wrappedTask,
				new CronTrigger(spec.getBatchSchedulerCron()));
		futures.add(future);
		log.info("Scheduled interfaceId={} with cron={}", spec.getInterfaceId(), spec.getBatchSchedulerCron());
	}

	private void runTask(InterfaceSpec spec) {
		String originalName = Thread.currentThread().getName();
		Thread.currentThread().setName("scheduler-" + spec.getInterfaceId());
		try {
			log.info("Task started for interfaceId={}", spec.getInterfaceId());

			//open-api 호출 
			
			//내부 esb로 호출
			interfaceCallService.sendData(spec.getInterfaceId(), null);

			log.info("Task finished for interfaceId={}", spec.getInterfaceId());
		} catch (Exception e) {
			log.error("Error executing interfaceId={}", spec.getInterfaceId(), e);
		} finally {
			// 스레드 이름 복원
			Thread.currentThread().setName(originalName);
		}
	}
}
