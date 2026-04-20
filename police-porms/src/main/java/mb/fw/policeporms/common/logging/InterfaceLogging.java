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
	
	// 새로 추가: 실제 DB 적재가 끝난 뒤에 호출하더라도 '원래 시작 시간'을 유지할 수 있도록 startTime 파라미터 추가
	@Async("loggingExecutor")
	public void asyncStartLogging(String interfaceId, String transactionId, String sendSystemdCode,
			String receiveSystemCode, int totalCount, String startTime) {
		log.debug("jms start logging with fixed time [{}]", transactionId);
		try {
			// 파라미터로 받은 startTime을 ATBUtil에 전달
			ATBUtil.startLogging(jmsTemplate, interfaceId, transactionId, null, totalCount, sendSystemdCode,
					receiveSystemCode, startTime, null);
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
