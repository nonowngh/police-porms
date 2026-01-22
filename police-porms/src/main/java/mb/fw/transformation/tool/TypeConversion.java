package mb.fw.transformation.tool;

import mb.fw.transformation.form.Record;
import mb.fw.transformation.type.AsciiType;
import mb.fw.transformation.type.CType;
import mb.fw.transformation.type.JSONType;
import mb.fw.transformation.type.MapType;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.codec.binary.BinaryCodec;
import org.apache.commons.lang3.StringUtils;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

/**
 * @author clupine
 */
public class TypeConversion {

    private static Logger logger = LoggerFactory.getLogger(TypeConversion.class);

    /**
     * Map to CType
     *
     * @param srcData
     * @param srcRecord
     * @param trgRecord
     * @param srcOrder
     * @param trgOrder
     * @return
     * @throws UnsupportedEncodingException
     */
    public static byte[] convertMapToCType(byte[] srcData, Record srcRecord, Record trgRecord, ByteOrder srcOrder,
                                           ByteOrder trgOrder) throws UnsupportedEncodingException {

        String srcType = srcRecord.getType();
        String trgType = trgRecord.getType();

        switch (MapType.valueOf(srcType)) {

            case STRING:
                return convertBytesToCType(srcData, CType.valueOf(trgType), trgRecord.getLength(), srcOrder, trgOrder);

            case BYTE:
                return convertBytesToCType(srcData, CType.valueOf(trgType), trgRecord.getLength(), srcOrder, trgOrder);

            case BITSTR:
                return TypeConversion.binAsciiToBytes(srcData);

            default:
                break;
        }
        return null;
    }

    /**
     * @param srcData
     * @param srcRecord
     * @param trgRecord
     * @param srcOrder
     * @param trgOrder
     * @return
     * @throws UnsupportedEncodingException
     */
    public static byte[] convertJsonToCType(byte[] srcData, Record srcRecord, Record trgRecord, ByteOrder srcOrder,
                                            ByteOrder trgOrder) throws UnsupportedEncodingException {

        String srcType = srcRecord.getType();
        String trgType = trgRecord.getType();

        switch (JSONType.valueOf(srcType)) {

            case STRING: {

                int lengthInt = trgRecord.getLength();
                byte[] retByte = new byte[trgRecord.getLength()];
                if (srcData.length >= lengthInt) {
                    Arrays.fill(retByte, (byte) 0x20);
                    System.arraycopy(srcData, 0, retByte, 0, lengthInt);
                } else {
                    Arrays.fill(retByte, (byte) 0x20);
                    System.arraycopy(srcData, 0, retByte, 0, srcData.length);
                }
                return retByte;

            }
            case BYTE: {
                srcData = Base64.decodeBase64(new String(srcData));
                int lengthInt = trgRecord.getLength();
                byte[] retByte = new byte[trgRecord.getLength()];
                if (srcData.length >= lengthInt) {
                    Arrays.fill(retByte, (byte) 0x00);
                    System.arraycopy(srcData, 0, retByte, 0, lengthInt);
                } else {
                    Arrays.fill(retByte, (byte) 0x00);
                    System.arraycopy(srcData, 0, retByte, 0, srcData.length);
                }
                return retByte;
            }
            case BITSTR:
                return TypeConversion.binAsciiToBytes(srcData);

            case INTEGER:
            case DECIMAL:
                return convertBytesToCType(srcData, CType.valueOf(trgType), trgRecord.getLength(), srcOrder, trgOrder);
        }

        return null;
    }

    /**
     * 가변 길이 필드 에서 값을 뽑아서 변환
     *
     * @param srcData
     * @param varLenTrgRecord
     * @param srcOrder
     * @param trgOrder
     * @return
     * @throws UnsupportedEncodingException
     */
    public static byte[] convertMapToVarChar(byte[] srcData, Record varLenTrgRecord, ByteOrder srcOrder,
                                             ByteOrder trgOrder) throws UnsupportedEncodingException {

        byte[] retByte;

        // CHAR Only
        switch (CType.valueOf(varLenTrgRecord.getType())) {

            case USHORT:

                int lengthInt = ChannelBuffers.wrappedBuffer(varLenTrgRecord.getData()).readUnsignedShort();

                retByte = new byte[lengthInt];
                if (srcData.length >= lengthInt) {
                    Arrays.fill(retByte, (byte) 0x00);
                    System.arraycopy(srcData, 0, retByte, 0, lengthInt);
                } else {
                    Arrays.fill(retByte, (byte) 0x20);
                    System.arraycopy(srcData, 0, retByte, 0, srcData.length);
                }
                return retByte;

            case UINT:
                long lengthLong = ChannelBuffers.wrappedBuffer(varLenTrgRecord.getData()).readUnsignedInt();

                retByte = new byte[(int) lengthLong];
                if (srcData.length >= lengthLong) {
                    Arrays.fill(retByte, (byte) 0x00);
                    System.arraycopy(srcData, 0, retByte, 0, (int) lengthLong);
                } else {
                    Arrays.fill(retByte, (byte) 0x20);
                    System.arraycopy(srcData, 0, retByte, 0, srcData.length);
                }
                return retByte;

            default:
                break;
        }

        return null;
    }

