package mb.fw.transformation.util;

public class Default {
	public static int toInt(Integer obj, int defaultVal) {
		if(obj == null) {
			return defaultVal;
		}
		return obj.intValue();
	}
}
