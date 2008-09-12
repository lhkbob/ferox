package com.ferox.core.states.manager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.atoms.GLSLShaderProgram;
import com.ferox.core.states.atoms.GLSLUniform;

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
}
