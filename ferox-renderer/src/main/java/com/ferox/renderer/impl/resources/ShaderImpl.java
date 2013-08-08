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
package com.ferox.renderer.impl.resources;

import com.ferox.renderer.DataType;
import com.ferox.renderer.Shader;
import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class ShaderImpl extends AbstractResource<ShaderImpl.ShaderHandle> implements Shader {
    private final int glslVersion;
    private final List<UniformImpl> uniforms;
    private final List<AttributeImpl> attributes;
    private final Map<String, Integer> bufferMap;

    public ShaderImpl(ShaderHandle handle, int glslVersion, List<UniformImpl> uniforms,
                      List<AttributeImpl> attributes, Map<String, Integer> bufferMap) {
        super(handle);
        this.glslVersion = glslVersion;

        // FIXME sort in appropriate order
        this.uniforms = Collections.unmodifiableList(uniforms);
        this.attributes = Collections.unmodifiableList(attributes);
        this.bufferMap = Collections.unmodifiableMap(bufferMap);
    }

    @Override
    public List<? extends Uniform> getUniforms() {
        return uniforms;
    }

    @Override
    public Uniform getUniform(String name) {
        for (Uniform u : uniforms) {
            if (u.getName().equals(name)) {
                return u;
            }
        }
        return null;
    }

    @Override
    public List<? extends Attribute> getAttributes() {
        return attributes;
    }

    @Override
    public Attribute getAttribute(String name) {
        for (Attribute a : attributes) {
            if (a.getName().equals(name)) {
                return a;
            }
        }
        return null;
    }

    @Override
    public int getColorBuffer(String outVariableName) {
        Integer mappedBuffer = bufferMap.get(outVariableName);
        if (mappedBuffer != null) {
            return mappedBuffer;
        } else {
            // -1 is flag for unknown output variable
            return -1;
        }
    }

    @Override
    public int getGLSLVersion() {
        return glslVersion;
    }

    @Override
    public boolean hasGeometryShader() {
        return getHandle().geometryShaderID > 0;
    }

    public static class UniformImpl implements Uniform {
        public final ShaderHandle owner;

        private final VariableType type;
        private final String name;
        private final int index;
        private final int length;

        public final FloatBuffer floatValues; // non-null for float types only
        public final IntBuffer intValues; // non-null for int and sampler types only

        // for samplers, the intValues holds a unique pre-assigned texture unit
        // reserved for any sampler assigned to the uniform
        public TextureImpl.TextureHandle texture;

        public boolean initialized;

        public UniformImpl(ShaderHandle owner, VariableType type, int length, String name, int index) {
            this.owner = owner;
            this.type = type;
            this.length = length;
            this.name = name;
            this.index = index;

            int bufferSize = type.getPrimitiveCount() * length;
            if (type == VariableType.FLOAT || type == VariableType.VEC2 ||
                type == VariableType.VEC3 || type == VariableType.VEC4 ||
                type == VariableType.MAT2 || type == VariableType.MAT3 ||
                type == VariableType.MAT4) {
                // only floating point types
                floatValues = BufferUtil.newByteBuffer(DataType.FLOAT, bufferSize).asFloatBuffer();
                intValues = null;
            } else {
                // all other types are stored as ints
                floatValues = null;
                intValues = BufferUtil.newByteBuffer(DataType.INT, bufferSize).asIntBuffer();
            }

            texture = null;
            initialized = false;
        }

        public UniformImpl(UniformImpl u) {
            owner = u.owner;
            type = u.type;
            name = u.name;
            index = u.index;
            length = u.length;

            texture = u.texture;
            initialized = u.initialized;

            if (u.floatValues != null) {
                floatValues = BufferUtil.newByteBuffer(DataType.FLOAT, u.floatValues.capacity())
                                        .asFloatBuffer();
                floatValues.put(u.floatValues).reset();
                intValues = null;
            } else {
                intValues = BufferUtil.newByteBuffer(DataType.INT, u.intValues.capacity()).asIntBuffer();
                intValues.put(u.intValues).reset();
                floatValues = null;
            }
        }

        @Override
        public VariableType getType() {
            return type;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isReserved() {
            return name.startsWith("gl_");
        }

        @Override
        public String toString() {
            if (length > 1) {
                return String.format("uniform %s[%d] %s at %d", type, length, name, index);
            } else {
                return String.format("uniform %s %s at %d", type, name, index);
            }
        }
    }

    public static class AttributeImpl implements Attribute {
        public final ShaderHandle owner;

        private final VariableType type;
        private final String name;
        private final int index;
        private final int length;

        public AttributeImpl(ShaderHandle owner, VariableType type, String name, int length, int index) {
            this.owner = owner;
            this.type = type;
            this.name = name;
            this.index = index;
            this.length = length;
        }

        @Override
        public int getLength() {
            return length;
        }

        @Override
        public VariableType getType() {
            return type;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public int getIndex() {
            return index;
        }

        @Override
        public boolean isReserved() {
            return name.startsWith("gl_");
        }

        @Override
        public String toString() {
            if (length > 1) {
                return String.format("attribute %s[%d] %s at %d", type, length, name, index);
            } else {
                return String.format("attribute %s %s at %d", type, name, index);
            }
        }
    }

    public static class ShaderHandle extends ResourceHandle {
        public final int programID;
        public final int vertexShaderID;
        public final int fragmentShaderID;
        public final int geometryShaderID;

        // considered final and immutable, these hold the same lists as reported by the shader
        // once they've been detected
        public List<UniformImpl> uniforms;
        public List<AttributeImpl> attributes;

        public ShaderHandle(FrameworkImpl framework, int programID, int vertexShaderID, int fragmentShaderID,
                            int geometryShaderID) {
            super(framework);
            this.programID = programID;
            this.vertexShaderID = vertexShaderID;
            this.fragmentShaderID = fragmentShaderID;
            this.geometryShaderID = geometryShaderID;
        }

        @Override
        protected void destroyImpl(OpenGLContext context) {
            if (context.getState().shader == this) {
                context.bindShader(null);
            }
            getFramework().getResourceFactory().deleteShader(context, this);
        }
    }
}
