package mb.fw.atb.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.aop.TimeTraceAspect;
import mb.fw.atb.enums.THeader;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.util.ATBUtil;
import mb.fw.atb.util.DateUtils;
import mb.fw.atb.util.MDCLogging;
import mb.fw.atb.util.OkHttpUtil;
import mb.fw.net.common.NetworkAdaptorCallback;
import mb.fw.net.common.message.CallRequest;
import mb.fw.net.common.message.CallResponse;
import mb.fw.net.product.et.CommonResultCode;
import mb.fw.transformation.engine.AsciiEngine;
import mb.fw.transformation.engine.MapEngine;
import mb.fw.transformation.form.MessageForm;
import mb.fw.transformation.form.MessageFormBox;
import mb.fw.transformation.tool.TypeConversion;
import okhttp3.*;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * NetworkAdaptor가 INBOUND 발생시 전문 변환  OR BYPASS 를 하고 HTTP로 호출하는 서비스
 * 어뎁터가 분리된 형태 또는 합쳐진 형태를 대응함
 */
@Slf4j
public class NAInboundToHttpService implements NetworkAdaptorCallback {

    @Setter
    IFConfig ifConfig;

    @Setter
    HashMap<String, MessageFormBox> myMessageFormBox;

    @Setter
    JmsTemplate jmsTemplate;

    @Setter
    ApplicationContext appContext;

    AsciiEngine asciiEngine = new AsciiEngine();
    MapEngine mapEngine = new MapEngine();

    ObjectMapper objectMapper = new ObjectMapper();
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    @Setter
    NAOutboundSendAsyncService naOutboundSendAsyncService;

