package mb.fw.policeporms.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterfaceEncryptUtils {

	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	
	protected static final String FILE_AES_256_GCM_KEY = "2026POLICEPORMSPRODUCTCOMMONKEY!";

	public static InputStream createFileDecryptInputStream(InputStream in) throws Exception {
		byte[] iv = new byte[16];
		int readIvSize = in.read(iv);
		if (readIvSize < 16) {
			throw new IOException("암호화 파일의 IV를 읽을 수 없습니다.");
		}

		byte[] keyBytes = FILE_AES_256_GCM_KEY.getBytes(StandardCharsets.UTF_8);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(iv);
		cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);

		log.info("파일 복호화 스트림 적용 (AES/CBC)...");
		return new CipherInputStream(in, cipher);
	}

	public static OutputStream createFileEncryptOutputStream(OutputStream out) throws Exception {
		byte[] keyBytes = FILE_AES_256_GCM_KEY.getBytes(StandardCharsets.UTF_8);
		
		byte[] iv = new byte[16];
		SECURE_RANDOM.nextBytes(iv);

		out.write(iv);

		Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		IvParameterSpec ivSpec = new IvParameterSpec(iv);
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);

		log.info("파일 암호화 스트림 적용 (AES/CBC)...");
		return new CipherOutputStream(out, cipher);
	}
}