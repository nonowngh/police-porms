package mb.fw.policeporms.common.logging;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.springframework.jms.core.JmsTemplate;
import org.springframework.scheduling.annotation.Async;

import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.util.ATBUtil;
import mb.fw.policeporms.common.constant.InterfaceStatus;

@Slf4j
public class InterfaceLogging {
	private final JmsTemplate jmsTemplate;

	public InterfaceLogging(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	@Async("loggingExecutor")
	public void asyncStartLogging(String interfaceId, String transactionId, String sendSystemdCode,
			String receiveSystemCode, int totalCount) {
		String nowDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		log.debug("jms start logging[{}]", transactionId);
		try {
			ATBUtil.startLogging(jmsTemplate, interfaceId, transactionId, null, totalCount, sendSystemdCode,
					receiveSystemCode, nowDateTime, null);
		} catch (Exception e) {
			log.error("interface start logging error!", e.getMessage());
		}
	}

	@Async("loggingExecutor")
	public void asyncEndLogging(String interfaceId, String transactionId, int errorCount, InterfaceStatus statusCode,
			String statusMessage) {
		String nowDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
		log.debug("jms end logging[{}]", transactionId);
		try {
			ATBUtil.endLogging(jmsTemplate, interfaceId, transactionId, "",
					statusCode == InterfaceStatus.SUCCESS ? 0 : errorCount,
					statusCode == InterfaceStatus.SUCCESS ? "S" : "F", statusMessage, nowDateTime);
		} catch (Exception e) {
			log.error("interface start logging error!", e.getMessage());
		}
	}
}
