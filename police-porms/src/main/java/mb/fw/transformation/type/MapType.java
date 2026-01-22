package mb.fw.transformation.type;

import java.util.HashSet;
import java.util.Set;

/**
 * Java Map 타입
 * key는 항목명
 * MapType 은 String과 Byte or Byte[] , Bit String 만 제공됨
 * @author clupine
 */
public enum MapType {

	STRING("STRING") , BYTE("BYTE"),BITSTR("BITSTR"),DECIMAL("DECIMAL"),MAP("MAP"),INTEGER("INTEGER");

	public final static String NAME = "MAP";

	private String attrStr;

	static Set typeSet;

	static {
		typeSet = new HashSet<String>();
		typeSet.add("STRING");
		typeSet.add("BYTE");
		typeSet.add("BITSTR");
		typeSet.add("DECIMAL");
		typeSet.add("INTEGER");
		typeSet.add("MAP");
	}

	MapType(String attrStr) {
		this.attrStr = attrStr.toUpperCase();
	}

	String getAttrString() {
		return attrStr;
	}

	public static boolean contains(String s) {
		return typeSet.contains(s);
	}

}
