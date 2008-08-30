package com.ferox.core.states.manager;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.atoms.GLSLShaderProgram;

public class GLSLShaderProgramManager extends UniqueStateManager<GLSLShaderProgram> {
		
	public GLSLShaderProgramManager() {
		super();
	}
	
	public GLSLShaderProgramManager(GLSLShaderProgram state) {
		super(state);
	}

	@Override
	public Class<? extends StateAtom> getAtomType() {
		return GLSLShaderProgram.class;
	}
}
