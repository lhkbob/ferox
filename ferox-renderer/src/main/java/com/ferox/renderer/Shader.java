/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.renderer;

import java.util.List;

/**
 * Shader represents a programmable GLSL shader. Shaders override the standard fixed-function pipeline. On
 * newer hardware and base OpenGL versions, the fixed function pipeline is done away with and either
 * unavailable or emulated by shaders. While other resource types can be used by both the {@link
 * FixedFunctionRenderer} and {@link GlslRenderer}, a Shader can only be used the the glsl renderer. To
 * activate a shader, {@link GlslRenderer#setShader(Shader)} must be called.
 * <p/>
 * Shaders do not have any refreshable state, so {@link #refresh()} does nothing.
 *
 * @author Michael Ludwig
 */
public interface Shader extends Resource {
    /**
     * VariableType represents the enumeration of all supported attribute and uniform types. Depending on the
     * target GLSL version, certain types may be unavailable. Some values are only valid for uniforms.
     */
    public static enum VariableType {
        FLOAT(1, 1, DataType.FLOAT),
        INT(1, 1, DataType.INT),
        UINT(1, 1, DataType.UNSIGNED_INT),
        BOOL(1, -1, DataType.UNSIGNED_INT),

        MAT2(2, 2, DataType.FLOAT),
        MAT3(3, 3, DataType.FLOAT),
        MAT4(4, 4, DataType.FLOAT),

        VEC2(2, 1, DataType.FLOAT),
        VEC3(3, 1, DataType.FLOAT),
        VEC4(4, 1, DataType.FLOAT),
        IVEC2(2, 1, DataType.INT),
        IVEC3(3, 1, DataType.INT),
        IVEC4(4, 1, DataType.INT),
        UVEC2(2, 1, DataType.UNSIGNED_INT),
        UVEC3(3, 1, DataType.UNSIGNED_INT),
        UVEC4(4, -1, DataType.UNSIGNED_INT),
        BVEC2(2, -1, DataType.UNSIGNED_INT),
        BVEC3(3, -1, DataType.UNSIGNED_INT),
        BVEC4(4, -1, DataType.UNSIGNED_INT),

        SAMPLER_1D(1, -1, null),
        SAMPLER_2D(1, -1, null),
        SAMPLER_3D(1, -1, null),
        SAMPLER_CUBE(1, -1, null),
        SAMPLER_1D_SHADOW(1, -1, null),
        SAMPLER_2D_SHADOW(1, -1, null),
        SAMPLER_CUBE_SHADOW(1, -1, null),
        SAMPLER_1D_ARRAY(1, -1, null),
        SAMPLER_2D_ARRAY(1, -1, null),

        USAMPLER_1D(1, -1, null),
        USAMPLER_2D(1, -1, null),
        USAMPLER_3D(1, -1, null),
        USAMPLER_CUBE(1, -1, null),
        USAMPLER_1D_ARRAY(1, -1, null),
        USAMPLER_2D_ARRAY(1, -1, null),

        ISAMPLER_1D(1, -1, null),
        ISAMPLER_2D(1, -1, null),
        ISAMPLER_3D(1, -1, null),
        ISAMPLER_CUBE(1, -1, null),
        ISAMPLER_1D_ARRAY(1, -1, null),
        ISAMPLER_2D_ARRAY(1, -1, null);

        private final int row;
        private final int col;
        private final DataType type;

        private VariableType(int r, int c, DataType type) {
            row = r;
            col = c;
            this.type = type;
        }

        /**
         * Get the primitive type used by the variable type. If the variable type has multiple components, it
         * is the type of an individual component. The sampler types return null because they are  not a
         * primitive.
         * <p/>
         * The primitive type will be null, UNSIGNED_INT, INT, or FLOAT. The other data types are never used
         * by the shader variable types.
         *
         * @return The component primitive type
         */
        public DataType getPrimitiveType() {
            return type;
        }

        /**
         * @return Get the number of components in a row.
         */
        public int getRowCount() {
            return row;
        }

        /**
         * @return Get the number of vertex attributes required to hold the complete data. Each column of a
         *         matrix gets its own vertex attribute. Returns -1 if the type is only available as a
         *         uniform
         */
        public int getColumnCount() {
            return col;
        }

        /**
         * @return The total number of primitives required for a single value
         */
        public int getPrimitiveCount() {
            return Math.abs(row * col);
        }
    }

    /**
     * Attribute represents a bindable vertex attribute declared in the vertex shader of the compiled program.
     * If the attribute type is a matrix type, it encapsulates N underlying attributes where each low-level
     * attribute maps to a column of the matrix.
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
         * @return True if the attribute name starts with 'gl_' and is one of the special or reserved GLSL
         *         attribute variables
         */
        public boolean isReserved();
    }

    public static interface Uniform {
        /**
         * Get the value type of this uniform. If getLength() > 1, then this is the component type of the
         * array for the uniform.
         *
         * @return The type of this uniform
         */
        public VariableType getType();

        /**
         * Return the number of primitives of getType() used by this uniform. If it's > 1, then the uniform
         * represents an array.
         *
         * @return Size of the uniform, in units of getType()
         */
        public int getLength();

        /**
         * Return the name of the uniform as declared in the glsl code of the uniform's owner.
         *
         * @return The name of the uniform
         */
        public String getName();

        /**
         * @return The compiled index of the uniform within the program
         */
        public int getIndex();

        /**
         * @return True if the uniform variable starts with 'gl_' and represents one of the special uniforms
         *         declared in the (older) GLSL specs
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
    public List<? extends Uniform> getUniforms();

    /**
     * Get the uniform object for the variable with the given {@code name}. This will return null if there is
     * no matching uniform.
     *
     * @param name The uniform variable name
     *
     * @return The Uniform for the given name, or null if it doesn't exist
     */
    public Uniform getUniform(String name);

    /**
     * @return All vertex attributes used by the shader, ordered by their compiled index
     */
    public List<? extends Attribute> getAttributes();

    /**
     * Get the attribute object for the variable with the given {@code name}. This will return null if there
     * is no matching attribute.
     *
     * @param name The attribute variable name
     *
     * @return The Attribute for the given name, or null if it doesn't exist
     */
    public Attribute getAttribute(String name);

    /**
     * Get the mapping from fragment shader output variable to the indexed color buffer. New versions support
     * defining custom output variables, in which case the mapping must have been specified by {@link
     * com.ferox.renderer.builder.ShaderBuilder#bindColorBuffer(String, int)}. If not defined manually, the
     * mapping will be assigned when the shader is compiled. However, to query that assigned binding, the
     * builder must be informed of the variable with a call to {@link com.ferox.renderer.builder.ShaderBuilder#requestBinding(String...)}
     * <p/>
     * The reserved 'gl_FragColor' will always return {@code 0}. The reserved 'gl_FragData[n]' will always
     * return {@code n}.
     *
     * @param outVariableName The output variable name
     *
     * @return The color buffer the output is stored into, or -1 if no mapping is found for that variable
     */
    //
    public int getColorBuffer(String outVariableName);

    /**
     * Get the GLSL version that the shader code was written against. The version is declared in the GLSL code
     * with the {@code #version INT} declaration on the first line. The version is reported in integer form,
     * so version 1.3 is 130 and 3.3 is 330, etc.
     * <p/>
     * If no version is declared, it defaults to 110.
     *
     * @return The GLSL version
     */
    public int getGLSLVersion();

    /**
     * @return True if the final program contains a custom geometry stage
     */
    public boolean hasGeometryShader();
}
