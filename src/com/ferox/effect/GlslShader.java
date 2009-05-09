package com.ferox.effect;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

import com.ferox.effect.EffectType.Type;
import com.ferox.resource.GlslProgram;
import com.ferox.resource.GlslUniform;

/**
 * <p>
 * GlslShader wraps a single GlslProgram and provides the functionality to bind
 * uniforms with values. The GlslProgram cannot be null and a shader's program
 * will not change during its lifetime.
 * </p>
 * <p>
 * It is highly recommended that declared uniforms in a program always have a
 * set value in the GlslShader. If a uniform is not set in the shader, the
 * program will use the last set value for the uniform, which is undetermined
 * when rendering.
 * </p>
 * 
 * @author Michael Ludwig
 */
@EffectType(Type.SHADER)
public class GlslShader extends AbstractEffect {
	/**
	 * UniformBinding is the union of a GlslUniform and its set value for a
	 * GlslShader instance. It also has a boolean property marking it as dirty,
	 * which is only of use if the uniform has a ValueUpdatePolicy of MANUAL.
	 */
	public static class UniformBinding {
		private GlslUniform variable;
		private Object value;
		private boolean isDirty;

		/**
		 * Returns true if the uniform's value should be updated if its policy
		 * if MANUAL. This is ignored if the policy is not MANUAL.
		 * 
		 * @return True if the uniform's value must be updated
		 */
		public boolean isDirty() {
			return isDirty;
		}

		/**
		 * <p>
		 * Set whether or not the value in this uniform is dirty. As above, it's
		 * only used if the uniform's policy is MANUAL.
		 * </p>
		 * <p>
		 * This should be set to false after a Renderer updates the value of the
		 * uniform.
		 * </p>
		 * 
		 * @param dirty Set return value for isDirty()
		 */
		public void setDirty(boolean dirty) {
			isDirty = dirty;
		}

		/**
		 * Get the current value for the uniform. Changes made to this instance
		 * will only be visible if the policy if PER_INSTANCE, or PER_FRAME (but
		 * only changes before the frame, not during), or MANUAL (if isDirty()
		 * returns true).
		 * 
		 * @return Value set to the uniform, will be int[] or float[]
		 */
		public Object getValue() {
			return value;
		}

		/**
		 * Get the GlslUniform for this binding.
		 * 
		 * @return The uniform in question
		 */
		public GlslUniform getUniform() {
			return variable;
		}

		@Override
		public String toString() {
			return "<"
				+ variable
				+ " = "
				+ (value instanceof int[] ? Arrays.toString((int[]) value)
					: Arrays.toString((float[]) value)) + ", dirty: " + isDirty
				+ ">";
		}
	}

	private final Map<GlslUniform, Integer> indexMap;
	private final List<UniformBinding> uniforms;
	private final List<UniformBinding> readOnlyUniforms;

	private final GlslProgram program;

	/**
	 * Construct a GlslShader with the given program. A NullPointerException is
	 * thrown if the program is null. No uniforms are bound, but it is highly
	 * recommended to set the uniform values before actually using the
	 * GlslShader to get the expected outcome.
	 * 
	 * @param program GlslProgram that this shader uses when rendering
	 * @throws NullPointerException if program is null
	 */
	public GlslShader(GlslProgram program) {
		if (program == null)
			throw new NullPointerException("Program cannot be null");

		indexMap = new IdentityHashMap<GlslUniform, Integer>();
		uniforms = new ArrayList<UniformBinding>();
		readOnlyUniforms = Collections.unmodifiableList(uniforms);

		this.program = program;
	}

	/**
	 * Return the GlslProgram that is activated whenever this effect is added to
	 * an Appearance.
	 * 
	 * @return GlslProgram used, will not be null
	 */
	public GlslProgram getProgram() {
		return program;
	}

	/**
	 * <p>
	 * Set the value to be used for the given uniform in this GlslShader. If the
	 * uniform is null, a NullPointerException is thrown. If the uniform's owner
	 * isn't this GlslShader's program, then an IllegalArgumentException is
	 * thrown. Also, one is thrown if the value isn't value as determined by
	 * uniform's isValid() method.
	 * </p>
	 * <p>
	 * If value is null, then this removes any previous UniformBinding for the
	 * given uniform. If it isn't, the value is stored in a UniformBinding for
	 * the uniform (possibly re-using an existing instance). The binding is
	 * marked as dirty so that MANUAL uniforms will be updated accordingly.
	 * </p>
	 * 
	 * @param uniform Uniform to assign the value binding to
	 * @param value Value assigned to uniform when this shader is used
	 * @throws NullPointerException if uniform is null
	 * @throws IllegalArgumentException if the uniform isn't owned by this
	 *             shader's program
	 */
	public void setUniform(GlslUniform uniform, Object value) {
		if (uniform == null)
			throw new NullPointerException("Uniform cannot be null");
		if (uniform.getOwner() != program)
			throw new IllegalArgumentException(
				"Can only set uniforms that match this shader's GlslProgram, not: "
					+ uniform.getOwner());

		Integer index = indexMap.get(uniform);

		if (value == null) {
			// remove the uniform from the shader
			if (index != null) {
				uniforms.remove(index.intValue());
				updateIndexMap();
			}
		} else {
			if (!uniform.isValid(value))
				throw new IllegalArgumentException(
					"Value is not valid for the given uniform: "
						+ uniform.getName() + " " + value);
			// set the value, and mark it as dirty
			UniformBinding binding;
			if (index != null)
				// re-use existing binding object
				binding = uniforms.get(index.intValue());
			else {
				// need a new binding
				binding = new UniformBinding();
				binding.variable = uniform;
				uniforms.add(binding);
				updateIndexMap();
			}

			binding.isDirty = true;
			binding.value = value;
		}
	}

	/**
	 * Return the uniform binding for the given uniform. If uniform is null, has
	 * a different GlslProgram than this shader, or it hasn't ever been set on
	 * this shader, null is returned.
	 * 
	 * @param uniform Uniform to request value binding for
	 * @return UniformBinding for uniform, set with setUniform(). May be null.
	 */
	public UniformBinding getUniform(GlslUniform uniform) {
		if (uniform == null || uniform.getOwner() != program)
			return null;

		Integer i = indexMap.get(uniform);
		return (i == null ? null : uniforms.get(i.intValue()));
	}

	/**
	 * Return an unmodifiable list of all currently set uniforms as a list of
	 * UniformBindings. It can be assumed that any bindings present will have
	 * uniforms that satisfy: uniform.getOwner() == this.getProgram().
	 * 
	 * @return List of all assigned UniformBindings, e.g. uniforms with non-null
	 *         values for this shader.
	 */
	public List<UniformBinding> getSetUniforms() {
		return readOnlyUniforms;
	}

	/*
	 * Re-calculate the indices of the index map. This must be called each time
	 * uniforms has an element added or removed.
	 */
	private void updateIndexMap() {
		indexMap.clear();
		int size = uniforms.size();
		for (int i = 0; i < size; i++)
			indexMap.put(uniforms.get(i).variable, Integer.valueOf(i));
	}

	@Override
	public String toString() {
		return "(GlslShader bindings: " + uniforms + ")";
	}
}
