package com.ferox.resource.glsl;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ferox.resource.Resource;
import com.ferox.resource.glsl.GlslUniform.UniformType;
import com.ferox.resource.glsl.GlslVertexAttribute.AttributeType;

/** GlslProgram represents a custom shader program written in glsl.
 * The exact shader model and glsl syntax supported depends on the 
 * hardware support for that model.  At the moment only vertex and
 * fragment shaders are allowed.  When geometry shaders become more
 * common, they will likely be added in.
 * 
 * GlslPrograms are a fairly complicated resource for the Renderer
 * to deal with.  Here are a few guidelines:
 * - If either shader fails to compile, it should have a status of ERROR
 * - Overlapping attribute slots cause a status of ERROR.
 * - If it fails to link, it has a status of ERROR.  Uniforms and attributes
 *   are left attached (uniforms status should be changed, though)
 * - If it does link, uniforms and attributes not declared in the code
 *   should be detached and cleaned-up
 * - Attributes and uniforms declared in the glsl code should be bound/attached
 *   on a success if they aren't already attached, when successfully linked.
 *   
 * Care should be given when using the uniforms and attributes attached
 * to a program.  If the program undergoes significant source code change, it
 * is likely that the uniforms will have been detached.  Only attach a Uniform
 * or attribute if you know that they are present in the code (even with compile errors);
 * if not, use the automatically attached uniforms after a successful update.
 * 
 * @author Michael Ludwig
 *
 */
// FIXME: vertex attributes should start at 1, not 0
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
	private final BitSet usedAttributeSlots;

	private final Map<String, GlslUniform> readOnlyUniforms;
	private final Map<String, GlslVertexAttribute> readOnlyAttrs;
	
	
	private final GlslProgramDirtyDescriptor dirty;
	
	private Object renderData;
	
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
		
		this.usedAttributeSlots = new BitSet();
		
		this.setShaderCode(vertex, fragment);
	}
	
	/** Functions identically to attachUniform() except that it is for GlslVertexAttributes.
	 * This also fails if the requested attribute cannot fit within the available slots.
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
		
		// check if they're used
		int maxSlot = bindingSlot + type.getSlotCount();
		for (int i = bindingSlot; i < maxSlot; i++) {
			if (this.usedAttributeSlots.get(i))
				throw new IllegalStateException("Requested attribute does not have enough room at the given slot: " + bindingSlot);
		}
		
		// mark slots as used
		for (int i = bindingSlot; i < maxSlot; i++)
			this.usedAttributeSlots.set(i, true);
		
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
	 * will unbind any attribute that is invalid (don't unbind if the
	 * shader doesn't compile, since the attribute may be "valid"). */
	public GlslVertexAttribute unbindAttribute(String name) {
		if (name == null)
			return null;
		
		GlslVertexAttribute a = this.boundAttrs.remove(name);
		if (a != null) {
			// mark the used slots as unused
			int maxSlot = a.getBindingSlot() + a.getType().getSlotCount();
			for (int i = a.getBindingSlot(); i < maxSlot; i++)
				this.usedAttributeSlots.set(i, false);
			this.dirty.attrsDirty = true;
		}
		
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
	 * It is allowed for programmers to manually attach the uniforms, to 
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
	 * been attached.  It will detach and clean-up unvalid uniforms. 
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
	 * The purpose of this method is to allow programmers to detach
	 * uniforms that have a status of ERROR, or the for the Renderer
	 * to remove erroneous uniforms conflicting with valid uniforms of
	 * the same name.
	 * 
	 * When a uniform is detached, the detacher is responsible
	 * for cleaning it up with the renderer. */
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
	
	/** Set the glsl code that executes during the vertex and fragment
	 * stages of the pipeline.
	 * 
	 * This method will copy each of the arrays over into new arrays,
	 * removing any null elements.  A null input array is treated like
	 * a String[0].  
	 * 
	 * If after copying, both vertex and fragment shaders sources have
	 * a length of 0, then an exception is thrown: at least one portion
	 * of the pipeline must be replaced by the shader. */
	public void setShaderCode(String[] vertex, String[] fragment) throws IllegalArgumentException {
		vertex = compress(vertex);
		fragment = compress(fragment);
		
		if (vertex.length == 0 && fragment.length == 0)
			throw new IllegalArgumentException("Must specify at least one non-empty shader source");
		
		this.vertexShader = vertex;
		this.fragmentShader = fragment;
		
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
	
	/* Utility to remove null strings from the source. */
	private static String[] compress(String[] source) {
		if (source == null) {
			return new String[0];
		} else {
			int nonNullCount = 0;
			for (int i = 0; i < source.length; i++) {
				if (source[i] != null)
					nonNullCount++;
			}
			
			String[] res = new String[nonNullCount];
			nonNullCount = 0;
			for (int i = 0; i < source.length; i++) {
				if (source[i] != null) {
					res[nonNullCount++] = source[i];
				}
			}
			
			return res;
		}
	}
}