    @Override
    @TimeTrace
    public CallResponse request(CallRequest callRequest) {
        log.info("NetworkAdaptorInbound.Request : {}", callRequest);
        MDCLogging.create(callRequest.getLnk_transaction_id(), callRequest.getLnk_interface_id(), callRequest.getLnk_adaptor());
        IFContext context = ifConfig.findContext(callRequest.getLnk_interface_id());

        String inDataAscii = callRequest.getData();
        String transformDt = DateUtils.today17();
        String transformType = context.getInMapperType();
        String interfaceId = context.getInterfaceId();
        try {
            //시작 로그
            String sendDt = DateUtils.today17();
            Map convertMap = null;
            try {
                startLogging(context, callRequest.getLnk_transaction_id(), sendDt, sendDt, 1);
                switch (transformType) {
                    case "MAPPER": {
                        //context check
                        if (context.getMapperGroupId() == null || context.getInBoundMapperMessageId() == null) {
                            throw new Exception("Mapper GroupId or MessageId is null");
                        }

                        //myMessageFormBox check
                        if (myMessageFormBox == null) {
                            throw new Exception("myMessageFormBox is null");
                        }

                        MessageForm messageForm = null;

                        try {
                            messageForm = myMessageFormBox.get(context.getMapperGroupId()).get(context.getInBoundMapperMessageId()).copidMessageForm();
                        } catch (Exception e) {
                            log.error("myMessageFormBox is null", e);
                        }

                        //messageForm check
                        if (messageForm == null) {
                            throw new Exception("messageForm not found");
                        }

                        convertMap = asciiEngine.conversionToMap(inDataAscii.getBytes(), messageForm.getInContext(), messageForm.getOutContext());

                        break;
                    }
                    case "BYPASS":
                    default: {
                        LinkedHashMap<String, Object> reqMap = Maps.newLinkedHashMap();
                        reqMap.put("data", inDataAscii);
                        convertMap = reqMap;

                    }
                }
            } catch (Exception e) {
                log.error("TRANSFORM ERROR", e);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = "###ERROR\n" + sw.toString();
                String resultMessage = null;

                CallResponse callResponseErr = new CallResponse();
                callResponseErr.setLnk_interface_id(callRequest.getLnk_interface_id());
                callResponseErr.setLnk_transaction_id(callRequest.getLnk_transaction_id());
                callResponseErr.setData(ifConfig.getId());
                callResponseErr.setLnk_alias(ifConfig.getId());
                callResponseErr.setLnk_result_cd(CommonResultCode.LNK_E200.getCode());
                callResponseErr.setLnk_result_msg(CommonResultCode.LNK_E200.getMessage());
                callResponseErr.setLnk_result_dt(DateUtils.today17());

                try {
                    resultMessage = "### REQUEST\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callRequest) + "\n\n" + "### RESPONSE\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callResponseErr) + "\n\n" + exceptionAsString;
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }

                try {
                    endLogging(context, callRequest.getLnk_transaction_id(), 1, context.getReceiverIds()[0], "F", resultMessage);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                return callResponseErr;
            }

            OkHttpClient client = null;
            Map inDataJson = null;

            try {
                client = OkHttpUtil.build(true);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            //convertMap To Json String
            String convertMapJson = null;
            try {
                convertMapJson = objectMapper.writeValueAsString(convertMap);
            } catch (JsonProcessingException e) {
                throw new RuntimeException(e);
            }

            String responseJson = null;
            RequestBody requestBody = RequestBody.create(convertMapJson, JSON);
            Request httpRequest = new Request.Builder().addHeader("Content-Type", "application/json").addHeader("Accept", "application/json").addHeader(THeader.INTERFACE_ID.key(), callRequest.getLnk_interface_id()).addHeader(THeader.TRANSACTION_ID.key(), callRequest.getLnk_transaction_id()).addHeader(THeader.SENDER_ID.key(), callRequest.getLnk_adaptor()).addHeader(THeader.SENDER_MSG_SEND_DT.key(), callRequest.getLnk_send_dt()).url(context.getInboundCallUrl()).post(requestBody).build();

            try (Response response = client.newCall(httpRequest).execute()) {
                ResponseBody responseBody = response.body();
                responseJson = responseBody.string();
                log.info("responseJson : {}", responseJson);
                inDataJson = objectMapper.readValue(responseJson, Map.class);

                //데이터 줄게 없으면 data에 null을 주면 네트워크어댑터에는 데이터를 발송 안함
                if (inDataJson.get("data") == null || inDataJson.get("data").equals("")) {
                    CallResponse callResponse = new CallResponse();
                    String msgCreDt = DateUtils.today17();
                    callResponse.setLnk_interface_id(callRequest.getLnk_interface_id());
                    callResponse.setLnk_transaction_id(callRequest.getLnk_transaction_id());
                    callResponse.setLnk_alias(ifConfig.getId());
                    callResponse.setLnk_result_cd(CommonResultCode.LNK_0000.getCode());
                    callResponse.setLnk_result_msg(CommonResultCode.LNK_0000.getMessage());
                    callResponse.setLnk_result_dt(DateUtils.today17());
                    callResponse.setData(null);

                    String timeTraceStr = "### TIME_TRACE\n" + TimeTraceAspect.generateTimeTraceAndRemove(callRequest.getLnk_transaction_id());
                    String resultMessage = "### REQUEST\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callRequest) + "\n\n" + "### CALL REQUEST\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(convertMap) + "\n\n" + "### CALL RESPONSE \n" + responseJson + "\n\n" + "### RESPONSE\n\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callResponse) + "\n\n" + timeTraceStr;
                    endLogging(context, callRequest.getLnk_transaction_id(), 0, context.getReceiverIds()[0], "S", resultMessage);
                    return callResponse;
                }

            } catch (IOException e) {
                log.error("INBOUND CALL ERROR", e);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = "###ERROR\n" + sw.toString();
                String resultMessage = null;

                CallResponse callResponseErr = new CallResponse();
                callResponseErr.setLnk_interface_id(callRequest.getLnk_interface_id());
                callResponseErr.setLnk_transaction_id(callRequest.getLnk_transaction_id());
                callResponseErr.setData("");
                callResponseErr.setLnk_alias(ifConfig.getId());
                callResponseErr.setLnk_result_cd(CommonResultCode.LNK_E104.getCode());
                callResponseErr.setLnk_result_msg(CommonResultCode.LNK_E104.getMessage());
                callResponseErr.setLnk_result_dt(DateUtils.today17());

                try {
                    resultMessage = "### REQUEST\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callRequest) + "\n\n" + "### RESPONSE\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callResponseErr) + "\n\n" + exceptionAsString;
                } catch (JsonProcessingException ex) {
                    throw new RuntimeException(ex);
                }
                try {
                    endLogging(context, callRequest.getLnk_transaction_id(), 1, context.getReceiverIds()[0], "F", resultMessage);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                return callResponseErr;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }

            CallResponse callResponse = new CallResponse();

            try {
                switch (transformType) {
                    case "MAPPER": {
                        //context check
                        if (context.getMapperGroupId() == null || context.getOutBoundMapperMessageId() == null) {
                            throw new Exception("Mapper GroupId or MessageId is null");
                        }
                        //myMessageFormBox check
                        if (myMessageFormBox == null) {
                            throw new Exception("myMessageFormBox is null");
                        }

                        MessageForm messageForm = null;
                        try {
                            messageForm = myMessageFormBox.get(context.getMapperGroupId()).get(context.getOutBoundMapperMessageId()).copidMessageForm();
                        } catch (Exception e) {
                            log.error("myMessageFormBox is null", e);
                        }

                        //messageForm check
                        if (messageForm == null) {
                            throw new Exception("messageForm not found");
                        }

                        byte[] bytes = mapEngine.conversionToAsciiBytes(inDataJson, messageForm.getInContext(), messageForm.getOutContext());

                        if (context.isLengthFieldInject()) {
                            byte[] lengthByte = null;
                            int totalLength = new String(bytes).getBytes(Charset.forName("EUC-KR")).length;
                            int lengthFieldValue = totalLength + context.getLengthFieldAddition();
                            log.info("totalLength : {}, lengthFieldValue : {}", totalLength, lengthFieldValue);
                            byte[] lengthFiledBytes = TypeConversion.asciiConvert((lengthFieldValue + "").getBytes(), "N", context.getLengthFieldLength());

                            ByteBuf byteBuf = Unpooled.wrappedBuffer(bytes);
                            byteBuf.setBytes(context.getLengthFieldPosition(), lengthFiledBytes);

                            byte[] sendBytes = new byte[byteBuf.readableBytes()];
                            byteBuf.getBytes(0, sendBytes);
                            callResponse.setData(new String(sendBytes));
                        } else {
                            callResponse.setData(new String(bytes));
                        }
                        break;
                    }
                    case "BYPASS":
                    default: {
                        String srcData = (String) inDataJson.get(THeader.DATA.key());
                        callResponse.setData(srcData);
                    }
                }
            } catch (Exception e) {
                log.error("TRANSFORM ERROR", e);
                StringWriter sw = new StringWriter();
                e.printStackTrace(new PrintWriter(sw));
                String exceptionAsString = "###ERROR\n" + sw.toString();
                String resultMessage = null;
                String msgRcvDt = DateUtils.today17();

                CallResponse callResponseErr = new CallResponse();
                callResponseErr.setLnk_interface_id(callRequest.getLnk_interface_id());
                callResponseErr.setLnk_transaction_id(callRequest.getLnk_transaction_id());
                callResponseErr.setData(ifConfig.getId());
                callResponseErr.setLnk_alias(ifConfig.getId());
                callResponseErr.setLnk_result_cd(CommonResultCode.LNK_E200.getCode());
                callResponseErr.setLnk_result_msg(CommonResultCode.LNK_E200.getMessage());
                callResponseErr.setLnk_result_dt(DateUtils.today17());
                try {
                    String timeTraceStr = "### TIME_TRACE\n" + TimeTraceAspect.generateTimeTraceAndRemove(callRequest.getLnk_transaction_id());
                    resultMessage = "### REQUEST\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callRequest) + "\n\n" + "### RESPONSE\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callResponse) + "\n\n" + exceptionAsString + "\n\n" + timeTraceStr;
                    ATBUtil.endLogging(jmsTemplate, context.getInterfaceId(), callRequest.getLnk_transaction_id(), context.getReceiverIds()[0], 1, "F", resultMessage, msgRcvDt);
                    return callResponseErr;
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }


            try {
                String msgCreDt = DateUtils.today17();
                callResponse.setLnk_interface_id(callRequest.getLnk_interface_id());
                callResponse.setLnk_transaction_id(callRequest.getLnk_transaction_id());
                callResponse.setLnk_alias(ifConfig.getId());
                callResponse.setLnk_result_cd(CommonResultCode.LNK_0000.getCode());
                callResponse.setLnk_result_msg(CommonResultCode.LNK_0000.getMessage());
                callResponse.setLnk_result_dt(DateUtils.today17());

                //동기면
                if (context.getProcessMode().equals("ASYNC")) {
                    String msg = callResponse.getData();
                    callResponse.setData(null);
                    naOutboundSendAsyncService.asyncProcess(callRequest.getLnk_transaction_id(), callRequest.getLnk_transaction_id(), msg, ifConfig, callRequest, context, transformType, interfaceId, convertMap, inDataJson, responseJson, msgCreDt);
                } else {
                    String timeTraceStr = "### TIME_TRACE\n" + TimeTraceAspect.generateTimeTraceAndRemove(callRequest.getLnk_transaction_id());
                    String resultMessage = "### REQUEST\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callRequest) + "\n\n" + "### CALL REQUEST\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(convertMap) + "\n\n" + "### CALL RESPONSE \n" + responseJson + "\n\n" + "### RESPONSE\n\n" + objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(callResponse) + "\n\n" + timeTraceStr;
                    endLogging(context, callRequest.getLnk_transaction_id(), 0, context.getReceiverIds()[0], "S", resultMessage);
                }

                return callResponse;

            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        } finally {
            MDCLogging.release();
        }


    }

    public void startLogging(IFContext context, String txid, String msgCreDt, String sendDt, long count) throws Exception {
        ATBUtil.startLogging(jmsTemplate, context.getInterfaceId(), txid, context.getReceiverIds(), count, context.getSendSystemCode(), context.getReceiveSystemCode(), msgCreDt, sendDt);
    }

    public void endLogging(IFContext context, String txid, long errCount, String receiverId, String resultCd, String resultMessage) throws Exception {
        String msgRcvDt = DateUtils.today17();
        ATBUtil.endLogging(jmsTemplate, context.getInterfaceId(), txid, receiverId, errCount, resultCd, resultMessage, msgRcvDt);
    }

}
