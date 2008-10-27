package com.ferox.impl.jsr231.peers;

import java.nio.ByteBuffer;
import java.util.Arrays;

import javax.media.opengl.GL;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUpdateException;
import com.ferox.core.states.StateAtom.StateRecord;
import com.ferox.core.states.atoms.GLSLShaderObject;
import com.ferox.core.states.atoms.GLSLShaderObject.GLSLType;
import com.ferox.core.util.FeroxException;
import com.ferox.impl.jsr231.JOGLRenderContext;
import com.sun.opengl.util.BufferUtil;

public class JOGLGLSLShaderObjectPeer extends SimplePeer<GLSLShaderObject, GLSLObjectRecord> {
	public static int getGLShaderType(GLSLType type) {
		switch(type) {
		case FRAGMENT: return GL.GL_FRAGMENT_SHADER;
		case VERTEX: return GL.GL_VERTEX_SHADER;
		default:
			throw new FeroxException("Unsupported glsl shader type");
		}
	}
	
	public JOGLGLSLShaderObjectPeer(JOGLRenderContext context) {
		super(context);
	}
	
	protected void applyState(GLSLShaderObject prev, GLSLObjectRecord prevR, GLSLShaderObject next, GLSLObjectRecord nextR, GL gl) {
		// do nothing
	}

	protected void restoreState(GLSLShaderObject clean, GLSLObjectRecord cleanR, GL gl) {
		// do nothing
	}
	
	public void cleanupStateAtom(StateRecord record) {
		GLSLObjectRecord r = (GLSLObjectRecord)record;
		if (r.id > 0)
			this.destroyShader(this.context.getGL(), r);
	}

	public StateRecord initializeStateAtom(StateAtom a) {
		GLSLShaderObject atom = (GLSLShaderObject)a;
		
		GL gl = this.context.getGL();
		GLSLObjectRecord r = new GLSLObjectRecord();
		
		r.type = getGLShaderType(atom.getShaderType());
		r.id = gl.glCreateShader(r.type);
		this.attachSourceAndCompile(gl, r, atom);
		
		return r;
	}

	public void updateStateAtom(StateAtom atom, StateRecord record) {
		this.updateStateAtom((GLSLShaderObject)atom, (GLSLObjectRecord)record);
	}
	
	private void updateStateAtom(GLSLShaderObject atom, GLSLObjectRecord record) {
		GL gl = ((JOGLRenderContext)context).getGL();
		GLSLObjectRecord r = (GLSLObjectRecord)record;
		
		if (r.type <= 0 || r.id <= 0) {
			r.type = getGLShaderType(atom.getShaderType());
			r.id = gl.glCreateShader(r.type);
		}
		this.attachSourceAndCompile(gl, r, atom);
	}
	
	public void validateStateAtom(StateAtom atom) throws StateUpdateException {
		if (!RenderManager.getSystemCapabilities().areGLSLShadersSupported()) {
			((GLSLShaderObject)atom).setCompiled(false, "GLSL isn't supported");
			throw new StateUpdateException(atom, "GLSL is not supported on this device");
		}
	}
	
	private void destroyShader(GL gl, GLSLObjectRecord r) {
		gl.glDeleteShader(r.id);
		r.id = 0;
		r.type = 0;
		r.compiled = false;
		r.infoLog = "";
	}
	
	private void attachSourceAndCompile(GL gl, GLSLObjectRecord r, GLSLShaderObject atom) {
		String[] source = atom.getSource();
		int[] temp = new int[source.length];
		for (int i = 0; i < temp.length; i++)
			temp[i] = source[i].length();
		gl.glShaderSource(r.id, source.length, source, temp, 0);
		gl.glCompileShader(r.id);
		temp = new int[1];
		gl.glGetShaderiv(r.id, GL.GL_INFO_LOG_LENGTH, temp, 0);
		ByteBuffer chars = BufferUtil.newByteBuffer(temp[0]);
		if (temp[0] > 0)
			gl.glGetShaderInfoLog(r.id, temp[0], null, chars);
		chars.rewind();
		StringBuffer buff = new StringBuffer();
		for (int i = 0; i < temp[0]; i++)
			buff.append((char)chars.get(i));
		r.infoLog = buff.toString();
		
		gl.glGetShaderiv(r.id, GL.GL_COMPILE_STATUS, temp, 0);
		if (temp[0] == GL.GL_FALSE) {
			r.infoLog = "ShaderObject failed to compile, error msg:\n" + r.infoLog;
			r.compiled = false;
		} else {
			r.infoLog = "ShaderObject compiled successfully, id = "+ r.id + " type = " + (r.type == GL.GL_VERTEX_SHADER ? "GL_VERTEX_SHADER" : "GL_FRAGMENT_SHADER");
			r.compiled = true;
		}
		atom.setCompiled(r.compiled, r.infoLog);
	}
}

