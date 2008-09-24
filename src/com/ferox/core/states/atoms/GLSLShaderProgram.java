package com.ferox.core.states.atoms;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

/**
 * ShaderProgram represents a glsl shader program (which is a collection of shader objects that
 * replace the vertex and/or fragment portions of the fixed function rendering pipeline for gl).
 * @author Michael Ludwig
 *
 */
public class GLSLShaderProgram extends StateAtom {
	private boolean fixedFunction;
	private GLSLShaderObject[] shaders;
	private boolean hasVertexShader;
	private boolean hasFragmentShader;
	
	private String infoLog;
	private boolean compiled;
	private Set<GLSLUniform> allUniforms;
	private Set<GLSLAttribute> allAttrs;
	
	public GLSLShaderProgram() {
		this(null);
	}
	
	/**
	 * Creates a shader program with the given shader objects.
	 */
	public GLSLShaderProgram(GLSLShaderObject[] shaders) {
		super();
		this.setCompiled(false, "");
		this.setShaders(shaders);
	}

	/**
	 * Gets the info log returned by opengl after linking the shader objects, it is an empty string
	 * if the linking was successful.
	 */
	public String getInfoLog() {
		return this.infoLog;
	}

	/**
	 * Get the shaders used for this program, any modification will not be reflected until the 
	 * program has been updated via GLVisitor.  It is not recommended to modify the references within the actual array.
	 * If you'd like to change the shaders, use setShaders() instead.
	 */
	public GLSLShaderObject[] getShaders() {
		return this.shaders;
	}

	/**
	 * Whether or not this shader program is fixed function.
	 * @return
	 */
	public boolean isFixedFunction() {
		return this.fixedFunction;
	}

	/**
	 * Whether or not this shader program overrides gl's fragment fixed pipeline.
	 * @return
	 */
	public boolean isFragmentShaderPresent() {
		return this.hasFragmentShader;
	}

	public boolean isCompiled() {
		return this.compiled;
	}
	
	public void setCompiled(boolean compiled, String msg) {
		this.compiled = compiled;
		this.infoLog = msg;
		if (!this.compiled)
			this.allUniforms = null;
	}
	
	/**
	 * Whether or not this shader program overrides gl's vertex fixed pipeline.
	 */
	public boolean isVertexShaderPresent() {
		return this.hasVertexShader;
	}

	public void setAvailableUniforms(Set<GLSLUniform> uniforms) {
		if (uniforms == null)
			this.allUniforms = null;
		else
			this.allUniforms = Collections.unmodifiableSet(uniforms);
	}
	
	public Set<GLSLUniform> getAvailableUniforms() {
		return this.allUniforms;
	}
	
	public GLSLUniform getUniformByName(String name) {
		if (name == null || this.allUniforms == null)
			return null;
		for (GLSLUniform u : this.allUniforms) {
			if (u.getName().equals(name))
				return u;
		}
		return null;
	}
	
	public void setAvailableVertexAttributes(Set<GLSLAttribute> attrs) {
		if (attrs == null)
			this.allAttrs = null;
		else
			this.allAttrs = Collections.unmodifiableSet(attrs);
	}
	
	public Set<GLSLAttribute> getAvailableVertexAttributes() {
		return this.allAttrs;
	}
	
	public GLSLAttribute getVertexAttributeByName(String name) {
		if (name == null || this.allAttrs == null)
			return null;
		for (GLSLAttribute u : this.allAttrs) {
			if (u.getName().equals(name))
				return u;
		}
		return null;
	}
	
	/**
	 * Sets the shader objects for this program.  Ignores null shader object elements in the array.
	 * If all shaders present are null (or if the array is null), then this program will behave as the fixed function
	 * pipeline of opengl.
	 */
	public void setShaders(GLSLShaderObject[] shaders) {
		if (this.shaders != null) {
			for (int i = 0; i < this.shaders.length; i++)
				this.shaders[i].unlinkFromProgram(this);
		}

		this.hasVertexShader = false;
		this.hasFragmentShader = false;
		
		if (shaders != null) {
			int non_null_count = 0;
			for (int i = 0; i < shaders.length; i++) 
				if (shaders[i] != null)
					non_null_count++;
			this.shaders = (non_null_count > 0 ? new GLSLShaderObject[non_null_count] : null);
			non_null_count = 0;
			for (int i = 0; i < shaders.length; i++) 
				if (shaders[i] != null && shaders[i].linkToProgram(this)) {
					this.shaders[non_null_count++] = shaders[i];
					this.hasVertexShader = this.hasVertexShader || shaders[i].getShaderType() == GLSLShaderObject.GLSLType.VERTEX;
					this.hasFragmentShader = this.hasFragmentShader || shaders[i].getShaderType() == GLSLShaderObject.GLSLType.FRAGMENT;
				}
			this.fixedFunction = this.shaders == null;
		} else {
			this.fixedFunction = true;
			this.shaders = null;
		}
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		Chunkable[] chunks = (Chunkable[])in.getChunkArray("shaders");
		GLSLShaderObject[] shaders = new GLSLShaderObject[chunks.length];
		System.arraycopy(chunks, 0, shaders, 0, shaders.length);
		this.setShaders(shaders);
		
		HashSet<GLSLAttribute> attribs = new HashSet<GLSLAttribute>();
		int count = in.getInt("attr_count");
		for (int i = 0; i < count; i++) {
			attribs.add((GLSLAttribute)in.getChunk("attrib_" + i));
		}
		this.setAvailableVertexAttributes(attribs);
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		Chunkable[] shaders = new Chunkable[this.shaders.length];
		System.arraycopy(this.shaders, 0, shaders, 0, shaders.length);
		out.set("shaders", shaders);
		
		out.set("attr_count", this.allAttrs.size());
		int counter = 0;
		for (GLSLAttribute at: this.allAttrs) {
			out.set("attrib_" + counter, at);
			counter++;
		}
	}

	@Override
	public Class<GLSLShaderProgram> getAtomType() {
		return GLSLShaderProgram.class;
	}

	@Override
	public boolean isValidUnit(StateUnit unit) {
		return unit instanceof NullUnit;
	}
}