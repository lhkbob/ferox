package com.ferox.core.util.io;

import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.DoubleBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.LongBuffer;
import java.nio.ShortBuffer;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;


public abstract class Chunk {
	public static enum VariableType {
		CHUNK(0, Chunk.class), CHUNK_ARRAY(1, Chunk[].class), STRING(2, String.class), STRING_ARRAY(3, String[].class), 
		INT(4, Integer.class), INT_ARRAY(5, int[].class), INT_BUFFER(6, IntBuffer.class), 
		SHORT(7, Short.class), SHORT_ARRAY(8, short[].class), SHORT_BUFFER(9, ShortBuffer.class), 
		LONG(10, Long.class), LONG_ARRAY(11, long[].class), LONG_BUFFER(12, LongBuffer.class),
		BYTE(13, Byte.class), BYTE_ARRAY(14, byte[].class), BYTE_BUFFER(15, ByteBuffer.class), 
		CHAR(16, Character.class), CHAR_ARRAY(17, char[].class), CHAR_BUFFER(18, CharBuffer.class),
		FLOAT(19, Float.class), FLOAT_ARRAY(20, float[].class), FLOAT_BUFFER(21, FloatBuffer.class), 
		DOUBLE(22, Double.class), DOUBLE_ARRAY(23, double[].class), DOUBLE_BUFFER(24, DoubleBuffer.class),
		BOOLEAN(25, Boolean.class), BOOLEAN_ARRAY(26, boolean[].class);
		
		private byte typeCode;
		private Class<?> type;
		private VariableType(int typeCode, Class<?> type) {
			this.typeCode = (byte)typeCode;
			this.type = type;
		}
		
		public byte getTypeCode() {
			return this.typeCode;
		}
		
		public boolean isValid(Object value) {
			return value == null || this.type.isInstance(value);
		}
	}
	
	private static class VarName {
		String name;
		int hash;
		boolean compressed;
		
		public int hashCode() {
			if (this.compressed)
				return this.hash;
			return this.name.hashCode();
		}
		
		public boolean equals(Object obj) {
			if (obj == null || !(obj instanceof VarName))
				return false;
			VarName v = (VarName)obj;
			if (this.compressed) {
				if (v.compressed)
					return v.hash == this.hash;
				return false;
			} else {
				if (v.compressed)
					return false;
				return v.name.equals(this.name);
			}
		}
	}
	
	private HashMap<VarName, Variable> variables;
	private Collection<Variable> readOnlyVars;
	private IOManager manager;
	private Class<? extends Chunkable> chunkType;
	
	public Chunk(IOManager manager, Class<? extends Chunkable> chunkType) {
		if (manager == null || chunkType == null)
			throw new NullPointerException("Can't create a chunk with null manager or type: " + manager + " " + chunkType);
		this.manager = manager;
		this.chunkType = chunkType;
		
		this.variables = new HashMap<VarName, Variable>();
		this.readOnlyVars = Collections.unmodifiableCollection(this.variables.values());
	}
	
	protected final Variable getVariable(String varName, VariableType type) throws VariableAccessException, NullPointerException {
		if (varName == null || type == null)
			throw new NullPointerException("Can't retrieve variable with null name or type: " + varName + " " + type);
		VarName var = new VarName();
		var.name = varName;
		var.hash = Chunk.getCompressedName(varName);
		var.compressed = true;
		Variable v = this.variables.get(var);
		if (v != null && (v.getName().equals(varName) || (this instanceof InputChunk && v.getName().equals("")))) {
			if (v.getType() == type)
				return v;
			throw new VariableAccessException("Variable exists but with a different type than requested: " + varName + " " + type);
		}
		
		var.compressed = false;
		v = this.variables.get(var);
		if (v != null && v.getName().equals(varName)) {
			if (v.getType() == type)
				return v;
			throw new VariableAccessException("Variable exists but with a different type than requested: " + varName + " " + type);
		}
		throw new VariableAccessException("Variable doesn't exist: " + varName + " " + type);
	}
	
	protected final boolean setVariable(String varName, Object value, VariableType type) throws VariableAccessException, NullPointerException {
		if (varName == null || type == null)
			throw new NullPointerException("Can't set a variable with null name or type: " + varName + " " + type);
		if (!type.isValid(value))
			throw new VariableAccessException("Can't set a variable's value to an object of wrong type: " + value + " " + type);
		
		VarName var = new VarName();
		var.name = varName;
		var.hash = Chunk.getCompressedName(varName);
		
		var.compressed = true;
		Variable v = this.variables.get(var);
		
		if (v != null) {
			if (v.getName().equals(varName)) {
				if (type == v.getType()) {
					v.setValue(value);
					return true;
				}
				throw new VariableAccessException("Can't set a variable that already exists with a differing type");
			} else {
				VarName conflict = new VarName();
				conflict.name = v.getName();
				conflict.hash = var.hash;
				conflict.compressed = false;
				v.setNameCompressed(false);
				this.variables.remove(var);
				this.variables.put(conflict, v);
				var.compressed = false;
			}
		}
		
		if (var.compressed) {
			var.compressed = false;
			v = this.variables.get(var);
			if (v != null && v.getName().equals(varName)) {
				if (type == v.getType()) {
					v.setValue(value);
					return true;
				}
				throw new VariableAccessException("Can't set a variable that already exists with a differing type");
			}
			var.compressed = true;
		}
		
		v = this.manager.newVariable(varName, type);
		v.setNameCompressed(var.compressed);
		v.setValue(value);
		this.variables.put(var, v);
		
		return false;
	}
	
	public int getID() {
		return this.manager.getChunkID(this);
	}
	
	public IOManager getIOManager() {
		return this.manager;
	}
	
	public Class<? extends Chunkable> getChunkType() {
		return this.chunkType;
	}
	
	public int getNumVariables() {
		return this.variables.size();
	}
	
	public Collection<Variable> getVariables() {
		return this.readOnlyVars;
	}
	
	public void setVariables(Collection<Variable> vars) {
		this.variables.clear();
		Variable prev;
		for (Variable v: vars) {
			VarName var = new VarName();
			var.compressed = v.isNameCompressed();
			var.name = v.getName();
			var.hash = v.getCompressedName();
			
			prev = this.variables.get(var);
			if (prev != null && prev != v && var.compressed) {
				VarName conflict = new VarName();
				conflict.name = prev.getName();
				conflict.hash = prev.getCompressedName();
				conflict.compressed = false;
				prev.setNameCompressed(false);
				
				this.variables.remove(var);
				this.variables.put(conflict, prev);
				
				var.compressed = false;
				v.setNameCompressed(false);
			}
			this.variables.put(var, v);
		}
	}
	
	public abstract Chunkable getRepresentedObject();
	
	public static int getCompressedName(String name) {
		if (name == null)
			throw new NullPointerException("Can't compress a null name");
		char[] cs = name.toCharArray();
		int c = 0;
		for (int i = 0; i < cs.length; i++)
			c ^= cs[i];
		return c;
	}
}
