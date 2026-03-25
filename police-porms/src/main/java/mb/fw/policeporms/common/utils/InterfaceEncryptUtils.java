package mb.fw.policeporms.common.utils;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InterfaceEncryptUtils {

	// 암호화용 시큐어 랜덤 (스레드 세이프하므로 재사용)
	private static final SecureRandom SECURE_RANDOM = new SecureRandom();
	
	protected static final String FILE_AES_256_GCM_KEY = "2026POLICEPORMSPRODUCTCOMMONKEY!";

	/**
	 * 파일 복호화 스트림 생성
	 */
	public static InputStream createFileDecryptInputStream(InputStream in) throws Exception {
		// 1. IV 12바이트 추출
		byte[] iv = new byte[12];
		int readIvSize = in.read(iv);
		if (readIvSize < 12) {
			throw new IOException("암호화 파일의 IV를 읽을 수 없습니다.");
		}

		// TODO: 운영 환경에서는 키를 별도 관리(application.yml 등) 하세요.
		byte[] keyBytes = FILE_AES_256_GCM_KEY.getBytes(StandardCharsets.UTF_8);

		javax.crypto.Cipher cipher = javax.crypto.Cipher.getInstance("AES/GCM/NoPadding");
		javax.crypto.spec.SecretKeySpec keySpec = new javax.crypto.spec.SecretKeySpec(keyBytes, "AES");
		javax.crypto.spec.GCMParameterSpec gcmSpec = new javax.crypto.spec.GCMParameterSpec(128, iv);
		cipher.init(javax.crypto.Cipher.DECRYPT_MODE, keySpec, gcmSpec);

		log.info("파일 복호화 스트림 적용...");
		return new javax.crypto.CipherInputStream(in, cipher);
	}

	/**
	 * 파일 암호화 스트림 생성
	 */
	public static OutputStream createFileEncryptOutputStream(OutputStream out) throws Exception {
		byte[] keyBytes = FILE_AES_256_GCM_KEY.getBytes(StandardCharsets.UTF_8);
		byte[] iv = new byte[12];
		SECURE_RANDOM.nextBytes(iv);

		// 복호화를 위해 파일 최상단에 IV 기록
		out.write(iv);

		Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
		SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");
		GCMParameterSpec gcmSpec = new GCMParameterSpec(128, iv);
		cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

		log.info("파일 암호화 스트림 적용...");
		return new CipherOutputStream(out, cipher);
	}
}
