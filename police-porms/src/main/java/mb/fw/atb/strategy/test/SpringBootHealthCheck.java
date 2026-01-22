package mb.fw.atb.strategy.test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.indigo.indigomq.pool.PooledConnectionFactory;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.model.file.FileInfo;
import mb.fw.atb.model.file.FileMessage;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.boot.actuate.health.PingHealthIndicator;
import org.springframework.boot.actuate.jms.JmsHealthIndicator;
import org.springframework.boot.actuate.system.DiskSpaceHealthIndicator;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component(value = "SpringBootHealthCheck")
@Slf4j
public class SpringBootHealthCheck extends ATBStrategy {


    @Autowired
    private DiskSpaceHealthIndicator diskSpaceHealthIndicator;

    @Autowired
    private JmsHealthIndicator jmsHealthIndicator;

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    ProcessorMetrics processorMetrics;


    @Override
    public Specifications specifications() {
        return null;
    }

    /**
     * example :
     *
     * @param context
     * @param txid
     * @param msgCreDt
     * @throws Exception
     */
    @Override
    @TimeTrace
    public OnSignalInfo onSignal(IFContext context, String txid, String msgCreDt) throws Exception {
        Health health = diskSpaceHealthIndicator.health();
        log.info("health : {}", health);

        Health health1 = jmsHealthIndicator.health();
        log.info("health1 : {}", health1);
        health1.getDetails().forEach((k, v) -> {
            log.info("k : {}, v : {}", k, v);
        });

        String[] beanNames = applicationContext.getBeanDefinitionNames();
        int beanCount = applicationContext.getBeanDefinitionCount();
        log.info("beanCount : {}", beanCount);
        for (String beanName : beanNames) {
            log.info("beanName : {}", beanName);
        }

        log.info("processorMetrics : {} ",processorMetrics.toString());

        return OnSignalInfo.builder().count(0).build();
    }

    @Override
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String eventDt, Map<String, String> propMap) throws Exception {
        return null;
    }


    @Override
    public Object onMessageData(IFContext context, String txid, String eventDt, Object obj
            , Map<String, String> propMap) throws Exception {
        return null;
    }

    @Override
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String
            resultMessage, String jsonStr, Map<String, String> propMap) throws Exception {
    }


}
