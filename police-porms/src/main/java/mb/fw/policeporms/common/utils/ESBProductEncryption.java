package mb.fw.policeporms.common.utils;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.intf.service.JasyptStatelessService;

public class ESBProductEncryption {
    static JasyptStatelessService service = new JasyptStatelessService();
    static final String ESBKEY = "INDIGO_PASS";
    static final String ALGORITHM = "PBEWithMD5AndDES";

    public static String jasyptEncryptString(String str, String password) {
        return service.encrypt(str, password, null, null, ALGORITHM, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static String encryptString(String str) {
        return "ENC(" + service.encrypt(str, ESBKEY, null, null, ALGORITHM, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null) + ")";
    }

    public static String decryptString(String str) {
        if (!(str.startsWith("ENC(") && str.endsWith(")"))) {
            throw new EncryptionOperationNotPossibleException("How to use : ENC( value )");
        }
        str = str.substring(4, str.lastIndexOf(")"));
        return service.decrypt(str, ESBKEY, null, null, ALGORITHM, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static void main(String[] args) {
//		String value = "indigo";
//		String enValue = ProductEncryption.encryptString(value);
//		String enValue = "ENC(u/DDblZzKupXKXo3iXMviQ==)";
        String enValue = "ENC(HE3iRFOfSE/83HXKe76ujjvIRrxhPuNerJzgVrEiXg+s2yv+gZiDTPeYUXBNDKkRY3Om1krfBgephLlue5z28spWzqZKm19FbMxHDVy5sXA=)";
        System.out.println(enValue);
        System.out.println(ESBProductEncryption.decryptString(enValue));
		System.out.println(ESBProductEncryption.encryptString("8e17c22a88263e440938b77801b8801d151231270db2b68a228cce7d0d9e7d95"));
    }


}
