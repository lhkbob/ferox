package com.ferox.impl.jsr231.peers;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.media.opengl.GL;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Matrix4f;
import org.openmali.vecmath.Vector2f;
import org.openmali.vecmath.Vector2i;
import org.openmali.vecmath.Vector3f;
import org.openmali.vecmath.Vector4f;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.scene.Transform;
import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateManager;
import com.ferox.core.states.StateUpdateException;
import com.ferox.core.states.StateAtom.StateRecord;
import com.ferox.core.states.atoms.GLSLAttribute;
import com.ferox.core.states.atoms.GLSLShaderObject;
import com.ferox.core.states.atoms.GLSLShaderProgram;
import com.ferox.core.states.atoms.GLSLUniform;
import com.ferox.core.states.atoms.GLSLAttribute.AttributeType;
import com.ferox.core.states.atoms.GLSLUniform.UniformType;
import com.ferox.core.states.manager.GLSLShaderProgramManager;
import com.ferox.core.states.manager.GLSLShaderProgramManager.GLSLValuePair;
import com.ferox.core.util.BufferUtil;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLGLSLShaderProgramPeer extends SimplePeer<GLSLShaderProgram, GLSLProgramRecord> {
	private static class UniformRecord {
		private static int[] intCache = new int[0];
		private static float[] floatCache = new float[16];
		
		private int location;
		private GLSLUniform variable;
		private Object lastValue;
		
		public boolean needsUpdate(Object value) {
			if (value == this.lastValue)
				return false;
			
			switch(this.variable.getType()) {
			case VEC2F:	case VEC2I:	case VEC3F:	case VEC4F:	case MAT3F:	case MAT4F:
			case FLOAT:	case INT: case BOOLEAN:
				return this.lastValue == null || !this.lastValue.equals(value);
			case VEC2F_ARRAY: case VEC3F_ARRAY: case VEC4F_ARRAY: case VEC2I_ARRAY:
			case MAT3F_ARRAY: case MAT4F_ARRAY:
				return Arrays.equals((Object[])this.lastValue, (Object[])value);
			case FLOAT_ARRAY:
				return Arrays.equals((float[])this.lastValue, (float[])value);
			case INT_ARRAY:
				return Arrays.equals((int[])this.lastValue, (int[])value);
			case BOOLEAN_ARRAY:
				return Arrays.equals((boolean[])this.lastValue, (boolean[])value);
			}
			return false;
		}
		
		public void setValue(Object value, GL gl) {
			if (this.needsUpdate(value)) {
				switch(this.variable.getType()) {
				case VEC2F: setUniform((Vector2f)value, this.location, gl); break;
				case VEC3F: setUniform((Vector3f)value, this.location, gl); break;
				case VEC4F: setUniform((Vector4f)value, this.location, gl); break;
				case VEC2I: setUniform((Vector2i)value, this.location, gl); break;
				case MAT3F: setUniform((Matrix3f)value, this.location, gl); break;
				case MAT4F: setUniform((Matrix4f)value, this.location, gl); break;
				case FLOAT: setUniform((Float)value, this.location, gl); break;
				case INT: setUniform((Integer)value, this.location, gl); break;
				case BOOLEAN: setUniform((Boolean)value, this.location, gl); break;
				
				case VEC2F_ARRAY: setUniform((Vector2f[])value, this.location, Math.min(this.variable.getSize(), ((Vector2f[])value).length), gl); break;
				case VEC3F_ARRAY: setUniform((Vector3f[])value, this.location, Math.min(this.variable.getSize(), ((Vector3f[])value).length),gl); break;
				case VEC4F_ARRAY: setUniform((Vector4f[])value, this.location, Math.min(this.variable.getSize(), ((Vector4f[])value).length),gl); break;
				case VEC2I_ARRAY: setUniform((Vector2i[])value, this.location, Math.min(this.variable.getSize(), ((Vector2i[])value).length),gl); break;
				case MAT3F_ARRAY: setUniform((Matrix3f[])value, this.location, Math.min(this.variable.getSize(), ((Matrix3f[])value).length),gl); break;
				case MAT4F_ARRAY: setUniform((Matrix4f[])value, this.location, Math.min(this.variable.getSize(), ((Matrix4f[])value).length),gl); break;
				case FLOAT_ARRAY: setUniform((float[])value, this.location, Math.min(this.variable.getSize(), ((float[])value).length),gl); break;
				case INT_ARRAY: setUniform((int[])value, this.location, Math.min(this.variable.getSize(), ((int[])value).length),gl); break;
				case BOOLEAN_ARRAY: setUniform((boolean[])value, this.location, Math.min(this.variable.getSize(), ((boolean[])value).length),gl); break;
				}
				this.lastValue = value;
			}
		}
		
		private static void setUniform(Vector2f v, int loc, GL gl) {
			gl.glUniform2f(loc, v.x, v.y);
		}
		
		private static void setUniform(Vector2f[] v, int loc, int count, GL gl) {
			if (floatCache.length < count << 1)
				floatCache = new float[count << 1];
			for (int i = 0; i < count; i++) 
				fillVector2f(floatCache, v[i], i << 1);
			gl.glUniform2fv(loc, count, floatCache, 0);
		}
		
		private static void setUniform(Vector3f v, int loc, GL gl) {
			gl.glUniform3f(loc, v.x, v.y, v.z);
		}
		
		private static void setUniform(Vector3f[] v, int loc, int count, GL gl) {
			if (floatCache.length < count * 3)
				floatCache = new float[count * 3];
			for (int i = 0; i < count; i++) 
				fillVector3f(floatCache, v[i], i * 3);
			gl.glUniform3fv(loc, count, floatCache, 0);
		}

		private static void setUniform(Vector4f v, int loc, GL gl) {
			gl.glUniform4f(loc, v.x, v.y, v.z, v.w);
		}

		private static void setUniform(Vector4f[] v, int loc, int count, GL gl) {
			if (floatCache.length < count << 2)
				floatCache = new float[count << 2];
			for (int i = 0; i < count; i++) 
				fillVector4f(floatCache, v[i], i << 2);
			gl.glUniform4fv(loc, count, floatCache, 0);
		}
		
		private static void setUniform(Vector2i v, int loc, GL gl) {
			gl.glUniform2i(loc, v.x, v.y);
		}
		
		private static void setUniform(Vector2i[] v, int loc, int count, GL gl) {
			if (intCache.length < count << 1)
				intCache = new int[count << 1];
			for (int i = 0; i < count; i++) 
				fillVector2i(intCache, v[i], i << 1);
			gl.glUniform2iv(loc, count, intCache, 0);
		}
		
		private static void setUniform(Matrix3f m, int loc, GL gl) {
			if (floatCache.length < 9)
				floatCache = new float[9];
			fillMatrix3f(floatCache, m, 0);
			gl.glUniformMatrix3fv(loc, 1, false, floatCache, 0);
		}
		
		private static void setUniform(Matrix3f[] v, int loc, int count, GL gl) {
			if (floatCache.length < count * 9)
				floatCache = new float[count * 9];
			for (int i = 0; i < count; i++) 
				fillMatrix3f(floatCache, v[i], i * 9);
			gl.glUniformMatrix3fv(loc, count, false, floatCache, 0);
		}
		
		private static void setUniform(Matrix4f m, int loc, GL gl) {
			if (floatCache.length < 16)
				floatCache = new float[16];
			Transform.getOpenGLMatrix(m, floatCache, 0);
			gl.glUniformMatrix4fv(loc, 1, false, floatCache, 0);
		}
		
		private static void setUniform(Matrix4f[] v, int loc, int count, GL gl) {
			if (floatCache.length < count << 4)
				floatCache = new float[count << 4];
			for (int i = 0; i < count; i++) 
				Transform.getOpenGLMatrix(v[i], floatCache, i << 4);
			gl.glUniformMatrix4fv(loc, count, false, floatCache, 0);
		}
		
		private static void setUniform(float f, int loc, GL gl) {
			gl.glUniform1f(loc, f);
		}
		
		private static void setUniform(float[] f, int loc, int count, GL gl) {
			gl.glUniform1fv(loc, count, f, 0);
		}
		
		private static void setUniform(int i, int loc, GL gl) {
			gl.glUniform1i(i, loc);
		}
		
		private static void setUniform(int[] f, int loc, int count, GL gl) {
			gl.glUniform1iv(loc, count, f, 0);
		}
		
		private static void setUniform(boolean b, int loc, GL gl) {
			gl.glUniform1i((b ? 1 : 0), loc);
		}
		
		private static void setUniform(boolean[] f, int loc, int count, GL gl) {
			if (intCache.length < count) 
				intCache = new int[count];
			for (int i = 0; i < count; i++)
				intCache[i] = (f[i] ? 1 : 0);
			gl.glUniform1iv(loc, count, intCache, 0);
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
	
	private static class AttributeRecord {
		private int location;
		
		public void updateAttribute(GLSLAttribute na, int prog, GL gl) {
			if (na.getBinding() != this.location) {
				gl.glBindAttribLocation(prog, na.getBinding(), na.getName());
				this.location = na.getBinding();
			}
		}
	}
	
	private HashMap<GLSLUniform, UniformRecord>[] uniforms;
	private HashMap<GLSLAttribute, AttributeRecord>[] attrs;
	private GLSLShaderProgramManager lastManager;
	
	public JOGLGLSLShaderProgramPeer(JOGLRenderContext context) {
		super(context);
		this.uniforms = new HashMap[1];
		this.attrs = new HashMap[1];
	}
	
	protected void applyState(GLSLShaderProgram prevA, GLSLProgramRecord prev, GLSLShaderProgram nextA, GLSLProgramRecord next, GL gl) {
		if (next.compiled) {
			gl.glUseProgram(next.id);
			this.applyUniforms(this.lastManager, nextA, next, gl);
			this.updateAttributeBindings(nextA, next, gl);
		} else if (prev != null && prev.compiled)
			gl.glUseProgram(0);
	}

	public void validateStateAtom(StateAtom atom) throws StateUpdateException {
		if (!RenderManager.getSystemCapabilities().areGLSLShadersSupported()) {
			((GLSLShaderProgram)atom).setCompiled(false, "GLSL isn't supported");
			throw new StateUpdateException(atom, "GLSL is not supported on this device");
		}
	}
	
	public void cleanupStateAtom(StateRecord record) {
		GLSLProgramRecord p = (GLSLProgramRecord)record;
		if (p.id > 0) {
			this.cleanupUniforms(p);
			this.deleteProgram(p, this.context.getGL());
		}
	}

	public StateRecord initializeStateAtom(StateAtom a) {
		GLSLShaderProgram atom = (GLSLShaderProgram)a;
		
		GL gl = this.context.getGL();
		GLSLProgramRecord r = new GLSLProgramRecord();
		
		if (atom.isFixedFunction()) {
			r.id = 0;
			r.compiled = false;
			r.infoLog = "Fixed Function";
			atom.setCompiled(false, r.infoLog);
		} else {
			r.id = gl.glCreateProgram();
			this.linkAndCompile(r, atom, this.context);
		}
		return r;
	}
	
	protected void restoreState(GLSLShaderProgram cleanA, GLSLProgramRecord cleanR, GL gl) {
		if (cleanR.compiled)
			gl.glUseProgram(0);
	}

	public void prepareManager(StateManager manager, StateManager prev) {
		GLSLShaderProgramManager m = (GLSLShaderProgramManager)manager;
		this.lastManager = m;
		if (prev != null) {
			GLSLShaderProgram p = ((GLSLShaderProgramManager)prev).getStateAtom();
			GLSLShaderProgram sm = m.getStateAtom();
			if (p == sm)
				this.applyUniforms(m, sm, (GLSLProgramRecord)sm.getStateRecord(this.context.getRenderManager()), this.context.getGL());
			else
				this.resetUniformRecord(p);
		}
	}
	
	private void resetUniformRecord(GLSLShaderProgram prog) {
		GLSLProgramRecord r = (GLSLProgramRecord)prog.getStateRecord(this.context.getRenderManager());
		Set<GLSLUniform> vars = prog.getAvailableUniforms();
		for (GLSLUniform ur : vars) {
			this.uniforms[r.id].get(ur).lastValue = null;
		}
	}
	
	public void disableManager(StateManager manager) {
		this.lastManager = null;
		this.resetUniformRecord(((GLSLShaderProgramManager)manager).getStateAtom());
	}
	
	public void updateStateAtom(StateAtom a, StateRecord record) {
		GLSLShaderProgram atom = (GLSLShaderProgram)a;
		GLSLProgramRecord r = (GLSLProgramRecord)record;
		
		GL gl = this.context.getGL();

		if (r.compiled && atom.isFixedFunction()) {
			this.deleteProgram(r, gl);
			this.cleanupUniforms(r);
			atom.setCompiled(false, "Fixed Function");
		} else if (!atom.isFixedFunction()) {
			if (r.id <= 0)
				r.id = gl.glCreateProgram();
			this.linkAndCompile(r, atom, this.context);
		}
	}
	
	private void deleteProgram(GLSLProgramRecord r, GL gl) {
		gl.glDeleteProgram(r.id);
		r.id = 0;
		r.compiled = false;
		r.infoLog = "";
	}
	
	private void linkAndCompile(GLSLProgramRecord r, GLSLShaderProgram prog, JOGLRenderContext context) {
		GL gl = context.getGL();
		int[] temp = new int[1];
		gl.glGetProgramiv(r.id, GL.GL_ATTACHED_SHADERS, temp, 0);
		if (temp[0] > 0) {
			int[] old_shaders = new int[temp[0]];
			gl.glGetAttachedShaders(r.id, temp[0], temp, 0, old_shaders, 0);
		
			if (temp[0] > 0) 
				for (int i = 0; i < old_shaders.length; i++)
					gl.glDetachShader(r.id, old_shaders[i]);
		}
		
		boolean valid = true;
		GLSLShaderObject[] shaders = prog.getShaders();
		for (int i = 0; i < shaders.length; i++) {
			shaders[i].applyState(context.getRenderManager(), NullUnit.get());
			GLSLObjectRecord s = (GLSLObjectRecord)shaders[i].getStateRecord(context.getRenderManager());
			if (!s.compiled) {
				valid = false;
				break;
			}
		}
		if (valid) {
			for (int i = 0; i < shaders.length; i++) {
				GLSLObjectRecord s = (GLSLObjectRecord)shaders[i].getStateRecord(context.getRenderManager());
				gl.glAttachShader(r.id, s.id);
			}
			gl.glLinkProgram(r.id);
			
			gl.glGetProgramiv(r.id, GL.GL_INFO_LOG_LENGTH, temp, 0);
			ByteBuffer chars = BufferUtil.newByteBuffer(temp[0]);
			gl.glGetProgramInfoLog(r.id, temp[0], null, chars);
			
			chars.rewind();
			StringBuffer buff = new StringBuffer();
			for (int i = 0; i < temp[0]; i++)
				buff.append((char)chars.get(i));
			r.infoLog = buff.toString();
			
			gl.glGetProgramiv(r.id, GL.GL_LINK_STATUS, temp, 0);
			if (temp[0] == GL.GL_FALSE) {
				r.infoLog = "Program failed to link, error msg:\n" + r.infoLog;
				r.compiled = false;
				this.cleanupUniforms(r);
			} else {
				r.infoLog = "Program linked successfully";
				r.compiled = true;
				this.detectUniforms(prog, r, gl);
			}
		} else {
			r.compiled = false;
			r.infoLog = "GLSL shaders didn't compile, can't link program";
		}
		prog.setCompiled(r.compiled, r.infoLog);
	}
	
	private void detectAttrs(GLSLShaderProgram prog, GLSLProgramRecord r, GL gl) {
		int[] totalA = new int[1];
		gl.glGetProgramiv(r.id, GL.GL_ACTIVE_ATTRIBUTES, totalA, 0);
		if (r.id >= this.attrs.length) {
			HashMap[] temp = new HashMap[r.id + 1];
			System.arraycopy(this.attrs, 0, temp, 0, this.attrs.length);
			this.attrs = temp;
		}
		
		HashMap<GLSLAttribute, AttributeRecord> map = new HashMap<GLSLAttribute, AttributeRecord>();
		this.attrs[r.id] = map;
		int[] maxName = new int[1];
		gl.glGetProgramiv(r.id, GL.GL_ACTIVE_ATTRIBUTE_MAX_LENGTH, maxName, 0);
		byte[] chars = new byte[maxName[0]];
		int[] type = new int[1];
		int[] size = new int[1];
		int[] nameL = new int[1];
		
		Set<GLSLAttribute> allAtt = prog.getAvailableVertexAttributes();
		Set<Integer> usedBindings = new HashSet<Integer>();
		
		for (int i = 0; i < totalA[0]; i++) {
			GLSLAttribute u;
			gl.glGetActiveAttrib(r.id, i, maxName[0], nameL, 0, size, 0, type, 0, chars, 0);
			StringBuffer buff = new StringBuffer();
			for (int c = 0; c < nameL[0]; c++) {
				buff.append((char)chars[c]);
			}
			
			AttributeType t = getAttributeType(type[0]);
			String name = buff.toString().trim();
			if (t != null && name.indexOf("gl") != 0) {
				u = new GLSLAttribute(name, t);
				AttributeRecord ar = new AttributeRecord();
				ar.location = gl.glGetAttribLocation(r.id, u.getName());
				u.setBinding(-1);
				if (allAtt != null && allAtt.contains(u)) {
					u = prog.getVertexAttributeByName(u.getName());
					usedBindings.add(u.getBinding());
				}
				map.put(u, ar);
			} else {
				System.out.println("Ignored attribute " + name + " " + t + " " + type[0]);
			}
		}
		
		int bindingCounter = 0;
		allAtt = map.keySet();
		ArrayList<GLSLAttribute> alphaSorted = new ArrayList<GLSLAttribute>();
		for (GLSLAttribute a : allAtt) {
			if (a.getBinding() < 0)
				alphaSorted.add(a);
		}
		
		Collections.sort(alphaSorted, new Comparator<GLSLAttribute>() {
			public int compare(GLSLAttribute a1, GLSLAttribute a2) {
				return a1.getName().compareTo(a2.getName());
			}
		});
		for (GLSLAttribute a : alphaSorted) {
			while(usedBindings.contains(bindingCounter))
				bindingCounter++;
			a.setBinding(bindingCounter);
			usedBindings.add(bindingCounter);
		}
		
		prog.setAvailableVertexAttributes(allAtt);
		this.updateAttributeBindings(prog, r, gl);
	}
	
	private void updateAttributeBindings(GLSLShaderProgram prog, GLSLProgramRecord r, GL gl) {
		Set<GLSLAttribute> allAtt = prog.getAvailableVertexAttributes();
		if (allAtt == null)
			this.detectAttrs(prog, r, gl);
		else {
			AttributeRecord ar;
			for (GLSLAttribute a : allAtt) {
				ar = this.attrs[r.id].get(a);
				if (ar != null)
					ar.updateAttribute(a, r.id, gl);
			}
		}
	}
	
	private AttributeType getAttributeType(int gl) {
		switch(gl) {
		case GL.GL_FLOAT: return AttributeType.FLOAT;
		case GL.GL_FLOAT_VEC2: return AttributeType.VEC2F;
		case GL.GL_FLOAT_VEC3: return AttributeType.VEC3F;
		case GL.GL_FLOAT_VEC4: return AttributeType.VEC4F;
		case GL.GL_FLOAT_MAT2: return AttributeType.MAT2F;
		case GL.GL_FLOAT_MAT3: return AttributeType.MAT3F;
		case GL.GL_FLOAT_MAT4: return AttributeType.MAT4F;
		}
		return null;
	}
	
	private void detectUniforms(GLSLShaderProgram prog, GLSLProgramRecord r, GL gl) {
		int[] totalU = new int[1];
		gl.glGetProgramiv(r.id, GL.GL_ACTIVE_UNIFORMS, totalU, 0);
		if (r.id >= this.uniforms.length) {
			HashMap[] temp = new HashMap[r.id + 1];
			System.arraycopy(this.uniforms, 0, temp, 0, this.uniforms.length);
			this.uniforms = temp;
		}
		
		HashMap<GLSLUniform, UniformRecord> map = new HashMap<GLSLUniform, UniformRecord>();
		this.uniforms[r.id] = map;
		int[] maxName = new int[1];
		gl.glGetProgramiv(r.id, GL.GL_ACTIVE_UNIFORM_MAX_LENGTH, maxName, 0);
		byte[] chars = new byte[maxName[0]];
		int[] type = new int[1];
		int[] size = new int[1];
		int[] nameL = new int[1];
		
		for (int i = 0; i < totalU[0]; i++) {
			GLSLUniform u;
			gl.glGetActiveUniform(r.id, i, maxName[0], nameL, 0, size, 0, type, 0, chars, 0);
			StringBuffer buff = new StringBuffer();
			for (int c = 0; c < nameL[0]; c++) {
				buff.append((char)chars[c]);
			}
			UniformType t = getArrayType(getUniformType(type[0]), size[0]);
			String name = buff.toString().trim();
			if (t != null && name.indexOf("gl") != 0) {
				u = new GLSLUniform(name, t, size[0]);
				UniformRecord ur = new UniformRecord();
				ur.lastValue = null;
				ur.location = gl.glGetUniformLocation(r.id, u.getName());
				ur.variable = u;
				map.put(u, ur);
			} else {
				System.out.println("Ignored uniform " + name + " " + t + " " + type[0]);
			}
		}
		
		prog.setAvailableUniforms(map.keySet());
	}
	
	private static UniformType getArrayType(UniformType base, int size) {
		if (size <= 1)
			return base;
		switch(base) {
		case FLOAT: return UniformType.FLOAT_ARRAY;
		case INT: return UniformType.INT_ARRAY;
		case BOOLEAN: return UniformType.BOOLEAN_ARRAY;
		case VEC2F: return UniformType.VEC2F_ARRAY;
		case VEC3F: return UniformType.VEC3F_ARRAY;
		case VEC4F: return UniformType.VEC4F_ARRAY;
		case VEC2I: return UniformType.VEC2I_ARRAY;
		case MAT3F: return UniformType.MAT3F_ARRAY;
		case MAT4F: return UniformType.MAT4F_ARRAY;
		default:
			return null;
		}
	}
	
	private static UniformType getUniformType(int gl) {
		switch(gl) {
		case GL.GL_FLOAT: return UniformType.FLOAT;
		case GL.GL_INT: case GL.GL_SAMPLER_2D:
		case GL.GL_SAMPLER_3D: case GL.GL_SAMPLER_CUBE: case GL.GL_SAMPLER_2D_SHADOW:
			return UniformType.INT;
		case GL.GL_BOOL: return UniformType.BOOLEAN;
		case GL.GL_FLOAT_VEC2: return UniformType.VEC2F;
		case GL.GL_FLOAT_VEC3: return UniformType.VEC3F;
		case GL.GL_FLOAT_VEC4: return UniformType.VEC4F;
		case GL.GL_INT_VEC2: return UniformType.VEC2I;
		case GL.GL_FLOAT_MAT3: return UniformType.MAT3F;
		case GL.GL_FLOAT_MAT4: return UniformType.MAT4F;
		default:
			return null;
		}
	}
	
	private void cleanupUniforms(GLSLProgramRecord r) {
		if (this.uniforms == null || r.id >= this.uniforms.length)
			return;
		this.uniforms[r.id] = null;
	}
	
	private void applyUniforms(GLSLShaderProgramManager prog, GLSLShaderProgram p, GLSLProgramRecord r, GL gl) {
		List<GLSLValuePair> vars = prog.getUniforms();
		Set<GLSLUniform> all = p.getAvailableUniforms();
		
		int size = vars.size();
		GLSLValuePair variable;
		for (int i = 0; i < size; i++) {
			variable = vars.get(i);
			if (all.contains(variable.getVariable())) 
				this.uniforms[r.id].get(variable.getVariable()).setValue(variable.getValue(), gl);
		}
	}
}
