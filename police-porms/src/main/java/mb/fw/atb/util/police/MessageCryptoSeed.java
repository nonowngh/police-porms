package mb.fw.atb.util.police;

import mb.fw.atb.util.crypto.Seed128Cipher;

public class MessageCryptoSeed {
	private String key;
	
	public MessageCryptoSeed(String key) {
		this.key = key;
	}
	
	public byte[] encode(String message) throws Exception{
		return Seed128Cipher.encrypt(message.getBytes(),key.getBytes());
	}
	
	public byte[] decode(byte[] message) throws Exception{
		return Seed128Cipher.decrypt(message,key.getBytes());
	}

	public byte[] getKey(){
		return key.getBytes();
	}
}
