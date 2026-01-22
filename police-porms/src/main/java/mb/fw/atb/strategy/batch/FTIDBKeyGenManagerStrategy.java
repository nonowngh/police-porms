package mb.fw.atb.strategy.batch;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mb.mci.common.util.HexUtil;
import lombok.extern.slf4j.Slf4j;
import mb.fw.atb.aop.TimeTrace;
import mb.fw.atb.config.IFConfig;
import mb.fw.atb.config.Specifications;
import mb.fw.atb.config.sub.FTIInfo;
import mb.fw.atb.config.sub.IFContext;
import mb.fw.atb.model.OnSignalInfo;
import mb.fw.atb.service.DBTransactionService;
import mb.fw.atb.strategy.ATBStrategy;
import mb.fw.atb.util.DateUtils;
import mb.fw.atb.util.crypto.FTICryptoUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.io.FileUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import java.io.File;
import java.security.*;
import java.util.*;

/**
 * 요구기관(현재프로젝트 기관)의 개인키,공개키(최초,필요시 처리 로직포함)를 생성하고 테이블에 입력
 * 제공기관별 OTK를 생성하여 테이블에 입력
 */
@Component(value = "FTIDBKeyGenManagerStrategy")
@Slf4j
public class FTIDBKeyGenManagerStrategy extends ATBStrategy {

    @Autowired(required = false)
    @Qualifier("DBTransactionService")
    DBTransactionService dbTransactionService;

    @Autowired(required = false)
    IFConfig ifConfig;

    Base64.Encoder base64Encoder = Base64.getEncoder();

    public FTIDBKeyGenManagerStrategy() {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());
    }


    @Override
    public Specifications specifications() {
        return null;
    }

    @Override
    @TimeTrace
    public OnSignalInfo onSignal(IFContext context, String txid, String msgCreDt) throws Exception {

        log.info("FTIDBKeyGenManagerStrategy onSignal");
        FTIInfo ftiInfo = ifConfig.getFtiInfo();

        List<Map<String, Object>> selectKeyList = dbTransactionService.select(context.getInterfaceId() + ".SELECT_KEY", context, Maps.newHashMap());
        if (CollectionUtils.isEmpty(selectKeyList)) {
            log.info("키 테이블에 데이터가 존재하지 않음 기관코드 생성");
            List<String> providerCodeList = ftiInfo.getProviderCodeList();
            List insertList = Lists.newArrayList();
            String initDate = DateUtils.today17();

            for (String providerCode : providerCodeList) {
                LinkedHashMap<@Nullable String, @Nullable Object> insertParameterMap = Maps.newLinkedHashMap();
                insertParameterMap.put("ORG_CODE", providerCode);
                insertParameterMap.put("LAST_UPDATE_DT", initDate);
                insertList.add(insertParameterMap);
            }

            LinkedHashMap<@Nullable String, @Nullable Object> insertParameterMap = Maps.newLinkedHashMap();
            insertParameterMap.put("ORG_CODE", ftiInfo.getRequesterCode());
            insertParameterMap.put("LAST_UPDATE_DT", DateUtils.today17());
            insertList.add(insertParameterMap);

            dbTransactionService.insertList(context.getInterfaceId() + ".INSERT_INIT_KEY", context, insertList, Maps.newHashMap());
        }

        ClassPathResource generateDateResource = new ClassPathResource("generate_date.txt");

        byte[] publicKeyBytes = null;
        byte[] privateKeyBytes = null;
        String publicKeyBase64Str = null;
        String privateKeyBase64Str = null;
        boolean isNewPublicKey = false;

        if (generateDateResource.exists()) {
            String generateDate = FileUtils.readFileToString(generateDateResource.getFile());
            String today = DateUtils.today8();

            if (today.equals(generateDate)) {
                log.info("신규 공개키 생성 ==> {}", today);
                //생성일이면 개인키와 공개키를 새로 생성한다.
                KeyPair keyPair = FTICryptoUtil.generateKeyPair();

                //공개키 생성 & 개인키 생성
                PublicKey publicKey = keyPair.getPublic();
                PrivateKey privateKey = keyPair.getPrivate();

                //키를 저장
                publicKeyBytes = publicKey.getEncoded();
                privateKeyBytes = privateKey.getEncoded();
                log.info("publicKeyBytes ==> " + HexUtil.hexToPrint(publicKeyBytes));
                log.info("privateKeyBytes ==> " + HexUtil.hexToPrint(privateKeyBytes));

                //base64로 변환
                publicKeyBase64Str = base64Encoder.encodeToString(publicKeyBytes);
                privateKeyBase64Str = base64Encoder.encodeToString(privateKeyBytes);

                log.info("publicKeyBase64Str ==> " + publicKeyBase64Str);
                log.info("privateKeyBase64Str ==> " + privateKeyBase64Str);
                isNewPublicKey = true;
            }
        }

        if (isNewPublicKey) {
            log.info("신규 공개키 생성, 키 생성 프로세스 시작");
        } else {
            log.info("기존 공개키 사용, 키 생성 프로세스 종료");
            return null;
        }

        List updateKeyList = Lists.newArrayList();
        List insertOTKList = Lists.newArrayList();

        /*
            GEN_DATE
            ORG_CODE
            PRIVATE_KEY
            PUBLIC_KEY
            OTK
            LAST_KEY_UPDATE_DT
            LAST_OTK_UPDATE_DT
         */
        LinkedHashMap<@Nullable Object, @Nullable Object> insertParameterMap = Maps.newLinkedHashMap();
        insertParameterMap.put("GEN_DATE", DateUtils.today8());
        insertParameterMap.put("ORG_CODE", ftiInfo.getRequesterCode());
        insertParameterMap.put("PRIVATE_KEY", privateKeyBase64Str);
        insertParameterMap.put("PUBLIC_KEY", publicKeyBase64Str);
        insertParameterMap.put("LAST_UPDATE_DT", DateUtils.today17());

        updateKeyList.add(insertParameterMap);

        List<String> providerCodeList = ftiInfo.getProviderCodeList();

        KeyGenerator generator = KeyGenerator.getInstance("AES");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");

        String prvGenDate = DateUtils.today8();
        String lastUpdateDt = DateUtils.today17();
        for (String providerCode : providerCodeList) {

            //기관별 난수 128bit 키 생성
            generator.init(128, random);
            Key secureKey = generator.generateKey();
            byte[] randomKey = secureKey.getEncoded();
            log.info("Random Key 128bit ==> " + HexUtil.hexToPrint(randomKey));
            String randomKeyBase64Str = base64Encoder.encodeToString(randomKey);
            log.info("Random Key length ==> " + randomKey.length);
            log.info("Random Key Base64 ==> " + randomKeyBase64Str);

            LinkedHashMap<@Nullable Object, @Nullable Object> providerMap = Maps.newLinkedHashMap();
            providerMap.put("GEN_DATE", prvGenDate);
            providerMap.put("ORG_CODE", providerCode);
            providerMap.put("GEN_KEY", null);
            providerMap.put("OTK", randomKeyBase64Str);
            providerMap.put("LAST_UPDATE_DT", lastUpdateDt);

            insertOTKList.add(providerMap);
        }

        dbTransactionService.updateList(context.getInterfaceId() + ".UPDATE_KEY", context,updateKeyList,  Maps.newHashMap());
        dbTransactionService.insertList(context.getInterfaceId() + ".INSERT_OTK", context,insertOTKList,  Maps.newHashMap());

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
