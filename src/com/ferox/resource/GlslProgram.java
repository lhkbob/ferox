package com.ferox.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ferox.resource.GlslUniform.UniformType;
import com.ferox.resource.GlslVertexAttribute.AttributeType;

/** GlslProgram represents a custom shader program written in glsl.
 * The exact shader model and glsl syntax supported depends on the 
 * hardware support for that model.  At the moment only vertex and
 * fragment shaders are allowed.  When geometry shaders become more
 * common, they will likely be added in.
 * 
 * GlslPrograms are a fairly complicated resource for the Renderer
 * to deal with.  Here are a few guidelines:
 * - If either shader fails to compile, it should have a status of ERROR
 * - If it fails to link, it has a status of ERROR
 * - If valid, bound GlslVertexAttributes overlap attribute slots, it should
 *   have a status of ERROR
 * - Invalid attributes must be unbound
 * - Invalid GlslUniforms must be left attached, unless they conflict with the
 *   name of a valid uniform
 * - Attributes and uniforms declared in the glsl code should be bound/attached
 *   on a success if they aren't already.
 * - Attached uniforms must have a status of ERROR if the program failed to
 *   link.
 * - Attached uniforms should be automatically updated and cleaned-up whenever the program
 *   is updated/cleaned-up.
 * 
 * @author Michael Ludwig
 *
 */
public class GlslProgram implements Resource {
	/** The dirty descriptor used by instances of GlslProgram. */
	public static class GlslProgramDirtyDescriptor {
		private boolean uniformsDirty;
		private boolean attrsDirty;
		private boolean glslDirty;
		
		/** Return true if a GlslUniform was attached or detached. */
		public boolean getUniformsDirty() { return this.uniformsDirty; }
		
		/** Return true if a GlslVertexAttribute was attached or detached. */
		public boolean getAttributesDirty() { return this.attrsDirty; }
		
		/** Return true if the glsl source code has changed for this program.
		 * If this is true, then the program's uniforms and attributes should
		 * be reloaded, too. */
		public boolean getGlslCodeDirty() { return this.glslDirty; }
		
		protected void clear() {
			this.uniformsDirty = false;
			this.attrsDirty = false;
			this.glslDirty = false;
		}
	}
	
	private String[] vertexShader;
	private String[] fragmentShader;
	
	private final Map<String, GlslUniform> attachedUniforms;
	private final Map<String, GlslVertexAttribute> boundAttrs;
	
	private final Map<String, GlslUniform> readOnlyUniforms;
	private final Map<String, GlslVertexAttribute> readOnlyAttrs;
	
	private final GlslProgramDirtyDescriptor dirty;
	
	private Object renderData;
	
	/** Create a new GlslProgram with empty source code. */
	public GlslProgram() {
		this(null, null);
	}
	
	/** Create a new GlslProgram with the given vertex and fragment
	 * shaders.  These string arrays will be passed directly into
	 * setVertexShader() and setFragmentShader(). */
	public GlslProgram(String[] vertex, String[] fragment) {
		this.dirty = this.createDescriptor();
		if (this.dirty == null)
			throw new NullPointerException("Sub-class returned a null dirty descriptor");
		
		this.attachedUniforms = new HashMap<String, GlslUniform>();
		this.boundAttrs = new HashMap<String, GlslVertexAttribute>();
		
		this.readOnlyUniforms = Collections.unmodifiableMap(this.attachedUniforms);
		this.readOnlyAttrs = Collections.unmodifiableMap(this.boundAttrs);
		
		this.setVertexShader(vertex);
		this.setFragmentShader(fragment);
	}
	
	/** Functions identically to attachUniform() except that it is for GlslVertexAttributes.
	 * This method does not check for overlap in binding slots, but if there is any overlap
	 * at the time of a Renderer update(), this program must have its status set to ERROR. 
	 * 
	 * Instead of length, however, bindingSlot is used for the attribute.  See GlslVertexAttribute
	 * for how the binding slot is used. */
	public GlslVertexAttribute bindAttribute(String name, AttributeType type, int bindingSlot) throws IllegalStateException, IllegalArgumentException, NullPointerException {
		if (name == null)
			throw new NullPointerException("Name cannot be null");
		
		GlslVertexAttribute u = this.boundAttrs.get(name);
		if (u != null)
			throw new IllegalStateException("Cannot re-bind an attribute with the same name: " + name);
		else
			u = new GlslVertexAttribute(name, type, bindingSlot, this); // throws IllegalArgumentException/NullPointerException
		
		this.boundAttrs.put(name, u);
		this.dirty.attrsDirty = true;
		return u;
	}
	
	/** Unbind an attribute so that it is no longer stored by this
	 * GlslProgram.  After a GlslVertexAttribute is unbound, it
	 * should no longer be used, since it represents invalid information.
	 * 
	 * Because a GlslVertexAttribute is not a resource, and is only 
	 * a convenient packaging for the binding of glsl attributes, a Renderer
	 * will unbind any attribute that is invalid. */
	public GlslVertexAttribute unbindAttribute(String name) {
		if (name == null)
			return null;
		
		GlslVertexAttribute a = this.boundAttrs.remove(name);
		if (a != null)
			this.dirty.attrsDirty = true;
		
		return a;
	}
	
	/** Return all currently bound GlslVertexAttributes, in an 
	 * unmodifiable map from attribute name to GlslVertexAttribute. */
	public Map<String, GlslVertexAttribute> getAttributes() {
		return this.readOnlyAttrs;
	}
	
