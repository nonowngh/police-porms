package mb.fw.transformation.engine;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;
import mb.fw.transformation.exception.MCIException;
import mb.fw.transformation.form.Record;
import mb.fw.transformation.form.RecordContext;
import mb.fw.transformation.tool.BufferedReadOut;
import mb.fw.transformation.tool.ExpressionEvaluator;
import mb.fw.transformation.tool.TypeConversion;
import mb.fw.transformation.type.CType;
import mb.fw.transformation.type.JSONType;
import mb.fw.transformation.util.JsonUtil;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * List type Support
 *
 * @author clupine
 */
public class CTypeEngine {

    private static Logger logger = LoggerFactory.getLogger(CTypeEngine.class);

    ByteOrder srcOrder = ByteOrder.BIG_ENDIAN;
    ByteOrder trgOrder = ByteOrder.BIG_ENDIAN;

    public void setSrcOrder(ByteOrder srcOrder) {
        this.srcOrder = srcOrder;
    }

    public void setTrgOrder(ByteOrder trgOrder) {
        this.trgOrder = trgOrder;
    }

    /**
     * Ctype to Xml Conversion
     *
     * @param msg
     * @param srcContext
     * @param trgContext
     * @return
     * @throws MCIException
     * @throws UnsupportedEncodingException
     */
    public String conversionToXml(byte[] msg, RecordContext srcContext, RecordContext trgContext) throws MCIException, UnsupportedEncodingException {
        XStream xStream = new XStream(new DomDriver());
        xStream.alias("tranduce", Map.class);
        return xStream.toXML(conversionToMap(msg, srcContext, trgContext));
    }

