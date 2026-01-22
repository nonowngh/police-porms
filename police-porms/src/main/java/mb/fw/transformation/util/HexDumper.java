/*
 * Copyright 2007 KIBO.  All rights reserved.  Use is subjected 
 * to license terms.

 * <file description>
 *
 * $Id: HexDumper.java,v 1.1 2012/03/30 01:12:28 mci Exp $
 */
package mb.fw.transformation.util;

import java.nio.ByteBuffer;

public class HexDumper {
	private static final byte[] highDigits;

	private static final byte[] lowDigits;

	// initialize lookup tables
	static {
		final byte[] digits = { '0', '1', '2', '3', '4', '5', '6', '7', '8',
				'9', 'A', 'B', 'C', 'D', 'E', 'F' };

		int i;
		byte[] high = new byte[256];
		byte[] low = new byte[256];

		for (i = 0; i < 256; i++) {
			high[i] = digits[i >>> 4];
			low[i] = digits[i & 0x0F];
		}

		highDigits = high;
		lowDigits = low;
	}

	public static String dump(ByteBuffer in) {
		int size = in.remaining();

		if (size == 0) {
			return "empty";
		}

		StringBuffer out = new StringBuffer((in.remaining() * 3) - 1);

		int mark = in.position();

		// fill the first
		int byteValue = in.get() & 0xFF;
		out.append((char) highDigits[byteValue]);
		out.append((char) lowDigits[byteValue]);
		size--;

		// and the others, too
		for (; size > 0; size--) {
			out.append(' ');
			byteValue = in.get() & 0xFF;
			out.append((char) highDigits[byteValue]);
			out.append((char) lowDigits[byteValue]);
		}

		in.position(mark);

		return out.toString();
	}

	public static String dump(byte[] in) {
		if (in == null) {
			return "empty";
		}

		int size = in.length;

		if (size == 0) {
			return "empty";
		}

		StringBuffer out = new StringBuffer((size * 3) - 1);

		// fill the first
		int byteValue = in[0] & 0xFF;
		out.append((char) highDigits[byteValue]);
		out.append((char) lowDigits[byteValue]);
		size--;

		// and the others, too
		for (int i = 1; size > 0; size--, i++) {
			out.append(' ');
			byteValue = in[i] & 0xFF;
			out.append((char) highDigits[byteValue]);
			out.append((char) lowDigits[byteValue]);
		}

		return out.toString();
	}
}