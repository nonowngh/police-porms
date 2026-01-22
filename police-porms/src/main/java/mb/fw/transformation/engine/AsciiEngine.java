package mb.fw.transformation.engine;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import mb.fw.transformation.exception.MCIException;
import mb.fw.transformation.form.Record;
import mb.fw.transformation.form.RecordContext;
import mb.fw.transformation.tool.BufferedReadOut;
import mb.fw.transformation.tool.ExpressionEvaluator;
import mb.fw.transformation.tool.TypeConversion;
import mb.fw.transformation.type.AsciiType;
import mb.fw.transformation.util.JsonUtil;
import org.apache.velocity.runtime.parser.ParseException;
import org.dom4j.DocumentException;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jdom2.JDOMException;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * List type Support
 *
 * @author clupine
 */
public class AsciiEngine {

    private static Logger logger = LoggerFactory.getLogger(AsciiEngine.class);

    ByteOrder srcOrder = ByteOrder.BIG_ENDIAN;
    ByteOrder trgOrder = ByteOrder.BIG_ENDIAN;

    public void setSrcOrder(ByteOrder srcOrder) {
        this.srcOrder = srcOrder;
    }

    public void setTrgOrder(ByteOrder trgOrder) {
        this.trgOrder = trgOrder;
    }

    static XStream xStream = new XStream(new DomDriver());

    static VMXmlEngine vmXmlSupportEngine = new VMXmlEngine();

    /**
     * Ascii to Xml Conversion
     *
     * @param msg
     * @param srcContext
     * @param trgContext
     * @return
     * @throws MCIException
     * @throws UnsupportedEncodingException
     */
    public String conversionToXml(byte[] msg, RecordContext srcContext, RecordContext trgContext) throws MCIException,
            UnsupportedEncodingException {
        return xStream.toXML(conversionToMap(msg, srcContext, trgContext));
    }