    public static byte[] convertJsonToVarChar(byte[] srcData, Record varLenTrgRecord, ByteOrder srcOrder,
                                              ByteOrder trgOrder) throws UnsupportedEncodingException {

        byte[] retByte;

        // CHAR Only
        switch (CType.valueOf(varLenTrgRecord.getType())) {

            case USHORT:
                int lengthInt = ChannelBuffers.wrappedBuffer(varLenTrgRecord.getData()).readUnsignedShort();

                retByte = new byte[lengthInt];
                if (srcData.length >= lengthInt) {
                    Arrays.fill(retByte, (byte) 0x00);
                    System.arraycopy(srcData, 0, retByte, 0, lengthInt);
                } else {
                    Arrays.fill(retByte, (byte) 0x20);
                    System.arraycopy(srcData, 0, retByte, 0, srcData.length);
                }
                return retByte;

            case UINT:
                long lengthLong = ChannelBuffers.wrappedBuffer(varLenTrgRecord.getData()).readUnsignedInt();

                retByte = new byte[(int) lengthLong];
                if (srcData.length >= lengthLong) {
                    Arrays.fill(retByte, (byte) 0x00);
                    System.arraycopy(srcData, 0, retByte, 0, (int) lengthLong);
                } else {
                    Arrays.fill(retByte, (byte) 0x20);
                    System.arraycopy(srcData, 0, retByte, 0, srcData.length);
                }
                return retByte;

            default:
                break;
        }

        return null;
    }

    /**
     * 가변 길이 필드 에서 값을 뽑아서 변환 (반복필드용)
     *
     * @param srcData
     * @param varLenTrgRecord
     * @param vaildIdx
     * @param srcOrder
     * @param trgOrder
     * @return
     * @throws UnsupportedEncodingException
     */
    public static byte[] convertMapToVarChar4List(byte[] srcData, Record varLenTrgRecord, int vaildIdx,
                                                  ByteOrder srcOrder, ByteOrder trgOrder) throws UnsupportedEncodingException {

        byte[] retByte;

        // CHAR Only
        switch (CType.valueOf(varLenTrgRecord.getType())) {

            case USHORT:

                int lengthInt = ChannelBuffers.wrappedBuffer(varLenTrgRecord.getData(vaildIdx)).readUnsignedShort();

                retByte = new byte[lengthInt];
                if (srcData.length >= lengthInt) {
                    Arrays.fill(retByte, (byte) 0x00);
                    System.arraycopy(srcData, 0, retByte, 0, lengthInt);
                } else {
                    Arrays.fill(retByte, (byte) 0x20);
                    System.arraycopy(srcData, 0, retByte, 0, srcData.length);
                }
                return retByte;

            case UINT:
                long lengthLong = ChannelBuffers.wrappedBuffer(varLenTrgRecord.getData(vaildIdx)).readUnsignedInt();

                retByte = new byte[(int) lengthLong];
                if (srcData.length >= lengthLong) {
                    Arrays.fill(retByte, (byte) 0x00);
                    System.arraycopy(srcData, 0, retByte, 0, (int) lengthLong);
                } else {
                    Arrays.fill(retByte, (byte) 0x20);
                    System.arraycopy(srcData, 0, retByte, 0, srcData.length);
                }
                return retByte;

            default:
                break;
        }

        return null;
    }

    public static byte[] convertJsonToVarChar4List(byte[] srcData, Record varLenTrgRecord, int vaildIdx,
                                                   ByteOrder srcOrder, ByteOrder trgOrder) throws UnsupportedEncodingException {

        byte[] retByte;

        // CHAR Only
        switch (CType.valueOf(varLenTrgRecord.getType())) {

            case USHORT:

                int lengthInt = ChannelBuffers.wrappedBuffer(varLenTrgRecord.getData(vaildIdx)).readUnsignedShort();

                retByte = new byte[lengthInt];
                if (srcData.length >= lengthInt) {
                    Arrays.fill(retByte, (byte) 0x00);
                    System.arraycopy(srcData, 0, retByte, 0, lengthInt);
                } else {
                    Arrays.fill(retByte, (byte) 0x20);
                    System.arraycopy(srcData, 0, retByte, 0, srcData.length);
                }
                return retByte;

            case UINT:
                long lengthLong = ChannelBuffers.wrappedBuffer(varLenTrgRecord.getData(vaildIdx)).readUnsignedInt();

                retByte = new byte[(int) lengthLong];
                if (srcData.length >= lengthLong) {
                    Arrays.fill(retByte, (byte) 0x00);
                    System.arraycopy(srcData, 0, retByte, 0, (int) lengthLong);
                } else {
                    Arrays.fill(retByte, (byte) 0x20);
                    System.arraycopy(srcData, 0, retByte, 0, srcData.length);
                }
                return retByte;

            default:
                break;
        }

        return null;
    }

