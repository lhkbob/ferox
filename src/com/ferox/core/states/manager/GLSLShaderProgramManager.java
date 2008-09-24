package com.ferox.core.states.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Matrix4f;
import org.openmali.vecmath.Vector2f;
import org.openmali.vecmath.Vector2i;
import org.openmali.vecmath.Vector3f;
import org.openmali.vecmath.Vector4f;

import com.ferox.core.scene.Transform;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.atoms.GLSLShaderProgram;
import com.ferox.core.states.atoms.GLSLUniform;
import com.ferox.core.states.atoms.GLSLUniform.UniformType;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class GLSLShaderProgramManager extends UniqueStateManager<GLSLShaderProgram> {
	public static class GLSLValuePair {
		private GLSLUniform variable;
		private Object value;
		
		public GLSLValuePair(GLSLUniform var, Object val) {
			this.variable = var;
			this.value = val;
		}

		public GLSLUniform getVariable() {
			return variable;
		}

		public Object getValue() {
			return value;
		}
	}
	
	private HashMap<GLSLUniform, Object> uniforms;
	private ArrayList<GLSLValuePair> listUniforms;
	private List<GLSLValuePair> readOnly;
	
	public GLSLShaderProgramManager() {
		super();
		this.init();
	}
	
	public GLSLShaderProgramManager(GLSLShaderProgram state) {
		super(state);
		this.init();
	}

	private void init() {
		this.uniforms = new HashMap<GLSLUniform, Object>();
		this.listUniforms = new ArrayList<GLSLValuePair>();
		this.readOnly = Collections.unmodifiableList(this.listUniforms);
	}
	
	@Override
	public Class<? extends StateAtom> getAtomType() {
		return GLSLShaderProgram.class;
	}
	
	public List<GLSLValuePair> getUniforms() {
		return this.readOnly;
	}
	
	public void setUniform(GLSLUniform u, Object val) throws IllegalArgumentException {
		if (u != null) {
			if (val == null) {
				if (this.uniforms.remove(u) != null) {
					int index = -1;
					for (int i = 0; i < this.listUniforms.size(); i++) {
						if (this.listUniforms.get(i).variable.equals(u)) {
							index = i;
							break;
						}
					}
					this.listUniforms.remove(index);
					this.invalidateAssociatedStateTrees();
				}
			} else {
				if (!u.getType().isValidValue(val))
					throw new IllegalArgumentException("Cannot set a value for a uniform with incompatible type, uniform=" + u.getType() + " value=" + val.getClass().getSimpleName());
				if (!this.uniforms.containsKey(u)) {
					GLSLValuePair p = new GLSLValuePair(u, val);
					this.listUniforms.add(p);
					this.invalidateAssociatedStateTrees();
				} else {
					for (int i = 0; i < this.listUniforms.size(); i++) {
						if (this.listUniforms.get(i).variable.equals(u)) {
							this.listUniforms.get(i).value = val;
							break;
						}
					}
				}
				
				this.uniforms.put(u, val);
			}
		}
	}
	
	public Object getUniform(GLSLUniform u) {
		return this.uniforms.get(u);
	}
	
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		int numUniforms = in.getInt("num_vars");
		for (int i = 0; i < numUniforms; i++) {
			GLSLUniform uniform = (GLSLUniform)in.getChunk("uniform_" + i);
			this.setUniform(uniform, readUniformValue(in, uniform, i));
		}
	}
	
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.set("num_vars", this.listUniforms.size());
		int counter = 0;
		for (GLSLValuePair val: this.listUniforms) {
			out.set("uniform_" + counter, val.variable);
			writeUniformValue(out, val.value, val.variable.getType(), counter);
			counter++;
		}
	}
	
	private static Object readUniformValue(InputChunk in, GLSLUniform variable, int var) {
		String varName = "value_" + var;
		switch(variable.getType()) {
		case BOOLEAN:
			return Boolean.valueOf(in.getBoolean(varName));
		case BOOLEAN_ARRAY:
			return in.getBooleanArray(varName);
		case FLOAT:
			return Float.valueOf(in.getFloat(varName));
		case FLOAT_ARRAY:
			return in.getFloatArray(varName);
		case INT:
			return Integer.valueOf(in.getInt(varName));
		case INT_ARRAY:
			return in.getIntArray(varName);
		case MAT3F:
			return readMatrix3f(in.getFloatArray(varName), 0);
		case MAT3F_ARRAY: {
			float[] vals = in.getFloatArray(varName);
			Matrix3f[] mats = new Matrix3f[vals.length / 9];
			for (int i = 0; i < mats.length; i++)
				mats[i] = readMatrix3f(vals, i * 9);
			return mats; }
		case MAT4F:
			return readMatrix4f(in.getFloatArray(varName), 0);
		case MAT4F_ARRAY: {
			float[] vals = in.getFloatArray(varName);
			Matrix4f[] mats = new Matrix4f[vals.length / 16];
			for (int i = 0; i < mats.length; i++)
				mats[i] = readMatrix4f(vals, i * 16);
			return mats; }
		case VEC2F: 
			return readVector2f(in.getFloatArray(varName), 0);
		case VEC2F_ARRAY: {
			float[] vals = in.getFloatArray(varName);
			Vector2f[] mats = new Vector2f[vals.length / 2];
			for (int i = 0; i < mats.length; i++)
				mats[i] = readVector2f(vals, i * 2);
			return mats; }
		case VEC2I: 
			return readVector2i(in.getIntArray(varName), 0);
		case VEC2I_ARRAY: {
			int[] vals = in.getIntArray(varName);
			Vector2i[] mats = new Vector2i[vals.length / 2];
			for (int i = 0; i < mats.length; i++)
				mats[i] = readVector2i(vals, i * 2);
			return mats; }
		case VEC3F:
			return readVector3f(in.getFloatArray(varName), 0);
		case VEC3F_ARRAY: {
			float[] vals = in.getFloatArray(varName);
			Vector3f[] mats = new Vector3f[vals.length / 3];
			for (int i = 0; i < mats.length; i++)
				mats[i] = readVector3f(vals, i * 2);
			return mats; }
		case VEC4F:
			return readVector4f(in.getFloatArray(varName), 0);
		case VEC4F_ARRAY: {
			float[] vals = in.getFloatArray(varName);
			Vector4f[] mats = new Vector4f[vals.length / 4];
			for (int i = 0; i < mats.length; i++)
				mats[i] = readVector4f(vals, i * 4);
			return mats; }
		}
		return null;
	}
	
	private static void writeUniformValue(OutputChunk out, Object value, UniformType type, int var) {
		String varName = "value_" + var;
		float[] fCache;
		int[] iCache;
		switch(type) {
		case BOOLEAN:
			out.set(varName, (Boolean)value);
			break;
		case BOOLEAN_ARRAY:
			out.set(varName, (boolean[])value);
			break;
		case FLOAT:
			out.set(varName, (Float)value);
			break;
		case FLOAT_ARRAY:
			out.set(varName, (float[])value);
			break;
		case INT:
			out.set(varName, (Integer)value);
			break;
		case INT_ARRAY:
			out.set(varName, (int[])value);
			break;
		case MAT3F:
			fCache = new float[9];
			fillMatrix3f(fCache, (Matrix3f)value, 0);
			out.set(varName, fCache);
			break;
		case MAT3F_ARRAY: {
			Matrix3f[] mats = (Matrix3f[])value;
			fCache = new float[9 * mats.length];
			for (int i = 0; i < mats.length; i++)
				fillMatrix3f(fCache, mats[i], i * 9);
			out.set(varName, fCache);
			break; }
		case MAT4F: 
			fCache = new float[16];
			Transform.getOpenGLMatrix((Matrix4f)value, fCache, 0);
			out.set(varName, fCache);
			break;
		case MAT4F_ARRAY: {
			Matrix4f[] mats = (Matrix4f[])value;
			fCache = new float[16 * mats.length];
			for (int i = 0; i < mats.length; i++)
				Transform.getOpenGLMatrix(mats[i], fCache, i * 16);
			out.set(varName, fCache);
			break; }
		case VEC2F:
			fCache = new float[2];
			fillVector2f(fCache, (Vector2f)value, 0);
			out.set(varName, fCache);
			break;
		case VEC2F_ARRAY: {
			Vector2f[] vecs = (Vector2f[])value;
			fCache = new float[2 * vecs.length];
			for (int i = 0; i < vecs.length; i++)
				fillVector2f(fCache, vecs[i], i * 2);
			out.set(varName, fCache);
			break; }
		case VEC2I: 
			iCache = new int[2];
			fillVector2i(iCache, (Vector2i)value, 0);
			out.set(varName, iCache);
			break;
		case VEC2I_ARRAY: {
			Vector2i[] vecs = (Vector2i[])value;
			iCache = new int[2 * vecs.length];
			for (int i = 0; i < vecs.length; i++)
				fillVector2i(iCache, vecs[i], i * 2);
			out.set(varName, iCache);
			break; }
		case VEC3F: 
			fCache = new float[3];
			fillVector3f(fCache, (Vector3f)value, 0);
			out.set(varName, fCache);
			break;
		case VEC3F_ARRAY: {
			Vector3f[] vecs = (Vector3f[])value;
			fCache = new float[3 * vecs.length];
			for (int i = 0; i < vecs.length; i++)
				fillVector3f(fCache, vecs[i], i * 3);
			out.set(varName, fCache);
			break; }
		case VEC4F:
			fCache = new float[4];
			fillVector4f(fCache, (Vector4f)value, 0);
			out.set(varName, fCache);
			break;
		case VEC4F_ARRAY: {
			Vector4f[] vecs = (Vector4f[])value;
			fCache = new float[4 * vecs.length];
			for (int i = 0; i < vecs.length; i++)
				fillVector4f(fCache, vecs[i], i * 4);
			out.set(varName, fCache);
			break; }
		}
	}
	
	private static Vector2f readVector2f(float[] cache, int off) {
		return new Vector2f(cache[0 + off], cache[1 + off]);
	}
	
	private static Vector3f readVector3f(float[] cache, int off) {
		return new Vector3f(cache[0 + off], cache[1 + off], cache[2 + off]);
	}
	
	private static Vector4f readVector4f(float[] cache, int off) {
		return new Vector4f(cache[0 + off], cache[1 + off], cache[2 + off], cache[3 + off]);
	}
	
	private static Vector2i readVector2i(int[] cache, int off) {
		return new Vector2i(cache[0 + off], cache[1 + off]);
	}
	
	private static Matrix3f readMatrix3f(float[] cache, int off) {
		Matrix3f m = new Matrix3f();
		m.m00 = cache[0 + off];
		m.m10 = cache[1 + off];
		m.m20 = cache[2 + off];
		m.m01 = cache[3 + off];
		m.m11 = cache[4 + off];
		m.m21 = cache[5 + off];
		m.m02 = cache[6 + off];
		m.m12 = cache[7 + off];
		m.m22 = cache[8 + off];
		return m;
	}
	
	private static Matrix4f readMatrix4f(float[] cache, int off) {
		Matrix4f m = new Matrix4f();
		m.m00 = cache[off + 0];
		m.m10 = cache[off + 1];
		m.m20 = cache[off + 2];
		m.m30 = cache[off + 3];
		m.m01 = cache[off + 4];
		m.m11 = cache[off + 5];
		m.m21 = cache[off + 6];
		m.m31 = cache[off + 7];
		m.m02 = cache[off + 8];
		m.m12 = cache[off + 9];
		m.m22 = cache[off + 10];
		m.m32 = cache[off + 11];
		m.m03 = cache[off + 12];
		m.m13 = cache[off + 13];
		m.m23 = cache[off + 14];
		m.m33 = cache[off + 15];
		return m;
	}
	
	private static void fillVector2f(float[] cache, Vector2f v, int off) {
		cache[0 + off] = v.x;
		cache[1 + off] = v.y;
	}
	
	private static void fillVector3f(float[] cache, Vector3f v, int off) {
		cache[0 + off] = v.x;
		cache[1 + off] = v.y;
		cache[2 + off] = v.z;
	}
	
	private static void fillVector4f(float[] cache, Vector4f v, int off) {
		cache[0 + off] = v.x;
		cache[1 + off] = v.y;
		cache[2 + off] = v.z;
		cache[3 + off] = v.w;
	}
	
	private static void fillVector2i(int[] cache, Vector2i v, int off) {
		cache[0 + off] = v.x;
		cache[1 + off] = v.y;
	}
	
	private static void fillMatrix3f(float[] cache, Matrix3f m, int off) {
		cache[0 + off] = m.m00;
		cache[1 + off] = m.m10;
		cache[2 + off] = m.m20;
		cache[3 + off] = m.m01;
		cache[4 + off] = m.m11;
		cache[5 + off] = m.m21;
		cache[6 + off] = m.m02;
		cache[7 + off] = m.m12;
		cache[8 + off] = m.m22;
	}
}
