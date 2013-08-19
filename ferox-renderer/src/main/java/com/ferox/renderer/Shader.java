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
        BOOL(1, 1, DataType.UNSIGNED_INT),

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
        UVEC4(4, 1, DataType.UNSIGNED_INT),
        BVEC2(2, 1, DataType.UNSIGNED_INT),
        BVEC3(3, 1, DataType.UNSIGNED_INT),
        BVEC4(4, 1, DataType.UNSIGNED_INT),

        SAMPLER_1D(1, 1, null),
        SAMPLER_2D(1, 1, null),
        SAMPLER_3D(1, 1, null),
        SAMPLER_CUBE(1, 1, null),
        SAMPLER_1D_SHADOW(1, 1, null),
        SAMPLER_2D_SHADOW(1, 1, null),
        SAMPLER_CUBE_SHADOW(1, 1, null),
        SAMPLER_1D_ARRAY(1, 1, null),
        SAMPLER_2D_ARRAY(1, 1, null),

        USAMPLER_1D(1, 1, null),
        USAMPLER_2D(1, 1, null),
        USAMPLER_3D(1, 1, null),
        USAMPLER_CUBE(1, 1, null),
        USAMPLER_1D_ARRAY(1, 1, null),
        USAMPLER_2D_ARRAY(1, 1, null),

        ISAMPLER_1D(1, 1, null),
        ISAMPLER_2D(1, 1, null),
        ISAMPLER_3D(1, 1, null),
        ISAMPLER_CUBE(1, 1, null),
        ISAMPLER_1D_ARRAY(1, 1, null),
        ISAMPLER_2D_ARRAY(1, 1, null);

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
         * @return Get the number of columns required to hold the complete data. Each column of a matrix gets
         *         its own vertex attribute when used for shader attribute inputs. Uniforms only use one
         *         uniform index regardless of the number of columns.
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
     * Abstract access to variable information exposed by GLSL shaders through OpenGL. The variables will be
     * either uniforms or attributes to the vertex shader. Array access and declarations may not be supported
     * depending on the GLSL version.
     */
    public static interface Variable {
        /**
         * Get the value type of this variable. If getLength() > 1, then this is the component type of the
         * array.
         *
         * @return The type of this variable
         */
        public VariableType getType();

        /**
         * Return the number of primitives of getType() used by this variable. If it's > 1, then the variable
         * represents an array.
         *
         * @return Size of the variable, in units of getType()
         */
        public int getLength();

        /**
         * Return the name of the variable as declared in the glsl code of the uniform's owner. See {@link
         * #getUniform(String)} for specifics on how names are formatted for complex variables.
         *
         * @return The name of the variable
         */
        public String getName();

        /**
         * @return The compiled index of the variable within the program
         */
        public int getIndex();

        /**
         * @return True if the variable starts with 'gl_' and represents one of the special variables declared
         *         in the (older) GLSL specs
         */
        public boolean isReserved();
    }

    /**
     * Variable type representing uniforms in a shader.
     */
    public static interface Uniform extends Variable {
    }

    public static interface Attribute extends Variable {
    }

    /**
     * Get the detected uniforms from the linked shader. These are the uniforms reported by OpenGL. Unused
     * uniforms that have been compiled away will not be included. The returned list will have expanded out
     * all struct members into individual uniform objects.
     *
     * @return All uniforms used in the shader, ordered by their compiled index
     */
    public List<? extends Uniform> getUniforms();

    /**
     * Get the uniform object for the variable with the given {@code name}. This will return null if there is
     * no matching uniform. The variable name must be the full name of the uniform excluding any last array
     * access. This is because intermediate struct and array accesses are hard-coded and expanded into uniform
     * slots by OpenGL. As an example, the following uniform declaration:
     * <pre>
     *     struct MyType {
     *         vec3 innerArray[2];
     *         vec4 var;
     *     }
     *     uniform MyType outerArray[3];
     * </pre>
     * would produce the following uniform names (assuming the outer array indices were all used): <ul>
     * <li>outerArray[0].innerArray (with a reported length of 2)</li> <li>outerArray[0].var</li>
     * <li>outerArray[1].innerArray (ditto)</li> <li>outerArray[1].var</li> </ul>
     * <p/>
     * Uniforms of built-in types and arrays of built-in types can be accessed using their declared variable
     * name.
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
     * is no matching attribute. As with uniform names, the attribute name should not include any array
     * index.
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
