package mb.fw.atb.util.crypto;
public class AnsiX923Padding implements CryptoPadding {
	private String name = "ANSI-X.923-Padding";

	private final byte PADDING_VALUE = 0x00;

	public byte[] addPadding(byte[] source, int blockSize) {
		int paddingCnt = source.length % blockSize;
		byte[] paddingResult = null;

		if (paddingCnt != 0) {
			paddingResult = new byte[source.length + (blockSize - paddingCnt)];

			System.arraycopy(source, 0, paddingResult, 0, source.length);

			int addPaddingCnt = blockSize - paddingCnt;
			for (int i = 0; i < addPaddingCnt; i++) {
				paddingResult[source.length + i] = PADDING_VALUE;
			}

			paddingResult[paddingResult.length - 1] = (byte) addPaddingCnt;
		} else {
			paddingResult = source;
		}

		return paddingResult;
	}

	public byte[] removePadding(byte[] source, int blockSize) {
		byte[] paddingResult = null;
		boolean isPadding = false;

		int lastValue = source[source.length - 1];
		if (lastValue < (blockSize - 1)) {
			int zeroPaddingCount = lastValue - 1;

			for (int i = 2; i < (zeroPaddingCount + 2); i++) {
				if (source[source.length - i] != PADDING_VALUE) {
					isPadding = false;
					break;
				}
			}

			isPadding = true;
		} else {
			isPadding = false;
		}

		if (isPadding && lastValue <= 1) { // minus, 0, 1
			isPadding = false;
		}

		if (isPadding) {
			for (int index = source.length - lastValue; index < source.length - 1; index++) {
				if (source[index] != (byte) 0) {
					isPadding = false;
					break;
				}
			}
		}

		if (isPadding) {
			paddingResult = new byte[source.length - lastValue];
			try {
				System.arraycopy(source, 0, paddingResult, 0, paddingResult.length);
			} catch (ArrayIndexOutOfBoundsException ex) {
				System.out.println("removePadding Exception.....");
				return source;
			}
		} else {
			paddingResult = source;
		}

		return paddingResult;
	}

	public String getName() {
		return name;
	}

	public void print(byte[] data) {
		StringBuffer buffer = new StringBuffer();

		buffer.append("[").append(data.length).append("] ");

		for (int i = 0; i < data.length; i++) {
			buffer.append(data[i]).append(" ");
		}

		System.out.println(buffer.toString());
	}
}