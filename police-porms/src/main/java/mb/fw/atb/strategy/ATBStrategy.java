package mb.fw.atb.strategy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.indigo.esb.nifs.IFTPClient;
import com.mb.indigo2.springsupport.AdaptorConfigBean;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.util.DateUtils;
import mb.fw.atb.util.ATBUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component(value = "ATBStrategy")
public abstract class ATBStrategy {

    @Autowired
    protected IFConfig config;

    @Autowired
    protected AdaptorConfigBean adaptorConfigBean;

    @Autowired(required = false)
    protected IFTPClient client;

    protected ObjectMapper objectMapper = new ObjectMapper();
    protected XmlMapper xmlMapper = new XmlMapper();
    @Autowired(required = false)
    protected JmsTemplate jmsTemplate;

    /**
     * 인터페이스 관리 페이지 연동을 위한 스펙정보
     *
     * @return Specifications
     */
    public abstract Specifications specifications();

    /**
     * 스케줄링에 의한 Sender 이벤트
     *
     * @param context context
     * @param txid    TransactionId
     * @param eventDt 이벤트 발생시간
     * @return OnSignalInfo
     * @throws Exception
     */
    public abstract OnSignalInfo onSignal(IFContext context, String txid, String eventDt) throws Exception;


    /**
     * 스케줄링에 의한 Sender 재처리 이벤트
     *
     * @param context  context
     * @param txid     TransactionId
     * @param msgCreDt 이벤트 발생시간
     * @return OnSignalInfo
     * @throws Exception
     */
    public abstract OnSignalInfo onSignalRetry(IFContext context, String txid, String msgCreDt, Map<String, String> propMap) throws Exception;

    /**
     * JMS 수신 Receiver 이벤트
     *
     * @param context context
     * @param txid    TransactionId
     * @param eventDt 이벤트 발생시간
     * @param data    데이터
     * @return 리턴할 응답 데이터
     * @throws Exception
     */
    public abstract Object onMessageData(IFContext context, String txid, String eventDt, Object data, Map<String, String> propMap) throws Exception;

    /**
     * JMS Sender의 결과처리
     *
     * @param context       context
     * @param txid          TransactionId
     * @param eventDt       이벤트 발생시간
     * @param resultCode    결과 코드
     * @param resultMessage 결과 메시지
     * @param dataStr       데이터
     * @param propMap       프로퍼티
     * @throws Exception
     */
    public abstract void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String resultMessage, String dataStr, Map<String, String> propMap) throws Exception;


    public void startLogging(IFContext context, String txid, String msgCreDt, String sendDt, long count) throws Exception {
        ATBUtil.startLogging(jmsTemplate, context.getInterfaceId(), txid, context.getReceiverIds(), count, context.getSendSystemCode(), context.getReceiveSystemCode(), msgCreDt, sendDt);
    }

    public void endLogging(IFContext context, String txid, long errCount, String receiverId, String resultCd, String resultMessage) throws Exception {
        String msgRcvDt = DateUtils.today17();
        ATBUtil.endLogging(jmsTemplate, context.getInterfaceId(), txid, receiverId, errCount, resultCd, resultMessage, msgRcvDt);
    }
}
