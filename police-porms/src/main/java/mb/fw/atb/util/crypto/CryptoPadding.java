package mb.fw.atb.util.crypto;
public interface CryptoPadding {

	public byte[] addPadding(byte[] source, int blockSize);

	public byte[] removePadding(byte[] source, int blockSize);

}
