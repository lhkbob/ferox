package com.ferox.state;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.ferox.resource.glsl.GlslProgram;
import com.ferox.resource.glsl.GlslUniform;

/** GlslShader wraps a single GlslProgram and provides the
 * functionality to bind uniforms with values.  The GlslProgram
 * cannot be null and a shader's program will not change during
 * its lifetime.
 * 
 * It is highly recommended that declared uniforms in a program
 * always have a set value in the GlslShader.  If a uniform is not
 * set in the shader, the program will use the last set value for
 * the uniform, which is undetermined when rendering.
 * 
 * @author Michael Ludwig
 *
 */
public class GlslShader implements State {
	/** UniformBinding is the union of a GlslUniform and its
	 * set value for a GlslShader instance.  It also has a boolean
	 * property marking it as dirty, which is only of use if the
	 * uniform has a ValueUpdatePolicy of MANUAL. */
	public static class UniformBinding {
		private GlslUniform variable;
		private Object value;
		private boolean isDirty;
		
		/** Returns true if the uniform's value should be updated if its
		 * policy if MANUAL.  This is ignored if the policy is not MANUAL. */
		public boolean isDirty() { return this.isDirty; }
		
		/** Set whether or not the value in this uniform is dirty.
		 * As above, it's only used if the uniform's policy is MANUAL. 
		 * 
		 * This should be set to false after a Renderer updates the value
		 * of the uniform. */
		public void setDirty(boolean dirty) { this.isDirty = dirty; }
		
		/** Get the current value for the uniform.  Changes made to this
		 * instance will only be visible if the policy if PER_INSTANCE, 
		 * or PER_FRAME (but only changes before the frame, not during),
		 * or MANUAL (if isDirty() returns true). */
		public Object getValue() { return this.value; }
		
		/** Get the GlslUniform for this binding. */
		public GlslUniform getUniform() { return this.variable; }
	}
	
	private final Map<GlslUniform, Integer> indexMap;
	private final List<UniformBinding> uniforms;
	private final List<UniformBinding> readOnlyUniforms;
	
	private final GlslProgram program;
	
	private Object stateData;
	
	/** Construct a GlslShader with the given program.  A NullPointerException
	 * is thrown if the program is null.  No uniforms are bound, but it is 
	 * highly recommended to set the uniform values before actually using the
	 * GlslShader to get the expected outcome. */
	public GlslShader(GlslProgram program) throws NullPointerException {
		if (program == null)
			throw new NullPointerException("Program cannot be null");
		
		this.indexMap = new IdentityHashMap<GlslUniform, Integer>();
		this.uniforms = new ArrayList<UniformBinding>();
		this.readOnlyUniforms = Collections.unmodifiableList(this.uniforms);
		
		this.program = program;
	}
	
	/** Return the GlslProgram that is activated whenever this state is added to
	 * an Appearance. This will not be null. */
	public GlslProgram getProgram() {
		return this.program;
	}
	
	/** Set the value to be used for the given uniform in this GlslShader.
	 * If the uniform is null, a NullPointerException is thrown.
	 * If the uniform's owner isn't this GlslShader's program, then an IllegalArgumentException
	 * is thrown. Also, one is thrown if the value isn't value as determined by
	 * uniform's isValid() method.
	 * 
	 * If value is null, then this removes any previous UniformBinding for the given uniform.
	 * If it isn't, the value is stored in a UniformBinding for the uniform (possibly re-using
	 * an existing instance).  The binding is marked as dirty so that MANUAL uniforms will
	 * be updated accordingly. */
	public void setUniform(GlslUniform uniform, Object value) throws IllegalArgumentException, NullPointerException {
		if (uniform == null)
			throw new NullPointerException("Uniform cannot be null");
		if (uniform.getOwner() != this.program)
			throw new IllegalArgumentException("Can only set uniforms that match this shader's GlslProgram, not: " + uniform.getOwner());
		
		Integer index = this.indexMap.get(uniform);

		if (value == null) {
			// remove the uniform from the shader
			if (index != null) {
				this.uniforms.remove(index.intValue());
				this.updateIndexMap();
			}
		} else {
			if (!uniform.isValid(value))
				throw new IllegalArgumentException("Value is not valid for the given uniform: " + uniform.getName() + " " + value);
			// set the value, and mark it as dirty
			UniformBinding binding;
			if (index != null) {
				// re-use existing binding object
				binding = this.uniforms.get(index.intValue());
			} else {
				// need a new binding
				binding = new UniformBinding();
				binding.variable = uniform;
				this.uniforms.add(binding);
				this.updateIndexMap();
			}
			
			binding.isDirty = true;
			binding.value = value;
		}
	}
	
	/** Return the uniform binding for the given uniform.
	 * If uniform is null, has a different GlslProgram than this
	 * shader, or it hasn't ever been set on this shader, null
	 * is returned. */
	public UniformBinding getUniform(GlslUniform uniform) {
		if (uniform == null || uniform.getOwner() != this.program)
			return null;
		
		Integer i = this.indexMap.get(uniform);
		return (i == null ? null : this.uniforms.get(i.intValue()));
	}
	
	/** Return an unmodifiable list of all currently set uniforms
	 * as a list of UniformBindings.  It can be assumed that any
	 * bindings present will have uniforms that satisfy:
	 *   uniform.getOwner() == this.getProgram() */
	public List<UniformBinding> getSetUniforms() {
		return this.readOnlyUniforms;
	}
	
	/* Re-calculate the indices of the index map.  This must be called each time
	 * uniforms has an element added or removed. */
	private void updateIndexMap() {
		this.indexMap.clear();
		int size = this.uniforms.size();
		for (int i = 0; i < size; i++) {
			this.indexMap.put(this.uniforms.get(i).variable, Integer.valueOf(i));
		}
	}

	@Override
	public Role getRole() {
		return Role.SHADER;
	}

	@Override
	public Object getStateData() {
		return this.stateData;
	}

	@Override
	public void setStateData(Object data) {
		this.stateData = data;
	}
}
