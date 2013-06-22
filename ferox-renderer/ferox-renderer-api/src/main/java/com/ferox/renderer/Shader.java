package com.ferox.renderer;

import java.util.List;

/**
 * Shader represents a programmable GLSL shader. Shaders override the standard
 * fixed-function pipeline. On newer hardware and base OpenGL versions, the fixed function
 * pipeline is done away with and either unavailable or emulated by shaders. While other
 * resource types can be used by both the {@link FixedFunctionRenderer} and {@link
 * GlslRenderer}, a Shader can only be used the the glsl renderer. To activate a shader,
 * {@link GlslRenderer#setShader(Shader)} must be called.
 * <p/>
 * Shaders do not have any refreshable state, so {@link #refresh()} does nothing.
 *
 * @author Michael Ludwig
 */
public interface Shader extends Resource {
    /**
     * VariableType represents the enumeration of all supported attribute and uniform
     * types. Depending on the target GLSL version, certain types may be unavailable. Some
     * values are only valid for uniforms.
     */
    public static enum VariableType {
        FLOAT(1, 1),
        INT(1, 1),
        UINT(1, 1),
        BOOL(1, 1),

        MAT2(2, 2),
        MAT3(3, 3),
        MAT4(4, 4),

        VEC2(2, 1),
        VEC3(3, 1),
        VEC4(4, 1),
        IVEC2(2, 1),
        IVEC3(3, 1),
        IVEC4(4, 1),
        UVEC2(2, 1),
        UVEC3(3, 1),
        UVEC4(4, 1),
        BVEC2(2, 1),
        BVEC3(3, 1),
        BVEC4(4, 1),

        SAMPLER_1D(-1, -1),
        SAMPLER_2D(-1, -1),
        SAMPLER_3D(-1, -1),
        SAMPLER_CUBE(-1, -1),
        SAMPLER_1D_SHADOW(-1, -1),
        SAMPLER_2D_SHADOW(-1, -1),
        SAMPLER_CUBE_SHADOW(-1, -1),
        SAMPLER_1D_ARRAY(-1, -1),
        SAMPLER_2D_ARRAY(-1, -1),

        USAMPLER_1D(-1, -1),
        USAMPLER_2D(-1, -1),
        USAMPLER_3D(-1, -1),
        USAMPLER_CUBE(-1, -1),
        USAMPLER_1D_ARRAY(-1, -1),
        USAMPLER_2D_ARRAY(-1, -1),

        ISAMPLER_1D(-1, -1),
        ISAMPLER_2D(-1, -1),
        ISAMPLER_3D(-1, -1),
        ISAMPLER_CUBE(-1, -1),
        ISAMPLER_1D_ARRAY(-1, -1),
        ISAMPLER_2D_ARRAY(-1, -1);

        private final int row;
        private final int col;

        private VariableType(int r, int c) {
            row = r;
            col = c;
        }

        /**
         * @return Get the number of primitives required for the vertex attribute, returns
         *         -1 if the type is only available as a uniform
         */
        public int getRowCount() {
            return row;
        }

        /**
         * @return Get the number of vertex attributes required to hold the complete data.
         *         Each column of a matrix gets its own vertex attribute.
         */
        public int getColumnCount() {
            return col;
        }

        /**
         * @return The total number of primitives required for a single value
         */
        public int getPrimitiveCount() {
            return row * col;
        }
    }

    /**
     * Attribute represents a bindable vertex attribute declared in the vertex shader of
     * the compiled program. If the attribute type is a matrix type, it encapsulates N
     * underlying attributes where each low-level attribute maps to a column of the
     * matrix.
     */
    public static interface Attribute {
        /**
         * @return The attribute's type
         */
        public VariableType getType();

        /**
         * @return The attribute's name as declared in the shader source
         */
        public String getName();

        /**
         * @return The compiled index of the attribute within the program
         */
        public int getIndex();

        /**
         * @return True if the attribute name starts with 'gl_' and is one of the special
         *         or reserved GLSL attribute variables
         */
        public boolean isReserved();
    }

    public static interface Uniform {
        /**
         * Get the value type of this uniform. If getLength() > 1, then this is the
         * component type of the array for the uniform.
         *
         * @return The type of this uniform
         */
        public VariableType getType();

        /**
         * Return the number of primitives of getType() used by this uniform. If it's > 1,
         * then the uniform represents an array.
         *
         * @return Size of the uniform, in units of getType()
         */
        public int getLength();

        /**
         * Return the name of the uniform as declared in the glsl code of the uniform's
         * owner.
         *
         * @return The name of the uniform
         */
        public String getName();

        /**
         * @return The compiled index of the uniform within the program
         */
        public int getIndex();

        /**
         * @return True if the uniform variable starts with 'gl_' and represents one of
         *         the special uniforms declared in the (older) GLSL specs
         */
        public boolean isReserved();

        /**
         * @return True if the uniform is an array
         */
        public boolean isArray();
    }

    /**
     * @return All uniforms used in the shader, ordered by their compiled index
     */
    public List<Uniform> getUniforms();

    /**
     * Get the uniform object for the variable with the given {@code name}. This will
     * return null if there is no matching uniform.
     *
     * @param name The uniform variable name
     *
     * @return The Uniform for the given name, or null if it doesn't exist
     */
    public Uniform getUniform(String name);

    /**
     * @return All vertex attributes used by the shader, ordered by their compiled index
     */
    public List<Attribute> getAttributes();

    /**
     * Get the attribute object for the variable with the given {@code name}. This will
     * return null if there is no matching attribute.
     *
     * @param name The attribute variable name
     *
     * @return The Attribute for the given name, or null if it doesn't exist
     */
    public Attribute getAttribute(String name);

    /**
     * Get the mapping from fragment shader output variable to the indexed color buffer.
     * New versions support defining custom output variables, in which case the mapping
     * must have been specified by {@link com.ferox.renderer.builder.ShaderBuilder#bindColorBuffer(String,
     * int)}. If not defined manually, the mapping will be assigned when the shader is
     * compiled.
     * <p/>
     * The reserved 'gl_FragColor' always will be mapped to the 0th color buffer if it's
     * in use. The reserved 'gl_FragData[n]' will be mapped to the nth color buffer if
     * used.
     *
     * @param outVariableName The output variable name
     *
     * @return The color buffer the output is stored into
     */
    public int getColorBuffer(String outVariableName);

    /**
     * Get the GLSL version that the shader code was written against. The version is
     * declared in the GLSL code with the {@code #version INT} declaration on the first
     * line. The version is reported in integer form, so version 1.3 is 130 and 3.3 is
     * 330, etc.
     * <p/>
     * If no version is declared, it defaults to 110.
     *
     * @return The GLSL version
     */
    public int getGLSLVersion();

    /**
     * @return True if the final program contains a custom vertex stage
     */
    public boolean hasVertexShader();

    /**
     * @return True if the final program contains a custom geometry stage
     */
    public boolean hasGeometryShader();

    /**
     * @return True if the final program contains a custom fragment stage
     */
    public boolean hasFragmentShader();
}
