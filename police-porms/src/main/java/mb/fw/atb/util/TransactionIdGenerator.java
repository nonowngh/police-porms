package mb.fw.atb.util;

import org.apache.commons.lang3.RandomStringUtils;

public class TransactionIdGenerator {
    public static String generate(String interfaceId, String alias, String msgCreDt) {
        return interfaceId + "_" + msgCreDt + "_" + alias + RandomStringUtils.randomAlphanumeric(3).toUpperCase();
    }
}
