package mb.fw.transformation.type;

import java.util.HashSet;
import java.util.Set;

/**
 * JSONType 은 String과 Byte[] , Bit String , NUMBER만 제공됨
 * @author clupine
 */
public enum JSONType {
	
	STRING("STRING") , BYTE("BYTE"),BITSTR("BITSTR"),DECIMAL("DECIMAL"),MAP("MAP"),INTEGER("INTEGER");

	public final static String NAME = "JSON";

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

	JSONType(String attrStr) {
		this.attrStr = attrStr.toUpperCase();
	}

	String getAttrString() {
		return attrStr;
	}

	public static boolean contains(String s) {
		return typeSet.contains(s);
	}

}
