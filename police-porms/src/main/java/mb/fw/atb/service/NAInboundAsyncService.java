package mb.fw.atb.service;

import com.mb.indigo2.springsupport.AdaptorConfigBean;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.util.MDCLogging;
import mb.fw.net.common.NetworkAdaptorCallback;
import mb.fw.net.common.NetworkExtractor;
import mb.fw.net.common.et.TransferType;
import mb.fw.net.common.message.CallRequest;
import mb.fw.net.common.message.CallResponse;
import mb.fw.net.common.message.ChannelMessage;
import mb.fw.net.common.util.ByteBufUtils;
import mb.fw.net.common.util.DateUtils;
import mb.fw.net.product.et.CommonResultCode;
import mb.fw.net.product.logging.ProductLoggingService;
import mb.fw.net.service.AsyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;

/**
 * NetworkAdaptor가 INBOUND DATA를 JMS 또는 합쳐진 PROVIDER로 전달하는 서비스
 */
@Slf4j
@Service
public class NAInboundAsyncService extends AsyncService {

    @Autowired(required = false)
    NAInboundToHttpService naInboundToHttpService;

    @Override
    @Async("threadPoolTaskExecutor")
    public void jmsCall(ChannelMessage channelMessage, String refTransactionId, String refInterfaceId, String interfaceId, String transactionId, String recvMsg, ProductLoggingService service, AdaptorConfigBean bean, String srcOrg, String trgOrg, int callRetry, String callBasePath, NetworkAdaptorCallback networkAdaptorCallback, int callRetrySleepms, NetworkExtractor extractor, boolean hexMode, String groupId, String pattern) throws InterruptedException, UnsupportedEncodingException {
        this.httpCall(channelMessage, refTransactionId, refInterfaceId, interfaceId, transactionId, recvMsg, service, bean, srcOrg, trgOrg, callRetry, callBasePath, callRetrySleepms, extractor, hexMode, groupId, pattern);
    }

