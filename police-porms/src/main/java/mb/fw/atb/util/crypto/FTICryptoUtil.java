package mb.fw.atb.util.crypto;

import com.mb.mci.common.util.HexUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.security.spec.EncodedKeySpec;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

import static javax.crypto.Cipher.ENCRYPT_MODE;

/**
 * 금융결제원에서 사용하는 금융거래정보 암호화 유틸리티
 * @author : clupine
 *
 */
@Slf4j
public class FTICryptoUtil {

    static Base64.Encoder base64Encoder = Base64.getEncoder();
    static Base64.Decoder base64Decoder = Base64.getDecoder();

    /**
     * 사용예제
     * @param args 
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        Security.addProvider(new org.bouncycastle.jce.provider.BouncyCastleProvider());

        KeyPair keyPair = generateKeyPair();

        //공개키 생성 & 개인키 생성
        PublicKey publicKey = keyPair.getPublic();
        PrivateKey privateKey = keyPair.getPrivate();

        //키를 저장
        byte[] publicKeyBytes = publicKey.getEncoded();
        byte[] privateKeyBytes = privateKey.getEncoded();

        log.info("publicKeyBytes ==> {} , {}" ,HexUtil.hexToPrint(publicKeyBytes) , publicKeyBytes.length);
        log.info("privateKeyBytes ==> {} , {}" , HexUtil.hexToPrint(privateKeyBytes) , privateKeyBytes.length);

        //publicKeyBytes 와 privateKeyBytes를 base64로 변환한후 byte 길이를 출력
        log.info("publicKeyBytes Base64 ==> {} , {}" , base64Encoder.encodeToString(publicKeyBytes) , base64Encoder.encodeToString(publicKeyBytes).length());
        log.info("privateKeyBytes Base64 ==> {} , {}" , base64Encoder.encodeToString(privateKeyBytes) , base64Encoder.encodeToString(privateKeyBytes).length());



        //키를 파일로 저장
        FileUtils.writeByteArrayToFile(new File("public.key"), publicKeyBytes);
        FileUtils.writeByteArrayToFile(new File("private.key"), privateKeyBytes);

        PublicKey loadPublicKey = null;
        PrivateKey loadPrivateKey = null;
        try {
            //키를 가져온다.
            loadPublicKey = getRsaPublicKey(publicKeyBytes);
            loadPrivateKey = getRsaPrivateKey(privateKeyBytes);
        } catch (InvalidKeySpecException e) {
            throw new RuntimeException(e);
        }

        //난수 128bit 키 생성
        KeyGenerator generator = KeyGenerator.getInstance("AES");
        SecureRandom random = SecureRandom.getInstance("SHA1PRNG");
        generator.init(128, random);
        Key secureKey = generator.generateKey();
        byte[] randomKey = secureKey.getEncoded();

        log.info("Random Key 128bit ==> " + HexUtil.hexToPrint(randomKey));
        //randomKey length
        String randomKeyBase64Str = base64Encoder.encodeToString(randomKey);
        log.info("Random Key length ==> " + randomKey.length);
        log.info("Random Key Base64 ==> " + randomKeyBase64Str);

        String message = "암호화할 값";

        log.info("평문 ==> [" + message + "]");
        String encryptMessage = rsaEncryptWith(loadPublicKey, message);
        log.info("암호화 ==> [" + encryptMessage+"]");

        String decryptMessage = rsaDecryptWith(loadPrivateKey, encryptMessage);
        log.info("복호화 ==> [" + decryptMessage+"]");

    }

    /**
     * 공개키 생성 ASN.1 (X.509)
     * @param publicKeyBytes
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PublicKey getRsaPublicKey(byte[] publicKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec publicKeySpec = new X509EncodedKeySpec(publicKeyBytes);
        return keyFactory.generatePublic(publicKeySpec);
    }

    /**
     * 개인키 생성 ASN.1 (PKCS#8)
     * @param publicKeyBytes
     * @return
     * @throws NoSuchAlgorithmException
     * @throws InvalidKeySpecException
     */
    public static PrivateKey getRsaPrivateKey(byte[] publicKeyBytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        EncodedKeySpec publicKeySpec = new PKCS8EncodedKeySpec(publicKeyBytes);
        return keyFactory.generatePrivate(publicKeySpec);
    }

    /**
     * RSA-OAEP 암호화
     *
     * @param publicKey
     * @param message
     * @return
     * @throws Exception
     */
    public static String rsaEncryptWith(PublicKey publicKey, String message) throws Exception {
        // 공용 키를 사용한 암호화를 위해 초기화된 Cipher 필요
        Cipher encryptCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding", "BC");
        encryptCipher.init(ENCRYPT_MODE, publicKey);

        // 암호화 메서드는 바이트 배열 인수만 받으므로 문자열 변환
        byte[] secretMessageBytes = message.getBytes(StandardCharsets.UTF_8);
        // doFinal 메서드를 호출하여 메시지 암호화
        byte[] encryptedMessageBytes = encryptCipher.doFinal(secretMessageBytes);

        // Base64 인코딩
        return base64Encoder.encodeToString(encryptedMessageBytes);
    }

    /**
     * RSA-OAEP 복호화
     *
     * @param privateKey
     * @param encryptedMessage
     * @return
     * @throws Exception
     */
    public static String rsaDecryptWith(PrivateKey privateKey, String encryptedMessage) throws Exception {
        // 복호화를 위해 별도의 Cipher 인스턴스 필요
        Cipher decryptCipher = Cipher.getInstance("RSA/NONE/OAEPWithSHA256AndMGF1Padding");
        decryptCipher.init(Cipher.DECRYPT_MODE, privateKey);

        byte[] encryptedMessageBytes = base64Decoder.decode(encryptedMessage.getBytes());
        byte[] decryptedMessageBytes = decryptCipher.doFinal(encryptedMessageBytes);

        // 암호화 과정과 동일하게 doFinal 메서드를 사용하여 복호화
        return new String(decryptedMessageBytes, StandardCharsets.UTF_8);
    }

    /**
     * RSA 2048bit의 KeyPair를 생성한다.
     *
     * @throws NoSuchAlgorithmException
     * @throws IOException
     */
    public static KeyPair generateKeyPair() throws NoSuchAlgorithmException, IOException {
        // RSA 키 쌍 생성
        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048, new SecureRandom());
        KeyPair keyPair = keyPairGenerator.generateKeyPair();

        return keyPair;
    }


}
