package mb.fw.policeporms.common.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.ATBUtil;
import mb.fw.policeporms.common.constant.InterfaceStatus;

@Slf4j
public class InterfaceLogging {
	private final Optional<JmsTemplate> jmsTemplate;

	public InterfaceLogging(Optional<JmsTemplate> jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	@Async("loggingExecutor")
	public void asyncStartLogging(String interfaceId, String transactionId, String sendSystemdCode,
			String receiveSystemCode, int totalCount) {
		jmsTemplate.ifPresent(jms -> {
			String nowDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
			log.debug("jms start logging[{}]", transactionId);
			try {
				ATBUtil.startLogging(jms, interfaceId, transactionId, null, totalCount, sendSystemdCode,
						receiveSystemCode, nowDateTime, null);
			} catch (Exception e) {
				log.error("interface start logging error!", e.getMessage());
			}
		});
	}

	@Async("loggingExecutor")
	public void asyncEndLogging(String interfaceId, String transactionId, int errorCount, InterfaceStatus statusCode,
			String statusMessage) {
		jmsTemplate.ifPresent(jms -> {
			String nowDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
			log.debug("jms end logging[{}]", transactionId);
			try {
				ATBUtil.endLogging(jms, interfaceId, transactionId, "",
						statusCode == InterfaceStatus.SUCCESS ? 0 : errorCount,
						statusCode == InterfaceStatus.SUCCESS ? "S" : "F", statusMessage, nowDateTime);
			} catch (Exception e) {
				log.error("interface start logging error!", e.getMessage());
			}
		});
	}
}
