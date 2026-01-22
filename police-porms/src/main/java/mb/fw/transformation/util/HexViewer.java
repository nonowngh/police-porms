package mb.fw.transformation.util;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;

import java.util.Arrays;

/**
 * @author clupine
 * 
 */
public class HexViewer {

	private static final char NULL = '_';
	private static final char LINE_WIDTH = '-';
	private static final char LINE_HEIGHT = '|';

	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		String str = "    20140806101915127.17889337.71556  경기도 남양주시 진접읍 장현리 600-2201                                                                                                    980102106621962.1";
		System.out.println(view(str.getBytes(),15));
		
//		 String hexNumber =  Integer.toHexString(0xff & 0xff); 
//		 System.out.println(hexNumber);


	}

	public static String view(String hexStr) throws Exception {
		return view(ChannelBuffers.wrappedBuffer(hexStr.getBytes()), 20, 30);
	}

	public static String view(String hexStr, int row) throws Exception {
		return view(ChannelBuffers.wrappedBuffer(hexStr.getBytes()), row, 30);
	}
	
	public static String view(String hexStr, int row , int paging) throws Exception {
		return view(ChannelBuffers.wrappedBuffer(hexStr.getBytes()), row, paging);
	}
	
	public static String view(byte[] bytes) throws Exception {
		byte[] dumpByte = ChannelBuffers.hexDump(ChannelBuffers.wrappedBuffer(bytes)).getBytes();
		return view(ChannelBuffers.wrappedBuffer(dumpByte), 20, 30);
	}

	public static String view(byte[] bytes, int row) throws Exception {
		byte[] dumpByte = ChannelBuffers.hexDump(ChannelBuffers.wrappedBuffer(bytes)).getBytes();
		return view(ChannelBuffers.wrappedBuffer(dumpByte), row, 30);
	}

	public static String view(byte[] bytes, int row, int paging) throws Exception {
		byte[] dumpByte = ChannelBuffers.hexDump(ChannelBuffers.wrappedBuffer(bytes)).getBytes();
		return view(ChannelBuffers.wrappedBuffer(dumpByte), row, paging);
	}

	private static String view(ChannelBuffer buff, int row, int block) throws Exception {

		StringBuffer retView = new StringBuffer();
		topLineAdd(row, retView);
		seqAdd(row, retView);
		lineAdd(row, retView);
		dataAdd(buff, retView, row, block);
		lineAdd(row, retView);

		return retView.toString();

	}

	private static void dataAdd(ChannelBuffer buff, StringBuffer retView, int row, int block) throws Exception {

		int lineSeq = 1;

		StringBuffer hexdataLine = null;
		StringBuffer txtdataLine = null;
		int cnt = 0;
		while (true) {

			if (buff.readableBytes() <= 0) {
				break;
			}
			if (lineSeq % (block + 1) == 0) {
				lineAdd(row, retView);
			 	 seqAdd(row, retView);
				lineAdd(row, retView);
			}

			hexdataLine = new StringBuffer();
			txtdataLine = new StringBuffer();
			hexdataLine.append(' ').append(String.format("%04d", lineSeq)).append(' ').append(LINE_HEIGHT).append(' ');
			
			for (int i = 1; i <= row; i++) {
				if (buff.readableBytes() <= 0) {
					if (i < row) {
						char[] blank = new char[((row - i) * 3) + 3];
						Arrays.fill(blank, ' ');
						hexdataLine.append(blank);
					}
					break;
				}

				byte[] oneHexBytes = new byte[2];
				
				try {
					buff.readBytes(oneHexBytes);
				} catch (IndexOutOfBoundsException e) {
					throw new IndexOutOfBoundsException("1 byte is not enough");
				}

				String valueStr = new String(oneHexBytes);
				hexdataLine.append(valueStr).append(' ');
				
				char valueChar = ' ';
				
				try {
					valueChar = (char) (Integer.parseInt(new String(valueStr), 16));
				} catch (NumberFormatException e) {
					throw new NumberFormatException("Invalid hex code : " + new String(valueStr));
				}				
				
				if (valueChar == 0x00) {
					txtdataLine.append(NULL);
				} else {
					txtdataLine.append(valueChar);
				}
				
				cnt++;
			}
			
			retView.append(hexdataLine).append(String.format("%05d", cnt)).append(" ").append(txtdataLine.append("\n"));

			lineSeq++;
		}
	}

	private static void seqAdd(int row, StringBuffer retView) {
		retView.append("sequen").append(LINE_HEIGHT).append(' ');
		for (int i = 1; i <= row; i++) {
			retView.append(String.format("%02d", i)).append(' ');
		}
		retView.append("accum").append(" ");
		for (int i = 1; i <= row; i++) {
			retView.append(i % 10);
		}
		retView.append("\n");
	}

	private static void lineAdd(int row, StringBuffer retView) {
		int hexLineSize = (row * 3) + 7;
		char[] hexLineBytes = new char[hexLineSize];
		Arrays.fill(hexLineBytes, LINE_WIDTH);
		char[] decLineBytes = new char[row + 1];
		Arrays.fill(decLineBytes, LINE_WIDTH);
		retView.append(hexLineBytes).append(" ").append(LINE_WIDTH).append(LINE_WIDTH).append(LINE_WIDTH).append(LINE_WIDTH).append(LINE_WIDTH).append(" ").append(decLineBytes).append("\n");
	}

	private static void topLineAdd(int row, StringBuffer retView) {
		retView.append(LINE_WIDTH).append(LINE_WIDTH).append(" [ Hex Viewer ] ");
		int hexLineSize = (row * 3) + 6 - 17;
		char[] hexLineBytes = new char[hexLineSize];
		Arrays.fill(hexLineBytes, LINE_WIDTH);
		char[] decLineBytes = new char[row + 1];
		Arrays.fill(decLineBytes, LINE_WIDTH);
		retView.append(hexLineBytes).append(" ").append(LINE_WIDTH).append(LINE_WIDTH).append(LINE_WIDTH).append(LINE_WIDTH).append(LINE_WIDTH).append(" ").append(decLineBytes).append("\n");
	}

}
