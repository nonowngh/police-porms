package mb.fw.atb.appender;

import com.google.common.collect.Queues;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingDeque;


@Component
@Slf4j
/**
 * This class is responsible for sending logs to the ESB
 */
public class ESBMQAppender extends AppenderSkeleton {

    @Getter
    static LinkedBlockingDeque queue = Queues.newLinkedBlockingDeque(1000);

    @Autowired
    Executor taskExecutor;

    @Autowired
    JmsTemplate jmsTemplate;

    @PostConstruct
    public void init() {
        System.out.println("LogDetector.init");
        taskExecutor.execute(() -> {
            while (true) {
                try {
                    String log = (String) queue.take();
                    taskExecutor.execute(() -> {
                        jmsTemplate.convertAndSend("ATB.LOGGING", log);
                    });
                } catch (InterruptedException e) {
                }
            }
        });
    }

    @Override
    protected void append(LoggingEvent event) {
        queue.offer(this.layout.format(event));
    }

    @Override
    public void close() {
    }

    @Override
    public boolean requiresLayout() {
        return false;
    }

}
