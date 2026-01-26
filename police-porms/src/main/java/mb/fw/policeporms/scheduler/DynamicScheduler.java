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
import reactor.core.publisher.Mono;

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
		if (spec == null) {
			log.error("InterfaceSpec is null. Cannot run task.");
			return;
		}
		String interfaceId = spec.getInterfaceId();
		log.info("[{}] Task started", interfaceId);
		try {
			interfaceCallService.callApi(spec).flatMap(dataList -> {
				// dataList가 null일 경우를 대비
				if (dataList == null || dataList.isEmpty()) {
					log.warn("[{}] API returned empty or null dataList", interfaceId);
					return Mono.empty();
				}
				log.info("[{}] API call finished, sending data. size={},\n datalist = {}", interfaceId, dataList.size(), dataList);
				return interfaceCallService.sendData(interfaceId, dataList);
			}).onErrorResume(e -> {
				log.error("[{}] Error occurred during API call or Data sending: {}", interfaceId, e.getMessage());
				return Mono.empty();
			}).doOnSuccess(v -> log.info("[{}] Task finished successfully", interfaceId)).subscribe(null,
					error -> log.error("[{}] Terminal error in subscribe: ", interfaceId, error));
		} catch (Exception e) {
			// 비동기 체인 생성 중에 터지는 에러 방어
			log.error("[{}] Critical error before subscription: ", interfaceId, e);
		}
	}
}