    /**
     * 가변 길이 필드 에서 값을 뽑아서 변환 (반복필드용)
     *
     * @param trgRecord
     * @param varLenTrgRecord
     * @param vaildIdx
     * @return
     * @throws UnsupportedEncodingException
     */
    public static byte[] convertMapToVarAscii4List(Record srcRecord, Record trgRecord, Record varLenTrgRecord,
                                                   int vaildIdx) throws UnsupportedEncodingException {

        switch (AsciiType.valueOf(varLenTrgRecord.getType())) {
            case N:
                int lengthInt = Integer.valueOf(ChannelBuffers.wrappedBuffer(varLenTrgRecord.getData(vaildIdx)).toString());
                return asciiConvert(srcRecord.getData(), trgRecord.getType(), lengthInt);

            default:
                break;
        }

        return null;
    }

    public static byte[] convertMapToVarAscii(Record srcRecord, Record trgRecord, Record varLenTrgRecord)
            throws UnsupportedEncodingException {

        switch (AsciiType.valueOf(varLenTrgRecord.getType())) {
            case N:
                int lengthInt = Integer.valueOf(ChannelBuffers.wrappedBuffer(varLenTrgRecord.getData()).toString());
                return asciiConvert(srcRecord.getData(), trgRecord.getType(), lengthInt);

            default:
                break;
        }

        return null;
    }

    /**
     * CType -> Map
     *
     * @param srcData
     * @param srcRecord
     * @param trgRecord
     * @param srcOrder
     * @param trgOrder
     * @return
     * @throws UnsupportedEncodingException
     */
    public static Object convertCTypeToMap(byte[] srcData, Record srcRecord, Record trgRecord, ByteOrder srcOrder,
                                           ByteOrder trgOrder) throws UnsupportedEncodingException {

        String srcType = srcRecord.getType();
        String trgType = trgRecord.getType();

        switch (CType.valueOf(srcType)) {
            case UCHAR:
                return convertUCharToMapType(srcData, MapType.valueOf(trgType), srcOrder, trgOrder);
            case CHAR:
                return convertCharToMapType(srcData, MapType.valueOf(trgType), trgRecord.getLength(), srcOrder, trgOrder);
            case USHORT:
                return convertUShortToMapType(srcData, MapType.valueOf(trgType), srcOrder, trgOrder);
            case SHORT:
                return convertShortToMapType(srcData, MapType.valueOf(trgType), srcOrder, trgOrder);
            case INT:
                return convertIntToMapType(srcData, MapType.valueOf(trgType), srcOrder, trgOrder);
            case UINT:
                return convertUIntToMapType(srcData, MapType.valueOf(trgType), srcOrder, trgOrder);
            case LONG:
                return convertLongToMapType(srcData, MapType.valueOf(trgType), srcOrder, trgOrder);
            case ULONG:
                return convertULongToMapType(srcData, MapType.valueOf(trgType), srcOrder, trgOrder);
            // case UINT24:
            // return convertInt24ToMapType(srcData, MapType.valueOf(trgType) ,
            // srcOrder, trgOrder);
            // case UINT48:
            // return convertInt48ToMapType(srcData, MapType.valueOf(trgType) ,
            // srcOrder, trgOrder);
            // case TDATE:
            // return convertInt48ToMapType(srcData, MapType.valueOf(trgType) ,
            // srcOrder, trgOrder);

            default:
                break;
        }
        return null;
    }

