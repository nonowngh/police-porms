package mb.fw.transformation.type;

import java.util.HashSet;
import java.util.Set;

/**
 * Java XML 타입 MAP 타입과 동일함
 * key는 항목명
 * MapType 은 String과 Byte or Byte[] , Bit String 만 제공됨
 * @author clupine
 */
public enum XMLType {
	
	STRING("STRING") , BYTE("BYTE"),BITSTR("BITSTR");

	public final static String NAME = "XML";
	
	private String attrStr;
	
	static Set typeSet;
	
	static {
		typeSet = new HashSet<String>();
		typeSet.add("STRING");
		typeSet.add("BYTE");
		typeSet.add("BITSTR");
	}
	
	XMLType(String attrStr) {
		this.attrStr = attrStr.toUpperCase();
	}

	String getAttrString() {
		return attrStr;
	}
	
	public static boolean contains(String s) {
		return typeSet.contains(s);
	}

}
