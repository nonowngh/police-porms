package mb.fw.atb.util;

import mb.fw.atb.enums.THeader;
import org.slf4j.MDC;

public class MDCLogging {

    public static void create(String txid, String interfaceId , String adaptorName) {
        MDC.put(THeader.ADAPTOR_NAME.key(), adaptorName);
        MDC.put(THeader.TRANSACTION_ID.key(), txid);
        MDC.put(THeader.INTERFACE_ID.key(), interfaceId);
    }

    public static void release() {
        MDC.remove(THeader.TRANSACTION_ID.key());
        MDC.remove(THeader.INTERFACE_ID.key());
    }


}
