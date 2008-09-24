package com.ferox.core.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.*;


public class IOUtil {
	public static final byte TYPE_NULL = -1;
	
	public static final byte TYPE_OBJECT = 0;
	public static final byte TYPE_OBJECT_ARRAY = 26;
	
	public static final byte TYPE_STRING = 1;
	public static final byte TYPE_STRING_ARRAY = 2;
	
	public static final byte TYPE_INT = 3;
	public static final byte TYPE_INT_ARRAY = 4;
	public static final byte TYPE_INT_BUFFER = 5;
	
	public static final byte TYPE_SHORT = 6;
	public static final byte TYPE_SHORT_ARRAY = 7;
	public static final byte TYPE_SHORT_BUFFER = 8;
	
	public static final byte TYPE_LONG = 9;
	public static final byte TYPE_LONG_ARRAY = 10;
	public static final byte TYPE_LONG_BUFFER = 11;
	
	public static final byte TYPE_BYTE = 12;
	public static final byte TYPE_BYTE_ARRAY = 13;
	public static final byte TYPE_BYTE_BUFFER = 14;
	
	public static final byte TYPE_CHAR = 15;
	public static final byte TYPE_CHAR_ARRAY = 16;
	public static final byte TYPE_CHAR_BUFFER = 17;
	
	public static final byte TYPE_FLOAT = 18;
	public static final byte TYPE_FLOAT_ARRAY = 19;
	public static final byte TYPE_FLOAT_BUFFER = 20;
	
	public static final byte TYPE_DOUBLE = 21;
	public static final byte TYPE_DOUBLE_ARRAY = 22;
	public static final byte TYPE_DOUBLE_BUFFER = 23;
	
	public static final byte TYPE_BOOLEAN = 24;
	public static final byte TYPE_BOOLEAN_ARRAY = 25;
	
	public static void convertShort(short s, byte[] out, int offset) {
		out[offset + 0] = (byte)(s >> 8);
		out[offset + 1] = (byte)(s);
	}
	
	public static void convertInt(int i, byte[] out, int offset) {
		out[offset + 0] = (byte)(i >> 24);
		out[offset + 1] = (byte)(i >> 16);
		out[offset + 2] = (byte)(i >> 8);
		out[offset + 3] = (byte)(i);
	}
	
	public static void convertLong(long l, byte[] out, int offset) {
		out[offset + 0] = (byte)(l >> 56);
		out[offset + 1] = (byte)(l >> 48);
		out[offset + 2] = (byte)(l >> 40);
		out[offset + 3] = (byte)(l >> 32);
		out[offset + 4] = (byte)(l >> 24);
		out[offset + 5] = (byte)(l >> 16);
		out[offset + 6] = (byte)(l >> 8);
		out[offset + 7] = (byte)(l);
	}
	
	public static void convertFloat(float f, byte[] out, int offset) {
		convertInt(Float.floatToIntBits(f), out, offset);
	}
	
	public static void convertDouble(double d, byte[] out, int offset) {
		convertLong(Double.doubleToLongBits(d), out, offset);
	}
	
	public static void convertChar(char c, byte[] out, int offset) {
		convertShort((short)c, out, offset);
	}
	
	public static short unconvertShort(byte[] in, int offset) {
		return (short)(((in[offset] & 0xff) << 8) + (in[offset + 1] & 0xff));
	}
	
	public static char unconvertChar(byte[] in, int offset) {
		return (char)unconvertShort(in, offset);
	}
	
	public static int unconvertInt(byte[] in, int offset) {
		return (((in[offset] & 0xff) << 24) + 
					 ((in[offset + 1] & 0xff) << 16) +
					 ((in[offset + 2] & 0xff) << 8) +
					 ((in[offset + 3] & 0xff)));
	}
	
	public static long unconvertLong(byte[] in, int offset) {
		return ((((long)(in[offset] & 0xff)) << 56) +
				 	  (((long)(in[offset + 1] & 0xff)) << 48) +
				 	  (((long)(in[offset + 2] & 0xff)) << 40) +
				 	  (((long)(in[offset + 3] & 0xff)) << 32) +
				 	  (((long)(in[offset + 4] & 0xff)) << 24) +
				 	  (((long)(in[offset + 5] & 0xff)) << 16) +
				 	  (((long)(in[offset + 6] & 0xff)) << 8) + 
				 	  (((in[offset + 7] & 0xff))));
	}
	
	public static float unconvertFloat(byte[] in, int offset) {
		return Float.intBitsToFloat(unconvertInt(in, offset));
	}
	
