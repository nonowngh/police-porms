package mb.fw.policeporms.common.config;

import java.util.Optional;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import com.indigo.indigomq.pool.PooledConnectionFactory;

import lombok.extern.slf4j.Slf4j;
import mb.fw.policeporms.common.logging.InterfaceLogging;

@Slf4j
@EnableAsync
@Configuration
public class InterfaceLoggingConfig {

	@Bean
	InterfaceLogging logging(@Qualifier("interfaceJmsTemplate") Optional<JmsTemplate> interfaceJmsTemplate) {
		return new InterfaceLogging(interfaceJmsTemplate);
	}

	@Bean(name = "interfaceJmsTemplate")
	@ConditionalOnProperty(name = "interface.logging.enabled", havingValue = "true", matchIfMissing = true)
	JmsTemplate jmsTemplate(@Autowired(required = false) PooledConnectionFactory jmsConnectionFactory) {
		if (jmsConnectionFactory == null) {
			log.error("❌ JMS 구동 실패: ConnectionFactory 빈을 찾을 수 없습니다. (jmsConnectionFactory 설정 확인 필요)");
			throw new IllegalStateException("JMS Logging is enabled, but no ConnectionFactory is available.");
		}
		log.info("✅ interfaceJmsTemplate 빈 등록 완료");
		return new JmsTemplate(jmsConnectionFactory);
	}

	@Bean(name = "loggingExecutor")
	@ConditionalOnProperty(name = "interface.logging.enabled", havingValue = "true", matchIfMissing = true)
	Executor taskExecutor() {
		ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
		executor.setCorePoolSize(5);
		executor.setMaxPoolSize(10);
		executor.setQueueCapacity(500);
		executor.setThreadNamePrefix("interface-logging-");
		executor.initialize();
		return executor;
	}
}
