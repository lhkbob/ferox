package com.ferox.core.util;

import java.io.IOException;

public class ByteOrderUtil {
	public static int swapInt(int in) {
		byte[] b = new byte[4];
		IOUtil.convertInt(in, b, 0);
		return ((b[0] & 0xff) | 
				((b[1] & 0xff) << 8) | 
				((b[2] & 0xff) << 16) | 
				((b[3] & 0xff) << 24));
	}
	
	public static short swapShort(short in) {
		byte[] b = new byte[2];
		IOUtil.convertShort(in, b, 0);
		return (short)((b[0] & 0xff) | ((b[1] & 0xff) << 8));
	}
	
	public static char swapChar(char in) {
		return (char)swapShort((short)in);
	}
	
	public static long swapLong(long in) {
		byte[] b = new byte[8];
		IOUtil.convertLong(in, b, 0);
		return (((long)(b[0] & 0xff) << 0) |
				((long)(b[1] & 0xff) << 8) |
				((long)(b[2] & 0xff) << 16) |
				((long)(b[3] & 0xff) << 24) |
				((long)(b[4] & 0xff) << 32) |
				((long)(b[5] & 0xff) << 40) |
				((long)(b[6] & 0xff) << 48) |
				((long)(b[7] & 0xff) << 56));
	}
	
	public static float swapFloat(float in) throws IOException {
		return Float.intBitsToFloat(swapInt(Float.floatToIntBits(in)));
	}
	
	public static double readDouble(double in) throws IOException {
		return Double.longBitsToDouble(swapLong(Double.doubleToLongBits(in)));
	}
}
