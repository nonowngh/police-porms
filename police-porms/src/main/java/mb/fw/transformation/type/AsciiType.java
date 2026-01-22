package mb.fw.transformation.type;

import java.util.HashSet;
import java.util.Set;



/**
 * 일반 아스키 문자형 ByteArray 데이터 타입
 * A 문자형(공백을 채운후 왼쪽정렬),N 숫자형(오른쪽정렬후 0값으로 채운후 오른쪽정렬),H 한글(전각 공백을 채운후 전각변환후 왼쪽정렬)
 * @author clupine
 *
 */
public enum AsciiType {
	A("A"), N("N"), H("H"),;

	private String attrStr;

	static Set<String> typeSet;

	public final static String NAME = "ASCII";

	static {
		typeSet = new HashSet<String>();
		typeSet.add("A");
		typeSet.add("N");
		typeSet.add("H");
	}

	AsciiType(String attrStr) {
		this.attrStr = attrStr.toUpperCase();
	}

	String getAttrString() {
		return attrStr;
	}

	public static boolean contains(String s) {
		return typeSet.contains(s);
	}


}
