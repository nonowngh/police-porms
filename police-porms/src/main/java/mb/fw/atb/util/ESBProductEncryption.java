package mb.fw.atb.util;

import org.jasypt.exceptions.EncryptionOperationNotPossibleException;
import org.jasypt.intf.service.JasyptStatelessService;

import com.indigo.esb.security.Base64;
import com.indigo.esb.security.Cipher;

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
//		490
//		210
//     2600
//		String value = "indigo";
//		String enValue = ProductEncryption.encryptString(value);
//		String enValue = "ENC(Jk+BVMNLBH31h7OP6lOoAQ==)";
//		String enValue = "ENC(u/DDblZzKupXKXo3iXMviQ==)";
        String enValue = "ENC(9ed4Np36xNPnpUMN3Y1VQSXCTUX6rRIY)";
        System.out.println(enValue);
        System.out.println(ESBProductEncryption.decryptString(enValue));
		System.out.println(ESBProductEncryption.encryptString("METABUILD_IFTP"));
    }


}