    public static Object convertCTypeToJson(byte[] srcData, Record srcRecord, Record trgRecord, ByteOrder srcOrder,
                                            ByteOrder trgOrder) throws UnsupportedEncodingException {

        String srcType = srcRecord.getType();
        String trgType = trgRecord.getType();

        switch (CType.valueOf(srcType)) {
            case UCHAR:
                return convertUCharToJsonType(srcData, JSONType.valueOf(trgType), srcOrder, trgOrder);
            case CHAR:
                return convertCharToJsonType(srcData, JSONType.valueOf(trgType), trgRecord.getLength(), srcOrder, trgOrder);
            case USHORT:
                return convertUShortToJsonType(srcData, JSONType.valueOf(trgType), srcOrder, trgOrder);
            case SHORT:
                return convertShortToJsonType(srcData, JSONType.valueOf(trgType), srcOrder, trgOrder);
            case INT:
                return convertIntToJsonType(srcData, JSONType.valueOf(trgType), srcOrder, trgOrder);
            case UINT:
                return convertUIntToJsonType(srcData, JSONType.valueOf(trgType), srcOrder, trgOrder);
            case LONG:
                return convertLongToJsonType(srcData, JSONType.valueOf(trgType), srcOrder, trgOrder);
            case ULONG:
                return convertULongToJsonType(srcData, JSONType.valueOf(trgType), srcOrder, trgOrder);
            case BYTE:
                return convertByteToJsonType(srcData, JSONType.valueOf(trgType), srcOrder, trgOrder);
            case FLOAT:
                return convertFloatToJsonType(srcData, JSONType.valueOf(trgType), srcOrder, trgOrder);
            case TIMET:
                return convertTimetToJsonType(srcData, JSONType.valueOf(trgType), srcOrder, trgOrder);
            // case UINT24:
            // return convertInt24ToMapType(srcData, MapType.valueOf(trgType) ,
            // srcOrder, trgOrder);
            // case UINT48:
            // return convertInt48ToMapType(srcData, MapType.valueOf(trgType) ,
            // srcOrder, trgOrder);
            // case TDATE:
            // return convertInt48ToMapType(srcData, MapType.valueOf(trgType) ,
            // srcOrder, trgOrder);

            default:
                break;
        }
        return null;
    }

    private static Object convertByteToJsonType(byte[] srcData, JSONType trgType, ByteOrder srcOrder,
                                                ByteOrder trgOrder) {
        switch (trgType) {
            case STRING:
                return new String(srcData).trim();
            case BYTE:
                return Base64.encodeBase64String(srcData);
            case BITSTR:
                return TypeConversion.bytesToBinAscii(srcData);
            default:
                break;
        }
        logger.info("############### trgType ==> {}", trgType);
        return null;
    }

    /**
     * ASCII -> MAP
     *
     * @param srcData
     * @param srcRecord
     * @param trgRecord
     * @param srcOrder
     * @param trgOrder
     * @return
     * @throws UnsupportedEncodingException
     */
    public static Object convertAsciiToMap(byte[] srcData, Record srcRecord, Record trgRecord, ByteOrder srcOrder,
                                           ByteOrder trgOrder) throws UnsupportedEncodingException {

        String srcType = srcRecord.getType();
        String trgType = trgRecord.getType();

        String value = null;
        switch (AsciiType.valueOf(srcType)) {
            case A:
            case N:
            case H:
                value = StringUtils.stripEnd(new String(srcData), null);
//			value = new String(srcData).trim();
                break;
            default:
                break;
        }

        switch (MapType.valueOf(trgType)) {
            case STRING:
                return value;
            case INTEGER:
                return Long.valueOf(value);
            case DECIMAL:
                return Double.valueOf(value);
            default:
                break;
        }
        return "";
    }

    public static Object convertMapToMap(byte[] srcData, Record srcRecord, Record trgRecord, ByteOrder srcOrder,
                                         ByteOrder trgOrder) throws UnsupportedEncodingException {
        String srcType = srcRecord.getType();
        switch (MapType.valueOf(srcType)) {
            case STRING:
                return new String(srcData).trim();
            case DECIMAL:
                try {
                    return Double.valueOf(new String(srcData));
                } catch (Exception e) {
                    return new Double(0);
                }
            default:
                break;
        }
        return null;
    }