    /**
     * Ascii to Java Map Conversion
     *
     * @param msg
     * @param srcContext
     * @param trgContext
     * @return
     * @throws MCIException
     * @throws UnsupportedEncodingException
     */
    public Map conversionToMap(byte[] msg, RecordContext srcContext, RecordContext trgContext) throws MCIException,
            UnsupportedEncodingException {
        srcContext.setOrder(srcOrder.toString());
        trgContext.setOrder(trgOrder.toString());

        String utf8toEuckr = new String(msg, Charset.forName("utf-8"));
        msg = utf8toEuckr.getBytes(Charset.forName("euc-kr"));


        // byteorder 맞춰서 Buffer 정의
        ChannelBuffer buff = ChannelBuffers.wrappedBuffer(msg);

        Map retMap = new LinkedHashMap();

        // 버퍼를 IN Record에 맞게 추출해서 data에 기록
        if (logger.isDebugEnabled())
            logger.debug("============================================================================================== [Before]");

        readOut(srcContext, buff, null);

        if (logger.isDebugEnabled()) {
            logger.debug("[Before] ============================================================================================== \n");
            logger.debug("[After ] ============================================================================================== ");
        }
        // 이 다음 어떻게할지 개발해야함
        int trgCnt = trgContext.size();
        for (int i = 1; i <= trgCnt; i++) {
            Record trgRecord = trgContext.get(i);
            if (!(trgRecord.getFunction() == null || trgRecord.getFunction() == "")) {
                // ExpressionEvaluator.evaluate(srcContext, retBuff, trgRecord,
                // trgRecord.getFunction(), srcOrder);
            } else {
                if (trgRecord.getType().equals("LIST") || trgRecord.getType().equals("MAP")) {
                    i = convertListorMap(srcContext, trgContext, retMap, i, trgRecord);
                } else {

                    int rank = Integer.parseInt(trgRecord.getRank());
                    Record srcRecord = srcContext.get(rank);
                    Object convertObj = TypeConversion.convertAsciiToMap(srcRecord.getData(), srcRecord, trgRecord, srcOrder, trgOrder);
                    retMap.put(trgRecord.getName(), convertObj);
                    if (logger.isDebugEnabled())
                        logger.debug(trgRecord.toStringSimple(convertObj));
                }
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("============================================================================================== [After ]");
        return retMap;
    }

    /**
     * @param msg
     * @param srcContext
     * @param trgContext
     * @return
     * @throws MCIException
     * @throws UnsupportedEncodingException
     */
    public String conversionToJson(byte[] msg, RecordContext srcContext, RecordContext trgContext) throws MCIException,
            UnsupportedEncodingException {
        return JsonUtil.jsonPretty(new JSONObject(conversionToMap(msg, srcContext, trgContext)).toJSONString());
    }

    public byte[] conversionToAscii(byte[] msg, RecordContext srcContext, RecordContext trgContext) throws MCIException,
            UnsupportedEncodingException {

        srcContext.setOrder(srcOrder.toString());
        trgContext.setOrder(trgOrder.toString());

        // byteorder 맞춰서 Buffer 정의
        ChannelBuffer buff = ChannelBuffers.wrappedBuffer(msg);

        Map retMap = new LinkedHashMap();

        // 버퍼를 IN Record에 맞게 추출해서 data에 기록
        if (logger.isDebugEnabled())
            logger.debug("============================================================================================== [Before]");

        readOut(srcContext, buff, null);

        if (logger.isDebugEnabled()) {
            logger.debug("[Before] ============================================================================================== \n");
            logger.debug("[After ] ============================================================================================== ");
        }

        ChannelBuffer retBuff = ChannelBuffers.dynamicBuffer();
        int trgCnt = trgContext.size();
        for (int i = 1; i <= trgCnt; i++) {

            Record trgRecord = trgContext.get(i);
            if (!(trgRecord.getFunction() == null || trgRecord.getFunction() == "")) {
                ExpressionEvaluator.evaluate(srcContext, retBuff, trgRecord, trgRecord.getFunction(), srcOrder);
            } else {
                if (trgRecord.getType().equals("LIST")) {
                    i = convertListorMapAscii(srcContext, trgContext, retBuff, i, trgRecord);
                } else {

                    int rank = Integer.parseInt(trgRecord.getRank());
                    Record srcRecord = srcContext.get(rank);
                    // 가변 데이터
                    if (trgRecord.getLength() == -1) {

                        int countNo = trgRecord.getCountNo();
                        Record varLenTrgRecord = trgContext.get(countNo);

                        byte[] convertBytes = TypeConversion.convertMapToVarAscii(srcRecord, trgRecord, varLenTrgRecord);
                        trgRecord.setData(convertBytes);
                        retBuff.writeBytes(convertBytes);

                    } else {
                        if (!trgRecord.getType().equals("MAP")) {
                            byte[] convertBytes = TypeConversion.asciiConvert(srcRecord.getData(), trgRecord.getType(), trgRecord.getLength());
                            trgRecord.setData(convertBytes);
                            retBuff.writeBytes(convertBytes);
                        }
                    }

                    if (logger.isDebugEnabled())
                        logger.debug(trgRecord.toStringSimple());
                }
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("============================================================================================== [After ]");

        byte[] retBytes = new byte[retBuff.readableBytes()];
        retBuff.readBytes(retBytes);

        return retBytes;
    }

    /**
     * @param inContext
     * @param outContext
     * @return
     * @throws MCIException
     * @throws ParseException
     * @throws IOException
     * @throws JDOMException
     * @throws DocumentException
     */
    public String conversionToVMXml(byte[] msg, RecordContext inContext, RecordContext outContext) throws MCIException, ParseException, IOException, JDOMException, DocumentException {
        Map outMap = conversionToMap(msg, inContext, outContext);
        return vmXmlSupportEngine.convertVMXml(outMap, outContext);
    }


    public void readOut(RecordContext srcContext, ChannelBuffer buff, List<Record> readOutList)
            throws UnsupportedEncodingException {

        int srcCnt = srcContext.size();

        for (int i = 1; i <= srcCnt; i++) {

            Record srcRecord = srcContext.get(i);
            String readData = null;

            if (srcRecord.getType().equals("LIST")) {
                i = readAasciiType(srcContext, buff, i, srcRecord, readOutList);
            } else {

                if (srcRecord.getLength() == -1) {
                    int countNo = srcRecord.getCountNo();
                    Record varLenSrcRecord = srcContext.get(countNo);
                    byte[] varLengthData = varLenSrcRecord.getData();
                    String varLenType = varLenSrcRecord.getType();

                    int varLen = 0;
                    switch (AsciiType.valueOf(varLenType)) {
                        case N:
                            varLen = Integer.valueOf(new String(varLengthData));
                            break;
                        default:
                            break;
                    }

                    readData = BufferedReadOut.stringReading(buff, varLen);
                    srcRecord.setData(readData.getBytes());
                } else {
                    if (!srcRecord.getType().equals("MAP")) {
                        readData = BufferedReadOut.stringReading(buff, srcRecord.getLength());
                        srcRecord.setData(readData.getBytes());
                    }
                }

                if (readOutList != null) {
                    readOutList.add(srcRecord);
                }

                if (logger.isDebugEnabled()) {
                    logger.debug(srcRecord.toStringSimple());
                }
            }
        }
    }

    private int readAasciiType(RecordContext srcContext, ChannelBuffer retBuff, int nowindex, Record srcRecord,
                               List<Record> readOutList) throws UnsupportedEncodingException {
        byte[] readData;
        int childCnt = srcRecord.getChildCount();
        int cntIdx = srcRecord.getCountNo();
        // logger.debug("cnt idx : "+inRecord);
        long validCnt = 0;
        if (cntIdx == 0) {
            validCnt = 1;
            srcRecord.setLength(1);
        } else {
            Record countIdxRecord = srcContext.get(cntIdx);
            byte[] data = countIdxRecord.getData();
            ChannelBuffer buff = ChannelBuffers.copiedBuffer(data);

            int length = countIdxRecord.getLength();
            byte[] lengthData = new byte[length];

            switch (AsciiType.valueOf(countIdxRecord.getType())) {

                case N:
                case A:
                    buff.readBytes(lengthData);
                    break;

                default:
                    break;
            }
            String countData = new String(lengthData);
            validCnt = Integer.valueOf(countData.trim());
        }


        // list는 건너띠고
        for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {

            // list 다음부터 계산
            for (int j2 = (nowindex + 1); j2 <= (nowindex + childCnt); j2++) {

                Record childRecord = srcContext.get(j2);
                readData = BufferedReadOut.stringReading(retBuff, childRecord.getLength()).getBytes();

                childRecord.putData(vaildIdx, readData);

                if (readOutList != null) {
                    try {
                        Record recordClone = (Record) childRecord.clone();
                        recordClone.setData(readData);
                        readOutList.add(recordClone);
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }

                if (logger.isDebugEnabled())
                    logger.debug(childRecord.toStringSimple(readData));
            }
        }

        nowindex += childCnt;
        return nowindex;
    }

    private int convertListorMapAscii(RecordContext srcContext, RecordContext trgContext, ChannelBuffer retBuff, int nowIndex, Record trgRecord) throws UnsupportedEncodingException {

        int childCnt = trgRecord.getChildCount();
        int cntIdx = trgRecord.getCountNo();

        Record countIdxRecord = trgContext.get(cntIdx);
        byte[] data = countIdxRecord.getData();
        ChannelBuffer buff = ChannelBuffers.copiedBuffer(data);

        long validCnt = 0;
        int validLength = countIdxRecord.getLength();
        byte[] validData = new byte[validLength];

        switch (AsciiType.valueOf(countIdxRecord.getType())) {

            case N:
            case A:
                buff.readBytes(validData);
                validCnt = Integer.valueOf(new String(validData));
                break;

            default:
                break;
        }

        for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {

            for (int j2 = (nowIndex + 1); j2 <= (nowIndex + childCnt); j2++) {

                Record childRecord = trgContext.get(j2);
                int rank = Integer.parseInt(childRecord.getRank());
                Record srcRecord = srcContext.get(rank);
                byte[] childData = srcRecord.getData(vaildIdx);

                if (childData == null) {
                    childData = srcRecord.getData();

                }
                byte[] convertBytes = null;
                if (childRecord.getLength() == -1) {
                    int countNo = childRecord.getCountNo();
                    Record varTagRecord = trgContext.get(countNo);
                    convertBytes = TypeConversion.convertMapToVarAscii4List(srcRecord, childRecord, varTagRecord, vaildIdx);
                    retBuff.writeBytes(convertBytes);
                } else {
                    convertBytes = TypeConversion.asciiConvert(childData, childRecord.getType(), childRecord.getLength());
                    trgRecord.putData(vaildIdx, convertBytes);
                    retBuff.writeBytes(convertBytes);
                }

                if (logger.isDebugEnabled())
                    logger.debug(childRecord.toStringSimple(convertBytes));

            }
        }

        nowIndex += childCnt;
        return nowIndex;
    }

    private int convertListorMap(RecordContext srcContext, RecordContext trgContext, Map map, int nowIndex, Record trgRecord)
            throws UnsupportedEncodingException {

        if (trgRecord.getType().equals("MAP")) {
            int childCnt = trgRecord.getChildCount();
            long validCnt = 1;

            Map childMap = new LinkedHashMap();
            // map 다음부터 계산
            for (int j2 = (nowIndex + 1); j2 <= (nowIndex + childCnt); j2++) {

                Record childRecord = trgContext.get(j2);
                int rank = Integer.parseInt(childRecord.getRank());
                Record srcRecord = srcContext.get(rank);
                byte[] childData = srcRecord.getData();
                int childLength = childRecord.getLength();

                if (childData == null) {
                    childData = srcRecord.getData();
                }

                Object convertObj = TypeConversion.convertAsciiToMap(childData, srcRecord, childRecord, srcOrder, trgOrder);
                childMap.put(childRecord.getName(), convertObj);

                if (logger.isDebugEnabled())
                    logger.debug(childRecord.toStringSimple(convertObj));
            }
            map.put(trgRecord.getName(), childMap);
            nowIndex += childCnt;
        } else {
            int childCnt = trgRecord.getChildCount();
            int cntIdx = trgRecord.getCountNo();
            long validCnt = 0;

            if (cntIdx == 0) {
                Record countIdxRecord = srcContext.get(nowIndex);
                validCnt = countIdxRecord.getLength();
            } else {
                Record countIdxRecord = srcContext.get(cntIdx);
                byte[] data = countIdxRecord.getData();
                ChannelBuffer buff = ChannelBuffers.copiedBuffer(data);
                int countLength = countIdxRecord.getLength();
                byte[] countvalueBytes = new byte[countLength];

                switch (AsciiType.valueOf(countIdxRecord.getType())) {

                    case N:
                    case A:
                        buff.readBytes(countvalueBytes);
                        String readCount = new String(countvalueBytes);
                        validCnt = Integer.valueOf(readCount.trim());
                        break;
                    default:
                        break;
                }
            }

            List childList = new ArrayList();
            Map childMap;
            // list는 건너띠고
            for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {
                childMap = new LinkedHashMap();
                // list 다음부터 계산
                for (int j2 = (nowIndex + 1); j2 <= (nowIndex + childCnt); j2++) {

                    Record childRecord = trgContext.get(j2);
                    int rank = Integer.parseInt(childRecord.getRank());
                    Record srcRecord = srcContext.get(rank);
                    byte[] childData = srcRecord.getData(vaildIdx);
                    int childLength = childRecord.getLength();

                    if (childData == null) {
                        childData = srcRecord.getData();
                    }

                    Object convertObj = TypeConversion.convertAsciiToMap(childData, srcRecord, childRecord, srcOrder, trgOrder);
                    childMap.put(childRecord.getName(), convertObj);

                    if (logger.isDebugEnabled())
                        logger.debug(childRecord.toStringSimple(convertObj));

                }
                childList.add(childMap);
            }

            map.put(trgRecord.getName(), childList);
            nowIndex += childCnt;
        }


        return nowIndex;
    }

}