	public static double unconvertDouble(byte[] in, int offset) {
		return Double.longBitsToDouble(unconvertLong(in, offset));
	}
	
	public static boolean readBoolean(InputStream in) throws IOException {
		return readByte(in) != 0;
	}
	
	public static byte readByte(InputStream in) throws IOException {
		byte[] b = new byte[1];
		readAll(in, b);
		return b[0];
	}
	
	public static short readShort(InputStream in) throws IOException {
		byte[] b = new byte[2];
		readAll(in, b);
		return unconvertShort(b, 0);
	}
	
	public static int readInt(InputStream in) throws IOException {
		byte[] b = new byte[4];
		readAll(in, b);
		return unconvertInt(b, 0);
	}
	
	public static long readLong(InputStream in) throws IOException {
		byte[] b = new byte[8];
		readAll(in, b);
		return unconvertLong(b, 0);
	}
	
	public static char readChar(InputStream in) throws IOException {
		byte[] b = new byte[2];
		readAll(in, b);
		return unconvertChar(b, 0);
	}
	
	public static float readFloat(InputStream in) throws IOException {
		byte[] b = new byte[4];
		readAll(in, b);
		return unconvertFloat(b, 0);
	}
	
	public static double readDouble(InputStream in) throws IOException {
		byte[] b = new byte[8];
		readAll(in, b);
		return unconvertDouble(b, 0);
	}
	
	public static String readString(InputStream in) throws IOException {
		return new String(readCharArray(in));
	}
	
	public static byte[] readByteArray(InputStream in) throws IOException {
		byte[] b = new byte[readInt(in)];
		readAll(in, b);
		return b;
	}
	
	public static void readAll(InputStream in, byte[] array) throws IOException {
		int remaining = array.length;
		int offset = 0;
		int read = 0;
		while (remaining > 0) {
			read = in.read(array, offset, remaining);
			if (read < 0)
				throw new IOException("End of stream unexpected");
			offset += read;
			remaining -= read;
		}
	}
	
	public static short[] readShortArray(InputStream in) throws IOException {
		int c = readInt(in);
		byte[] b = new byte[c << 1];
		readAll(in, b);
		
		short[] bb = new short[c];
		for (int i = 0; i < c; i++)
			bb[i] = unconvertShort(b, i << 1);
		return bb;
	}
	
	public static int[] readIntArray(InputStream in) throws IOException {
		int c = readInt(in);
		byte[] b = new byte[c << 2];
		readAll(in, b);
		
		int[] bb = new int[c];
		for (int i = 0; i < c; i++)
			bb[i] = unconvertInt(b, i << 2);
		return bb;
	}
	
	public static long[] readLongArray(InputStream in) throws IOException {
		int c = readInt(in);
		byte[] b = new byte[c << 3];
		readAll(in, b);
		
		long[] bb = new long[c];
		for (int i = 0; i < c; i++)
			bb[i] = unconvertLong(b, i << 3);
		return bb;
	}
	
	public static float[] readFloatArray(InputStream in) throws IOException {
		int c = readInt(in);
		byte[] b = new byte[c << 2];
		readAll(in, b);
		
		float[] bb = new float[c];
		for (int i = 0; i < c; i++)
			bb[i] = unconvertFloat(b, i << 2);
		return bb;
	}
	
	public static double[] readDoubleArray(InputStream in) throws IOException {
		int c = readInt(in);
		byte[] b = new byte[c << 3];
		readAll(in, b);
		
		double[] bb = new double[c];
		for (int i = 0; i < c; i++)
			bb[i] = unconvertDouble(b, i << 3);
		return bb;
	}
	
	public static char[] readCharArray(InputStream in) throws IOException {
		int c = readInt(in);
		byte[] b = new byte[c << 1];
		readAll(in, b);
		
		char[] bb = new char[c];
		for (int i = 0; i < c; i++)
			bb[i] = unconvertChar(b, i << 1);
		return bb;
	}
	
	public static boolean[] readBooleanArray(InputStream in) throws IOException {
		int c = readInt(in);
		byte[] b = new byte[c];
		readAll(in, b);
		
		boolean[] bb = new boolean[c];
		for (int i = 0; i < c; i++)
			bb[i] = b[i] != 0;
		return bb;
	}
	
	public static String[] readStringArray(InputStream in) throws IOException {
		String[] b = new String[readInt(in)];
		for (int i = 0; i < b.length; i++)
			b[i] = readString(in);
		return b;
	}
	
