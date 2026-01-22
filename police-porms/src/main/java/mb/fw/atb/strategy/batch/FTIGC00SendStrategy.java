package mb.fw.atb.strategy.batch;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mb.mci.common.util.HexUtil;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;
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
import mb.fw.net.common.et.TransferType;
import mb.fw.net.common.util.ByteBufUtils;
import mb.fw.transformation.tool.BufferedReadOut;
import mb.fw.transformation.tool.TypeConversion;
import mb.fw.transformation.util.HexViewer;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.io.FileUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.crypto.KeyGenerator;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.util.*;

/**
 * 요구기관 공개키 내역(GC00) 전송
 */
@Component(value = "FTIGC00SendStrategy")
@Slf4j
public class FTIGC00SendStrategy extends ATBStrategy {

    @Autowired(required = false)
    @Qualifier("DBTransactionService")
    DBTransactionService dbTransactionService;

    @Autowired(required = false)
    IFConfig ifConfig;

    Base64.Encoder base64Encoder = Base64.getEncoder();

    public FTIGC00SendStrategy() {
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

        log.info("FTIGC00SendStrategy onSignal start");
        FTIInfo ftiInfo = ifConfig.getFtiInfo();

        HashMap<@Nullable Object, @Nullable Object> parameterMap = Maps.newHashMap();
        parameterMap.put("ORG_CODE", ftiInfo.getRequesterCode());

        //생성일이 오늘 일경우 파일을 생성하고 발송한다.
        Map requestOrgInfo = (Map) dbTransactionService.selectOne(context.getInterfaceId() + ".SELECT_KEY", context, parameterMap);

        if(requestOrgInfo == null){
            log.info("공개키 금일 생성기록 없음 발송진행 안함");
            return null;
        }

        String publicKey = (String) requestOrgInfo.get("PUBLIC_KEY");
        String today = DateUtils.today8();
        String yyyy = DateUtils.todayyyyy();
        String todaymmdd = DateUtils.todaymmdd();
        String batchSystemCode = "0000000000";

        ByteBuf headerRecordBuff = Unpooled.buffer(1000);
        headerRecordBuff.writeBytes(TypeConversion.asciiConvert("GC00".getBytes(), "A", 4));
        headerRecordBuff.writeBytes(TypeConversion.asciiConvert("11".getBytes(), "N", 2));
        headerRecordBuff.writeBytes(TypeConversion.asciiConvert("".getBytes(), "N", 8));
        headerRecordBuff.writeBytes(TypeConversion.asciiConvert(batchSystemCode.getBytes(), "N", 9));
        headerRecordBuff.writeBytes(TypeConversion.asciiConvert("1".getBytes(), "N", 8));
        headerRecordBuff.writeBytes(TypeConversion.asciiConvert("999".getBytes(), "N", 3));
        headerRecordBuff.writeBytes(TypeConversion.asciiConvert(today.getBytes(), "N", 8));
        headerRecordBuff.writeBytes(TypeConversion.asciiConvert("".getBytes(), "A", 958));

        ByteBuf bodyRecordBuff = Unpooled.buffer(1000);

        bodyRecordBuff.writeBytes(TypeConversion.asciiConvert("GC00".getBytes(), "A", 4));
        bodyRecordBuff.writeBytes(TypeConversion.asciiConvert("22".getBytes(), "N", 2));
        bodyRecordBuff.writeBytes(TypeConversion.asciiConvert("1".getBytes(), "N", 8));
        bodyRecordBuff.writeBytes(TypeConversion.asciiConvert("".getBytes(), "A", 2));
        bodyRecordBuff.writeBytes(TypeConversion.asciiConvert("999".getBytes(), "N", 3));
        bodyRecordBuff.writeBytes(TypeConversion.asciiConvert("99999".getBytes(), "N", 5));
        bodyRecordBuff.writeBytes(TypeConversion.asciiConvert("관부코드명".getBytes(Charset.forName("EUC-KR")), "A", 50));
        bodyRecordBuff.writeBytes(TypeConversion.asciiConvert(today.getBytes(), "N", 8));
        bodyRecordBuff.writeBytes(TypeConversion.asciiConvert(publicKey.getBytes(), "A", 392));
        bodyRecordBuff.writeBytes(TypeConversion.asciiConvert("".getBytes(), "A", 526));

        ByteBuf trailerRecordBuff = Unpooled.buffer(1000);

        trailerRecordBuff.writeBytes(TypeConversion.asciiConvert("GC00".getBytes(), "A", 4));
        trailerRecordBuff.writeBytes(TypeConversion.asciiConvert("33".getBytes(), "N", 2));
        trailerRecordBuff.writeBytes(TypeConversion.asciiConvert("99999999".getBytes(), "N", 8));
        trailerRecordBuff.writeBytes(TypeConversion.asciiConvert(batchSystemCode.getBytes(), "N", 9));
        trailerRecordBuff.writeBytes(TypeConversion.asciiConvert("1".getBytes(), "N", 8));
        trailerRecordBuff.writeBytes(TypeConversion.asciiConvert("".getBytes(), "A", 969));

        boolean isHexMode = false;

        ByteBufUtils.printPretty(TransferType.OUTBOUND, headerRecordBuff, "GC00", "HEADER_RECORD", isHexMode, Charset.forName("EUC-KR"));
        ByteBufUtils.printPretty(TransferType.OUTBOUND, bodyRecordBuff, "GC00", "BODY_RECORD", isHexMode, Charset.forName("EUC-KR"));
        ByteBufUtils.printPretty(TransferType.OUTBOUND, trailerRecordBuff, "GC00", "TRAILER_RECORD", isHexMode, Charset.forName("EUC-KR"));

        Path yyyyDir = Paths.get(context.getFileTempPath()+"/"+yyyy);

        if(Files.notExists(yyyyDir)){
            Files.createDirectories(yyyyDir);
        }

        Path path = Paths.get(context.getFileTempPath()+"/GC00"+batchSystemCode+todaymmdd);

        if(Files.notExists(path)){
            Files.createFile(path);
        }else{
            Files.delete(path);
            Files.createFile(path);
        }

        String fileName = path.getFileName().toString();

        Files.write(path, ByteBufUtil.getBytes(headerRecordBuff, 0, 1000));
        Files.write(path, ByteBufUtil.getBytes(bodyRecordBuff, 0, 1000));
        Files.write(path, ByteBufUtil.getBytes(trailerRecordBuff, 0, 1000));

        ByteBufUtil.getBytes(bodyRecordBuff, 0, 1000);
        ByteBufUtil.getBytes(trailerRecordBuff, 0, 1000);

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
