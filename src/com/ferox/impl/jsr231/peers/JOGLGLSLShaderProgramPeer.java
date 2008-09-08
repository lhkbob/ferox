package com.ferox.impl.jsr231.peers;

import java.nio.ByteBuffer;

import javax.media.opengl.GL;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUpdateException;
import com.ferox.core.states.StateAtom.StateRecord;
import com.ferox.core.states.atoms.GLSLShaderObject;
import com.ferox.core.states.atoms.GLSLShaderProgram;
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

	public void validateStateAtom(StateAtom atom) throws StateUpdateException {
		if (RenderManager.getSystemCapabilities().areGLSLShadersSupported())
			throw new StateUpdateException(atom, "GLSL is not supported on this device");
	}
	
	public void cleanupStateAtom(StateRecord record) {
		GLSLProgramRecord p = (GLSLProgramRecord)record;
		if (p.id > 0)
			this.deleteProgram(p, this.context.getGL());
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

		if (r.compiled && atom.isFixedFunction()) {
			this.deleteProgram(r, gl);
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
			} else {
				r.infoLog = "Program linked successfully";
				r.compiled = true;
			}
			prog.setCompiled(r.compiled, r.infoLog);
		} else {
			r.compiled = false;
			r.infoLog = "Uncompiled shaders";
			prog.setCompiled(false, "GLSL shaders didn't compile, can't link program");
		}
	}
}
