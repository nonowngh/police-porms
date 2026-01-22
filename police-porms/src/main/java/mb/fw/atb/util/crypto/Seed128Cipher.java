package mb.fw.atb.util.crypto;
import java.io.UnsupportedEncodingException;


/**
 * SEED algorithm to encrypt or decrypt the data is the class that provides the ability to.
 * @author devhome.tistory.com
 *
 */
public class Seed128Cipher {
	
	/**
	 * SEED encryption algorithm block size
	 */
	private static final int SEED_BLOCK_SIZE = 16;
	private static AnsiX923Padding padding = new AnsiX923Padding();
	
	/**
	 * SEED algorithm to encrypt the data.
	 * @param data Target Data
	 * @param key Masterkey
	 * @param charset Data character set
	 * @return Encrypted data
	 * @throws UnsupportedEncodingException If character is not supported
	 */
	public static byte[] encrypt(byte[] data, byte[] key) {
		
		byte[] encrypt = padding.addPadding(data, SEED_BLOCK_SIZE);
		
		int pdwRoundKey[] = new int[32];
		SEED_KISA.SeedRoundKey(pdwRoundKey, key);
		
		int blockCount = encrypt.length / SEED_BLOCK_SIZE;
		
		for( int i = 0; i < blockCount; i++ ) {
			
			byte sBuffer[] = new byte[SEED_BLOCK_SIZE];
			byte tBuffer[] = new byte[SEED_BLOCK_SIZE];
			
			System.arraycopy(encrypt, (i * SEED_BLOCK_SIZE), sBuffer, 0, SEED_BLOCK_SIZE);
			
			SEED_KISA.SeedEncrypt(sBuffer, pdwRoundKey, tBuffer);
			
			System.arraycopy(tBuffer, 0, encrypt, (i * SEED_BLOCK_SIZE), tBuffer.length);
		}
		
		return encrypt;
	}
	
	/**
	 * ARIA algorithm to decrypt the data.
	 * @param data Target Data
	 * @param key Masterkey
	 * @param keySize Masterkey Size
	 * @param charset Data character set
	 * @return Decrypted data
	 * @throws UnsupportedEncodingException If character is not supported
	 */
	public static byte[] decrypt(byte[] data, byte[] key){
		
		int pdwRoundKey[] = new int[32];
		SEED_KISA.SeedRoundKey(pdwRoundKey, key);
		
		byte[] decrypt = data;
		int blockCount = decrypt.length / SEED_BLOCK_SIZE;
		for( int i = 0; i < blockCount; i++ ) {
			
			byte sBuffer[] = new byte[SEED_BLOCK_SIZE];
			byte tBuffer[] = new byte[SEED_BLOCK_SIZE];
			
			System.arraycopy(decrypt, (i * SEED_BLOCK_SIZE), sBuffer, 0, SEED_BLOCK_SIZE);
			
			SEED_KISA.SeedDecrypt(sBuffer, pdwRoundKey, tBuffer);
			
			System.arraycopy(tBuffer, 0, decrypt, (i * SEED_BLOCK_SIZE), tBuffer.length);
		}
		
			return padding.removePadding(decrypt, SEED_BLOCK_SIZE);
	}
	
}
