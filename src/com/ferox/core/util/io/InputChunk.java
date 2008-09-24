package com.ferox.core.util.io;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;


public class InputChunk extends Chunk {
	private Chunkable toImport;
	private boolean constructed;

	public InputChunk(IOManager manager, Class<? extends Chunkable> chunkType) {
		super(manager, chunkType);
		this.toImport = null;
		this.constructed = false;
	}
	
	public Buffer getBuffer(String varName) throws VariableAccessException {
		try {
			return (Buffer)this.getVariable(varName, VariableType.BYTE_BUFFER).getValue();
		} catch(VariableAccessException e) { }
		try {
			return (Buffer)this.getVariable(varName, VariableType.SHORT_BUFFER).getValue();
		} catch(VariableAccessException e) { }
		try {
			return (Buffer)this.getVariable(varName, VariableType.INT_BUFFER).getValue();
		} catch(VariableAccessException e) { }
		try {
			return (Buffer)this.getVariable(varName, VariableType.LONG_BUFFER).getValue();
		} catch(VariableAccessException e) { }
		try {
			return (Buffer)this.getVariable(varName, VariableType.CHAR_BUFFER).getValue();
		} catch(VariableAccessException e) { }
		try {
			return (Buffer)this.getVariable(varName, VariableType.FLOAT_BUFFER).getValue();
		} catch(VariableAccessException e) { }
		try {
			return (Buffer)this.getVariable(varName, VariableType.DOUBLE_BUFFER).getValue();
		} catch(VariableAccessException e) { }
		throw new VariableAccessException("Variable doesn't exist, or variable is of the wrong type");
	}
	
	public byte[] getByteArray(String varName) throws VariableAccessException {
		return (byte[])this.getVariable(varName, VariableType.BYTE_ARRAY).getValue();
	}
	
	public ByteBuffer getByteBuffer(String varName) throws VariableAccessException {
		return (ByteBuffer)this.getVariable(varName, VariableType.BYTE_BUFFER).getValue();
	}

	public byte getByte(String varName) throws VariableAccessException {
		return ((Number)this.getVariable(varName, VariableType.BYTE).getValue()).byteValue();
	}

	public double[] getDoubleArray(String varName) throws VariableAccessException {
		return (double[])this.getVariable(varName, VariableType.DOUBLE_ARRAY).getValue();
	}
	
	public DoubleBuffer getDoubleBuffer(String varName) throws VariableAccessException {
		return (DoubleBuffer)this.getVariable(varName, VariableType.DOUBLE_BUFFER).getValue();
	}
	
	public double getDouble(String varName) throws VariableAccessException {
		return ((Number)this.getVariable(varName, VariableType.DOUBLE).getValue()).doubleValue();
	}

	public float[] getFloatArray(String varName) throws VariableAccessException {
		return (float[])this.getVariable(varName, VariableType.FLOAT_ARRAY).getValue();
	}
	
	public FloatBuffer getFloatBuffer(String varName) throws VariableAccessException {
		return (FloatBuffer)this.getVariable(varName, VariableType.FLOAT_BUFFER).getValue();
	}
	
	public float getFloat(String varName) throws VariableAccessException {
		return ((Number)this.getVariable(varName, VariableType.FLOAT).getValue()).floatValue();
	}

	public int[] getIntArray(String varName) throws VariableAccessException {
		return (int[])this.getVariable(varName, VariableType.INT_ARRAY).getValue();
	}
	
	public IntBuffer getIntBuffer(String varName) throws VariableAccessException {
		return (IntBuffer)this.getVariable(varName, VariableType.INT_BUFFER).getValue();
	}
	
	public int getInt(String varName) throws VariableAccessException {
		return ((Number)this.getVariable(varName, VariableType.INT).getValue()).intValue();
	}

	public long[] getLongArray(String varName) throws VariableAccessException {
		return (long[])this.getVariable(varName, VariableType.LONG_ARRAY).getValue();
	}
	
	public LongBuffer getLongBuffer(String varName) throws VariableAccessException {
		return (LongBuffer)this.getVariable(varName, VariableType.LONG_BUFFER).getValue();
	}
	
	public long getLong(String varName) throws VariableAccessException {
		return ((Number)this.getVariable(varName, VariableType.LONG).getValue()).longValue();
	}

	public short[] getShortArray(String varName) throws VariableAccessException {
		return (short[])this.getVariable(varName, VariableType.SHORT_ARRAY).getValue();
	}
	
	public ShortBuffer getShortBuffer(String varName) throws VariableAccessException {
		return (ShortBuffer)this.getVariable(varName, VariableType.SHORT_BUFFER).getValue();
	}
	
	public short getShort(String varName) throws VariableAccessException {
		return ((Number)this.getVariable(varName, VariableType.SHORT).getValue()).shortValue();
	}
	
	public char[] getCharArray(String varName) throws VariableAccessException {
		return (char[])this.getVariable(varName, VariableType.CHAR_ARRAY).getValue();
	}
	
	public CharBuffer getCharBuffer(String varName) throws VariableAccessException {
		return (CharBuffer)this.getVariable(varName, VariableType.CHAR_BUFFER).getValue();
	}
	
	public char getChar(String varName) throws VariableAccessException {
		return ((Character)this.getVariable(varName, VariableType.SHORT).getValue()).charValue();
	}
	
	public boolean[] getBooleanArray(String varName) throws VariableAccessException {
		return (boolean[])this.getVariable(varName, VariableType.BOOLEAN_ARRAY).getValue();
	}
	
	public boolean getBoolean(String varName) throws VariableAccessException {
		return ((Boolean)this.getVariable(varName, VariableType.BOOLEAN).getValue()).booleanValue();
	}
	
	public String[] getStringArray(String varName) throws VariableAccessException {
		return (String[])this.getVariable(varName, VariableType.STRING_ARRAY).getValue();
	}
	
	public String getString(String varName) throws VariableAccessException {
		return (String)this.getVariable(varName, VariableType.STRING).getValue();
	}
	
	public Chunkable getChunk(String varName) throws VariableAccessException {
		Chunk chunk = (Chunk)this.getVariable(varName, VariableType.CHUNK).getValue();
		if (chunk != null)
			return chunk.getRepresentedObject();
		return null;
	}
	
	public Chunkable[] getChunkArray(String varName) throws VariableAccessException {
		Chunk[] chunks = (Chunk[])this.getVariable(varName, VariableType.CHUNK_ARRAY).getValue();
		if (chunks == null)
			return null;
		Chunkable[] res = new Chunkable[chunks.length];
		for (int i = 0; i < chunks.length; i++)
			if (chunks[i] != null)
				res[i] = chunks[i].getRepresentedObject();
		return res;
	}
	
	public <T extends Enum<T>> T getEnum(String varName, Class<T> enumType) {
		String name = this.getString(varName);
		return Enum.valueOf(enumType, name);
	}
	
	@Override
	public Chunkable getRepresentedObject() throws VariableAccessException {
		if (!this.constructed) {
			this.toImport = this.getIOManager().newInstance(this.getChunkType());
			this.toImport.readChunk(this);
			this.constructed = true;
		}
		return this.toImport;
	}
	
}
