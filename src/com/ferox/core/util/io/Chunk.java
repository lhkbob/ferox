package com.ferox.core.util.io;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;


abstract class Chunk {
	protected HashMap<Integer, Variable> compressedVars;
	protected ArrayList<Variable> uncompressedVars;
	
	protected final IOManager manager;
	protected final int id;
	protected final int type;
	
	public Chunk(IOManager manager, int id, int chunkType) {
		this.manager = manager;
		this.id = id;
		this.type = chunkType;
		
		this.compressedVars = new HashMap<Integer, Variable>();
		this.uncompressedVars = new ArrayList<Variable>();
	}
	
	protected static int compressVarName(String varName) {
		char[] cs = varName.toCharArray();
		int c = 0;
		for (int i = 0; i < cs.length; i++)
			c ^= cs[i];
		return c;
	}
	
	public int getNumVariables() {
		return this.uncompressedVars.size() + this.compressedVars.size();
	}
	
	public String computeCanonicalPath(String relPath) throws IOException {
		return this.manager.computeCanonicalPath(relPath);
	}
	
	public String computeRelativePath(String path) throws IOException {
		return this.manager.computeRelativePath(path);
	}
	
	protected Variable getVariable(String varName, byte type) {
		int hash = compressVarName(varName);
		Variable c = this.compressedVars.get(hash);
		if (c != null && (c.name == null || c.name.equals(varName))) {
			// found variable
			if (type == c.valueType)
				return c;
			throw new ValueAccessException("Variable exists but with a different type than requested");
		}
		int size = this.uncompressedVars.size();
		for (int i = 0; i < size; i++) {
			c = this.uncompressedVars.get(i);
			if (c.name.equals(varName)) {
				// found variable, uncomprssed
				if (type == c.valueType)
					return c;
				throw new ValueAccessException("Variable exists but with a different type than requested");
			}
		}
		
		// variable doesn't exist
		throw new ValueAccessException("Variable doesn't exist");
	}
	
	protected boolean setVariable(String varName, Object value, byte type) {
		int hash = compressVarName(varName);
		Variable c = this.compressedVars.get(hash);
		boolean compressed = true;
		
		if (c != null) {
			if (c.name == null || c.name.equals(varName)) {
				// variable exists, change its value
				if (type == c.valueType) {
					c.value = value;
					return true;
				}
				throw new ValueAccessException("Can't set a variable that already exists with a differing type");
			} else {
				// conflict exists, move other variable to uncompressed vars as well
				c.compressed = false;
				this.compressedVars.remove(c);
				this.uncompressedVars.add(c);
				compressed = false;
			}
		}
		
		if (compressed) {
			int size = this.uncompressedVars.size();
			for (int i = 0; i < size; i++) {
				c = this.uncompressedVars.get(i);
				if (c.name.equals(varName)) {
					// variable exists, change its value
					if (type == c.valueType) {
						c.value = value;
						return true;
					}
					throw new ValueAccessException("Can't set a variable that already exists with a differing type");
				}
			}
		}
		
		// variable doesn't exist, create a new one
		c = new Variable();
		c.name = varName;
		c.hash = hash;
		c.value = value;
		c.valueType = type;
		
		if (compressed)
			this.compressedVars.put(hash, c);
		else
			this.uncompressedVars.add(c);
		
		return false;
	}
	
	abstract Chunkable getRepresentedObject();
}
