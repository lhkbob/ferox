package com.ferox.core.util;

import java.nio.*;

public class BufferUtil {
	public static final int BYTESIZE_BYTE = 1;
	public static final int BYTESIZE_INT = 4;
	public static final int BYTESIZE_FLOAT = 4;
	public static final int BYTESIZE_SHORT = 2;
	public static final int BYTESIZE_DOUBLE = 8;
	public static final int BYTESIZE_LONG = 8;
	public static final int BYTESIZE_CHAR = 2;
	
	public static ByteBuffer newByteBuffer(int size, boolean direct, boolean readOnly) {
		ByteBuffer data;
		if (direct)
			data = ByteBuffer.allocateDirect(size);
		else
			data = ByteBuffer.allocate(size);
		data.order(java.nio.ByteOrder.nativeOrder());
		if (readOnly)
			return data.asReadOnlyBuffer();
		return data;
	}
	
	public static IntBuffer newIntBuffer(int size, boolean direct, boolean readOnly) {
		if (direct)
			return newByteBuffer(size * BYTESIZE_INT, direct, readOnly).asIntBuffer();
		else {
			IntBuffer data = IntBuffer.allocate(size);
			if (readOnly)
				return data.asReadOnlyBuffer();
			return data;
		}
	}
	
	public static ShortBuffer newShortBuffer(int size, boolean direct, boolean readOnly) {
		if (direct)
			return newByteBuffer(size * BYTESIZE_SHORT, direct, readOnly).asShortBuffer();
		else {
			ShortBuffer data = ShortBuffer.allocate(size);
			if (readOnly)
				return data.asReadOnlyBuffer();
			return data;
		}
	}
	
	public static LongBuffer newLongBuffer(int size, boolean direct, boolean readOnly) {
		if (direct)
			return newByteBuffer(size * BYTESIZE_LONG, direct, readOnly).asLongBuffer();
		else {
			LongBuffer data = LongBuffer.allocate(size);
			if (readOnly)
				return data.asReadOnlyBuffer();
			return data;
		}
	}
	
	public static FloatBuffer newFloatBuffer(int size, boolean direct, boolean readOnly) {
		if (direct)
			return newByteBuffer(size * BYTESIZE_FLOAT, direct, readOnly).asFloatBuffer();
		else {
			FloatBuffer data = FloatBuffer.allocate(size);
			if (readOnly)
				return data.asReadOnlyBuffer();
			return data;
		}
	}
	
	public static DoubleBuffer newDoubleBuffer(int size, boolean direct, boolean readOnly) {
		if (direct)
			return newByteBuffer(size * BYTESIZE_DOUBLE, direct, readOnly).asDoubleBuffer();
		else {
			DoubleBuffer data = DoubleBuffer.allocate(size);
			if (readOnly)
				return data.asReadOnlyBuffer();
			return data;
		}
	}
	
	public static CharBuffer newCharBuffer(int size, boolean direct, boolean readOnly) {
		if (direct)
			return newByteBuffer(size * BYTESIZE_CHAR, direct, readOnly).asCharBuffer();
		else {
			CharBuffer data = CharBuffer.allocate(size);
			if (readOnly)
				return data.asReadOnlyBuffer();
			return data;
		}
	}
	
	public static ByteBuffer newByteBuffer(int size) {
		return newByteBuffer(size, true, false);
	}
	
	public static IntBuffer newIntBuffer(int size) {
		return newIntBuffer(size, true, false);
	}
	
	public static ShortBuffer newShortBuffer(int size) {
		return newShortBuffer(size, true, false);
	}
	
	public static FloatBuffer newFloatBuffer(int size) {
		return newFloatBuffer(size, true, false);
	}
	
	public static DoubleBuffer newDoubleBuffer(int size) {
		return newDoubleBuffer(size, true, false);
	}
	
	public static LongBuffer newLongBuffer(int size) {
		return newLongBuffer(size, true, false);
	}
	
	public static CharBuffer newCharBuffer(int size) {
		return newCharBuffer(size, true, false);
	}
	
	public static int getBufferByteSize(Buffer buffer) {
		if (buffer instanceof FloatBuffer)
			return BYTESIZE_FLOAT;
		else if (buffer instanceof IntBuffer)
			return BYTESIZE_INT;
		else if (buffer instanceof ByteBuffer)
			return BYTESIZE_BYTE;
		else if (buffer instanceof DoubleBuffer)
			return BYTESIZE_DOUBLE;
		else if (buffer instanceof ShortBuffer)
			return BYTESIZE_SHORT;
		else if (buffer instanceof LongBuffer)
			return BYTESIZE_LONG;
		else if (buffer instanceof CharBuffer)
			return BYTESIZE_CHAR;
		else
			return 0;
	}
}
