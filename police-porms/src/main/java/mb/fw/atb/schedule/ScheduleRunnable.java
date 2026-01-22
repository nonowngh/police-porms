package mb.fw.atb.schedule;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mb.indigo2.springsupport.AdaptorConfigBean;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTraceAspect;
import mb.fw.atb.error.ATBException;
import mb.fw.atb.error.ErrorCode;
import mb.fw.atb.job.com.ToJMSData;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.DateUtils;
import mb.fw.atb.util.ATBUtil;
import mb.fw.atb.util.MDCLogging;
import mb.fw.atb.util.TransactionIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import io.opentelemetry.api.trace.Span;

@Slf4j
public class ScheduleRunnable implements Runnable {

    @Setter
    @Getter
    private IFContext context;

    @Setter
    @Getter
    private ApplicationContext appContext;

    @Setter
    @Getter
    JmsTemplate jmsTemplate;

    @Setter
    @Getter
    AdaptorConfigBean adaptorConfigBean;

    @Setter
    @Getter
    IFConfig config;

    ObjectMapper mapper = new ObjectMapper();

    ToJMSData toJMSData;

    public void setToJMSData(ToJMSData toJMSData) {
        this.toJMSData = toJMSData;
    }

    @Override
    public void run() {

        boolean firstSleep = false;
        long count = 0;

        do {
            if (firstSleep) {
                try {
                    Thread.sleep(500L);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }

//            while(HARunnable.checking.get()) {
//                log.info("HA Master Checking... Sleep 3sec");
//                try {
//                    Thread.sleep(3000L);
//                } catch (InterruptedException e) {
//                    throw new RuntimeException(e);
//                }
//            }

            count = scheduleStart();
            firstSleep = true;

        } while (count > 0);
    }

    private long scheduleStart() {
        String msgCreDt = DateUtils.today17();
        String txid = TransactionIdGenerator.generate(context.getInterfaceId(), "T", msgCreDt);

        Span currentSpan = Span.current();
        currentSpan.setAttribute("transactionId", txid);
        currentSpan.setAttribute("interfaceId", context.getInterfaceId());
        currentSpan.setAttribute("adaptorName", adaptorConfigBean.getAdaptorName());

        MDCLogging.create(txid, context.getInterfaceId(), adaptorConfigBean.getAdaptorName());

        log.info("SCHEDULE STARTED ", txid);
        log.info("JMSTEMPLATE: {}", jmsTemplate);
        log.debug("{}", context);
        long count = 0;
        OnSignalInfo signalInfo = null;
        try {
            ATBStrategy strategy = null;
            String strategyName = context.getStrategy();
            if (StringUtils.isEmpty(strategyName)) {
                //strategy = appContext.getBean("StandardFTFStrategy", ATBStrategy.class);
                log.info("UNREGISTERED STRATEGY: {},{}", context.getInterfaceId(), strategyName);
                return 0;
            } else {
                strategy = appContext.getBean(strategyName, ATBStrategy.class);
            }
            signalInfo = strategy.onSignal(context, txid, msgCreDt);
        } catch (Exception e) {
            ErrorCode errorCode = ErrorCode.SCHEDULE_BATCH;
            ATBException atbException = new ATBException("SCHEDULE BATCH FAILURE", errorCode, e);
            log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
            throw atbException;
        }

        if (signalInfo == null || signalInfo.getCount() <= 0) {
            return 0;
        } else {
            count = signalInfo.getCount();
        }
        log.debug("###### onSignalInfo {} :", signalInfo);

        if (!signalInfo.isProcessEnd()) {
            log.info("####### DataSend ######");

            String sendData = null;
            Object sendObject = signalInfo.getSendObject();

            if (sendObject instanceof String) {
                sendData = (String) sendObject;
            } else {
                try {
                    sendData = mapper.writeValueAsString(sendObject);
                } catch (JsonProcessingException e) {
                    ErrorCode errorCode = ErrorCode.JSON_PARSE;
                    ATBException atbException = new ATBException("JSON PARSE FAILURE", errorCode, e.getCause());
                    log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
                    throw atbException;
                }
            }

            String timeTraceStr = TimeTraceAspect.generateTimeTraceAndRemove(txid);

            toJMSData.eachSend(jmsTemplate, config, context, txid, msgCreDt, count, adaptorConfigBean.getAdaptorName(), sendData, signalInfo.getSendObject().getClass().getCanonicalName(), timeTraceStr);

            if (config.isSendLogging()) {
                try {
                    String sendDt = DateUtils.today17();
                    ATBUtil.startLogging(jmsTemplate, context.getInterfaceId(), txid, context.getReceiverIds(), count, context.getSendSystemCode(), context.getReceiveSystemCode(), msgCreDt, sendDt);
                } catch (Exception e) {
                    ErrorCode errorCode = ErrorCode.JMS_SEND;
                    ATBException atbException = new ATBException("JMS SEND FAILURE", errorCode, e.getCause());
                    log.error((errorCode.getStatus() + "," + errorCode.getCode() + "," + errorCode.getMessage()), atbException.getCause());
                    throw atbException;
                }
            }

        } else {
            log.info("####### ProcessEnd ######");
        }

        MDCLogging.release();

        if (context.isOnSignalLoop()) {
            return count;
        } else {
            return 0;
        }


    }


}