    /**
     * Integer 6byte to MapType
     *
     * @param srcData
     * @param trgType
     * @param srcOrder
     * @param trgOrder
     * @return
     */
    public static Object convertInt48ToMapType(byte[] srcData, MapType trgType, ByteOrder srcOrder,
                                               ByteOrder trgOrder) {
        long value = 0;
        if (srcData.length == 6) {
            value = BigEndianByteHandler.byteToInt48(srcData);
        } else {
            return null;
        }

        switch (trgType) {
            case STRING:
                return String.valueOf(value);
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    /**
     * TDATE Ctype -> MapType
     *
     * @param srcData
     * @param trgType
     * @param srcOrder
     * @param trgOrder
     * @return
     */
    public static Object convertTDATEToMapType(byte[] srcData, MapType trgType, ByteOrder srcOrder,
                                               ByteOrder trgOrder) {
        switch (trgType) {
            case STRING:
                ChannelBuffer bb = ChannelBuffers.wrappedBuffer(srcData);
                short yy = bb.getUnsignedByte(0);
                short mon = bb.getUnsignedByte(1);
                short dd = bb.getUnsignedByte(2);
                short hh = bb.getUnsignedByte(3);
                short mm = bb.getUnsignedByte(4);
                short ss = bb.getUnsignedByte(5);
                return String.valueOf(yy) + String.valueOf(mon) + String.valueOf(dd) + String.valueOf(hh)
                        + String.valueOf(mm) + String.valueOf(ss);
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    /**
     * Integer 3Byte -> MapType
     *
     * @param srcData
     * @param trgType
     * @param srcOrder
     * @param trgOrder
     * @return
     */
    public static Object convertInt24ToMapType(byte[] srcData, MapType trgType, ByteOrder srcOrder,
                                               ByteOrder trgOrder) {
        long value = 0;

        switch (trgType) {
            case STRING:
                value = ChannelBuffers.wrappedBuffer(srcData).readUnsignedMedium();
                return String.valueOf(value);
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertLongToMapType(byte[] srcData, MapType trgType, ByteOrder srcOrder, ByteOrder trgOrder) {
        long value;
        switch (trgType) {
            case STRING:
                value = ChannelBuffers.wrappedBuffer(srcData).readLong();
                return String.valueOf(value);
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertLongToJsonType(byte[] srcData, JSONType trgType, ByteOrder srcOrder,
                                               ByteOrder trgOrder) {
        switch (trgType) {
            case INTEGER:
                return ChannelBuffers.wrappedBuffer(srcData).readLong();
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertFloatToJsonType(byte[] srcData, JSONType trgType, ByteOrder srcOrder,
                                                ByteOrder trgOrder) {
        switch (trgType) {
            case DECIMAL:
                return ChannelBuffers.wrappedBuffer(srcData).readFloat();
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    /**
     * ONLY BYTE SUPPORT
     *
     * @param srcData
     * @param trgType
     * @param srcOrder
     * @param trgOrder
     * @return
     */
    private static Object convertULongToMapType(byte[] srcData, MapType trgType, ByteOrder srcOrder,
                                                ByteOrder trgOrder) {
        switch (trgType) {
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    private static Object convertULongToJsonType(byte[] srcData, JSONType trgType, ByteOrder srcOrder,
                                                 ByteOrder trgOrder) {
        switch (trgType) {
            case BYTE:
                return ChannelBuffers.wrappedBuffer(srcData).readLong();
            default:
                break;
        }

        return null;
    }

    public static Object convertIntToMapType(byte[] srcData, MapType trgType, ByteOrder srcOrder, ByteOrder trgOrder) {
        long value;
        switch (trgType) {
            case STRING:
                value = ChannelBuffers.wrappedBuffer(srcData).readInt();
                return String.valueOf(value);
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertIntToJsonType(byte[] srcData, JSONType trgType, ByteOrder srcOrder,
                                              ByteOrder trgOrder) {
        switch (trgType) {
            case INTEGER:
                return ChannelBuffers.wrappedBuffer(srcData).readInt();
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertUIntToMapType(byte[] srcData, MapType trgType, ByteOrder srcOrder, ByteOrder trgOrder) {
        long value;
        switch (trgType) {
            case STRING:
                value = ChannelBuffers.wrappedBuffer(srcData).readUnsignedInt();
                return String.valueOf(value);
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertUIntToJsonType(byte[] srcData, JSONType trgType, ByteOrder srcOrder,
                                               ByteOrder trgOrder) {
        switch (trgType) {
            case INTEGER:
                return ChannelBuffers.wrappedBuffer(srcData).readUnsignedInt();
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertTimetToJsonType(byte[] srcData, JSONType trgType, ByteOrder srcOrder,
                                                ByteOrder trgOrder) {
        switch (trgType) {
            case STRING:
                long dateLong = ChannelBuffers.wrappedBuffer(srcData).readUnsignedInt() * 1000;
                Date date = new Date(dateLong);
                Format format = new SimpleDateFormat("yyyyMMddHHmmssSSS", Locale.KOREA);
                String todayStr = format.format(date);
                return todayStr;
            default:
                break;
        }

        return null;
    }

    public static Object convertShortToMapType(byte[] srcData, MapType trgType, ByteOrder srcOrder,
                                               ByteOrder trgOrder) {
        long value;
        switch (trgType) {
            case STRING:
                value = ChannelBuffers.wrappedBuffer(srcData).readShort();
                return String.valueOf(value);
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertShortToJsonType(byte[] srcData, JSONType trgType, ByteOrder srcOrder,
                                                ByteOrder trgOrder) {
        switch (trgType) {
            case INTEGER:
                return ChannelBuffers.wrappedBuffer(srcData).readShort();
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertUShortToMapType(byte[] srcData, MapType trgType, ByteOrder srcOrder,
                                                ByteOrder trgOrder) {
        long value;
        switch (trgType) {
            case STRING:
                value = ChannelBuffers.wrappedBuffer(srcData).readUnsignedShort();
                return String.valueOf(value);
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertUShortToJsonType(byte[] srcData, JSONType trgType, ByteOrder srcOrder,
                                                 ByteOrder trgOrder) {
        switch (trgType) {
            case INTEGER:
                return ChannelBuffers.wrappedBuffer(srcData).readUnsignedShort();
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertUCharToMapType(byte[] srcData, MapType trgType, ByteOrder srcOrder,
                                               ByteOrder trgOrder) {
        long value;
        switch (trgType) {
            case STRING:
                value = ChannelBuffers.wrappedBuffer(srcData).readUnsignedByte();
                return String.valueOf(value);
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertUCharToJsonType(byte[] srcData, JSONType trgType, ByteOrder srcOrder,
                                                ByteOrder trgOrder) {
        switch (trgType) {
            case INTEGER:
                return ChannelBuffers.wrappedBuffer(srcData).readUnsignedByte();
            case BYTE:
                return srcData;
            default:
                break;
        }

        return null;
    }

    public static Object convertCharToMapType(byte[] srcData, MapType trgType, int length, ByteOrder srcOrder,
                                              ByteOrder trgOrder) {

        switch (trgType) {
            case STRING:
                return new String(srcData);
            case BYTE:
                return srcData;
            case BITSTR:
                return TypeConversion.bytesToBinAscii(srcData);
            default:
                break;
        }

        return null;
    }

    public static Object convertCharToJsonType(byte[] srcData, JSONType trgType, int length, ByteOrder srcOrder,
                                               ByteOrder trgOrder) {

        switch (trgType) {
            case INTEGER:
                return ChannelBuffers.wrappedBuffer(srcData).readByte();
            case BYTE:
                return srcData;
            case BITSTR:
                return TypeConversion.bytesToBinAscii(srcData);
            default:
                break;
        }

        return null;
    }

    /**
     * @param srcData
     * @param trgType
     * @param length
     * @param srcOrder
     * @param trgOrder
     * @return
     */
    public static byte[] convertBytesToCType(byte[] srcData, CType trgType, int length, ByteOrder srcOrder,
                                             ByteOrder trgOrder) {

        switch (trgType) {
            case BYTE: {
                return srcData;
            }
            case UCHAR:
            case CHAR: {
                int value = Integer.parseInt(new String(srcData));
                ChannelBuffer cb = ChannelBuffers.buffer(1);
                cb.writeByte(value);
                byte[] dst = new byte[1];
                cb.getBytes(0, dst);
                return dst;
            }
            case USHORT:
            case SHORT: {
                int value = Integer.parseInt(new String(srcData));
                ChannelBuffer cb = ChannelBuffers.buffer(2);
                cb.writeShort(value);
                byte[] dst = new byte[2];
                cb.getBytes(0, dst);
                return dst;
            }
            case UINT:
            case INT: {
                int value = Integer.parseInt(new String(srcData));
                ChannelBuffer cb = ChannelBuffers.buffer(4);
                cb.writeShort(value);
                byte[] dst = new byte[4];
                cb.getBytes(0, dst);
                return dst;
            }
            case ULONG:
            case LONG: {
                long value = Long.parseLong(new String(srcData));
                ChannelBuffer cb = ChannelBuffers.buffer(8);
                cb.writeLong(value);
                byte[] dst = new byte[8];
                cb.getBytes(0, dst);
                return dst;
            }
            case FLOAT:
            case UFLOAT: {
                float value = Float.parseFloat(new String(srcData));
                ChannelBuffer cb = ChannelBuffers.buffer(4);
                cb.writeFloat(value);
                byte[] dst = new byte[4];
                cb.getBytes(0, dst);
                return dst;
            }
            case DOUBLE:
            case UDOUBLE: {
                double value = Double.parseDouble(new String(srcData));
                ChannelBuffer cb = ChannelBuffers.buffer(8);
                cb.writeDouble(value);
                byte[] dst = new byte[8];
                cb.getBytes(0, dst);
                return dst;
            }

            // case UINT24 :
            // int valueInt24 = Integer.parseInt(new String(srcData));
            // return toUnsignedInt24bytes(valueInt24);
            // case UINT48 :
            // Long valueInt48 = Long.parseLong(new String(srcData));
            // return toUnsignedInt48bytes(valueInt48);
            // case TDATE :
            // return toTDATEbytes(srcData);

            default:
                break;
        }

        return null;
    }

    /**
     * String --> C-STRUCTURE
     *
     * @param srcStr
     * @param trgType
     * @param srcOrder
     * @param trgOrder
     * @return
     */
    public static byte[] convertStringToCSTR(String srcStr, CType trgType, int length, ByteOrder srcOrder,
                                             ByteOrder trgOrder) {
        return convertBytesToCType(srcStr.getBytes(), trgType, length, srcOrder, trgOrder);
    }


    public static byte[] asciiConvert(byte[] valueBytes, String type, int length) throws UnsupportedEncodingException {

        if (AsciiType.contains(type)) {

            byte[] retBytes = new byte[length];

            switch (AsciiType.valueOf(type)) {

                case A: {
                    Arrays.fill(retBytes, (byte) 0x20);
                    String utf8Str = new String(valueBytes);
                    valueBytes = utf8Str.getBytes("euc-kr");

                    if (valueBytes.length > length) {
                        System.arraycopy(valueBytes, 0, retBytes, 0, length);
                    } else {
                        System.arraycopy(valueBytes, 0, retBytes, 0, valueBytes.length);
                    }
                    break;
                }
                case H: {
                    Arrays.fill(retBytes, (byte) 0xA1);
                    String fullStr = toFullChar(new String(valueBytes));
                    String utf8Str = new String(fullStr);
                    valueBytes = utf8Str.getBytes("euc-kr");

                    if (valueBytes.length > length) {
                        System.arraycopy(valueBytes, 0, retBytes, 0, length);
                    } else {
                        System.arraycopy(valueBytes, 0, retBytes, 0, valueBytes.length);
                    }

                    break;
                }
                case N:
                    retBytes = leftPad(valueBytes, length, (byte) 0x30);
                    break;

                default:
                    break;
            }
            return retBytes;
        }

        return null;
    }

    public static byte[] byteN2ByteShort(byte[] valueBytes, ByteOrder order) {
        Short value = Short.parseShort(new String(valueBytes));
        ByteBuffer buff = ByteBuffer.allocate(2);
        buff.order(order);
        buff.putShort(value);
        return buff.array();
    }

    public static byte[] Short2ByteShort(Short value, ByteOrder order) {
        ByteBuffer buff = ByteBuffer.allocate(2);
        buff.order(order);
        buff.putShort(value);
        return buff.array();
    }

    public static byte toUnsinedCharByte(Short value) {
        return (byte) (value & 0xff);
    }

    public static byte toUnsignedCharByte(int value) {
        return (byte) (value & 0xff);
    }

    public static byte[] toUnsignedShortbytes(int val) {
        byte[] header = new byte[2];
        header[1] = (byte) (val & 0xff);
        header[0] = (byte) ((val >> 8) & 0xff);
        return header;

    }

    public static byte[] toIntbytes(int val) {
        return toUnsignedIntbytes(val);
    }

    public static byte[] toUnsignedIntbytes(long val) {
        byte[] header = new byte[4];
        header[3] = (byte) (val & 0xff);
        header[2] = (byte) ((val >> 8) & 0xff);
        header[1] = (byte) ((val >> 16) & 0xff);
        header[0] = (byte) ((val >> 24) & 0xff);
        return header;
    }

    public static byte[] toUnsignedInt24bytes(long val) {
        byte[] header = new byte[3];
        header[2] = (byte) (val & 0xff);
        header[1] = (byte) ((val >> 8) & 0xff);
        header[0] = (byte) ((val >> 16) & 0xff);
        return header;
    }

    public static byte[] toUnsignedInt48bytes(long val) {
        byte[] header = new byte[6];
        header[5] = (byte) (val & 0xff);
        header[4] = (byte) ((val >> 8) & 0xff);
        header[3] = (byte) ((val >> 16) & 0xff);
        header[2] = (byte) ((val >> 24) & 0xff);
        header[1] = (byte) ((val >> 32) & 0xff);
        header[0] = (byte) ((val >> 40) & 0xff);
        return header;
    }

    public static byte[] toTDATEbytes(byte[] val) {
        ChannelBuffer bb = ChannelBuffers.wrappedBuffer(val);
        byte[] yy = new byte[2];
        byte[] mon = new byte[2];
        byte[] dd = new byte[2];
        byte[] hh = new byte[2];
        byte[] mm = new byte[2];
        byte[] ss = new byte[2];
        bb.readBytes(yy);
        bb.readBytes(mon);
        bb.readBytes(dd);
        bb.readBytes(hh);
        bb.readBytes(mm);
        bb.readBytes(ss);

        byte yybyte = Byte.valueOf(new String(yy));
        byte monbyte = Byte.valueOf(new String(mon));
        byte ddbyte = Byte.valueOf(new String(dd));
        byte hhbyte = Byte.valueOf(new String(hh));
        byte mmbyte = Byte.valueOf(new String(mm));
        byte ssbyte = Byte.valueOf(new String(ss));

        ChannelBuffer retBuff = ChannelBuffers.buffer(6);
        retBuff.writeByte(yybyte);
        retBuff.writeByte(monbyte);
        retBuff.writeByte(ddbyte);
        retBuff.writeByte(hhbyte);
        retBuff.writeByte(mmbyte);
        retBuff.writeByte(ssbyte);

        return retBuff.array();
    }

    public static byte[] byteN2ByteLong(byte[] valueBytes, ByteOrder order) {
        Long value = Long.parseLong(new String(valueBytes));
        ByteBuffer buff = ByteBuffer.allocate(8);
        buff.order(order);
        buff.putLong(value);
        return buff.array();
    }

    public static byte[] byteN2ByteFloat(byte[] valueBytes, ByteOrder order) {
        float value = Float.parseFloat(new String(valueBytes));
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.order(order);
        buff.putDouble(value);
        return buff.array();
    }

    public static byte[] byteN2ByteDouble(byte[] valueBytes, ByteOrder order) {
        double value = Double.parseDouble(new String(valueBytes));
        ByteBuffer buff = ByteBuffer.allocate(8);
        buff.order(order);
        buff.putDouble(value);
        return buff.array();
    }

    public static byte[] byteN2ByteInt(byte[] valueBytes, ByteOrder order) {
        int value = Integer.parseInt(new String(valueBytes));
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.order(order);
        buff.putInt(value);
        return buff.array();
    }

    public static byte[] strN2ByteInt(String valueStr, ByteOrder order) {
        int value = Integer.parseInt(valueStr);
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.order(order);
        buff.putInt(value);
        return buff.array();
    }

    public static int byteInt2Int(byte[] valueBytes, ByteOrder order) {
        ByteBuffer buff = ByteBuffer.allocate(4);
        buff.order(order);
        buff.put(valueBytes);
        buff.position(0);
        return buff.getInt();
    }

    private static byte[] leftPad(byte[] in, int len, byte fillByte) {

        if (in.length == len) {
            return in;
        }
        byte[] bytes = new byte[len];
        Arrays.fill(bytes, fillByte);
        if (in != null) {
            try {
                if (in.length < len) {
                    System.arraycopy(in, 0, bytes, bytes.length - in.length, in.length);
                } else {
                    System.arraycopy(in, 0, bytes, 0, len);
                }
            } catch (ArrayIndexOutOfBoundsException e) {
                throw e;
            }
        }
        return bytes;
    }

    public static String toHalfChar(String src) {
        StringBuffer strBuf = new StringBuffer();
        char c = 0;
        int nSrcLength = src.length();
        for (int i = 0; i < nSrcLength; i++) {
            c = src.charAt(i);
            if (c >= '!' && c <= '~') {
                c -= 0xfee0;
            } else if (c == '　') {
                c = 0x20;
            }
            strBuf.append(c);
        }
        return strBuf.toString();
    }

    public static String toFullChar(String src) {
        if (src == null)
            return null;
        StringBuffer strBuf = new StringBuffer();
        char c = 0;
        int nSrcLength = src.length();
        for (int i = 0; i < nSrcLength; i++) {
            c = src.charAt(i);
            if (c >= 0x21 && c <= 0x7e) {
                c += 0xfee0;
            } else if (c == 0x20) {
                c = 0x3000;
            }
            strBuf.append(c);
        }
        return strBuf.toString();
    }


    public static String toKSC5601(String str) {
        try {
            return new String(str.getBytes(Charset.forName("KSC5601").toString()));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return str;

    }

    public static byte[] toTypeBytesFullChar(String value, int length) throws UnsupportedEncodingException {

        byte[] srcBytes = null;
        byte[] retBytes = new byte[length];

        srcBytes = toFullChar(value).getBytes(Charset.forName("KSC5601").toString());
        Arrays.fill(retBytes, (byte) 0xA1);
        if (srcBytes.length > length) {
            System.arraycopy(srcBytes, 0, retBytes, 0, length);
        } else {
            System.arraycopy(srcBytes, 0, retBytes, 0, srcBytes.length);
        }
        return retBytes;
    }


    public static byte[] binAsciiToBytes(String bitStr) {
        return BinaryCodec.fromAscii(bitStr.getBytes());
    }

    public static byte[] binAsciiToBytes(byte[] bitStr) {
        return BinaryCodec.fromAscii(bitStr);
    }

    public static String bytesToBinAscii(byte[] data) {
        return new String(BinaryCodec.toAsciiString(data));
    }

    public static byte[] convertCtypeToCType(byte[] srcData, Record inRecord, Record outRecord, ByteOrder srcOrder,
                                             ByteOrder trgOrder) {
        logger.info("srcData : {}", srcData.length);
        String srcType = inRecord.getType();
        String trgType = outRecord.getType();

        if (srcType.equals(trgType) && inRecord.getLength() == outRecord.getLength()) {
            return srcData;
        }

        switch (CType.valueOf(srcType)) {
            case UCHAR:
            case CHAR:
            case USHORT:
            case SHORT:
            case INT:
            case UINT:
            case LONG:
            case ULONG:
            case BYTE:
            default:
                break;
        }
        return null;
    }

}
