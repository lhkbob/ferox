package com.ferox.core.states.atoms;

import com.ferox.core.states.manager.Geometry;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class GLSLAttribute implements Chunkable {
	public static enum AttributeType {
		FLOAT, VEC2F, VEC3F, VEC4F, MAT2F, MAT3F, MAT4F
	}
	
	private AttributeType type;
	private int binding;
	private String name;
	
	public GLSLAttribute(String name, AttributeType type) throws NullPointerException, IllegalArgumentException {
		if (name == null || type == null)
			throw new NullPointerException("Name or type can't be null: " + name + " " + type);
		if (name.indexOf("gl") == 0)
			throw new IllegalArgumentException("Name cannot begin with reserved 'gl': " + name);
		this.name = name;
		this.type = type;
		this.binding = 0;
	}

	public int getBinding() {
		return binding;
	}

	public void setBinding(int binding) {
		this.binding = binding;
		if (Geometry.getMaxVertexAttributes() > 0)
			this.binding = Math.min(this.binding, Geometry.getMaxVertexAttributes());
	}

	public AttributeType getType() {
		return type;
	}

	public String getName() {
		return name;
	}
	
	public boolean equals(Object other) {
		if (other == null || !(other instanceof GLSLAttribute))
			return false;
		if (other == this)
			return true;
		GLSLAttribute a = (GLSLAttribute)other;
		return a.name.equals(this.type) && a.type == this.type;
	}
	
	public int hashCode() {
		return this.name.hashCode();
	}

	public void readChunk(InputChunk in) {
		this.binding = in.getInt("binding");
		this.name = in.getString("name");
		this.type = in.getEnum("type", AttributeType.class);
	}

	public void writeChunk(OutputChunk out) {
		out.set("binding", this.binding);
		out.set("name", this.name);
		out.set("type", this.type);
	}
}