    /**
     * C-TYPE to Java Map Conversion
     *
     * @param msg
     * @param srcType
     * @param srcContext
     * @param trgType
     * @param trgContext
     * @return
     * @throws MCIException
     * @throws UnsupportedEncodingException
     */
    public Map conversionToMap(byte[] msg, RecordContext srcContext, RecordContext trgContext) throws MCIException, UnsupportedEncodingException {
        srcContext.setOrder(srcOrder.toString());
        trgContext.setOrder(trgOrder.toString());

        // byteorder 맞춰서 Buffer 정의
        ChannelBuffer buff = ChannelBuffers.wrappedBuffer(msg);

        Map retMap = new HashMap();

        // 버퍼를 IN Record에 맞게 추출해서 data에 기록
        if (logger.isDebugEnabled())
            logger.debug("============================================================================================== [Before]");

        readOut(srcContext, buff, null);

        if (logger.isDebugEnabled()) {
            logger.debug("[Before] ============================================================================================== \n");
            logger.debug("[After ] ============================================================================================== ");
        }

        int trgCnt = trgContext.size();
        for (int i = 1; i <= trgCnt; i++) {
            Record trgRecord = trgContext.get(i);
            if (!(trgRecord.getFunction() == null || trgRecord.getFunction().equals(""))) {
                // ExpressionEvaluator.evaluate(srcContext, retBuff, trgRecord,
                // trgRecord.getFunction(), srcOrder);
            } else {
                if (trgRecord.getType().equals("LIST")) {
                    i = convertListMap(srcContext, trgContext, retMap, i, trgRecord);
                } else {

                    int rank = Integer.parseInt(trgRecord.getRank());
                    Record srcRecord = srcContext.get(rank);
                    // logger.info("type : " + srcRecord.getData());
                    Object convertObj = TypeConversion.convertCTypeToMap(srcRecord.getData(), srcRecord, trgRecord, srcOrder, trgOrder);
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
     * C-TYPE to JSON Conversion
     *
     * @param msg
     * @param srcType
     * @param srcContext
     * @param trgType
     * @param trgContext
     * @return
     * @throws MCIException
     * @throws UnsupportedEncodingException
     */
    public String conversionToJson(byte[] msg, RecordContext srcContext, RecordContext trgContext) throws MCIException, UnsupportedEncodingException {
        srcContext.setOrder(srcOrder.toString());
        trgContext.setOrder(trgOrder.toString());

        // byteorder 맞춰서 Buffer 정의
        ChannelBuffer buff = ChannelBuffers.wrappedBuffer(msg);

        JSONObject jsonObj = new JSONObject();

        // 버퍼를 IN Record에 맞게 추출해서 data에 기록
        if (logger.isDebugEnabled())
            logger.debug("============================================================================================== [Before]");

        readOut(srcContext, buff, null);

        if (logger.isDebugEnabled()) {
            logger.debug("[Before] ============================================================================================== \n");
            logger.debug("[After ] ============================================================================================== ");
        }

        int trgCnt = trgContext.size();
        for (int i = 1; i <= trgCnt; i++) {
            Record trgRecord = trgContext.get(i);
            if (!(trgRecord.getFunction() == null || trgRecord.getFunction().equals(""))) {
                // ExpressionEvaluator.evaluate(srcContext, retBuff, trgRecord,
                // trgRecord.getFunction(), srcOrder);
            } else {
                if (trgRecord.getType().equals("LIST")) {
                    i = convertListJson(srcContext, trgContext, jsonObj, i, trgRecord);
                } else {
                    int rank = Integer.parseInt(trgRecord.getRank());
                    Record srcRecord = srcContext.get(rank);
                    // logger.info("type : " + srcRecord.getData());
                    Object convertObj = TypeConversion.convertCTypeToJson(srcRecord.getData(), srcRecord, trgRecord, srcOrder, trgOrder);
                    switch (JSONType.valueOf(trgRecord.getType())) {
                        case BITSTR:
                            char[] bits = ((String) convertObj).toCharArray();
                            String name = trgRecord.getName();
                            String[] names = name.split(",");
                            JSONObject bitObj = new JSONObject();
                            int bitCnt = names.length - 1;
                            for (int j = 1; j < names.length; j++) {
                                String key = names[j];
                                bitObj.put(key, bits[bitCnt - 1]);
                                bitCnt--;
                            }
                            jsonObj.put(names[0], bitObj);
                            break;

                        default:
                            jsonObj.put(trgRecord.getName(), convertObj);
                            break;

                    }

                    if (logger.isDebugEnabled())
                        logger.debug(trgRecord.toStringSimple(convertObj));
                }
            }
        }

        if (logger.isDebugEnabled())
            logger.debug("============================================================================================== [After ]");

        return JsonUtil.jsonPretty(jsonObj.toJSONString());
    }

    private int convertListJson(RecordContext srcContext, RecordContext trgContext, JSONObject cvtObj, int nowIndex, Record trgRecord)
            throws UnsupportedEncodingException {
        int childCnt = trgRecord.getChildCount();
        int cntIdx = trgRecord.getCountNo();

        Record countIdxRecord = srcContext.get(cntIdx);

        byte[] data = countIdxRecord.getData();
        ChannelBuffer buff = ChannelBuffers.copiedBuffer(data);
        long validCnt = 0;

        switch (CType.valueOf(countIdxRecord.getType())) {

            case UCHAR:
                validCnt = buff.readUnsignedByte();
                break;
            case USHORT:
                validCnt = buff.readUnsignedShort();
                break;
            case UINT:
                validCnt = buff.readUnsignedInt();
                break;

            default:
                break;
        }

        JSONArray childList = new JSONArray();

        JSONObject childObj;
        // list는 건너띠고
        for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {
            childObj = new JSONObject();
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

                Object convertObj = TypeConversion.convertCTypeToMap(childData, srcRecord, childRecord, srcOrder, trgOrder);
                childObj.put(childRecord.getName(), convertObj);

                if (logger.isDebugEnabled())
                    logger.debug(childRecord.toStringSimple(convertObj));

            }
            childList.add(childObj);
        }

        cvtObj.put(trgRecord.getName(), childList);
        nowIndex += childCnt;
        return nowIndex;
    }

    private void readOut(RecordContext srcContext, ChannelBuffer buff, List<Record> readOutList) throws UnsupportedEncodingException {

        int srcCnt = srcContext.size();

        for (int i = 1; i <= srcCnt; i++) {

            Record srcRecord = srcContext.get(i);
            byte[] readData = null;

            if (srcRecord.getType().equals("LIST")) {
                i = readCType(srcContext, buff, i, srcRecord, readOutList);
            } else {

                if (srcRecord.getLength() == -1) {
                    int countNo = srcRecord.getCountNo();
                    Record varLenSrcRecord = srcContext.get(countNo);
                    byte[] varLengthData = varLenSrcRecord.getData();
                    String varLenType = varLenSrcRecord.getType();

                    int varLen = 0;
                    switch (CType.valueOf(varLenType)) {
                        case USHORT:
                            varLen = ChannelBuffers.wrappedBuffer(varLengthData).readUnsignedShort();
                            break;
                        case UINT:
                            varLen = (int) ChannelBuffers.wrappedBuffer(varLengthData).readUnsignedInt();
                            break;
                        default:
                            break;
                    }

                    readData = BufferedReadOut.CTypeReading(buff, srcRecord.getType(), varLen);

                } else if (srcRecord.getLength() == 0) {
                    readData = new byte[buff.readableBytes()];
                    buff.readBytes(readData);

                } else {
                    readData = BufferedReadOut.CTypeReading(buff, srcRecord.getType(), srcRecord.getLength());
                }
                srcRecord.setData(readData);

                if (readOutList != null)
                    readOutList.add(srcRecord);

                if (logger.isDebugEnabled()) {
                    logger.debug(srcRecord.toStringSimple());
                }
            }
        }
    }

    private int readCType(RecordContext srcContext, ChannelBuffer retBuff, int nowindex, Record srcRecord, List<Record> readOutList)
            throws UnsupportedEncodingException {
        byte[] readData;
        int childCnt = srcRecord.getChildCount();
        int cntIdx = srcRecord.getCountNo();
        // logger.debug("cnt idx : "+inRecord);
        Record countIdxRecord = srcContext.get(cntIdx);
        byte[] data = countIdxRecord.getData();
        ChannelBuffer buff = ChannelBuffers.copiedBuffer(data);
        long validCnt = 0;

        switch (CType.valueOf(countIdxRecord.getType())) {

            case UCHAR:
                validCnt = buff.readUnsignedByte();
                break;
            case USHORT:
                validCnt = buff.readUnsignedShort();
                break;
            case UINT:
                validCnt = buff.readUnsignedInt();
                break;

            default:
                break;
        }

        // list는 건너띠고
        for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {

            // list 다음부터 계산
            for (int j2 = (nowindex + 1); j2 <= (nowindex + childCnt); j2++) {

                Record childRecord = srcContext.get(j2);

                readData = BufferedReadOut.CTypeReading(retBuff, childRecord.getType(), childRecord.getLength());

                childRecord.putData(vaildIdx, readData);

                if (readOutList != null)
                    readOutList.add(childRecord);

                if (logger.isDebugEnabled())
                    logger.debug(childRecord.toStringSimple(readData));
            }
        }

        nowindex += childCnt;
        return nowindex;
    }

    private int convertListMap(RecordContext srcContext, RecordContext trgContext, Map map, int nowIndex, Record trgRecord)
            throws UnsupportedEncodingException {
        int childCnt = trgRecord.getChildCount();
        int cntIdx = trgRecord.getCountNo();

        Record countIdxRecord = srcContext.get(cntIdx);

        byte[] data = countIdxRecord.getData();
        ChannelBuffer buff = ChannelBuffers.copiedBuffer(data);
        long validCnt = 0;

        switch (CType.valueOf(countIdxRecord.getType())) {

            case UCHAR:
                validCnt = buff.readUnsignedByte();
                break;
            case USHORT:
                validCnt = buff.readUnsignedShort();
                break;
            case UINT:
                validCnt = buff.readUnsignedInt();
                break;

            default:
                break;
        }

        List childList = new ArrayList();
        Map childMap;
        // list는 건너띠고
        for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {
            childMap = new HashMap();
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

                Object convertObj = TypeConversion.convertCTypeToMap(childData, srcRecord, childRecord, srcOrder, trgOrder);
                childMap.put(childRecord.getName(), convertObj);

                if (logger.isDebugEnabled())
                    logger.debug(childRecord.toStringSimple(convertObj));

            }
            childList.add(childMap);
        }

        map.put(trgRecord.getName(), childList);
        nowIndex += childCnt;
        return nowIndex;
    }

    public byte[] conversionToCType(byte[] msg, RecordContext inContext, RecordContext outContext) throws UnsupportedEncodingException {

        inContext.setOrder(srcOrder.toString());
        outContext.setOrder(trgOrder.toString());

        // byteorder 맞춰서 Buffer 정의
        ChannelBuffer buff = ChannelBuffers.wrappedBuffer(msg);
        // 버퍼를 IN Record에 맞게 추출해서 data에 기록
        if (logger.isDebugEnabled())
            logger.debug("============================================================================================== [Before]");

        readOut(inContext, buff, null);

        if (logger.isDebugEnabled()) {
            logger.debug("[Before] ============================================================================================== \n");
            logger.debug("[After ] ============================================================================================== ");
        }

        ChannelBuffer retBuff = ChannelBuffers.dynamicBuffer();

        int trgCnt = outContext.size();

        for (int index = 1; index <= trgCnt; index++) {

            Record trgRecord = outContext.get(index);
            if (!(trgRecord.getFunction() == null || trgRecord.getFunction().equals(""))) {
                ExpressionEvaluator.evaluate(inContext, buff, trgRecord, trgRecord.getFunction(), srcOrder);
            } else {
                // if (trgRecord.getType().equals("LIST")) {
                // index = convertListCType(inContext, outContext, retBuff,
                // index, trgRecord);
                // } else {
                int rank = Integer.parseInt(trgRecord.getRank());
                Record inRecord = inContext.get(rank);
                // 가변 데이터
                // if(trgRecord.getLength() == -1){
                // int countNo = trgRecord.getCountNo();
                // Record varLenTrgRecord = outContext.get(countNo);
                // byte[] convertBytes =
                // TypeConversion.convertCtypeToVarChar(srcRecord.getData(),
                // varLenTrgRecord,srcOrder, trgOrder);
                // trgRecord.setData(convertBytes);
                // retBuff.writeBytes(convertBytes);
                // }else{
                byte[] convertBytes = TypeConversion.convertCtypeToCType(inRecord.getData(), inRecord, trgRecord, srcOrder, trgOrder);
                trgRecord.setData(convertBytes);
                retBuff.writeBytes(convertBytes);
                // }

                if (logger.isDebugEnabled())
                    logger.debug(trgRecord.toStringSimple());
            }
            // }
        }

        if (logger.isDebugEnabled())
            logger.debug("============================================================================================== [After ]");

        byte[] retBytes = new byte[retBuff.readableBytes()];
        retBuff.readBytes(retBytes);

        return retBytes;

    }

    private int convertListCType(RecordContext inContext, RecordContext outContext, ChannelBuffer retBuff, int nowIndex, Record trgRecord)
            throws UnsupportedEncodingException {
        int childCnt = trgRecord.getChildCount();
        int cntIdx = trgRecord.getCountNo();

        Record countIdxRecord = outContext.get(cntIdx);
        byte[] data = countIdxRecord.getData();
        ChannelBuffer buff = ChannelBuffers.copiedBuffer(data);

        long validCnt = 0;

        switch (CType.valueOf(countIdxRecord.getType())) {

            case UCHAR:
                validCnt = buff.readUnsignedByte();
                break;
            case USHORT:
                validCnt = buff.readUnsignedShort();
                break;
            case UINT:
                validCnt = buff.readUnsignedInt();
                break;

            default:
                break;
        }

        for (int vaildIdx = 1; vaildIdx <= validCnt; vaildIdx++) {

            for (int j2 = (nowIndex + 1); j2 <= (nowIndex + childCnt); j2++) {

                Record childRecord = outContext.get(j2);
                int rank = Integer.parseInt(childRecord.getRank());
                Record srcRecord = inContext.get(rank);
                byte[] childData = srcRecord.getData(vaildIdx);

                if (childData == null) {
                    childData = srcRecord.getData();

                }
                byte[] convertBytes = null;
                convertBytes = TypeConversion.convertCtypeToCType(childData, srcRecord, childRecord, srcOrder, trgOrder);
                trgRecord.putData(vaildIdx, convertBytes);
                retBuff.writeBytes(convertBytes);

                if (logger.isDebugEnabled())
                    logger.debug(childRecord.toStringSimple(convertBytes));

            }
        }

        nowIndex += childCnt;
        return nowIndex;
    }
}