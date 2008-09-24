package com.ferox.core.states.atoms;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Matrix4f;
import org.openmali.vecmath.Vector2f;
import org.openmali.vecmath.Vector2i;
import org.openmali.vecmath.Vector3f;
import org.openmali.vecmath.Vector4f;

import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class GLSLUniform implements Chunkable {
	public static enum UniformType {
		FLOAT(Float.class), INT(Integer.class), BOOLEAN(Boolean.class), 
		VEC2F(Vector2f.class), VEC3F(Vector3f.class), VEC4F(Vector4f.class), 
		VEC2I(Vector2i.class),
		MAT3F(Matrix3f.class), MAT4F(Matrix4f.class),
		FLOAT_ARRAY(float[].class), INT_ARRAY(int[].class), BOOLEAN_ARRAY(boolean[].class),
		VEC2F_ARRAY(Vector2f[].class), VEC3F_ARRAY(Vector3f[].class), VEC4F_ARRAY(Vector4f[].class),
		VEC2I_ARRAY(Vector2i[].class),
		MAT3F_ARRAY(Matrix3f[].class), MAT4F_ARRAY(Matrix4f[].class);
		
		private Class<?> validTypes;
		private UniformType(Class<?> valid) {
			this.validTypes = valid;
		}
		
		public boolean isValidValue(Object value) {
			if (value == null)
				return false;
			Class<?> t = value.getClass();
			return this.validTypes.isAssignableFrom(t);
		}
	}
	
	private String name;
	private UniformType type;
	private int count;
	
	public GLSLUniform(String name, UniformType type, int size) throws NullPointerException, IllegalArgumentException {
		if (name == null || type == null)
			throw new NullPointerException("Name and type can't be null: " + name + " " + type);
		if (name.indexOf("gl") == 0)
			throw new IllegalArgumentException("Name cannot begin with prefix 'gl': " + name);
		this.name = name;
		this.type = type;
		this.count = Math.max(1, size);
	}
	
	public String getName() {
		return this.name;
	}
	
	public UniformType getType() {
		return this.type;
	}
	
	public int getSize() {
		return this.count;
	}
	
	public boolean equals(Object obj) {
		if (obj == null || !(obj instanceof GLSLUniform))
			return false;
		if (obj == this)
			return true;
		GLSLUniform u = (GLSLUniform)obj;
		return u.name.equals(this.name) && u.type == this.type;
	}
	
	public int hashCode() {
		return this.name.hashCode() ^ this.type.hashCode();
	}

	public void readChunk(InputChunk in) {
		this.name = in.getString("name");
		this.type = in.getEnum("type", UniformType.class);
		this.count = in.getInt("count");
	}

	public void writeChunk(OutputChunk out) {
		out.set("name", this.name);
		out.set("type", this.type);
		out.set("count", this.count);
	}
}
