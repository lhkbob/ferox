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
        FLOAT(1),
        FLOAT_VEC2(2),
        FLOAT_VEC3(3),
        FLOAT_VEC4(4),
        FLOAT_MAT2(4),
        FLOAT_MAT3(9),
        FLOAT_MAT4(16),

        INT(1),
        INT_VEC2(2),
        INT_VEC3(3),
        INT_VEC4(4),

        TEXTURE_1D(1),
        TEXTURE_2D(1),
        TEXTURE_3D(1),
        TEXTURE_CUBEMAP(1),
        SHADOW_MAP(1),

        BOOL(1),
        UNSUPPORTED(0);

        private final int primCount;

        private UniformType(int primCount) {
            this.primCount = primCount;
        }

        public int getPrimitiveCount() {
            return primCount;
        }
    }

    private final UniformType type;
    private final int length;
    private final String name;

    public GlslUniform(String name, UniformType type, int length) {
        if (name == null) {
            throw new NullPointerException("Cannot specify a null name");
        }
        if (type == null) {
            throw new NullPointerException("Cannot specify a null uniform type");
        }

        if (length < 1) {
            throw new IllegalArgumentException("Cannot specify length < 1: " + length);
        }

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
