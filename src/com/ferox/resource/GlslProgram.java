package com.ferox.resource;

import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ferox.renderer.Framework;
import com.ferox.resource.GlslUniform.UniformType;
import com.ferox.resource.GlslVertexAttribute.AttributeType;

/**
 * <p>
 * GlslProgram represents a custom shader program written in glsl. The exact
 * shader model and glsl syntax supported depends on the hardware support for
 * that model. At the moment only vertex and fragment shaders are allowed. When
 * geometry shaders become more common, they will likely be added in.
 * </p>
 * <p>
 * GlslPrograms are a fairly complicated resource for the Framework to deal with.
 * Here are a few guidelines: - If either shader fails to compile, it should
 * have a status of ERROR - Overlapping attribute slots cause a status of ERROR.
 * - If it fails to link, it has a status of ERROR. Uniforms and attributes are
 * left attached (uniforms status should be changed, though) - If it does link,
 * uniforms and attributes not declared in the code should be detached and
 * cleaned-up - Attributes and uniforms declared in the glsl code should be
 * bound/attached on a success if they aren't already attached, when
 * successfully linked.
 * </p>
 * <p>
 * Care should be given when using the uniforms and attributes attached to a
 * program. If the program undergoes significant source code change, it is
 * likely that the uniforms will have been detached. Only attach a Uniform or
 * attribute if you know that they are present in the code (even with compile
 * errors); if not, use the automatically attached uniforms after a successful
 * update.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class GlslProgram implements Resource {
	/** The dirty descriptor used by instances of GlslProgram. */
	public static class GlslProgramDirtyDescriptor {
		private boolean uniformsDirty;
		private boolean attrsDirty;
		private boolean glslDirty;

		/** @return True if a GlslUniform was attached or detached. */
		public boolean getUniformsDirty() {
			return uniformsDirty;
		}

		/** @return True if a GlslVertexAttribute was attached or detached. */
		public boolean getAttributesDirty() {
			return attrsDirty;
		}

		/**
		 * Return true if the glsl source code has changed for this program. If
		 * this is true, then the program's uniforms and attributes should be
		 * reloaded, too.
		 * 
		 * @return Whether or not glsl source code is changed
		 */
		public boolean getGlslCodeDirty() {
			return glslDirty;
		}

		/* Subclasses should override this to also clear themselves */
		protected void clear() {
			uniformsDirty = false;
			attrsDirty = false;
			glslDirty = false;
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

	private final RenderDataCache renderData;

	/**
	 * Create a new GlslProgram with the given vertex and fragment shaders.
	 * These string arrays will be passed directly into setVertexShader() and
	 * setFragmentShader().
	 * 
	 * @param vertex String array of glsl code for vertex shader
	 * @param fragment String array of glsl code for fragment shader
	 * @throws IllegalArgumentException if both vertex and fragment are empty
	 *             shaders
	 */
	public GlslProgram(String[] vertex, String[] fragment) {
		dirty = createDescriptor();
		if (dirty == null)
			throw new NullPointerException(
				"Sub-class returned a null dirty descriptor");
		renderData = new RenderDataCache();

		attachedUniforms = new HashMap<String, GlslUniform>();
		boundAttrs = new HashMap<String, GlslVertexAttribute>();

		readOnlyUniforms = Collections.unmodifiableMap(attachedUniforms);
		readOnlyAttrs = Collections.unmodifiableMap(boundAttrs);

		usedAttributeSlots = new BitSet();

		setShaderCode(vertex, fragment);
	}

	/**
	 * <p>
	 * Functions identically to attachUniform() except that it is for
	 * GlslVertexAttributes. This also fails if the requested attribute cannot
	 * fit within the available slots.
	 * </p>
	 * <p>
	 * Instead of length, however, bindingSlot is used for the attribute. See
	 * GlslVertexAttribute for how the binding slot is used.
	 * </p>
	 * 
	 * @param name The name of the vertex attribute
	 * @param type The type of the attribute
	 * @param bindingSlot Which generic attribute slot it should have for a 1st
	 *            slot (some attributes require more than one).
	 * @return The newly bound vertex attribute
	 * @throws NullPointerException if name or type is null
	 * @throws IllegalArgumentException if name starts with 'gl' or if
	 *             bindingSlot < 1
	 * @throws IllegalStateException if an attribute is already bound with name,
	 *             or if there's not enough room for the new attribute
	 */
	public GlslVertexAttribute bindAttribute(String name, AttributeType type,
		int bindingSlot) {
		if (name == null)
			throw new NullPointerException("Name cannot be null");

		GlslVertexAttribute u = boundAttrs.get(name);
		if (u != null)
			throw new IllegalStateException(
				"Cannot re-bind an attribute with the same name: " + name);
		else
			// throws IllegalArgumentException/NullPointerException
			u = new GlslVertexAttribute(name, type, bindingSlot, this);

		// check if they're used
		int maxSlot = bindingSlot + type.getSlotCount();
		for (int i = bindingSlot; i < maxSlot; i++)
			if (usedAttributeSlots.get(i))
				throw new IllegalStateException(
					"Requested attribute does not have enough room at the given slot: "
						+ bindingSlot);

		// mark slots as used
		for (int i = bindingSlot; i < maxSlot; i++)
			usedAttributeSlots.set(i, true);

		boundAttrs.put(name, u);
		dirty.attrsDirty = true;
		return u;
	}

	/**
	 * <p>
	 * Unbind an attribute so that it is no longer stored by this GlslProgram.
	 * After a GlslVertexAttribute is unbound, it should no longer be used,
	 * since it represents invalid information.
	 * </p>
	 * <p>
	 * Because a GlslVertexAttribute is not a resource, and is only a convenient
	 * packaging for the binding of glsl attributes, a Framework will unbind any
	 * attribute that is invalid (don't unbind if the shader doesn't compile,
	 * since the attribute may be "valid").
	 * </p>
	 * 
	 * @param name The name of the vertex attribute to unbind
	 * @return The attribute that has been unbound, null if name was unbound
	 */
	public GlslVertexAttribute unbindAttribute(String name) {
		if (name == null)
			return null;

		GlslVertexAttribute a = boundAttrs.remove(name);
		if (a != null) {
			// mark the used slots as unused
			int maxSlot = a.getBindingSlot() + a.getType().getSlotCount();
			for (int i = a.getBindingSlot(); i < maxSlot; i++)
				usedAttributeSlots.set(i, false);
			dirty.attrsDirty = true;
		}

		return a;
	}

	/**
	 * Return all currently bound GlslVertexAttributes, in an unmodifiable map
	 * from attribute name to GlslVertexAttribute.
	 * 
	 * @return Unmodifiable map of currently bound attributes: <name: attribute>
	 */
	public Map<String, GlslVertexAttribute> getAttributes() {
		return readOnlyAttrs;
	}

	/**
	 * <p>
	 * Attach a new GlslUniform instance to this GlslProgram so that it will be
	 * automatically updated and cleaned-up with this program instance.
	 * <p>
	 * <p>
	 * It is allowed for programmers to manually attach the uniforms to use the
	 * instances sooner, or they wait to let the Framework attach all uniforms
	 * detected by the glsl compiler.
	 * </p>
	 * <p>
	 * When a glsl program is updated, if it links successfully, it should
	 * attach any uniform that is present in the code, that's not already been
	 * attached. It will detach and clean-up unvalid uniforms.
	 * </p>
	 * <p>
	 * If a program is not linked successfully, all attached uniforms should
	 * have a status of ERROR.
	 * </p>
	 * 
	 * @param name Name of the uniform to attach
	 * @param type The uniform type of the new variable
	 * @param length The length of the arrar (or 1 if it's not an array)
	 * @return The attached uniform
	 * @throws NullPointerException if name or type is null
	 * @throws IllegalArgumentException if name starts with 'gl' or length < 1
	 * @throws IllegalStateException if name is already attached
	 */
	public GlslUniform attachUniform(String name, UniformType type, int length) {
		if (name == null)
			throw new NullPointerException("Name cannot be null");

		GlslUniform u = attachedUniforms.get(name);
		if (u != null)
			throw new IllegalStateException(
				"Cannot re-attach a uniform with the same name: " + name);
		else
			u = new GlslUniform(name, type, length, this); // throws
		// IllegalArgumentException/NullPointerException

		attachedUniforms.put(name, u);
		dirty.uniformsDirty = true;
		return u;
	}

	/**
	 * <p>
	 * Detach and return the GlslUniform with the given name. Returns null if a
	 * uniform wasn't previously attached with that name, or if name is null.
	 * </p>
	 * <p>
	 * This allows the given name to be used in an attachUniform() call without
	 * having to worry about an IllegalStateException.
	 * </p>
	 * <p>
	 * The purpose of this method is to allow programmers to detach uniforms
	 * that have a status of ERROR, or the for the Framework to remove erroneous
	 * uniforms conflicting with valid uniforms of the same name.
	 * </p>
	 * <p>
	 * When a uniform is detached, the detacher is responsible for cleaning it
	 * up with the renderer.
	 * </p>
	 * 
	 * @param name The uniform to detach
	 * @return The previousy attached uniform, or null if name wasn't attached
	 */
	public GlslUniform detachUniform(String name) {
		if (name == null)
			return null;

		GlslUniform u = attachedUniforms.remove(name);
		if (u != null)
			dirty.uniformsDirty = true;

		return u;
	}

	/**
	 * Return an unmodifiable map of all currently attached GlslUniforms,
	 * accessed by the uniform name.
	 * 
	 * @return Unmodifiable map of currently bound uniforms: <name, uniform>
	 */
	public Map<String, GlslUniform> getUniforms() {
		return readOnlyUniforms;
	}

	/**
	 * <p>
	 * Return the array of strings that store the glsl code for executing the
	 * vertex stage of a pipeline. If the returned array has a length of 0, then
	 * no vertex shader is used by this program.
	 * </p>
	 * <p>
	 * If the array is modified, there will be undefined results.
	 * </p>
	 * 
	 * @return String array of source code for the vertex shader
	 */
	public String[] getVertexShader() {
		return vertexShader;
	}

	/**
	 * <p>
	 * Return the array of strings that store the glsl code for executing the
	 * fragment stage of a pipeline. If the returned array has a length of 0,
	 * then no fragment shader is used by this program.
	 * </p>
	 * <p>
	 * If the array is modified, there will be undefined results.
	 * </p>
	 * 
	 * @return String array of source code for the fragment shader
	 */
	public String[] getFragmentShader() {
		return fragmentShader;
	}

	/**
	 * <p>
	 * Set the glsl code that executes during the vertex and fragment stages of
	 * the pipeline.
	 * </p>
	 * <p>
	 * This method will copy each of the arrays over into new arrays, removing
	 * any null elements. A null input array is treated like a String[0].
	 * </p>
	 * <p>
	 * If after copying, both vertex and fragment shaders sources have a length
	 * of 0, then an exception is thrown: at least one portion of the pipeline
	 * must be replaced by the shader.
	 * </p>
	 * 
	 * @param vertex The source code for the vertex shader of the program
	 * @param fragment The source code for the fragment shader
	 * @throws IllegalArgumentException if both vertex and fragment are empty
	 *             shaders
	 */
	public void setShaderCode(String[] vertex, String[] fragment)
		throws IllegalArgumentException {
		vertex = compress(vertex);
		fragment = compress(fragment);

		if (vertex.length == 0 && fragment.length == 0)
			throw new IllegalArgumentException(
				"Must specify at least one non-empty shader source");

		vertexShader = vertex;
		fragmentShader = fragment;

		dirty.glslDirty = true;
	}

	/**
	 * Construct the dirty descriptor to use for this program. Must not return
	 * null, or the constructor will throw an exception.
	 * 
	 * @return The dirty descriptor, default implementation returns new
	 *         GlslProgramDirtyDescriptor()
	 */
	protected GlslProgramDirtyDescriptor createDescriptor() {
		return new GlslProgramDirtyDescriptor();
	}

	@Override
	public void clearDirtyDescriptor() {
		dirty.clear();
	}

	/**
	 * Subclasses should override this with a more specific dirty descriptor
	 * 
	 * @return Dirty descriptor created by createDescriptor().
	 */
	@Override
	public GlslProgramDirtyDescriptor getDirtyDescriptor() {
		return dirty;
	}

	@Override
	public Object getRenderData(Framework renderer) {
		return renderData.getRenderData(renderer);
	}

	@Override
	public void setRenderData(Framework renderer, Object data) {
		renderData.setRenderData(renderer, data);
	}

	/* Utility to remove null strings from the source. */
	private static String[] compress(String[] source) {
		if (source == null)
			return new String[0];
		else {
			int nonNullCount = 0;
			for (int i = 0; i < source.length; i++)
				if (source[i] != null)
					nonNullCount++;

			String[] res = new String[nonNullCount];
			nonNullCount = 0;
			for (int i = 0; i < source.length; i++)
				if (source[i] != null)
					res[nonNullCount++] = source[i];

			return res;
		}
	}
}
