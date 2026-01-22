package mb.fw.atb.enums;

public enum TResult {
	SUCCESS{
		@Override
		public String value() {
			return "S";
		}
	},
	FAIL{
		@Override
		public String value() {
			return "F";
		}
	},
	PROCESS{
		@Override
		public String value() {
			return "P";
		}
	};
	
	public abstract String value();

	public static TResult fromValue(String value) {
		for (TResult result : TResult.values()) {
			if (result.value().equals(value)) {
				return result;
			}
		}
		throw new IllegalArgumentException("No enum constant with value " + value);
	}
}
