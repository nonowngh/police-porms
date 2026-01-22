package mb.fw.transformation.tool;

import mb.fw.transformation.type.CType;
import org.jboss.netty.buffer.ChannelBuffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * ChannelBuffer 의 타입에 따른 데이터 읽기
 * @author clupine
 * 
 */
public class BufferedReadOut {
	
	private static Logger logger = LoggerFactory.getLogger(BufferedReadOut.class);

	public static byte[] CTypeReading(ChannelBuffer buff, String type, int length) {
		
		byte[] field = null;
		
		switch (CType.valueOf(type)) {
		
		case UCHAR:
					field = new byte[1];
					buff.readBytes(field);
			break;
		case CHAR:
					field = new byte[length];
					buff.readBytes(field);
			break;
		case USHORT:
		case SHORT:
					field = new byte[2];
					buff.readBytes(field);
			break;
		case INT:
		case UINT:
		case FLOAT :
		case TIMET :
					field = new byte[4];
					buff.readBytes(field);
			break; 
		case LONG:
		case ULONG:
					field = new byte[8];
					buff.readBytes(field);
			break;
		case BYTE:
					field = new byte[length];
					buff.readBytes(field);
			break;
		default:
			break;
		}
		
		return field;
	}
	
	/**
	 * byte[]로 읽어오기
	 * @param buff
	 * @param length
	 * @return
	 */
	public static byte[] byteReading(ChannelBuffer buff, int length) {

		byte[] field = null;
		
		if(length!=0){
			field = new byte[length];
			buff.readBytes(field);
		}else{
			field = new byte[buff.readableBytes()];
			buff.readBytes(field);
		}
		return field;
	}
	
	/**
	 * String으로 읽어오기
	 * @param buff
	 * @param length
	 * @return
	 */
	public static String stringReading(ChannelBuffer buff, int length) {
		
		byte[] field = null;
		
		if(length!=0){
			field = new byte[length];
			buff.readBytes(field);
		}else{
			field = new byte[buff.readableBytes()];
			buff.readBytes(field);
		}
		return new String(field,Charset.forName("KSC5601"));
	}

}
