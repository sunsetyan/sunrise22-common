package com.sunrise22.common.encrypt;

import java.util.Arrays;

/**
 * 加密常用类之 Base64 的整理
 * 
 * 代码示例：
 * 
 *	String s = "123qwa11s";		
 *	String encodeS = Base64Builder.encode(s);		
 *	System.out.println(encodeS);
 *	System.out.println(Base64Builder.decode(encodeS));
 *
 */
public class Base64Builder {
	
	private static final char[] ALPHAS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
			.toCharArray();
	/** 表示上述字符的编码。 */
	private static final int[] ALPHA_ASCII = new int[256];
	static {
		Arrays.fill(ALPHA_ASCII, -1);
		// 因为ALPHAS中的字符全部编码在0~256之间了，可以表示为下标 来个汉字就超过标记了。
		for (int i = 0; i < ALPHAS.length; i++)
			ALPHA_ASCII[ALPHAS[i]] = i;
		ALPHA_ASCII['='] = 0;
	}
	
	// ****************************************************************************************
	// * char[] version
	// ****************************************************************************************
	/**
	 * Encodes a raw byte array into a BASE64 <code>char[]</code>
	 * representation i accordance with RFC 2045.
	 * 
	 * @param toCode
	 *            The bytes to convert. If <code>null</code> or length 0 an
	 *            empty array will be returned.
	 * @param lineSep
	 *            Optional "\r\n" after 76 characters, unless end of file.<br>
	 *            No line separator will be in breach of RFC 2045 which
	 *            specifies max 76 per line but will be a little faster.
	 * @return A BASE64 encoded array. Never <code>null</code>.
	 */
	public final static char[] encodeToChar(byte[] toCode, boolean lineSep) {
		// 特殊情况
		int sLen = toCode != null ? toCode.length : 0;
		if (sLen == 0) 
			return new char[0];
		
		int even24Length = (sLen / 3) * 3; 
		int charSum = ((sLen - 1) / 3 + 1) << 2; 
		int encodeLength = charSum + (lineSep ? (charSum - 1) / 76 << 1 : 0);
		char[] result = new char[encodeLength];
		
		// Encode even 24-bits
		for (int s = 0, d = 0, cc = 0; s < even24Length;) {
			// Copy next three bytes into lower 24 bits of int, paying attension
			// to sign.
			int i = (toCode[s++] & 0xff) << 16 | (toCode[s++] & 0xff) << 8
					| (toCode[s++] & 0xff);

			// Encode the int into four chars
			result[d++] = ALPHAS[(i >>> 18) & 0x3f];
			result[d++] = ALPHAS[(i >>> 12) & 0x3f];
			result[d++] = ALPHAS[(i >>> 6) & 0x3f];
			result[d++] = ALPHAS[i & 0x3f];

			// Add optional line separator
			if (lineSep && ++cc == 19 && d < encodeLength - 2) {
				result[d++] = '\r';
				result[d++] = '\n';
				cc = 0;
			}
		}

		// Pad and encode last bits if source isn't even 24 bits.
		int left = sLen - even24Length; // 0 - 2.
		if (left > 0) {
			// Prepare the int
			int i = ((toCode[even24Length] & 0xff) << 10)
					| (left == 2 ? ((toCode[sLen - 1] & 0xff) << 2) : 0);

			// Set last four chars
			result[encodeLength - 4] = ALPHAS[i >> 12];
			result[encodeLength - 3] = ALPHAS[(i >>> 6) & 0x3f];
			result[encodeLength - 2] = left == 2 ? ALPHAS[i & 0x3f] : '=';
			result[encodeLength - 1] = '=';
		}
		return result;
	}
	
	public static void main(String[] args) {
		for(int key : ALPHA_ASCII)
			System.out.print(key + " ");
		System.out.println();
		
		for (int i = 0; i < ALPHA_ASCII.length; i++)
			System.out.print(ALPHA_ASCII[i] + " ");
		System.out.println();
	}

}
