package com.ferox.resource;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Matrix4f;
import org.openmali.vecmath.Vector2f;
import org.openmali.vecmath.Vector2i;
import org.openmali.vecmath.Vector3f;
import org.openmali.vecmath.Vector4f;

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
	 * to access a texture from. 
	 * 
	 * The given types may also be used in a GlslProgram as
	 * an array.  This difference is determined by the GlslUniform
	 * based on its length value. */
	public static enum UniformType {
		SAMPLER_1D, 		/** Must use int, Integer, or int[]. */ 
		SAMPLER_2D, 		/** Must use int, Integer, or int[]. */ 
		SAMPLER_3D, 		/** Must use int, Integer, or int[]. */ 
		SAMPLER_CUBEMAP, 	/** Must use int, Integer, or int[]. */ 
		SAMPLER_RECT, 		/** Must use int, Integer, or int[]. */ 
		FLOAT,			    /** Must use float, Float, or float[]. */ 
		INT, 				/** Must use int, Integer, or int[]. */ 
		BOOLEAN,          	/** Must use boolean, Boolean, or boolean[]. */  
		VEC2F,    			/** Must use Vector2f or Vector2f[]. */  
		VEC3F,    			/** Must use Vector3f or Vector3f[]. */   
		VEC4F,    			/** Must use Vector4f or Vector4f[]. */  
		VEC2I,    			/** Must use Vector2i or Vector2i[]. */  
		MAT3F,    			/** Must use Matrix3f or Matrix3f[]. */  
		MAT4F,    			/** Must use Matrix4f or Matrix4f[]. */  
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
		
		if (this.length > 1) {
			switch(this.type) {
			case BOOLEAN: return (value instanceof boolean[]) && ((boolean[]) value).length == this.length;
			
			case INT: case SAMPLER_1D: case SAMPLER_2D: case SAMPLER_3D: case SAMPLER_CUBEMAP: case SAMPLER_RECT:
				return (value instanceof int[]) && ((int[]) value).length == this.length;
				
			case FLOAT: return (value instanceof float[]) && ((float[]) value).length == this.length;
			
			case MAT3F: return (value instanceof Matrix3f[]) && ((Matrix3f[]) value).length == this.length;
			case MAT4F: return (value instanceof Matrix4f[]) && ((Matrix4f[]) value).length == this.length;
			
			case VEC2F: return (value instanceof Vector2f[]) && ((Vector2f[]) value).length == this.length;
			case VEC2I: return (value instanceof Vector2i[]) && ((Vector2i[]) value).length == this.length;
			case VEC3F: return (value instanceof Vector3f[]) && ((Vector3f[]) value).length == this.length;
			case VEC4F: return (value instanceof Vector4f[]) && ((Vector4f[]) value).length == this.length;
			}
		} else {
			switch(this.type) {
			case BOOLEAN: return value instanceof Boolean;
			
			case INT: case SAMPLER_1D: case SAMPLER_2D: case SAMPLER_3D: case SAMPLER_CUBEMAP: case SAMPLER_RECT:
				return value instanceof Integer;
				
			case FLOAT: return value instanceof Float;
			
			case MAT3F: return value instanceof Matrix3f;
			case MAT4F: return value instanceof Matrix4f;
			
			case VEC2F: return value instanceof Vector2f;
			case VEC2I: return value instanceof Vector2i;
			case VEC3F: return value instanceof Vector3f;
			case VEC4F: return value instanceof Vector4f;
			}
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
