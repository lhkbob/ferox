package com.ferox.impl.jsr231.peers;

import java.nio.ByteBuffer;

import javax.media.opengl.GL;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateAtom.StateRecord;
import com.ferox.core.states.atoms.GLSLShaderProgram;
import com.ferox.core.util.FeroxException;
import com.ferox.impl.jsr231.JOGLRenderContext;
import com.sun.opengl.util.BufferUtil;

public class JOGLGLSLShaderProgramPeer extends SimplePeer<GLSLShaderProgram, GLSLProgramRecord> {
	public JOGLGLSLShaderProgramPeer(JOGLRenderContext context) {
		super(context);
	}
	
	protected void applyState(GLSLShaderProgram prevA, GLSLProgramRecord prev, GLSLShaderProgram nextA, GLSLProgramRecord next, GL gl) {
		if (next.compiled) 
			gl.glUseProgram(next.id);
		else if (prev.compiled)
			gl.glUseProgram(0);
	}

	public void cleanupStateAtom(StateRecord record) {
		if (((GLSLProgramRecord)record).id > 0)
			this.deleteProgram((GLSLProgramRecord)record, this.context.getGL());
	}

	public StateRecord initializeStateAtom(StateAtom a) {
		GLSLShaderProgram atom = (GLSLShaderProgram)a;
		
		GL gl = this.context.getGL();
		GLSLProgramRecord r = new GLSLProgramRecord();
		
		if (this.isPossiblyValid(atom)) {
			r.id = gl.glCreateProgram();
			this.linkAndCompile(r, atom, this.context);
		} else {
			r.id = 0;
			r.compiled = false;
			r.infoLog = "Fixed Function";
			atom.setInfoLog("Program not compiled/linked, requies non-ff program and that glsl is supported");
		}
		
		return null;
	}
	
	protected void restoreState(GLSLShaderProgram cleanA, GLSLProgramRecord cleanR, GL gl) {
		if (cleanR.compiled)
			gl.glUseProgram(0);
	}

	public void updateStateAtom(StateAtom a, StateRecord record) {
		GLSLShaderProgram atom = (GLSLShaderProgram)a;
		GLSLProgramRecord r = (GLSLProgramRecord)record;
		
		GL gl = this.context.getGL();
		r.compiled = this.isPossiblyValid(atom);

		if (r.compiled) {
			if (r.id <= 0) 
				r.id = gl.glCreateProgram();
			this.linkAndCompile(r, atom, this.context);
		} else if (r.id > 0) {
			this.deleteProgram(r, gl);
			atom.setInfoLog("ShaderProgram destroyed");
		}
	}

	private boolean isPossiblyValid(GLSLShaderProgram atom) {
		return RenderManager.getSystemCapabilities().areGLSLShadersSupported() && !atom.isFixedFunction();
	}
	
	private void deleteProgram(GLSLProgramRecord r, GL gl) {
		gl.glDeleteProgram(r.id);
		r.id = 0;
		r.compiled = false;
		r.infoLog = "ShaderProgram Destroyed";
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
		
		for (int i = 0; i < prog.getShaders().length; i++) {
			prog.getShaders()[i].applyState(context.getRenderManager(), NullUnit.get());
			GLSLObjectRecord s = (GLSLObjectRecord)prog.getShaders()[i].getStateRecord(context.getRenderManager());
			if (s.compiled)
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
			r.infoLog = "Program failed to link, error msg: " + r.infoLog;
			prog.setInfoLog(r.infoLog);
			r.compiled = false;
			throw new FeroxException("ShaderProgram failed to link correctly " + r.infoLog);
		} else {
			r.infoLog = "Program linked successfully";
			r.compiled = true;
			prog.setInfoLog(r.infoLog);
		}
	}
}
