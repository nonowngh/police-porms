package mb.fw.transformation.form;

import lombok.extern.slf4j.Slf4j;
import mb.fw.transformation.type.AsciiType;
import mb.fw.transformation.type.CType;
import mb.fw.transformation.type.MapType;
import org.apache.commons.jexl2.JexlContext;
import org.jboss.netty.buffer.ChannelBuffers;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.nio.charset.Charset;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Slf4j
public class RecordContext implements JexlContext, Serializable {

    private static final long serialVersionUID = -6147736771606504291L;


    Map<Integer, Record> recordMap = new LinkedHashMap<Integer, Record>();

    String tmpData;

    String order = "BIG_ENDIAN";

    public void setOrder(String order) {
        this.order = order;
    }

    public String getOrder() {
        return order;
    }

    public String getTmpData() {
        return tmpData;
    }

    public void setTmpData(String tmpData) {
        this.tmpData = tmpData;
    }

    @Override
    public String toString() {
        return "RecordContext [recordMap=" + recordMap + ", tmpData=" + (tmpData == null ? "null" : tmpData.getBytes().length) + ", order=" + order + "]";
    }

    public Record put(int key, Record record) {
        return recordMap.put(key, record);
    }

    public void setRecordMap(Map<Integer, Record> recordMap) {
        this.recordMap = recordMap;
    }

    public Map<Integer, Record> getRecordMap() {
        return recordMap;
    }

    public Record getRecord(int num) {
        return recordMap.get(num);
    }

    /**
     * @param key
     * @return
     */
    public Record get(int key) {
        return recordMap.get(key);
    }

    public Set<Integer> keySet() {
        return recordMap.keySet();
    }

    /**
     * jexl
     */
    @Override
    public Object get(String name) {
        if (!name.startsWith("SEQ")) {
            return null;
        }
        Record record = recordMap.get(Integer.parseInt(name.substring(3)));
        if (record != null) {

            byte[] data = record.getData();

            if (data == null) {
                data = record.take();
                if (data == null)
                    return null;
            }

            String type = record.getType();

            if (AsciiType.contains(type)) {
                switch (AsciiType.valueOf(type)) {
                    case A:
                    case H: {
                        try {
                            return new String(data, Charset.forName("KSC5601").toString()).trim();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                            return null;
                        }
                    }
                    case N: {
                        return Integer.valueOf(new String(data));
                    }
                    default:
                        break;
                }

            } else if (CType.contains(type)) {

                switch (CType.valueOf(type)) {
                    case CHAR: {
                        return new String(data);
                    }
                    case DOUBLE: {
                        return ChannelBuffers.wrappedBuffer(data).readDouble();
                    }
                    case FLOAT: {
                        return ChannelBuffers.wrappedBuffer(data).readFloat();
                    }
                    case INT: {
                        return ChannelBuffers.wrappedBuffer(data).readInt();
                    }
                    case LONG: {
                        return ChannelBuffers.wrappedBuffer(data).readLong();
                    }
                    // case NCHAR:
                    // {
                    //
                    // }
                    // break;
                    // case SCHAR:
                    // {
                    //
                    // }
                    // break;
                    case SHORT: {
                        return ChannelBuffers.wrappedBuffer(data).readShort();
                    }
                    // case TDATE:
                    // {
                    //
                    // }
                    // break;
                    case UCHAR: {
                        return ChannelBuffers.wrappedBuffer(data).readUnsignedByte();
                    }
                    case UINT: {
                        return ChannelBuffers.wrappedBuffer(data).readUnsignedInt();
                    }
                    // case UINT24:
                    // {
                    // //24-bit
                    // return
                    // ChannelBuffers.wrappedBuffer(data).readUnsignedMedium();
                    // }
                    // case UINT48:
                    // {
                    // return BigEndianByteHandler.byteToInt48(data);
                    // }
                    case ULONG: {
                        BigInteger ulong = BigInteger.valueOf(ChannelBuffers.wrappedBuffer(data).readLong());
                        if (ulong.signum() < 0) {
                            return ulong.add(BigInteger.ONE.shiftLeft(64));
                        } else {
                            return ulong;
                        }
                    }
                    case USHORT: {
                        return ChannelBuffers.wrappedBuffer(data).readUnsignedShort();
                    }
                    default:
                        break;
                }

                // Map , XML 동일한 타입임
            } else if (MapType.contains(type)) {
                switch (MapType.valueOf(type)) {
                    // case BITSTR:
                    // {
                    //
                    // }
                    // break;
                    // case BYTE:
                    // {
                    //
                    // }
                    // break;
                    case STRING:
                    case INTEGER:
                    case DECIMAL: {
                        return new String(data, Charset.forName("KSC5601"));
                    }
                    default:
                        break;
                }
            }
        }
        return null;
    }

    public void set(String name, Object value) {
        log.info("set???");
    }

    public boolean has(String name) {
        return recordMap.containsKey(name);
    }

    public int size() {
        return recordMap.size();
    }
}
