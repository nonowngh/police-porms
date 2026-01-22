package mb.fw.atb.enums;

public enum THeader {
    IF_RESULT {
        @Override
        public String key() {
            return "ifResult";
        }
    },
    IF_HEADER {
        @Override
        public String key() {
            return "ifHeader";
        }
    },
    TCP_MODE {
        @Override
        public String key() {
            return "tcpMode";
        }
    },
    DATA {
        @Override
        public String key() {
            return "data";
        }
    },
    SENDER_MSG_SEND_DT {
        @Override
        public String key() {
            return "senderMsgSendDt";
        }
    },
    INTERFACE_ID {
        @Override
        public String key() {
            return "interfaceId";
        }
    },
    SENDER_ADAPTOR_NAME {
        @Override
        public String key() {
            return "senderAdaptorName";
        }
    },
    ADAPTOR_NAME {
        @Override
        public String key() {
            return "adaptorName";
        }
    },
    TRANSACTION_ID {
        @Override
        public String key() {
            return "transactionId";
        }
    },
    RECEIVER_ID {
        @Override
        public String key() {
            return "receiverId";
        }
    },
    RESEND_YN {
        @Override
        public String key() {
            return "resendYn";
        }
    },
    SENDER_ID {
        @Override
        public String key() {
            return "senderId";
        }
    },
    SENDER_MSG_CREATE_DT {
        @Override
        public String key() {
            return "senderMsgCreateDt";
        }
    },
    SENDER_DATA_COUNT {
        @Override
        public String key() {
            return "senderDataCount";
        }
    },
    SENDER_DATA_CLASS {
        @Override
        public String key() {
            return "senderDataClass";
        }
    },
    SENDER_STRATEGY {
        @Override
        public String key() {
            return "senderStrategy";
        }
    },

    RECEIVER_STRATEGY {
        @Override
        public String key() {
            return "receiverStrategy";
        }
    },
    RECEIVER_MSG_RECV_DT {
        @Override
        public String key() {
            return "receiverMsgRecvDt";
        }
    },
    RECEIVER_ADAPTOR_NAME {
        @Override
        public String key() {
            return "receiverAdaptorName";
        }
    },

    RECEIVER_RESULT_CD {
        @Override
        public String key() {
            return "receiverResultCd";
        }
    },
    RECEIVER_RESULT_MSG {
        @Override
        public String key() {
            return "receiverResultMsg";
        }
    },
    TCP_SEND_MSG {
        @Override
        public String key() {
            return "tcpSendMsg";
        }
    },
    TCP_RECV_MSG {
        @Override
        public String key() {
            return "tcpRecvMsg";
        }
    },

    JMSCorrelationID {
        @Override
        public String key() {
            return "JMSCorrelationID";
        }
    },
    TIME_TRACE {
        @Override
        public String key() {
            return "TimeTrace";
        }
    };

    public abstract String key();
}
