package com.ferox.core.util.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ferox.core.util.io.Chunk.VariableType;


public abstract class Variable {
	private String name;
	private int hash;
	private boolean compressed;
	private VariableType type;
	private Object value;
	
	public Variable(String name, VariableType type) {
		this(name, type, Chunk.getCompressedName(name));
	}
	
	public Variable(String name, VariableType type, int hash) {
		if (type == null || name == null)
			throw new NullPointerException("Can't create a variable with a null name or type: " + name + " " + type);
		this.name = name;
		this.type = type;
		this.hash = hash;
		this.setNameCompressed(true);
		this.setValue(null);
	}
	
	public String getName() {
		return name;
	}
	
	public boolean isNameCompressed() {
		return compressed;
	}
	
	public void setNameCompressed(boolean compressed) {
		this.compressed = compressed;
	}
	
	public final int getCompressedName() {
		return this.hash;
	}
	
	public VariableType getType() {
		return type;
	}
	
	public Object getValue() {
		return value;
	}
	
	public void setValue(Object value) {
		if (!this.type.isValid(value))
			throw new VariableAccessException("Can't set a value of incorrect type: " + value.getClass());
		this.value = value;
	}
	
	public abstract void write(OutputStream stream, IOManager manager) throws IOException;
	public abstract void read(InputStream stream, IOManager manager) throws IOException;
}
