package mb.fw.atb.schedule;

import com.mb.indigo2.springsupport.AdaptorConfigBean;
import lombok.extern.slf4j.Slf4j;
import mb.fw.adaptor.configuration.AutoConfig;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.job.com.ToJMSData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import javax.annotation.PostConstruct;
import java.net.ConnectException;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@Component
@DependsOn("autoIfCfg")
public class TaskSchedulingService {

    @Autowired
    AutoConfig autoConfig;

    @Autowired
    private ThreadPoolTaskScheduler taskScheduler;

    @Autowired
    private IFConfig ifConfig;

    @Autowired(required = false)
    JmsTemplate jmsTemplate;

    @Autowired
    AdaptorConfigBean adaptorConfigBean;

    @Autowired
    private ApplicationContext appContext;

    @Autowired
    ToJMSData toJMSData;

    Map<String, ScheduledFuture<?>> jobsMap = new HashMap<>();

    public void scheduleATask(String jobId, Runnable tasklet, String cronExpression) {
        log.info("Scheduling task with job id: " + jobId + " and cron expression: " + cronExpression);
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(tasklet, new CronTrigger(cronExpression, TimeZone.getTimeZone(TimeZone.getDefault().getID())));
        jobsMap.put(jobId, scheduledTask);

    }

    public void scheduleActFist(String jobId, Runnable tasklet, int intervalSec) {
        log.info("Scheduling task with job id: " + jobId + " and ActFirst " + intervalSec + " seconds");
        Calendar calendar = Calendar.getInstance();
        Date currentTime = calendar.getTime();
        // 5초 추가하기
        calendar.setTime(currentTime);
        calendar.add(Calendar.SECOND, intervalSec);
        Date futureTime = calendar.getTime();
        ScheduledFuture<?> scheduledTask = taskScheduler.schedule(tasklet, futureTime);
        jobsMap.put(jobId, scheduledTask);
    }

    public void removeScheduledTask(String jobId) {

        ScheduledFuture<?> scheduledTask = jobsMap.get(jobId);
        if (scheduledTask != null) {
            scheduledTask.cancel(true);
            jobsMap.put(jobId, null);
        }
    }

    @PostConstruct
    public void scheduleStart() {

            /**
             *            esb stater에 개발된 어노테이션 기반 스케줄 ha 스케줄이 개발베이스 스캐줄을 등록하면 동작안하던것이 Spring boot 버전업을
             *            했더니 작동하기 시작했다. 혹시나 동작을 안할까봐 코드를 살려둠
                    final byte[] count = {0};

                    if (autoConfig.getHaEnabled().equals("true")) {
                        new Thread() {
                            public void run() {

                                while (true) {
                                    try {
                                        Thread.sleep(5000L);
                                    } catch (InterruptedException e) {
                                        throw new RuntimeException(e);
                                    }
                                    String targetIp = autoConfig.getRemoteIp();
                                    int port = autoConfig.getRemotePort();
                                    count[0]++;
                                    log.debug("[MASTER_CHECK] Target ==> " + targetIp + ":" + port);

                                    try (Socket socket = new Socket(targetIp, port)) {
                                        log.info("[MASTER_CHECK] TARGET IS RUNNING...");
                                        if (jobsMap.keySet().size() > 0) {
                                            log.info("[MASTER_CHECK] STOP SCHEDULE...");
                                            Set<String> jobIds = jobsMap.keySet();
                                            for (String jobId : jobIds) {
                                                ScheduledFuture<?> scheduledTask = jobsMap.get(jobId);
                                                if (scheduledTask != null) {
                                                    scheduledTask.cancel(true);
                                                    log.info("[MASTER_CHECK] CANCELED SCHEDULE... {}" , jobId);
                                                    jobsMap.remove(jobId);
                                                }
                                            }
                                        }
                                    } catch (ConnectException e) {
                                        if (jobsMap.keySet().size() == 0) {
                                            log.info("[MASTER_CHECK] SCHEDULE REGIST...");
                                            registSchedule();
                                        }
                                        if (count[0] % 10 == 0)
                                            log.info("[MASTER_CHECK] TARGET IS SHUTDOWN DETECTED : " + e.getMessage());
                                    } catch (Exception e) {
                                        log.info("[MASTER_CHECK] TARGET IS SHUTDOWN DETECTED : " + e.getMessage());
                                        if (jobsMap.keySet().size() == 0) {
                                            log.info("[MASTER_CHECK] SCHEDULE REGIST...");
                                            registSchedule();
                                        }
                                    }
                                }
                            }
                        }.start();
                    }
            **/
        registSchedule();


    }

    private void registSchedule() {
        log.info("ScheduleStart Ready...");
        log.info("ifConfig : {}", ifConfig);
        if (ifConfig.getContext() != null) {
            ifConfig.getContext().forEach(ifContext -> {
                if (ifContext.isActFirst()) {
                    ScheduleRunnable runnable = new ScheduleRunnable();
                    runnable.setContext(ifContext);
                    runnable.setAppContext(appContext);
                    runnable.setJmsTemplate(jmsTemplate);
                    runnable.setContext(ifContext);
                    runnable.setConfig(ifConfig);
                    runnable.setAdaptorConfigBean(adaptorConfigBean);
                    runnable.setToJMSData(toJMSData);
                    scheduleActFist("SCH_"+ifContext.getInterfaceId(), runnable, ifContext.getActFirstIntervalSec());
                } else {
                    if (ifContext.getCronExpression() != null) {
                        String[] cronExpressions = ifContext.getCronExpression();
                        for (int i = 0; i < cronExpressions.length; i++) {

                            String cronExpression = cronExpressions[i];

                            ScheduleRunnable runnable = new ScheduleRunnable();
                            runnable.setContext(ifContext);
                            runnable.setAppContext(appContext);
                            runnable.setJmsTemplate(jmsTemplate);
                            runnable.setContext(ifContext);
                            runnable.setConfig(ifConfig);
                            runnable.setAdaptorConfigBean(adaptorConfigBean);
                            runnable.setToJMSData(toJMSData);
                            scheduleATask("SCH_"+ifContext.getInterfaceId()+"_"+(i+1), runnable, cronExpression);
                        }

                    }
                }
            });
        }
    }

    public static void main(String[] args) {
        AtomicBoolean checking = new AtomicBoolean(false);
        checking.set(true);
        log.info("checking : {}", checking.toString());
    }
}