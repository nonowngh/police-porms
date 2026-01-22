package mb.fw.atb.configuration;

import org.springframework.boot.actuate.jms.JmsHealthIndicator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;

@Configuration
public class HealthIndicatorConfig {

    @Bean
    public JmsHealthIndicator jmsHealthIndicator(JmsTemplate jmsTemplate) {
        return new JmsHealthIndicator(jmsTemplate.getConnectionFactory());
    }
}
