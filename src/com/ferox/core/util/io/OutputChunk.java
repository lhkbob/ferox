package com.ferox.core.util.io;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;


public class OutputChunk extends Chunk {
	private Chunkable toExport;
	private int referenceCount;
	
	public OutputChunk(IOManager manager, Class<? extends Chunkable> chunkType, Chunkable object) {
		super(manager, chunkType);
		this.toExport = object;
		this.referenceCount = 0;
	}

	public boolean set(String varName, byte[] value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.BYTE_ARRAY);
	}
	
	public boolean set(String varName, ByteBuffer value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.BYTE_BUFFER);
	}

	public boolean set(String varName, byte value) throws VariableAccessException {
		return this.setVariable(varName, Byte.valueOf(value), VariableType.BYTE);
	}

	public boolean set(String varName, double[] value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.DOUBLE_ARRAY);
	}
	
	public boolean set(String varName, DoubleBuffer value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.DOUBLE_BUFFER);
	}
	
	public boolean set(String varName, double value) throws VariableAccessException {
		return this.setVariable(varName, Double.valueOf(value), VariableType.DOUBLE);
	}

	public boolean set(String varName, float[] value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.FLOAT_ARRAY);
	}
	
	public boolean set(String varName, FloatBuffer value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.FLOAT_BUFFER);
	}
	
	public boolean set(String varName, float value) throws VariableAccessException {
		return this.setVariable(varName, Float.valueOf(value), VariableType.FLOAT);
	}

	public boolean set(String varName, int[] value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.INT_ARRAY);
	}
	
	public boolean set(String varName, IntBuffer value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.INT_BUFFER);
	}
	
	public boolean set(String varName, int value) throws VariableAccessException {
		return this.setVariable(varName, Integer.valueOf(value), VariableType.INT);
	}

	public boolean set(String varName, long[] value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.BYTE_ARRAY);
	}
	
	public boolean set(String varName, LongBuffer value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.LONG_BUFFER);
	}
	
	public boolean set(String varName, long value) throws VariableAccessException {
		return this.setVariable(varName, Long.valueOf(value), VariableType.LONG);
	}

	public boolean set(String varName, short[] value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.SHORT_ARRAY);
	}
	
	public boolean set(String varName, ShortBuffer shortBuffer) throws VariableAccessException {
		return this.setVariable(varName, shortBuffer, VariableType.SHORT_BUFFER);
	}
	
	public boolean set(String varName, short value) throws VariableAccessException {
		return this.setVariable(varName, Short.valueOf(value), VariableType.SHORT);
	}

	public boolean set(String varName, char[] value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.CHAR_ARRAY);
	}
	
	public boolean set(String varName, CharBuffer value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.CHAR_BUFFER);
	}
	
	public boolean set(String varName, char value) throws VariableAccessException {
		return this.setVariable(varName, Character.valueOf(value), VariableType.CHAR);
	}

	public boolean set(String varName, boolean[] value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.BOOLEAN_ARRAY);
	}
	
	public boolean set(String varName, boolean value) throws VariableAccessException {
		return this.setVariable(varName, Boolean.valueOf(value), VariableType.BOOLEAN);
	}
	
	public boolean set(String varName, String[] value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.STRING_ARRAY);
	}

	public boolean set(String varName, String value) throws VariableAccessException {
		return this.setVariable(varName, value, VariableType.STRING);
	}
	
	public boolean set(String varName, Chunkable value) throws VariableAccessException {
		Chunk chunk = null;
		if (value != null)
			chunk = this.getIOManager().getChunk(value);
		return this.setVariable(varName, chunk, VariableType.CHUNK);
	}
	
	public boolean set(String varName, Chunkable[] value) throws VariableAccessException {
		if (value != null) {
			Chunk[] chunks = new Chunk[value.length];
			boolean val = this.setVariable(varName, chunks, VariableType.CHUNK_ARRAY);
			for (int i = 0; i < chunks.length; i++) {
				if (value[i] != null) 
					chunks[i] = this.getIOManager().getChunk(value[i]);;
			}
			return val;
		} else
			return this.setVariable(varName, null, VariableType.CHUNK_ARRAY);
	}
	
	public boolean set(String varName, Buffer value) throws VariableAccessException {
		if (value instanceof IntBuffer)
			return this.set(varName, (IntBuffer)value);
		else if (value instanceof FloatBuffer)
			return this.set(varName, (FloatBuffer)value);
		else if (value instanceof DoubleBuffer)
			return this.set(varName, (DoubleBuffer)value);
		else if (value instanceof ShortBuffer)
			return this.set(varName, (ShortBuffer)value);
		else if (value instanceof ByteBuffer)
			return this.set(varName, (ByteBuffer)value);
		else if (value instanceof CharBuffer)
			return this.set(varName, (CharBuffer)value);
		else if (value instanceof LongBuffer)
			return this.set(varName, (LongBuffer)value);
		return false;
	}
	
	public boolean set(String varName, Enum<?> value) throws VariableAccessException {
		return this.set(varName, value.name());
	}
	
	public int getReferenceCount() {
		return this.referenceCount;
	}
	
	public void resetReferenceCount() {
		this.referenceCount = 0;
	}
	
	public Set<IOManager> getReferencedIOManagers() {
		Set<IOManager> iom = new HashSet<IOManager>();
		Collection<Variable> vars = this.getVariables();
		for (Variable v: vars) {
			if (v.getType() == VariableType.CHUNK) {
				Chunk value = (Chunk)v.getValue();
				if (value != null && value.getIOManager() != this.getIOManager())
					iom.add(value.getIOManager());
			} else if (v.getType() == VariableType.CHUNK_ARRAY) {
				Chunk[] values = (Chunk[])v.getValue();
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						if (values[i] != null && values[i].getIOManager() != this.getIOManager())
							iom.add(values[i].getIOManager());
					}
				}
			}
		}
		return iom;
	}
	
	public void updateReferencedChunks() {
		Collection<Variable> vars = this.getVariables();
		for (Variable v: vars) {
			if (v.getType() == VariableType.CHUNK) {
				Chunk value = (Chunk)v.getValue();
				if (value != null && value.getIOManager() == this.getIOManager())
					((OutputChunk)value).referenceCount++;
			} else if (v.getType() == VariableType.CHUNK_ARRAY) {
				Chunk[] values = (Chunk[])v.getValue();
				if (values != null) {
					for (int i = 0; i < values.length; i++) {
						if (values[i] != null && values[i].getIOManager() == this.getIOManager())
							((OutputChunk)values[i]).referenceCount++;
					}
				}
			}
		}
	}
	
	@Override
	public Chunkable getRepresentedObject() {
		return this.toExport;
	}
}
