package com.ferox.resource;

import com.ferox.renderer.GlslRenderer;

/**
 * <p>
 * GlslUniform represents a "static" variable that is assigned values from
 * outside the execution of its owning GlslShader. Each GlslShader has a set of
 * active uniforms that become defined after the hardware successfully compiles
 * the shader code. Use a {@link GlslRenderer} to query available GlslUniforms
 * and set uniform values.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class GlslUniform {
    public static enum UniformType {
        FLOAT(1), FLOAT_VEC2(2), FLOAT_VEC3(3), FLOAT_VEC4(4),
        FLOAT_MAT2(4), FLOAT_MAT3(9), FLOAT_MAT4(16),

        INT(1), INT_VEC2(2), INT_VEC3(3), INT_VEC4(4),
        
        TEXTURE_1D(1), TEXTURE_2D(1), TEXTURE_3D(1), TEXTURE_CUBEMAP(1), SHADOW_MAP(1),
        
        BOOL(1),
        UNSUPPORTED(0);
        
        private final int primCount;
        private UniformType(int primCount) { this.primCount = primCount; }

        public int getPrimitiveCount() {
            return primCount;
        }
    }
    
    private final UniformType type;
    private final int length;
    private final String name;

    public GlslUniform(String name, UniformType type, int length) {
        if (name == null)
            throw new NullPointerException("Cannot specify a null name");
        if (type == null)
            throw new NullPointerException("Cannot specify a null uniform type");

        if (length < 1)
            throw new IllegalArgumentException("Cannot specify length < 1: " + length);

        this.name = name;
        this.type = type;
        this.length = length;
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
    
    public boolean isReserved() {
        return name.startsWith("gl_");
    }
    
    public boolean isArray() {
        return length > 1;
    }
}
