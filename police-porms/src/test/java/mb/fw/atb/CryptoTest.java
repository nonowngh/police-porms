package mb.fw.atb;

import com.indigo.esb.commons.security.Base64;
import com.indigo.esb.commons.security.Cipher;
import groovy.util.logging.Slf4j;


public class CryptoTest {
    public static void main(String[] args) {
//        byte[] data = Base64.decode("6rK97LCw66+87JuQ7Y+s7YS4");
//        System.out.println("length : " + data.leng`th);
//        System.out.println("["+new String(data)+"]");
        String password = "JATDLiW0xmhU5qriVfZR8g==";
        Cipher cipher = new Cipher();
        byte[] base64Password = Base64.decode(password);
        byte[] dec = cipher.decrypt(base64Password);
        System.out.println("dec : " + new String(dec));
    }

}
