package com.ferox.resource;


/** GlslUniform represents a "static" variable that is assigned
 * values from outside the execution of its owning GlslProgram.
 * 
 * The actual values are assigned by a GlslShader, since it is likely
 * that they will change when the GlslProgram does not.
 * 
 * In many respects GlslUniform's are Resources (and hence implement Resource),
 * but in others, they are an anomaly.  They are updated and cleaned-up
 * along with their owning GlslProgram.  It is not recommended that you update
 * or clean-up GlslUniforms individually, as behavior is affected by a number
 * of factors including the current status of its owning program. 
 * 
 * It is not necessary for you to request updates on a uniform after
 * changing the value update policy.
 * 
 * Renderers should set the uniform's status to ERROR when it is used
 * and its owning program has a status of CLEANED or ERROR.
 * 
 * @author Michael Ludwig
 *
 */
public class GlslUniform implements Resource {
	/** Each GlslUniform has a value type, which will
	 * be one of the following possibilities.
	 * 
	 * SAMPLER_x refer to Texture accessors.  The values
	 * for sampler variables are the texture units with which
	 * to access a texture from.  Textures bound to these units
	 * should be colored textures.
	 * 
	 * SHADOW_SAMPLER_x are like samplers, except they are
	 * intended to be used with depth textures.
	 * 
	 * BOOL_x types use int[], except that 0 is treated as false,
	 * and anything else is treated like true.
	 * 
	 * For simplicity, all values assigned to a GlslUniform with
	 * a GlslShader are arrays of primitive types (float[], or int[]). 
	 * A single-valued uniform should just use an array of length 1.  
	 * Vector or matrix types will use consecutive
	 * primitives to form one uniform element within the array. 
	 * 
	 * Matrices are specified in column major order. */
	public static enum UniformType {
		SAMPLER_1D(1, int[].class), 
		SAMPLER_2D(1, int[].class),	
		SAMPLER_3D(1, int[].class),
		SAMPLER_CUBEMAP(1, int[].class), 
		SAMPLER_RECT(1, int[].class), 
		SAMPLER_1D_SHADOW(1, int[].class), 
		SAMPLER_2D_SHADOW(1, int[].class), 
		SAMPLER_RECT_SHADOW(1, int[].class),
		
		FLOAT(1, float[].class), 
		FLOAT_VEC2(2, float[].class),
		FLOAT_VEC3(3, float[].class), 
		FLOAT_VEC4(4, float[].class),
		
		INT(1, int[].class), 
		INT_VEC2(2, int[].class),
		INT_VEC3(3, int[].class),
		INT_VEC4(4, int[].class),
		
		BOOL(1, int[].class),
		BOOL_VEC2(2, int[].class),
		BOOL_VEC3(3, int[].class),
		BOOL_VEC4(4, int[].class),
		
		FLOAT_MAT2(4, float[].class),
		FLOAT_MAT3(9, float[].class), 
		FLOAT_MAT4(16, float[].class);
		
		private int primitiveCount;
		private Class<?> type;
		private UniformType(int primitiveCount, Class<?> type) { 
			this.primitiveCount = primitiveCount; 
			this.type = type;
		}
		
		/** Return the class type of variables that can be assigned
		 * to the GlslUniform of this type.  All of these types are
		 * primitive arrays. */
		public Class<?> getVariableType() { return this.type; }
		
		/** Return the number of primitive elements that are used in
		 * each uniform.  For example, FLOAT_MAT3 requires 9 float primitives
		 * per each matrix. */
		public int getPrimitiveCount() { return this.primitiveCount; }
	}
	
	/** To make the updates of a GlslUniform's value as fast as possible,
	 * a Renderer is not expected to remember each uniform's last set value
	 * and update that only when necessary.  
	 * 
	 * Instead each uniform has an update policy that determines when the
	 * value stored by a GlslShader is passed to the graphics card.
	 * 
	 * When MANUAL is set, the value is only changed when the GlslShader
	 * says that the value is dirty.  When PER_FRAME is used, it is updated
	 * the first time the shader is used each frame.  Both of these assume
	 * that all GlslShader's using the uniform's owner program use the same
	 * value.
	 * 
	 * PER_INSTANCE forces the GlslShader to update the uniforms value each
	 * time it is used.  This is useful when the uniform has multiple values
	 * for different GlslShaders using it. */
	public static enum ValueUpdatePolicy {
		MANUAL, PER_FRAME, PER_INSTANCE
	}
	
	private final UniformType type;
	private final int length;
	private final String name;
	
	private final GlslProgram owner;
	
	private ValueUpdatePolicy policy;

	private Object renderData;
	
	/** GlslUniforms should only be constructed with a GlslProgram's attachUniform()
	 * method. */
	protected GlslUniform(String name, UniformType type, int length, GlslProgram owner) throws IllegalArgumentException, NullPointerException {
		if (name == null)
			throw new NullPointerException("Cannot specify a null name");
		if (type == null)
			throw new NullPointerException("Cannot specify a null uniform type");
		if (owner == null)
			throw new NullPointerException("Cannot create a GlslUniform with a null GlslProgram");
		
		if (length < 1)
			throw new IllegalArgumentException("Cannot specify length < 1: " + length);
		if (name.startsWith("gl"))
			throw new IllegalArgumentException("Uniform names may not start with 'gl': " + name + ", they are reserved");
		
		this.name = name;
		this.type = type;
		this.owner = owner;
		this.length = length;
		
		this.setValueUpdatePolicy(null);
	}
	
	/** Return the update policy in use by this uniform. */
	public ValueUpdatePolicy getValueUpdatePolicy() {
		return this.policy;
	}

	/** Set the update policy to use.  If policy is null, then
	 * PER_INSTANCE is used by default.  You do need to update
	 * this resource to see this change have any effect. */
	public void setValueUpdatePolicy(ValueUpdatePolicy policy) {
		if (policy == null)
			policy = ValueUpdatePolicy.PER_INSTANCE;
		this.policy = policy;
	}

	/** Get the value type of this uniform.  If getLength() > 1, then
	 * this is the component type of the array for the uniform. */
	public UniformType getType() {
		return this.type;
	}

	/** Return the number of primitives of getType() used by this uniform.
	 * If it's > 1, then the uniform represents an array. */
	public int getLength() {
		return this.length;
	}

	/** Return the name of the uniform as declared in the glsl code
	 * of the uniform's owner. */
	public String getName() {
		return this.name;
	}

	/** Return the GlslProgram that this uniform was declared in.
	 * It could have been declared in either the vertex or the fragment
	 * shader portion of the program. */
	public GlslProgram getOwner() {
		return this.owner;
	}
	
	/** Return true if the value is of the correct type and
	 * size to be assigned to this uniform in a GlslShader.
	 * 
	 * A primitive type is INT, FLOAT, or BOOLEAN.
	 * For primitive non-array types, it uses the boxed classes
	 * since the actual primitives will not be passed in.
	 * 
	 * Returns false if value is null. */
	public boolean isValid(Object value) {
		if (value == null)
			return false;
		
		Class<?> expectedType = this.type.getVariableType();
		if (!value.getClass().equals(expectedType)) 
			return false;
		
		int expectedLength = this.length * this.type.getPrimitiveCount();
		if (expectedType.equals(float[].class)) {
			return ((float[]) value).length == expectedLength;
		} else if (expectedType.equals(int[].class)) {
			return ((int[]) value).length == expectedLength;
		}
		
		return false;
	}
	
	@Override
	public void clearDirtyDescriptor() {
		// do nothing
	}
	
	@Override
	public Object getDirtyDescriptor() {
		return null;
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
