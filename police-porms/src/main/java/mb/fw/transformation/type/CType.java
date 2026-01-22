package mb.fw.transformation.type;

import java.util.HashSet;
import java.util.Set;

/**
 * 데이터 타입
 * @author clupine
 *
 */
public enum CType {
	
	BYTE("BYTE"),UCHAR("UCHAR"),CHAR("CHAR"),SHORT("SHORT"),USHORT("USHORT"),UINT("UINT"),INT("INT"),ULONG("ULONG"),LONG("LONG"),UFLOAT("UFLOAT"),FLOAT("FLOAT"),DOUBLE("DOUBLE"),UDOUBLE("UDOUBLE"),TIMET("TIMET");

	private String attrStr;
	
	static Set<String> typeSet;
	
	public final static String NAME = "C-TYPE";
	
	static {
		typeSet = new HashSet<String>();
		typeSet.add("BYTE");
		typeSet.add("UCHAR");
		typeSet.add("CHAR");
		typeSet.add("SHORT");
		typeSet.add("USHORT");
		typeSet.add("UINT");
		typeSet.add("INT");
		typeSet.add("ULONG");
		typeSet.add("LONG");
		typeSet.add("UFLOAT");
		typeSet.add("FLOAT");
		typeSet.add("DOUBLE");
		typeSet.add("UDOUBLE");
		typeSet.add("TDATE");
		typeSet.add("TIMET");
		
	}

	CType(String attrStr) {
		this.attrStr = attrStr.toUpperCase();
	}

	String getAttrString() {
		return attrStr;
	}
		
	public static boolean contains(String s) {
		return typeSet.contains(s);
	}
	

}
