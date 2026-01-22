package mb.fw.atb.parser;

import com.google.common.collect.Maps;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;
import mb.fw.transformation.tool.TypeConversion;
import mb.fw.transformation.util.HexViewer;
import org.apache.commons.lang3.StringUtils;
import org.checkerframework.checker.nullness.qual.Nullable;
import org.springframework.stereotype.Component;

import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;

@Component(value = "SHineSCIParser")
@Slf4j
public class SHineSCIParser implements ATBParser {

    /**
     * Map을 전문 byte[]로 변환
     *
     * @param map
     * @return
     */
    @Override
    public byte[] MapToBytes(Map map) throws Exception {

        /**
         {
         "ambleLen":"0143",
         "memberId":"HEHY001 ",
         "cpCode":"SHDI00200000",
         "jpCode":"0200",
         "wkCode":"295",
         "resCode":"000",
         "keyNum":"0001",
         "ciYn":"C",
         "seq":"00000000000000000000",
         "residentNo":"7701201234567",
         "hash":"5g3fEkxe4INT1vrI0x5JjYlGPBV/mtNdcquT0XCGeOY\u003d"
         }
         */

        String memberId = (String) map.get("memberId");
        String cpCode = (String) map.get("cpCode");
        String jpCode = (String) map.get("jpCode");
        String wkCode = (String) map.get("wkCode");
        String resCode = (String) map.get("resCode");
        String keyNum = (String) map.get("keyNum");
        String ciYn = (String) map.get("ciYn");
        String seq = (String) map.get("seq");
        String residentNo = (String) map.get("residentNo");
        String hash = (String) map.get("hash");

        ByteBuf buf = io.netty.buffer.Unpooled.buffer();

        buf.writeBytes("0000".getBytes());
        buf.writeBytes(TypeConversion.asciiConvert(memberId.getBytes(), "A", 8));
        buf.writeBytes(TypeConversion.asciiConvert(cpCode.getBytes(), "A", 12));
        buf.writeBytes(TypeConversion.asciiConvert(jpCode.getBytes(), "N", 4));
        buf.writeBytes(TypeConversion.asciiConvert(wkCode.getBytes(), "N", 3));
        buf.writeBytes(TypeConversion.asciiConvert(resCode.getBytes(), "N", 3));
        buf.writeBytes(TypeConversion.asciiConvert(keyNum.getBytes(), "N", 4));
        buf.writeBytes(TypeConversion.asciiConvert(ciYn.getBytes(), "A", 1));
        buf.writeBytes(TypeConversion.asciiConvert(seq.getBytes(), "A", 20));
        buf.writeBytes(TypeConversion.asciiConvert(residentNo.getBytes(), "N", 13));
        buf.writeBytes(TypeConversion.asciiConvert(hash.getBytes(), "A", 44));
        int length = buf.readableBytes() - 4;
        buf.setBytes(0, String.format("%04d", length).getBytes());
        byte[] convertBytes = new byte[buf.readableBytes()];
        buf.readBytes(convertBytes);
        log.info("convertBytes : {}", HexViewer.view(convertBytes));

        return convertBytes;
    }

    @Override
    public Map MapToMap(Map map) {
        return null;
    }

    /**
     * 전문bytes를 Map으로 변환
     *
     * @param bytes
     * @return
     */
    @Override
    public Map BytesToMap(byte[] bytes) {

        /**
         {
         "ambleLen": "0439",
         "memberId": "HEHY001 ",
         "cpCode": "SHDI00200000",
         "jpCode": "0210",
         "wkCode": "295",
         "resCode": "000",
         "keyNum": "0001",
         "ciYn": "C",
         "seq": "00000000000000000000",
         "repeInfo": "1234567890123456789012345678901234567890123456789012345678901234",
         "ciVer": "1",
         "ciInfo1": "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678",
         "ciInfo2": "1234567890123456789012345678901234567890123456789012345678901234567890123456789012345678",
         "hash": "12345678901234567890123456789012345678901234"
         }
         */

        ByteBuf buf = io.netty.buffer.Unpooled.wrappedBuffer(bytes);

        byte[] ambleLen = new byte[4];
        byte[] memberId = new byte[8];
        byte[] cpCode = new byte[12];
        byte[] jpCode = new byte[4];
        byte[] wkCode = new byte[3];
        byte[] resCode = new byte[3];
        byte[] keyNum = new byte[4];
        byte[] ciYn = new byte[1];
        byte[] seq = new byte[20];
        byte[] repeInfo = new byte[64];
        byte[] ciVer = new byte[1];
        byte[] ciInfo1 = new byte[88];
        byte[] ciInfo2 = new byte[88];
        byte[] hash = new byte[44];

        buf.readBytes(ambleLen);
        buf.readBytes(memberId);
        buf.readBytes(cpCode);
        buf.readBytes(jpCode);
        buf.readBytes(wkCode);
        buf.readBytes(resCode);
        buf.readBytes(keyNum);
        buf.readBytes(ciYn);
        buf.readBytes(seq);
        buf.readBytes(repeInfo);
        buf.readBytes(ciVer);
        buf.readBytes(ciInfo1);
        buf.readBytes(ciInfo2);
        buf.readBytes(hash);

        LinkedHashMap<@Nullable Object, @Nullable Object> returnMap = Maps.newLinkedHashMap();
        //returnMap.put("ambleLen", new String(ambleLen);
        returnMap.put("memberId", StringUtils.trim(new String(memberId)));
        returnMap.put("cpCode", StringUtils.trim(new String(cpCode)));
        returnMap.put("jpCode", StringUtils.trim(new String(jpCode)));
        returnMap.put("wkCode", StringUtils.trim(new String(wkCode)));
        returnMap.put("resCode", StringUtils.trim(new String(resCode)));
        returnMap.put("keyNum", StringUtils.trim(new String(keyNum)));
        returnMap.put("ciYn", StringUtils.trim(new String(ciYn)));
        returnMap.put("seq", StringUtils.trim(new String(seq)));
        returnMap.put("repeInfo", StringUtils.trim(new String(repeInfo)));
        returnMap.put("ciVer", StringUtils.trim(new String(ciVer)));
        returnMap.put("ciInfo1", StringUtils.trim(new String(ciInfo1)));
        returnMap.put("ciInfo2", StringUtils.trim(new String(ciInfo2)));
        returnMap.put("hash", StringUtils.trim(new String(hash)));

        log.info("convertMap : {}", returnMap);

        return returnMap;
    }
}