	public static ByteBuffer readByteBuffer(InputStream in) throws IOException {
		boolean direct = readBoolean(in);
		ByteBuffer b = BufferUtil.newByteBuffer(readInt(in), direct, false);
		
		byte[] bb = new byte[b.capacity()];
		readAll(in, bb);
		b.put(bb);
		return b;
	}
	
	public static ShortBuffer readShortBuffer(InputStream in) throws IOException {
		boolean direct = readBoolean(in);
		int c = readInt(in);
		ShortBuffer b = BufferUtil.newShortBuffer(c, direct, false);
		
		byte[] bb = new byte[c << 1];
		readAll(in, bb);
		for (int i = 0; i < c; i++) 
			b.put(i, unconvertShort(bb, i << 1));
		return b;
	}
	
	public static IntBuffer readIntBuffer(InputStream in) throws IOException {
		boolean direct = readBoolean(in);
		int c = readInt(in);
		IntBuffer b = BufferUtil.newIntBuffer(c, direct, false);
		
		byte[] bb = new byte[c << 2];
		readAll(in, bb);
		for (int i = 0; i < c; i++) 
			b.put(i, unconvertInt(bb, i << 2));
		return b;
	}
	
	public static LongBuffer readLongBuffer(InputStream in) throws IOException {
		boolean direct = readBoolean(in);
		int c = readInt(in);
		LongBuffer b = BufferUtil.newLongBuffer(c, direct, false);
		
		byte[] bb = new byte[c << 3];
		readAll(in, bb);
		for (int i = 0; i < c; i++) 
			b.put(i, unconvertLong(bb, i << 3));
		return b;
	}
	
	public static FloatBuffer readFloatBuffer(InputStream in) throws IOException {
		boolean direct = readBoolean(in);
		int c = readInt(in);
		FloatBuffer b = BufferUtil.newFloatBuffer(c, direct, false);
		
		byte[] bb = new byte[c << 2];
		readAll(in, bb);
		for (int i = 0; i < c; i++) 
			b.put(i, unconvertFloat(bb, i << 2));
		return b;
	}
	
	public static DoubleBuffer readDoubleBuffer(InputStream in) throws IOException {
		boolean direct = readBoolean(in);
		int c = readInt(in);
		DoubleBuffer b = BufferUtil.newDoubleBuffer(c, direct, false);
		
		byte[] bb = new byte[c << 3];
		readAll(in, bb);
		for (int i = 0; i < c; i++) 
			b.put(i, unconvertDouble(bb, i << 3));
		return b;
	}
	
	public static CharBuffer readCharBuffer(InputStream in) throws IOException {
		boolean direct = readBoolean(in);
		int c = readInt(in);
		CharBuffer b = BufferUtil.newCharBuffer(c, direct, false);
		
		byte[] bb = new byte[c << 1];
		readAll(in, bb);
		for (int i = 0; i < c; i++) 
			b.put(i, unconvertChar(bb, i << 1));
		return b;
	}
	
	public static void write(OutputStream out, boolean b) throws IOException {
		write(out, (b ? (byte)1 : (byte)0));
	}
	
	public static void write(OutputStream out, byte b) throws IOException {
		out.write(b);
	}
	
	public static void write(OutputStream out, short s) throws IOException {
		byte[] b = new byte[2];
		convertShort(s, b, 0);
		out.write(b);
	}
	
	public static void write(OutputStream out, int i) throws IOException {
		byte[] b = new byte[4];
		convertInt(i, b, 0);
		out.write(b);
	}
	
	public static void write(OutputStream out, long i) throws IOException {
		byte[] b = new byte[8];
		convertLong(i, b, 0);
		out.write(b);
	}
	
	public static void write(OutputStream out, float i) throws IOException {
		byte[] b = new byte[4];
		convertFloat(i, b, 0);
		out.write(b);
	}
	
	public static void write(OutputStream out, double i) throws IOException {
		byte[] b = new byte[8];
		convertDouble(i, b, 0);
		out.write(b);
	}
	
	public static void write(OutputStream out, char i) throws IOException {
		byte[] b = new byte[2];
		convertChar(i, b, 0);
		out.write(b);
	}
	
	public static void write(OutputStream out, String s) throws IOException {
		write(out, s.toCharArray());
	}
	
	public static void write(OutputStream out, byte[] b) throws IOException{
		write(out, b.length);
		out.write(b);
	}
	
