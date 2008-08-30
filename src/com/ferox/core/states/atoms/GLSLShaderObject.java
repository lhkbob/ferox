package com.ferox.core.states.atoms;

import java.util.Iterator;
import java.util.WeakHashMap;

import com.ferox.core.states.NullUnit;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.ChunkableInstantiator;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

/**
 * GLSLShaderObject represents the source code for a vertex or fragment shader object in the glsl language.
 * They can't be applied directly using ShaderManager's, instead you link them to GLSLShaderPrograms which 
 * function similarly to how programs are used in opengl.
 * @author Michael Ludwig
 *
 */
public class GLSLShaderObject extends StateAtom implements ChunkableInstantiator {
	public static enum GLSLType {
		VERTEX, FRAGMENT
	}
	
	private WeakHashMap<GLSLShaderProgram, Boolean> linkedPrograms;
	
	private String[] source;
	private GLSLType type;
	private String infoLog;
	
	/**
	 * Creates a given glsl of type VERTEX_SHADER or FRAGMENT_SHADER with
	 * the source code in the array of strings (source is concatentation of the elements, which
	 * shouldn't be null strings).
	 */
	public GLSLShaderObject(String[] source, GLSLType type) throws IllegalArgumentException {
		this();
		this.source = source;
		this.type = type;
		this.infoLog = "";
	}
	
	private GLSLShaderObject() { 
		super();
		this.linkedPrograms = new WeakHashMap<GLSLShaderProgram, Boolean>();
	}
	
	/**
	 * Get the info log after the shader object was compiled, empty string if compilation was 
	 * successful.
	 */
	public String getInfoLog() {
		return this.infoLog;
	}
	
	/**
	 * Sets the info log to the given log, shouldn't be set by the user.  Instead, it should be used
	 * by a shader atom peer to properly set the log after it is compiled on the graphics card.
	 */
	public void setInfoLog(String log) {
		this.infoLog = log;
	}
	
	/**
	 * Get the shader type, either VERTEX_SHADER, or FRAGMENT_SHADER.
	 */
	public GLSLType getShaderType() {
		return this.type;
	}
	
	/**
	 * Get the source code, any modification to the source will not be reflected until update
	 * is called (as per rules of GLVisitors).
	 */
	public String[] getSource() {
		return this.source;
	}
	
	/**
	 * Sets the source code for the shader, won't take affect until after an update.
	 */
	public void setSource(String[] source) {
		this.source = source;
	}
	
	@Override
	public void updateStateAtom() {
		Iterator<GLSLShaderProgram> p = this.linkedPrograms.keySet().iterator();
		while (p.hasNext()) 
			p.next().updateStateAtom();
		super.updateStateAtom();
	}
	
	boolean linkToProgram(GLSLShaderProgram prog) {
		if (this.linkedPrograms.containsKey(prog))
			return false;
		this.linkedPrograms.put(prog, true);
		return true;
	}
	
	void unlinkFromProgram(GLSLShaderProgram prog) {
		this.linkedPrograms.remove(prog);
	}

	public Class<? extends Chunkable> getChunkableClass() {
		return GLSLShaderObject.class;
	}

	public Chunkable newInstance() {
		return new GLSLShaderObject();
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.type = in.getEnum("type", GLSLType.class);
		this.source = in.getStringArray("source");
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setEnum("type", this.type);
		out.setStringArray("source", this.source);
	}

	@Override
	public Class<GLSLShaderObject> getAtomType() {
		return GLSLShaderObject.class;
	}

	@Override
	public boolean isValidUnit(StateUnit unit) {
		return unit instanceof NullUnit;
	}
}
