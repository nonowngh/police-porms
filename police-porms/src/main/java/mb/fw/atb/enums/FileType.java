package mb.fw.atb.enums;

public enum FileType {
    DELIMITER {
        @Override
        public String value() {
            return "DELIMITER";
        }
    },
    FIXEDLENGTH {
        @Override
        public String value() {
            return "FIXEDLENGTH";
        }
    },
    XML {
        @Override
        public String value() {
            return "XML";
        }
    },
    JSON {
        @Override
        public String value() {
            return "JSON";
        }
    };

    public abstract String value();
}
