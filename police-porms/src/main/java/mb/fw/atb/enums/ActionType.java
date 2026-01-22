package mb.fw.atb.enums;

public enum ActionType {
	SENDER{
		@Override
		public String value() {
				return "SENDER";
		}
	},
	RECEIVER{
		@Override
		public String value() {
			return "RECEIVER";
		}
	},
	CUSTOMIZE{
		@Override
		public String value() {
			return "CUSTOMIZE";
		}
	};

	public abstract String value();
}
