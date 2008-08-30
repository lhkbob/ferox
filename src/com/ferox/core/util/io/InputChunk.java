package com.ferox.core.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.nio.*;

import com.ferox.core.util.IOUtil;


public final class InputChunk extends Chunk {
	private int numVars;
	private Chunkable repObject;
	private boolean constructed;
	
	InputChunk(IOManager manager, int id, int type, int numVars) {
		super(manager, id, type);
		this.numVars = numVars;
	}

	public Buffer getBuffer(String varName) {
		Variable c = this.compressedVars.get(varName);
		if (c == null) {
			for (int i = 0; i < this.uncompressedVars.size(); i++) {
				c = this.uncompressedVars.get(i);
				if (c.name.equals(varName))
					break;
			}
		}
		
		if (c != null) {
			switch (c.valueType) {
			case IOUtil.TYPE_BYTE_BUFFER: case IOUtil.TYPE_CHAR_BUFFER: case IOUtil.TYPE_SHORT_BUFFER:
			case IOUtil.TYPE_INT_BUFFER: case IOUtil.TYPE_LONG_BUFFER: case IOUtil.TYPE_FLOAT_BUFFER:
			case IOUtil.TYPE_DOUBLE_BUFFER:
				return (Buffer)c.value;
			default:
				throw new ValueAccessException("Variable exists but with a different type than requested");
			}
		}
		
		throw new ValueAccessException("Variable doesn't exist");
	}
	
	public byte[] getByteArray(String varName) {
		return (byte[])this.getVariable(varName, IOUtil.TYPE_BYTE_ARRAY).value;
	}
	
	public ByteBuffer getByteBuffer(String varName) {
		return (ByteBuffer)this.getVariable(varName, IOUtil.TYPE_BYTE_BUFFER).value;
	}

	public byte getByte(String varName) {
		return ((Number)this.getVariable(varName, IOUtil.TYPE_BYTE).value).byteValue();
	}

	public double[] getDoubleArray(String varName) {
		return (double[])this.getVariable(varName, IOUtil.TYPE_DOUBLE_ARRAY).value;
	}
	
	public DoubleBuffer getDoubleBuffer(String varName) {
		return (DoubleBuffer)this.getVariable(varName, IOUtil.TYPE_DOUBLE_BUFFER).value;
	}
	
	public double getDouble(String varName) {
		return ((Number)this.getVariable(varName, IOUtil.TYPE_DOUBLE).value).doubleValue();
	}

	public float[] getFloatArray(String varName) {
		return (float[])this.getVariable(varName, IOUtil.TYPE_FLOAT_ARRAY).value;
	}
	
	public FloatBuffer getFloatBuffer(String varName) {
		return (FloatBuffer)this.getVariable(varName, IOUtil.TYPE_FLOAT_BUFFER).value;
	}
	
	public float getFloat(String varName) {
		return ((Number)this.getVariable(varName, IOUtil.TYPE_FLOAT).value).floatValue();
	}

	public int[] getIntArray(String varName) {
		return (int[])this.getVariable(varName, IOUtil.TYPE_INT_ARRAY).value;
	}
	
	public IntBuffer getIntBuffer(String varName) {
		return (IntBuffer)this.getVariable(varName, IOUtil.TYPE_INT_BUFFER).value;
	}
	
	public int getInt(String varName) {
		return ((Number)this.getVariable(varName, IOUtil.TYPE_INT).value).intValue();
	}

	public long[] getLongArray(String varName) {
		return (long[])this.getVariable(varName, IOUtil.TYPE_LONG_ARRAY).value;
	}
	
	public LongBuffer getLongBuffer(String varName) {
		return (LongBuffer)this.getVariable(varName, IOUtil.TYPE_LONG_BUFFER).value;
	}
	
	public long getLong(String varName) {
		return ((Number)this.getVariable(varName, IOUtil.TYPE_LONG).value).longValue();
	}

	public short[] getShortArray(String varName) {
		return (short[])this.getVariable(varName, IOUtil.TYPE_SHORT_ARRAY).value;
	}
	
	public ShortBuffer getShortBuffer(String varName) {
		return (ShortBuffer)this.getVariable(varName, IOUtil.TYPE_SHORT_BUFFER).value;
	}
	
	public short getShort(String varName) {
		return ((Number)this.getVariable(varName, IOUtil.TYPE_SHORT).value).shortValue();
	}
	
	public char[] getCharArray(String varName) {
		return (char[])this.getVariable(varName, IOUtil.TYPE_CHAR_ARRAY).value;
	}
	
	public CharBuffer getCharBuffer(String varName) {
		return (CharBuffer)this.getVariable(varName, IOUtil.TYPE_CHAR_BUFFER).value;
	}
	
	public char getChar(String varName) {
		return ((Character)this.getVariable(varName, IOUtil.TYPE_SHORT).value).charValue();
	}
	
	public boolean[] getBooleanArray(String varName) {
		return (boolean[])this.getVariable(varName, IOUtil.TYPE_BOOLEAN_ARRAY).value;
	}
	
	public boolean getBoolean(String varName) {
		return ((Number)this.getVariable(varName, IOUtil.TYPE_BOOLEAN).value).byteValue() != 0;
	}
	
	public String[] getStringArray(String varName) {
		return (String[])this.getVariable(varName, IOUtil.TYPE_STRING_ARRAY).value;
	}
	
	public String getString(String varName) {
		return (String)this.getVariable(varName, IOUtil.TYPE_STRING).value;
	}
	
	public Chunkable getObject(String varName) {
		int id = (Integer)this.getVariable(varName, IOUtil.TYPE_OBJECT).value;
		if (id >= 0)
			return this.manager.getChunk(id).getRepresentedObject();
		return null;
	}
	
	public <T extends Enum<T>> T getEnum(String varName, Class<T> enumType) {
		String name = this.getString(varName);
		return Enum.valueOf(enumType, name);
	}

	@Override
	Chunkable getRepresentedObject() {
		if (!this.constructed) {
			try {
				this.repObject = this.manager.newInstance(this.type);
				this.repObject.readChunk(this);
			} catch (IOException io) {
				throw new ValueAccessException(io.getMessage());
			}
			this.constructed = true;
		}
		return this.repObject;
	}
	
	void unserialize(InputStream in) throws IOException {
		Variable v;
		for (int i = 0; i < this.numVars; i++) {
			v = Variable.read(in, this);
			if (v.compressed)
				this.compressedVars.put(v.hash, v);
			else
				this.uncompressedVars.add(v);
		}
	}
}