	public static void write(OutputStream out, int[] b) throws IOException {
		byte[] bb = new byte[b.length << 2]; // * 4
		for (int i = 0; i < b.length; i++)
			convertInt(b[i], bb, (i << 2));
		
		write(out, b.length);
		out.write(bb);
	}
	
	public static void write(OutputStream out, short[] b) throws IOException {
		byte[] bb = new byte[b.length << 1]; // * 2
		for (int i = 0; i < b.length; i++)
			convertShort(b[i], bb, (i << 1));
		
		write(out, b.length);
		out.write(bb);
	}
	
	public static void write(OutputStream out, long[] b) throws IOException {
		byte[] bb = new byte[b.length << 3]; // * 8
		for (int i = 0; i < b.length; i++)
			convertLong(b[i], bb, (i << 3));
		
		write(out, b.length);
		out.write(bb);
	}
	
	public static void write(OutputStream out, char[] b) throws IOException {
		byte[] bb = new byte[b.length << 1]; // * 2
		for (int i = 0; i < b.length; i++)
			convertChar(b[i], bb, (i << 1));
		
		write(out, b.length);
		out.write(bb);
	}
	
	public static void write(OutputStream out, float[] b) throws IOException {
		byte[] bb = new byte[b.length << 2]; // * 4
		for (int i = 0; i < b.length; i++)
			convertFloat(b[i], bb, (i << 2));
		
		write(out, b.length);
		out.write(bb);
	}
	
	public static void write(OutputStream out, double[] b) throws IOException {
		byte[] bb = new byte[b.length << 3]; // * 8
		for (int i = 0; i < b.length; i++)
			convertDouble(b[i], bb, (i << 3));
		
		write(out, b.length);
		out.write(bb);
	}
	
	public static void write(OutputStream out, boolean[] b) throws IOException {
		byte[] bb = new byte[b.length];
		for (int i = 0; i < b.length; i++)
			bb[i] = (b[i] ? (byte)1 : (byte)0);
		
		write(out, b.length);
		out.write(bb);
	}
	
	public static void write(OutputStream out, String[] s) throws IOException {
		write(out, s.length);
		for (int i = 0; i < s.length; i++)
			write(out, s[i]);
	}
	
	public static void write(OutputStream out, ByteBuffer b) throws IOException {
		int c = b.capacity();
		byte[] bb = new byte[c];
		for (int i = 0; i < c; i++)
			bb[i] = b.get(i);
		
		write(out, b.isDirect());
		write(out, c);
		out.write(bb);
	}
	
	public static void write(OutputStream out, IntBuffer b) throws IOException {
		int c = b.capacity();
		byte[] bb = new byte[c << 2]; // * 4;
		for (int i = 0; i < c; i++)
			convertInt(b.get(i), bb, (i << 2));
		
		write(out, b.isDirect());
		write(out, c);
		out.write(bb);
	}
	
	public static void write(OutputStream out, CharBuffer b) throws IOException {
		int c = b.capacity();
		byte[] bb = new byte[c << 1]; // * 2;
		for (int i = 0; i < c; i++)
			convertChar(b.get(i), bb, (i << 1));
		
		write(out, b.isDirect());
		write(out, c);
		out.write(bb);
	}
	
	public static void write(OutputStream out, ShortBuffer b) throws IOException {
		int c = b.capacity();
		byte[] bb = new byte[c << 1]; // * 2;
		for (int i = 0; i < c; i++)
			convertShort(b.get(i), bb, (i << 1));
		
		write(out, b.isDirect());
		write(out, c);
		out.write(bb);
	}
	
	public static void write(OutputStream out, LongBuffer b) throws IOException {
		int c = b.capacity();
		byte[] bb = new byte[c << 3]; // * 8;
		for (int i = 0; i < c; i++)
			convertLong(b.get(i), bb, (i << 3));
		
		write(out, b.isDirect());
		write(out, c);
		out.write(bb);
	}
	
	public static void write(OutputStream out, FloatBuffer b) throws IOException {
		int c = b.capacity();
		byte[] bb = new byte[c << 2]; // * 4;
		for (int i = 0; i < c; i++)
			convertFloat(b.get(i), bb, (i << 2));
		
		write(out, b.isDirect());
		write(out, c);
		out.write(bb);
	}
	
	public static void write(OutputStream out, DoubleBuffer b) throws IOException {
		int c = b.capacity();
		byte[] bb = new byte[c << 3]; // * 8;
		for (int i = 0; i < c; i++)
			convertDouble(b.get(i), bb, (i << 3));
		
		write(out, b.isDirect());
		write(out, c);
		out.write(bb);
	}
}