	/** Attach a new GlslUniform instance to this GlslProgram so that it will
	 * be automatically updated and cleaned-up with this program instance.
	 * 
	 * It is possible for programmers to manually attach the uniforms, to 
	 * use the instances sooner, or they may let the Renderer attach all
	 * uniforms detected by the glsl compiler.
	 * 
	 * This method will fail if name or type is null, if length < 0, 
	 * or if there is an already attached uniform of the same name 
	 * as requested.  That uniform must be detached before a successful,
	 * new attach may be performed.
	 * 
	 * When a glsl program is updated, if it links successfully, it should
	 * attach any uniform that is present in the code, that's not already
	 * been attached.  It should not detach invalid uniforms unless they
	 * conflict with the name of a valid uniform.  In either case, invalid
	 * uniforms must have their status set to ERROR.
	 * 
	 * If a program is not linked successfully, all attached uniforms
	 * must have a status of ERROR. */
	public GlslUniform attachUniform(String name, UniformType type, int length) throws IllegalStateException, IllegalArgumentException, NullPointerException {
		if (name == null)
			throw new NullPointerException("Name cannot be null");
		
		GlslUniform u = this.attachedUniforms.get(name);
		if (u != null)
			throw new IllegalStateException("Cannot re-attach a uniform with the same name: " + name);
		else
			u = new GlslUniform(name, type, length, this); // throws IllegalArgumentException/NullPointerException
		
		this.attachedUniforms.put(name, u);
		this.dirty.uniformsDirty = true;
		return u;
	}
	
	/** Detach and return the GlslUniform with the given name.
	 * Returns null if a uniform wasn't previously attached with
	 * that name, or if name is null.
	 * 
	 * This allows the given name to be used in an attachUniform()
	 * call without having to worry about an IllegalStateException.
	 * 
	 * The uniform will be auto-matically cleaned-up the next time
	 * the program is updated or cleaned-up and should no longer be
	 * used in GlslShaders.  After that clean-up, its status will no
	 * longer be updated by the program's.
	 * 
	 * The purpose of this method is to allow programmers to detach
	 * uniforms that have a status of ERROR, or the for the Renderer
	 * to remove erroneous uniforms conflicting with valid uniforms of
	 * the same name. */
	public GlslUniform detachUniform(String name) {
		if (name == null)
			return null;
		
		GlslUniform u = this.attachedUniforms.remove(name);
		if (u != null)
			this.dirty.uniformsDirty = true;
		
		return u;
	}
	
	/** Return an unmodifiable map of all currently attached GlslUniforms,
	 * accessed by the uniform name. */
	public Map<String, GlslUniform> getUniforms() {
		return this.readOnlyUniforms;
	}
	
	/** Return the array of strings that store the glsl code for
	 * executing the vertex stage of a pipeline.  If the returned 
	 * array has a length of 0, then no vertex shader is used by
	 * this program.
	 * 
	 * If the array is modified, there will be undefined results. */
	public String[] getVertexShader() {
		return this.vertexShader;
	}
	
	/** Return the array of strings that store the glsl code for
	 * executing the fragment stage of a pipeline.  If the returned
	 * array has a length of 0, then no fragment shader is used
	 * by this program.
	 * 
	 * If the array is modified, there will be undefined results. */
	public String[] getFragmentShader() {
		return this.fragmentShader;
	}
	
	/** Set the glsl code that executes during the vertex stage of the pipeline.
	 * If source is null, or (after ignoring null elements) there are no source
	 * lines, getVertexShader() will return an array of length 0.
	 * 
	 * If not, then all non-null elements will be copied into a new array.
	 * This is to prevent accidental tampering, and to allow programmers to
	 * assume that all String elements in the returned array will not be null. */
	public void setVertexShader(String[] source) {
		if (source == null) {
			this.vertexShader = new String[0];
		} else {
			int nonNullCount = 0;
			for (int i = 0; i < source.length; i++) {
				if (source[i] != null)
					nonNullCount++;
			}
			
			this.vertexShader = new String[nonNullCount];
			nonNullCount = 0;
			for (int i = 0; i < source.length; i++) {
				if (source[i] != null) {
					this.vertexShader[nonNullCount++] = source[i];
				}
			}
		}
		
		this.dirty.glslDirty = true;
	}
	
	/** Set the glsl code that executes during the fragment stage of the pipeline.
	 * If source is null, or (after ignoring null elements) there are no source
	 * lines, getFragmentShader() will return an array of length 0.
	 * 
	 * If not, then all non-null elements will be copied into a new array.
	 * This is to prevent accidental tampering, and to allow programmers to
	 * assume that all String elements in the returned array will not be null. */
	public void setFragmentShader(String[] source) {
		if (source == null) {
			this.fragmentShader = new String[0];
		} else {
			int nonNullCount = 0;
			for (int i = 0; i < source.length; i++) {
				if (source[i] != null)
					nonNullCount++;
			}
			
			this.fragmentShader = new String[nonNullCount];
			nonNullCount = 0;
			for (int i = 0; i < source.length; i++) {
				if (source[i] != null) {
					this.fragmentShader[nonNullCount++] = source[i];
				}
			}
		}
		
		this.dirty.glslDirty = true;
	}
	
	/** Construct the dirty descriptor to use for this program. 
	 * Must not return null, or the constructor will throw an
	 * exception. */
	protected GlslProgramDirtyDescriptor createDescriptor() {
		return new GlslProgramDirtyDescriptor();
	}
	
	@Override
	public void clearDirtyDescriptor() {
		this.dirty.clear();
	}

	@Override
	public final GlslProgramDirtyDescriptor getDirtyDescriptor() {
		return this.dirty;
	}

	@Override
	public Object getResourceData() {
		return this.renderData;
	}

	@Override
	public void setResourceData(Object data) {
		this.renderData = data;
	}
}