    @Override
    @Async("threadPoolTaskExecutor")
    @TimeTrace
    public void httpCall(ChannelMessage channelMessage, String refTransactionId, String refInterfaceId, String interfaceId, String transactionId, String recvMsg, ProductLoggingService service, AdaptorConfigBean bean, String srcOrg, String trgOrg, int callRetry, String callUrl, int callRetrySleepms, NetworkExtractor extractor, boolean hexMode, String groupId, String pattern) throws InterruptedException, UnsupportedEncodingException {
        try {
            MDCLogging.create(transactionId, interfaceId, bean.getAdaptorName());
            service.logging(bean.getAdaptorName(), interfaceId, transactionId,
                    pattern, srcOrg, trgOrg, null, "RECEIVE",
                    CommonResultCode.LNK_0001.getCode(), CommonResultCode.LNK_0001.getMessage(),
                    recvMsg, refInterfaceId, refTransactionId, groupId, DateUtils.today17());

            CallRequest request = new CallRequest();
            request.setLnk_interface_id(interfaceId);
            request.setLnk_transaction_id(transactionId);
            request.setLnk_send_dt(DateUtils.today17());
            request.setLnk_adaptor(bean.getAdaptorName());
            request.setLnk_pattern(pattern);
            request.setLnk_src_org(srcOrg);
            request.setLnk_trg_org(trgOrg);
            request.setData(recvMsg);

            CallResponse response = null;
            for (int j = 0; j <= callRetry; j++) {
                try {
                    if (j > 0) {
                        log.info("재시도 => [ " + interfaceId + " , " + transactionId + "] retry " + j);
                    }
                    response = naInboundToHttpService.request(request);
                } catch (Exception e) {
                    String body = e.getMessage();
                    log.info("수신 데이터 전달 실패 => [ " + interfaceId + " , " + transactionId + "]");
                    log.info("수신 데이터 실패 내용 => [ " + body + " ]");
                    log.info("오류 내용 : " + e.getMessage());
                    log.error("HTTP CALL 전달 실패 ", e);
                    service.logging(bean.getAdaptorName(), interfaceId, transactionId,
                            pattern, srcOrg, trgOrg, null, "RECEIVE_RESULT",
                            CommonResultCode.LNK_E102.getCode(),
                            CommonResultCode.LNK_E102.getMessage() + "throw," + body, null,
                            refInterfaceId, refTransactionId, groupId, DateUtils.today17());
                    Thread.sleep(callRetrySleepms);
                    continue;
                }

                if (response.getLnk_result_cd().equals(CommonResultCode.LNK_0000.getCode())) {
                    log.info("수신 데이터 전달 완료 => [ " + interfaceId + " , " + transactionId + "]");
                    service.logging(bean.getAdaptorName(), interfaceId, transactionId,
                            pattern, srcOrg, trgOrg, null, "RECEIVE_RESULT",
                            CommonResultCode.LNK_0000.getCode(), CommonResultCode.LNK_0000.getMessage(),
                            null, refInterfaceId, refTransactionId, groupId, DateUtils.today17());

                    // 값이 존재하면 왔던 응답으로 발송 , 2021-03-16 (수신 서버 동기처리) --->
                    if (response != null) {
                        String resData = response.getData();
                        if (!mb.fw.net.common.util.StringUtils.isEmpty(resData)) {
                            Channel resChannel = channelMessage.getChannel();

                            ByteBuf resBuff = Unpooled.wrappedBuffer(resData.getBytes("euc-kr"));
                            String resRefTransactionId = extractor.extractMessageId(resBuff);
                            String resRefInterfaceId = extractor.extractWorkCode(resBuff);

                            service.logging(bean.getAdaptorName(), response.getLnk_interface_id(),
                                    response.getLnk_transaction_id(), pattern,
                                    srcOrg, trgOrg, null, "SEND", CommonResultCode.LNK_0001.getCode(),
                                    CommonResultCode.LNK_0001.getMessage(), resData, resRefInterfaceId,
                                    resRefTransactionId, groupId, DateUtils.today17());

                            if (resChannel.isOpen()) {

                                ByteBuf logbuf = Unpooled.copiedBuffer(resBuff);
                                resChannel.writeAndFlush(resBuff);
                                ByteBufUtils.printPretty(TransferType.OUTBOUND, logbuf,
                                        pattern, "SEND_MESSAGE S", hexMode,
                                        Charset.forName("euc-kr"));

                                service.logging(bean.getAdaptorName(), response.getLnk_interface_id(),
                                        response.getLnk_transaction_id(), pattern,
                                        srcOrg, trgOrg, null, "SEND_RESULT",
                                        CommonResultCode.LNK_0000.getCode(), CommonResultCode.LNK_0000.getMessage(), null,
                                        resRefInterfaceId, resRefTransactionId, groupId, DateUtils.today17());

                            } else {
                                service.logging(bean.getAdaptorName(), response.getLnk_interface_id(),
                                        response.getLnk_transaction_id(), pattern,
                                        trgOrg, trgOrg, null, "SEND_RESULT",
                                        CommonResultCode.LNK_E002.getCode(),
                                        CommonResultCode.LNK_E002.getMessage(), null, resRefInterfaceId,
                                        resRefTransactionId, groupId, DateUtils.today17());
                            }
                        }
                    }

                } else {
                    log.info("수신 데이터 전달 완료후 실패응답 수신 => [ " + response.getLnk_interface_id() + " , " + response.getLnk_transaction_id() + "]");
                    service.logging(bean.getAdaptorName(), response.getLnk_interface_id(), response.getLnk_transaction_id(),
                            pattern, srcOrg, trgOrg, null, "RECEIVE_RESULT",
                            response.getLnk_result_cd(), response.getLnk_result_msg(), null, refInterfaceId,
                            refTransactionId, groupId, DateUtils.today17());
                }
                break;
            }
        } finally {
            MDCLogging.release();
        }

    }


}
