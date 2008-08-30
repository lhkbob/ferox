package com.ferox.core.util.io;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.*;
import java.util.Iterator;
import java.util.Map.Entry;

import com.ferox.core.util.IOUtil;


public final class OutputChunk extends Chunk {
	private Chunkable repObject;
	private int refCount;
	
	OutputChunk(IOManager manager, int id, int type, Chunkable object) {
		super(manager, id, type);
		this.repObject = object;
	}
	
	@Override
	Chunkable getRepresentedObject() {
		return this.repObject;
	}

	public boolean setByteArray(String varName, byte[] value) {
		return this.setVariable(varName, value, IOUtil.TYPE_BYTE_ARRAY);
	}
	
	public boolean setByteBuffer(String varName, ByteBuffer value) {
		return this.setVariable(varName, value, IOUtil.TYPE_BYTE_BUFFER);
	}

	public boolean setByte(String varName, byte value) {
		return this.setVariable(varName, Byte.valueOf(value), IOUtil.TYPE_BYTE);
	}

	public boolean setDoubleArray(String varName, double[] value) {
		return this.setVariable(varName, value, IOUtil.TYPE_DOUBLE_ARRAY);
	}
	
	public boolean setDoubleBuffer(String varName, DoubleBuffer value) {
		return this.setVariable(varName, value, IOUtil.TYPE_DOUBLE_BUFFER);
	}
	
	public boolean setDouble(String varName, double value) {
		return this.setVariable(varName, Double.valueOf(value), IOUtil.TYPE_DOUBLE);
	}

	public boolean setFloatArray(String varName, float[] value) {
		return this.setVariable(varName, value, IOUtil.TYPE_FLOAT_ARRAY);
	}
	
	public boolean setFloatBuffer(String varName, FloatBuffer value) {
		return this.setVariable(varName, value, IOUtil.TYPE_FLOAT_BUFFER);
	}
	
	public boolean setFloat(String varName, float value) {
		return this.setVariable(varName, Float.valueOf(value), IOUtil.TYPE_FLOAT);
	}

	public boolean setIntArray(String varName, int[] value) {
		return this.setVariable(varName, value, IOUtil.TYPE_INT_ARRAY);
	}
	
	public boolean setIntBuffer(String varName, IntBuffer value) {
		return this.setVariable(varName, value, IOUtil.TYPE_INT_BUFFER);
	}
	
	public boolean setInt(String varName, int value) {
		return this.setVariable(varName, Integer.valueOf(value), IOUtil.TYPE_INT);
	}

	public boolean setLongArray(String varName, long[] value) {
		return this.setVariable(varName, value, IOUtil.TYPE_BYTE_ARRAY);
	}
	
	public boolean setLongBuffer(String varName, LongBuffer value) {
		return this.setVariable(varName, value, IOUtil.TYPE_LONG_BUFFER);
	}
	
	public boolean setLong(String varName, long value) {
		return this.setVariable(varName, Long.valueOf(value), IOUtil.TYPE_LONG);
	}

	public boolean setShortArray(String varName, short[] value) {
		return this.setVariable(varName, value, IOUtil.TYPE_SHORT_ARRAY);
	}
	
	public boolean setShortBuffer(String varName, ShortBuffer shortBuffer) {
		return this.setVariable(varName, shortBuffer, IOUtil.TYPE_SHORT_BUFFER);
	}
	
	public boolean setShort(String varName, short value) {
		return this.setVariable(varName, Short.valueOf(value), IOUtil.TYPE_SHORT);
	}

	public boolean setCharArray(String varName, char[] value) {
		return this.setVariable(varName, value, IOUtil.TYPE_CHAR_ARRAY);
	}
	
	public boolean setCharBuffer(String varName, CharBuffer value) {
		return this.setVariable(varName, value, IOUtil.TYPE_CHAR_BUFFER);
	}
	
	public boolean setChar(String varName, char value) {
		return this.setVariable(varName, Character.valueOf(value), IOUtil.TYPE_CHAR);
	}

	public boolean setBooleanArray(String varName, boolean[] value) {
		return this.setVariable(varName, value, IOUtil.TYPE_BOOLEAN_ARRAY);
	}
	
	public boolean setBoolean(String varName, boolean value) {
		return this.setVariable(varName, Byte.valueOf((value ? (byte)1 : (byte)0)), IOUtil.TYPE_BOOLEAN);
	}
	
	public boolean setStringArray(String varName, String[] value) {
		return this.setVariable(varName, value, IOUtil.TYPE_STRING_ARRAY);
	}

	public boolean setString(String varName, String value) {
		return this.setVariable(varName, value, IOUtil.TYPE_STRING);
	}
	
	public boolean setBuffer(String varName, Buffer value) {
		if (value instanceof IntBuffer)
			return this.setIntBuffer(varName, (IntBuffer)value);
		else if (value instanceof FloatBuffer)
			return this.setFloatBuffer(varName, (FloatBuffer)value);
		else if (value instanceof DoubleBuffer)
			return this.setDoubleBuffer(varName, (DoubleBuffer)value);
		else if (value instanceof ShortBuffer)
			return this.setShortBuffer(varName, (ShortBuffer)value);
		else if (value instanceof ByteBuffer)
			return this.setByteBuffer(varName, (ByteBuffer)value);
		else if (value instanceof CharBuffer)
			return this.setCharBuffer(varName, (CharBuffer)value);
		else if (value instanceof LongBuffer)
			return this.setLongBuffer(varName, (LongBuffer)value);
		return false;
	}
	
	public boolean setEnum(String varName, Enum<?> value) {
		return this.setString(varName, value.name());
	}
	
	public boolean setObject(String varName, Chunkable value) {
		OutputChunk chunk = null;
		if (value != null)
			chunk = this.manager.addChunk(value);
		boolean val = this.setVariable(varName, chunk, IOUtil.TYPE_OBJECT);
		if (chunk != null) {
			this.manager.pushChunkableOnRefStack(chunk.repObject);
			value.writeChunk(chunk);
			this.manager.popChunkableOffRefStack();
		}
		return val;
	}
	
	void serialize(OutputStream out) throws IOException {
		Iterator<Entry<Integer, Variable>> it = this.compressedVars.entrySet().iterator();
		while(it.hasNext()) 
			it.next().getValue().write(out);
		
		int s = this.uncompressedVars.size();
		for (int i = 0; i < s; i++)
			this.uncompressedVars.get(i).write(out);
	}
	
	int getRefCount() {
		return this.refCount;
	}
	
	void updateReferences() {
		Iterator<Entry<Integer, Variable>> it = this.compressedVars.entrySet().iterator();
		while(it.hasNext()) {
			Variable v = it.next().getValue();
			if (v.valueType == IOUtil.TYPE_OBJECT && v.value != null) 
				((OutputChunk)v.value).refCount++;
		}
		
		int s = this.uncompressedVars.size();
		for (int i = 0; i < s; i++) {
			Variable v = this.uncompressedVars.get(i);
			if (v.valueType == IOUtil.TYPE_OBJECT && v.value != null) 
				((OutputChunk)v.value).refCount++;
		}		
	}
}
