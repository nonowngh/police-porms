package mb.fw.atb.strategy.batch;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.FTIInfo;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.job.file.DirectoryPolling;
import mb.fw.atb.job.file.TempMove;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.service.DBTransactionService;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 제공기관 공개키 내역(GC01) 수신
 */
@Component(value = "FTIGC01RecvStrategy")
@Slf4j
public class FTIGC01RecvStrategy extends ATBStrategy {

    @Autowired(required = false)
    @Qualifier("DBTransactionService")
    DBTransactionService dbTransactionService;

    @Autowired(required = false)
    IFConfig ifConfig;

    @Autowired
    TempMove tempMove;

    Base64.Encoder base64Encoder = Base64.getEncoder();

    public FTIGC01RecvStrategy() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
        System.setProperty("javax.xml.parsers.DocumentBuilderFactory", "com.sun.org.apache.xerces.internal.jaxp.DocumentBuilderFactoryImpl");
    }

    @Override
    public Specifications specifications() {
        return null;
    }

    /**
     * example :
     *
     * @param context
     * @param txid
     * @param msgCreDt
     * @throws Exception
     */
    @Override
    @TimeTrace
    public OnSignalInfo onSignal(IFContext context, String txid, String msgCreDt) throws Exception {

        log.info("FTIGC01RecvStrategy onSignal start");
        FTIInfo ftiInfo = ifConfig.getFtiInfo();

        List<Path> pollList = DirectoryPolling.fileRecvPoll(txid, context);

        if (pollList.size() == 0) {
            log.info("SCHEDULE STOPPED ");
            return null;
        }

        List<Path> tempList = tempMove.tempMove(pollList, context, txid);
        //temp로 이동한 파일을 DB에 INSERT

        for (Path path : tempList) {
            try {
                try (FileChannel fileChannel = FileChannel.open(path)) {
                    ByteBuffer buffer = ByteBuffer.allocate(1000);

                    // 파일의 총 바이트 크기 가져오기
                    long fileSize = Files.size(path);

                    // 1000바이트씩 읽어오기
                    int bytesRead;
                    long currentPosition = 0;

                    List<Map<String, Object>> orgPublicKeyList = Lists.newArrayList();


                    while (currentPosition < fileSize) {
                        // 현재 위치부터 1000바이트 읽기
                        bytesRead = fileChannel.read(buffer);
                        if (bytesRead == -1) {
                            break; // 파일 끝에 도달한 경우 종료
                        }

                        // ByteBuffer에 읽어온 데이터가 있으면 처리
                        if (bytesRead > 0) {
                            buffer.flip(); // 버퍼를 읽기 모드로 변경

                            // 버퍼의 데이터를 문자열로 변환하여 출력
                            byte[] bWorkGubun = new byte[4];
                            byte[] bDataGubun = new byte[2];
                            byte[] bSeq = new byte[8];

                            buffer.get(bWorkGubun);
                            buffer.get(bDataGubun);
                            buffer.get(bSeq);

                            String workGubun = new String(bWorkGubun, Charset.forName("EUC-KR"));
                            String dataGubun = new String(bDataGubun, Charset.forName("EUC-KR"));
                            String seq = new String(bSeq, Charset.forName("EUC-KR"));

                            switch (workGubun + dataGubun) {
                                case "GC0111": {
                                    log.info("HEADER Read");
                                    break;
                                }
                                case "GC0122": {
                                    log.info("DATA Read");
                                    //데이터 인서트
                                    Map orgPublicKeyMap = Maps.newHashMap();

                                    //filler 2
                                    byte[] bFiller = new byte[2];
                                    //제공기관코드 3
                                    byte[] bOrgCode = new byte[3];
                                    //filler2 2
                                    byte[] bFiller2 = new byte[2];
                                    //제공기관명 50
                                    byte[] bOrgName = new byte[50];
                                    //공개키 생성일자 8
                                    byte[] bPublicKeyCreDt = new byte[8];
                                    //제공기관공캐키 392
                                    byte[] bOrgPublicKey = new byte[392];
                                    //filler3 526
                                    byte[] bFiller3 = new byte[526];
                                    //위의 바이트의 합산 값은? 1000

                                    buffer.get(bFiller);
                                    buffer.get(bOrgCode);
                                    buffer.get(bFiller2);
                                    buffer.get(bOrgName);
                                    buffer.get(bPublicKeyCreDt);
                                    buffer.get(bOrgPublicKey);
                                    buffer.get(bFiller3);

                                    String filler = new String(bFiller, Charset.forName("EUC-KR"));
                                    String orgCode = new String(bOrgCode, Charset.forName("EUC-KR"));
                                    String filler2 = new String(bFiller2, Charset.forName("EUC-KR"));
                                    String orgName = new String(bOrgName, Charset.forName("EUC-KR"));
                                    String publicKeyCreDt = new String(bPublicKeyCreDt, Charset.forName("EUC-KR"));
                                    String orgPublicKey = new String(bOrgPublicKey, Charset.forName("EUC-KR"));
                                    String filler3 = new String(bFiller3, Charset.forName("EUC-KR"));


                                    orgPublicKeyMap.put("FILLER", filler);
                                    orgPublicKeyMap.put("ORG_CODE", orgCode);
                                    orgPublicKeyMap.put("FILLER2", filler2);
                                    orgPublicKeyMap.put("ORG_NAME", orgName);
                                    orgPublicKeyMap.put("GEN_DATE", publicKeyCreDt);
                                    orgPublicKeyMap.put("PUBLIC_KEY", orgPublicKey);
                                    orgPublicKeyMap.put("FILLER3", filler3);
                                    orgPublicKeyMap.put("LAST_KEY_UPDATE_DT", DateUtils.today17());

                                    orgPublicKeyList.add(orgPublicKeyMap);

                                    break;
                                }
                                case "GC0133": {
                                    log.info("TRAILER Read");
                                    //제공기관 공개키 정보 업데이트 진행
                                    dbTransactionService.updateList(context.getInterfaceId() + ".UPDATE_KEY", context, orgPublicKeyList, Maps.newHashMap());
                                    break;
                                }
                                default:
                                    log.info("workGubun : {}", workGubun);
                                    log.info("dataGubun : {}", dataGubun);
                                    log.info("seq : {}", seq);
                                    break;
                            }


                            buffer.clear(); // 버퍼를 초기화하여 다음 데이터를 읽을 준비
                        }

                        currentPosition += bytesRead;
                    }

                } // FileChannel 자동으로 close() 됨

            } catch (IOException e) {
                log.error("{} 처리중 실패", path.getFileName(), e);
            }
        }

        return OnSignalInfo.builder().count(1).processEnd(true).build();
    }

    @Override
    public OnSignalInfo onSignalRetry(IFContext context, String txid, String eventDt, Map<String, String> propMap) throws Exception {
        return null;
    }

    @Override
    public Object onMessageData(IFContext context, String txid, String eventDt, Object obj, Map<String, String> propMap) throws Exception {
        return null;
    }

    @Override
    public void onMessageResult(IFContext context, String txid, String eventDt, String resultCode, String resultMessage, String dataStr, Map<String, String> propMap) throws Exception {
    }

}
