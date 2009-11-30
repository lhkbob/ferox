package com.ferox.resource;

import com.ferox.renderer.Framework;
import com.ferox.util.FastMap;

/**
 * <p>
 * GlslUniform represents a "static" variable that is assigned values from
 * outside the execution of its owning GlslProgram.
 * </p>
 * <p>
 * The actual values are assigned by a GlslShader, since it is likely that they
 * will change when the GlslProgram does not.
 * </p>
 * <p>
 * In many respects GlslUniform's are Resources (and hence implement Resource),
 * but in others, they are an anomaly. They are updated and cleaned-up along
 * with their owning GlslProgram. It is not recommended that you update or
 * clean-up GlslUniforms individually, as behavior is affected by a number of
 * factors including the current status of its owning program.
 * </p>
 * <p>
 * It is not necessary for you to request updates on a uniform after changing
 * the value update policy.
 * </p>
 * <p>
 * Renderers should set the uniform's status to ERROR when it is used and its
 * owning program has a status of CLEANED or ERROR.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class GlslUniform implements Resource {
	/**
	 * <p>
	 * Each GlslUniform has a value type, which will be one of the following
	 * possibilities.
	 * </p>
	 * <p>
	 * SAMPLER_x refer to TextureEnvironment accessors. The values for sampler variables
	 * are the texture units with which to access a texture from. Textures bound
	 * to these units should be colored textures.
	 * </p>
	 * <p>
	 * SHADOW_SAMPLER_x are like samplers, except they are intended to be used
	 * with depth textures.
	 * </p>
	 * <p>
	 * BOOL_x types use int[], except that 0 is treated as false, and anything
	 * else is treated like true.
	 * </p>
	 * <p>
	 * For simplicity, all values assigned to a GlslUniform with a GlslShader
	 * are arrays of primitive types (float[], or int[]). A single-valued
	 * uniform should just use an array of length 1. Vector or matrix types will
	 * use consecutive primitives to form one uniform element within the array.
	 * </p>
	 * <p>
	 * Matrices are specified in column major order.
	 * </p>
	 */
	public static enum UniformType {
		FLOAT(1, float[].class), FLOAT_VEC2(2, float[].class), 
		FLOAT_VEC3(3, float[].class), FLOAT_VEC4(4, float[].class),

		FLOAT_MAT2(4, float[].class), FLOAT_MAT3(9, float[].class), 
		FLOAT_MAT4(16, float[].class),
		
		SAMPLER_1D(1, int[].class), SAMPLER_2D(1, int[].class), 
		SAMPLER_3D(1, int[].class), SAMPLER_CUBEMAP(1, int[].class), 
		SAMPLER_RECT(1, int[].class), SAMPLER_1D_SHADOW(1, int[].class), 
		SAMPLER_2D_SHADOW(1, int[].class), SAMPLER_RECT_SHADOW(1, int[].class),
		
		INT(1, int[].class), INT_VEC2(2, int[].class), 
		INT_VEC3(3, int[].class), INT_VEC4(4, int[].class),

		BOOL(1, int[].class), BOOL_VEC2(2, int[].class), 
		BOOL_VEC3(3, int[].class), BOOL_VEC4(4, int[].class);

		private int primitiveCount;
		private Class<?> type;

		private UniformType(int primitiveCount, Class<?> type) {
			this.primitiveCount = primitiveCount;
			this.type = type;
		}

		/**
		 * Return the class type of variables that can be assigned to the
		 * GlslUniform of this type. All of these types are primitive arrays.
		 * 
		 * @return The class that values for this uniform must be instances of
		 */
		public Class<?> getVariableType() {
			return type;
		}

		/**
		 * Return the number of primitive elements that are used in each
		 * uniform. For example, FLOAT_MAT3 requires 9 float primitives per each
		 * matrix.
		 * 
		 * @return The number of primitives required to specify a uniform value
		 */
		public int getPrimitiveCount() {
			return primitiveCount;
		}
	}

	/**
	 * <p>
	 * To make the updates of a GlslUniform's value as fast as possible, a
	 * Framework is not expected to remember each uniform's last set value and
	 * update that only when necessary.
	 * </p>
	 * <p>
	 * Instead each uniform has an update policy that determines when the value
	 * stored by a GlslShader is passed to the graphics card.
	 * </p>
	 * <p>
	 * When MANUAL is set, the value is only changed when the GlslShader says
	 * that the value is dirty. When PER_FRAME is used, it is updated the first
	 * time the shader is used each frame. Both of these assume that all
	 * GlslShader's using the uniform's owner program use the same value.
	 * </p>
	 * <p>
	 * PER_INSTANCE forces the GlslShader to update the uniforms value each time
	 * it is used. This is useful when the uniform has multiple values for
	 * different GlslShaders using it.
	 * </p>
	 */
	public static enum ValueUpdatePolicy {
		MANUAL, PER_FRAME, PER_INSTANCE
	}

	private final UniformType type;
	private final int length;
	private final String name;

	private final GlslProgram owner;

	private ValueUpdatePolicy policy;

	private final FastMap<Framework, Object> renderData;

	/**
	 * GlslUniforms should only be constructed with a GlslProgram's
	 * attachUniform() method.
	 * 
	 * @param name The name of the uniform
	 * @param type The type of the uniform
	 * @param length The length of uniform, in units of type
	 * @param owner The GlslProgram that this uniform should be declared in
	 * @throws IllegalArgumentException if length < 1 or if name starts with
	 *             'gl'
	 * @throws NullPointerException if any arguments are null
	 */
	protected GlslUniform(String name, UniformType type, int length, GlslProgram owner) {
		if (name == null)
			throw new NullPointerException("Cannot specify a null name");
		if (type == null)
			throw new NullPointerException("Cannot specify a null uniform type");
		if (owner == null)
			throw new NullPointerException("Cannot create a GlslUniform with a null GlslProgram");

		if (length < 1)
			throw new IllegalArgumentException("Cannot specify length < 1: " + length);
		if (name.startsWith("gl"))
			throw new IllegalArgumentException("Uniform names may not start with 'gl': " 
											   + name + ", they are reserved");
		this.name = name;
		this.type = type;
		this.owner = owner;
		this.length = length;

		renderData = new FastMap<Framework, Object>(Framework.class);

		setValueUpdatePolicy(null);
	}

	/**
	 * Return the update policy in use by this uniform.
	 * 
	 * @return The current update policy
	 */
	public ValueUpdatePolicy getValueUpdatePolicy() {
		return policy;
	}

	/**
	 * Set the update policy to use. If policy is null, then PER_INSTANCE is
	 * used by default. You do need to update this resource to see this change
	 * have any effect.
	 * 
	 * @param policy The new update policy, if null uses PER_INSTANCE
	 */
	public void setValueUpdatePolicy(ValueUpdatePolicy policy) {
		if (policy == null)
			policy = ValueUpdatePolicy.PER_INSTANCE;
		this.policy = policy;
	}

	/**
	 * Get the value type of this uniform. If getLength() > 1, then this is the
	 * component type of the array for the uniform.
	 * 
	 * @return The type of this uniform
	 */
	public UniformType getType() {
		return type;
	}

	/**
	 * Return the number of primitives of getType() used by this uniform. If
	 * it's > 1, then the uniform represents an array.
	 * 
	 * @return Size of the uniform, in units of getType()
	 */
	public int getLength() {
		return length;
	}

	/**
	 * Return the name of the uniform as declared in the glsl code of the
	 * uniform's owner.
	 * 
	 * @return The name of the uniform
	 */
	public String getName() {
		return name;
	}

	/**
	 * Return the GlslProgram that this uniform was declared in. It could have
	 * been declared in either the vertex or the fragment shader portion of the
	 * program.
	 * 
	 * @return The GlslProgram that defines this uniform
	 */
	public GlslProgram getOwner() {
		return owner;
	}

	/**
	 * <p>
	 * Return true if the value is of the correct type and size to be assigned
	 * to this uniform in a GlslShader.
	 * </p>
	 * <p>
	 * A primitive type is INT, FLOAT, or BOOLEAN. For primitive non-array
	 * types, it uses the boxed classes since the actual primitives will not be
	 * passed in.
	 * </p>
	 * 
	 * @return Whether or not value can be assigned to this uniform, returns
	 *         false if value is null
	 */
	public boolean isValid(Object value) {
		if (value == null)
			return false;

		Class<?> expectedType = type.getVariableType();
		if (!value.getClass().equals(expectedType))
			return false;

		int expectedLength = length * type.getPrimitiveCount();
		if (expectedType.equals(float[].class))
			return ((float[]) value).length == expectedLength;
		else if (expectedType.equals(int[].class))
			return ((int[]) value).length == expectedLength;

		return false;
	}

	@Override
	public void clearDirtyDescriptor() {
		// do nothing
	}

	@Override
	public Object getDirtyState() {
		return null;
	}

	@Override
	public Object getRenderData(Framework renderer) {
		return renderData.get(renderer);
	}

	@Override
	public void setRenderData(Framework renderer, Object data) {
		renderData.put(renderer, data);
	}
}
