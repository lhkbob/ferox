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
package com.ferox.renderer.impl;

import com.ferox.math.*;
import com.ferox.renderer.ContextState;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.impl.drivers.GlslShaderHandle;
import com.ferox.renderer.impl.drivers.GlslShaderHandle.Attribute;
import com.ferox.renderer.impl.drivers.GlslShaderHandle.Uniform;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.*;
import com.ferox.resource.GlslShader.AttributeType;
import com.ferox.resource.GlslUniform.UniformType;
import com.ferox.resource.Texture.Target;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * <p/>
 * The AbstractGlslRenderer is an abstract implementation of {@link GlslRenderer}. It uses
 * a {@link RendererDelegate} to handle implementing the methods exposed by {@link
 * Renderer}. The AbstractGlslRenderer tracks the current state, and when necessary,
 * delegate to protected abstract methods which have the responsibility of actually making
 * OpenGL calls.
 * <p/>
 * It makes a best-effort attempt to preserve the texture, vertex attribute, and shader
 * state when resource deadlocks must be resolved. It is possible that a texture must be
 * unbound or will have its data changed based on the actions of another render task.
 * Additionally, the preserving a shader's state when it undergoes concurrent updates is
 * quite complicated, so it is possible that it cannot be preserved if uniforms or
 * attributes are changed or reordered in the shader definition.
 *
 * @author Michael Ludwig
 */
public abstract class AbstractGlslRenderer extends AbstractRenderer
        implements GlslRenderer {
    protected class VertexAttributeBinding {
        // Used to handle relocking/unlocking
        public final int attributeIndex;

        public VertexBufferObject vbo;
        public VertexBufferObjectHandle handle;

        public int offset;
        public int stride;
        public int elementSize;

        private VertexAttributeBinding(int index) {
            attributeIndex = index;
        }
    }

    protected class TextureBinding {
        public Texture texture;
        public TextureHandle handle;

        public int referenceCount;

        public final int texUnit;

        public TextureBinding(int unit) {
            texUnit = unit;
            referenceCount = 0;
        }
    }

    protected class AttributeBinding {
        public final Attribute attr;

        public final VertexAttributeBinding[] columnVBOs;
        public final FloatBuffer columnValues; // column-major if multiple columns exist
        public final boolean[] columnValuesValid;

        public AttributeBinding(Attribute attr, AttributeBinding oldBinding) {
            this.attr = attr;
            columnVBOs = oldBinding.columnVBOs;
            columnValues = oldBinding.columnValues;
            columnValuesValid = oldBinding.columnValuesValid;
        }

        public AttributeBinding(Attribute attr) {
            this.attr = attr;
            columnVBOs = new VertexAttributeBinding[attr.type.getColumnCount()];
            for (int i = 0; i < columnVBOs.length; i++) {
                columnVBOs[i] = genericAttributeStates[attr.index + i];
            }

            columnValues = BufferUtil
                    .newFloatBuffer(attr.type.getColumnCount() * attr.type.getRowCount());
            columnValuesValid = new boolean[attr.type.getColumnCount()];
        }
    }

    protected class UniformBinding {
        public final Uniform uniform;

        public final FloatBuffer floatValues;
        public final IntBuffer intValues;

        public boolean valuesValid;
        public boolean isTextureBinding;

        public UniformBinding(Uniform uniform, UniformBinding old) {
            this.uniform = uniform;

            floatValues = old.floatValues;
            intValues = old.intValues;
            valuesValid = old.valuesValid;
            isTextureBinding = old.isTextureBinding;
        }

        public UniformBinding(Uniform uniform) {
            this.uniform = uniform;
            isTextureBinding = false;
            valuesValid = false;

            int length = uniform.uniform.getLength() *
                         uniform.uniform.getType().getPrimitiveCount();
            switch (uniform.uniform.getType()) {
            case FLOAT:
            case FLOAT_MAT2:
            case FLOAT_MAT3:
            case FLOAT_MAT4:
            case FLOAT_VEC2:
            case FLOAT_VEC3:
            case FLOAT_VEC4:
                floatValues = BufferUtil.newFloatBuffer(length);
                intValues = null;
                break;
            case INT:
            case INT_VEC2:
            case INT_VEC3:
            case INT_VEC4:
            case BOOL:
            case SHADOW_MAP:
            case TEXTURE_1D:
            case TEXTURE_2D:
            case TEXTURE_3D:
            case TEXTURE_CUBEMAP:
                intValues = BufferUtil.newIntBuffer(length);
                floatValues = null;
                break;
            default:
                intValues = null;
                floatValues = null;
                break;
            }
        }
    }

    /*
     * Valid type definitions for the different setUniform() calls
     */
    private static final UniformType[] VALID_FLOAT = { UniformType.FLOAT };
    private static final UniformType[] VALID_FLOAT2 = { UniformType.FLOAT_VEC2 };
    private static final UniformType[] VALID_FLOAT3 = { UniformType.FLOAT_VEC3 };
    private static final UniformType[] VALID_FLOAT4 = {
            UniformType.FLOAT_VEC4, UniformType.FLOAT_MAT2
    };
    private static final UniformType[] VALID_FLOAT_MAT3 = { UniformType.FLOAT_MAT3 };
    private static final UniformType[] VALID_FLOAT_MAT4 = { UniformType.FLOAT_MAT4 };
    private static final UniformType[] VALID_FLOAT_ANY = {
            UniformType.FLOAT, UniformType.FLOAT_VEC2, UniformType.FLOAT_VEC3,
            UniformType.FLOAT_VEC3, UniformType.FLOAT_VEC4, UniformType.FLOAT_MAT2,
            UniformType.FLOAT_MAT3, UniformType.FLOAT_MAT4
    };
    private static final UniformType[] VALID_INT = { UniformType.INT };
    private static final UniformType[] VALID_INT2 = { UniformType.INT_VEC2 };
    private static final UniformType[] VALID_INT3 = { UniformType.INT_VEC3 };
    private static final UniformType[] VALID_INT4 = { UniformType.INT_VEC4 };
    private static final UniformType[] VALID_INT_ANY = {
            UniformType.INT, UniformType.INT_VEC2, UniformType.INT_VEC3,
            UniformType.INT_VEC4
    };
    private static final UniformType[] VALID_BOOL = { UniformType.BOOL };
    private static final UniformType[] VALID_T1D = { UniformType.TEXTURE_1D };
    private static final UniformType[] VALID_T2D = {
            UniformType.TEXTURE_2D, UniformType.SHADOW_MAP
    };
    private static final UniformType[] VALID_T3D = { UniformType.TEXTURE_3D };
    private static final UniformType[] VALID_TCM = { UniformType.TEXTURE_CUBEMAP };
    private static final UniformType[] VALID_TEXTURE_ANY = {
            UniformType.TEXTURE_1D, UniformType.TEXTURE_2D, UniformType.TEXTURE_3D,
            UniformType.TEXTURE_CUBEMAP, UniformType.SHADOW_MAP
    };

    protected GlslShader shader;
    protected GlslShaderHandle shaderHandle;

    protected Map<String, AttributeBinding> attributeBindings;
    protected Map<String, UniformBinding> uniformBindings;
    protected TextureBinding[] textureBindings; // "final" after first activate()

    // vertex attribute handling
    protected VertexAttributeBinding[] genericAttributeStates; // "final" after first activate()
    protected VertexBufferObject arrayVboBinding = null;
    protected int activeArrayVbos = 0;

    public AbstractGlslRenderer(RendererDelegate delegate) {
        super(delegate);
    }

    @Override
    public void activate(AbstractSurface surface, OpenGLContext context,
                         ResourceManager manager) {
        super.activate(surface, context, manager);

        if (textureBindings == null) {
            RenderCapabilities caps = surface.getFramework().getCapabilities();
            int numTextures = Math.max(caps.getMaxVertexShaderTextures(),
                                       caps.getMaxFragmentShaderTextures());
            textureBindings = new TextureBinding[numTextures];
            for (int i = 0; i < numTextures; i++) {
                textureBindings[i] = new TextureBinding(i);
            }
        }

        if (genericAttributeStates == null) {
            int numAttrs = surface.getFramework().getCapabilities()
                                  .getMaxVertexAttributes();
            genericAttributeStates = new VertexAttributeBinding[numAttrs];
            for (int i = 0; i < numAttrs; i++) {
                genericAttributeStates[i] = new VertexAttributeBinding(i);
            }
        }
    }

    protected void resetAttributeAndTextureBindings() {
        // unbind all textures and reset reference counts to 0
        for (int i = 0; i < textureBindings.length; i++) {
            if (textureBindings[i].texture != null) {
                glBindTexture(i, textureBindings[i].handle.target, null);
                resourceManager.unlock(textureBindings[i].texture);
            }

            textureBindings[i].texture = null;
            textureBindings[i].handle = null;
            textureBindings[i].referenceCount = 0;
        }

        // unbind all vertex attributes
        for (int i = 0; i < genericAttributeStates.length; i++) {
            if (genericAttributeStates[i].vbo != null) {
                glEnableAttribute(i, false);
                unbindArrayVboMaybe(genericAttributeStates[i].vbo);
                resourceManager.unlock(genericAttributeStates[i].vbo);
            }

            genericAttributeStates[i].vbo = null;
            genericAttributeStates[i].handle = null;
        }
    }

    @Override
    public void setShader(GlslShader shader) {
        if (this.shader != shader) {
            // reset the extra state as a safety precaution
            resetAttributeAndTextureBindings();

            GlslShaderHandle oldHandle = null;
            if (this.shader != null) {
                oldHandle = shaderHandle;
                resourceManager.unlock(this.shader);
                this.shader = null;
                shaderHandle = null;
            }

            if (shader != null) {
                GlslShaderHandle newHandle = (GlslShaderHandle) resourceManager
                        .lock(context, shader);
                if (newHandle != null) {
                    this.shader = shader;
                    shaderHandle = newHandle;
                }
            }

            if (this.shader != null) {
                // we have a valid shader, so use it and rebuild attribute/uniform bindings
                glUseProgram(shaderHandle);

                // fill in the attribute bindings
                attributeBindings = new HashMap<String, AttributeBinding>();
                for (Entry<String, Attribute> a : shaderHandle.attributes.entrySet()) {
                    attributeBindings.put(a.getKey(), new AttributeBinding(a.getValue()));
                }

                // fill in the uniform bindings
                uniformBindings = new HashMap<String, UniformBinding>();
                for (Entry<String, Uniform> u : shaderHandle.uniforms.entrySet()) {
                    uniformBindings.put(u.getKey(), new UniformBinding(u.getValue()));
                }
            } else {
                // no valid shader
                if (oldHandle != null) {
                    glUseProgram(null);
                }

                attributeBindings = null;
                uniformBindings = null;
            }
        }
    }

    /**
     * Bind the given shader handle so that subsequent invocations of render() will use it
     * as the active GLSL shader. If null, it should unbind the current program.
     */
    protected abstract void glUseProgram(GlslShaderHandle shader);

    @Override
    public Map<String, AttributeType> getAttributes() {
        if (shader == null) {
            return Collections.emptyMap();
        }

        Attribute attr;
        Map<String, AttributeType> attrs = new HashMap<String, AttributeType>();
        for (Entry<String, AttributeBinding> a : attributeBindings.entrySet()) {
            attr = a.getValue().attr;
            attrs.put(attr.name, attr.type);
        }

        return Collections.unmodifiableMap(attrs);
    }

    @Override
    public Map<String, GlslUniform> getUniforms() {
        if (shader == null) {
            return Collections.emptyMap();
        }

        Uniform uniform;
        Map<String, GlslUniform> uniforms = new HashMap<String, GlslUniform>();
        for (Entry<String, UniformBinding> u : uniformBindings.entrySet()) {
            uniform = u.getValue().uniform;
            uniforms.put(uniform.name, uniform.uniform);
        }

        return Collections.unmodifiableMap(uniforms);
    }

    @Override
    public void bindAttribute(String glslAttrName, VertexAttribute attr) {
        bindAttribute(glslAttrName, 0, attr);
    }

    @Override
    public void bindAttribute(String glslAttrName, int column, VertexAttribute attr) {
        if (attr != null) {
            AttributeBinding a = verifyAttribute(glslAttrName, attr.getElementSize(),
                                                 column + 1);
            if (a == null) {
                return; // ignore call for unsupported attribute types
            }

            VertexAttributeBinding state = a.columnVBOs[column];
            boolean accessDiffers = (state.offset != attr.getOffset() ||
                                     state.stride != attr.getStride() ||
                                     state.elementSize != attr.getElementSize());
            if (state.vbo != attr.getVBO() || accessDiffers) {
                VertexBufferObject oldVbo = state.vbo;
                if (state.vbo != null && oldVbo != attr.getVBO()) {
                    // unlock the old vbo
                    resourceManager.unlock(state.vbo);
                    state.vbo = null;
                    state.handle = null;
                }

                if (state.vbo == null) {
                    // lock the new vbo
                    VertexBufferObjectHandle newHandle = (VertexBufferObjectHandle) resourceManager
                            .lock(context, attr.getVBO());
                    if (newHandle != null) {
                        state.vbo = attr.getVBO();
                        state.handle = newHandle;
                    }
                }

                if (state.vbo != null) {
                    // At this point, state.handle is the handle for the new VBO (or possibly old VBO)
                    state.elementSize = attr.getElementSize();
                    state.offset = attr.getOffset();
                    state.stride = attr.getStride();

                    bindArrayVbo(attr.getVBO(), state.handle, oldVbo);

                    if (oldVbo == null) {
                        glEnableAttribute(state.attributeIndex, true);
                    }
                    glAttributePointer(state.attributeIndex, state.handle, state.offset,
                                       state.stride, state.elementSize);
                    a.columnValuesValid[column] = false;
                } else if (oldVbo != null) {
                    // Since there was an old vbo we need clean some things up
                    glEnableAttribute(state.attributeIndex, false);
                    unbindArrayVboMaybe(oldVbo);

                    // set a good default attribute value
                    bindAttribute(a, column, a.attr.type.getRowCount(), 0f, 0f, 0f, 0f);
                }
            }
        } else {
            // Since we don't have a row count to use, we fudge the verifyAttribute method here
            if (glslAttrName == null) {
                throw new NullPointerException("GLSL attribute name cannot be null");
            }

            AttributeBinding a = (attributeBindings != null ? attributeBindings
                    .get(glslAttrName) : null);
            if (a != null) {
                if (a.attr.type.getColumnCount() <= column) {
                    throw new IllegalArgumentException(
                            "GLSL attribute with a type of " + a.attr.type +
                            " cannot use " + (column + 1) + " columns");
                }

                // The attribute is meant to be unbound
                VertexAttributeBinding state = a.columnVBOs[column];
                if (state.vbo != null) {
                    // disable the attribute
                    glEnableAttribute(state.attributeIndex, false);
                    // possibly unbind it from the array vbo
                    unbindArrayVboMaybe(state.vbo);

                    // unlock it
                    resourceManager.unlock(state.vbo);
                    state.vbo = null;
                    state.handle = null;

                    // set a good default attribute value
                    bindAttribute(a, column, a.attr.type.getRowCount(), 0f, 0f, 0f, 0f);
                }
            }
        }
    }

    private AttributeBinding verifyAttribute(String glslAttrName, int rowCount,
                                             int colCount) {
        if (glslAttrName == null) {
            throw new NullPointerException("GLSL attribute name cannot be null");
        }
        if (colCount <= 0) {
            throw new IllegalArgumentException("Column must be at least 0");
        }

        AttributeBinding a = (attributeBindings != null ? attributeBindings
                .get(glslAttrName) : null);
        if (a != null) {
            if (a.attr.type == AttributeType.UNSUPPORTED) {
                // no useful binding
                a = null;
            } else {
                if (a.attr.type.getColumnCount() < colCount) {
                    throw new IllegalArgumentException(
                            "GLSL attribute with a type of " + a.attr.type +
                            " cannot use " + colCount + " columns");
                }
                if (a.attr.type.getRowCount() != rowCount) {
                    throw new IllegalArgumentException(
                            "GLSL attribute with a type of " + a.attr.type +
                            " cannot use " + rowCount + " rows");
                }
            }
        }
        return a;
    }

    private void bindAttribute(AttributeBinding a, int col, int rowCount, float v1,
                               float v2, float v3, float v4) {
        if (a == null) {
            return; // unsupported or unknown binding, ignore at this stage
        }

        if (a.columnVBOs[col].vbo != null) {
            // there was a previously bound vertex attribute, so unbind it
            glEnableAttribute(a.attr.index + col, false);
            unbindArrayVboMaybe(a.columnVBOs[col].vbo);
            resourceManager.unlock(a.columnVBOs[col].vbo);
            a.columnVBOs[col].vbo = null;
            a.columnVBOs[col].handle = null;
        }

        int index = col * rowCount;
        boolean pushChanges = !a.columnValuesValid[col];
        switch (rowCount) {
        case 4:
            pushChanges = pushChanges || a.columnValues.get(index + 3) != v4;
            a.columnValues.put(index + 3, v4);
        case 3:
            pushChanges = pushChanges || a.columnValues.get(index + 2) != v3;
            a.columnValues.put(index + 2, v3);
        case 2:
            pushChanges = pushChanges || a.columnValues.get(index + 1) != v2;
            a.columnValues.put(index + 1, v2);
        case 1:
            pushChanges = pushChanges || a.columnValues.get(index) != v1;
            a.columnValues.put(index, v1);
        }

        a.columnValuesValid[col] = true;
        if (pushChanges) {
            glAttributeValue(a.attr.index + col, rowCount, v1, v2, v3, v4);
        }
    }

    /**
     * Set the generic vertex attribute at attr to the given vector marked by v1, v2, v3,
     * and v4. Depending on rowCount, certain vector values can be ignored (i.e. if
     * rowCount is 3, v4 is meaningless).
     */
    protected abstract void glAttributeValue(int attr, int rowCount, float v1, float v2,
                                             float v3, float v4);

    @Override
    public void bindAttribute(String glslAttrName, float val) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 1, 1);
        bindAttribute(ab, 0, 1, val, -1f, -1f, -1f);
    }

    @Override
    public void bindAttribute(String glslAttrName, float v1, float v2) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 2, 1);
        bindAttribute(ab, 0, 2, v1, v2, -1f, -1f);
    }

    @Override
    public void bindAttribute(String glslAttrName, @Const Vector3 v) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 3, 1);
        bindAttribute(ab, 0, 3, (float) v.x, (float) v.y, (float) v.z, -1f);
    }

    @Override
    public void bindAttribute(String glslAttrName, @Const Vector4 v) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 4, 1);
        bindAttribute(ab, 0, 4, (float) v.x, (float) v.y, (float) v.z, (float) v.w);
    }

    @Override
    public void bindAttribute(String glslAttrName, @Const Matrix3 v) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 3, 3);

        bindAttribute(ab, 0, 3, (float) v.get(0, 0), (float) v.get(1, 0),
                      (float) v.get(2, 0), -1f);
        bindAttribute(ab, 1, 3, (float) v.get(0, 1), (float) v.get(1, 1),
                      (float) v.get(2, 1), -1f);
        bindAttribute(ab, 2, 3, (float) v.get(0, 2), (float) v.get(1, 2),
                      (float) v.get(2, 2), -1f);
    }

    @Override
    public void bindAttribute(String glslAttrName, @Const Matrix4 v) {
        AttributeBinding ab = verifyAttribute(glslAttrName, 4, 4);

        bindAttribute(ab, 0, 4, (float) v.get(0, 0), (float) v.get(1, 0),
                      (float) v.get(2, 0), (float) v.get(3, 0));
        bindAttribute(ab, 1, 4, (float) v.get(0, 1), (float) v.get(1, 1),
                      (float) v.get(2, 1), (float) v.get(3, 1));
        bindAttribute(ab, 2, 4, (float) v.get(0, 2), (float) v.get(1, 2),
                      (float) v.get(2, 2), (float) v.get(3, 2));
        bindAttribute(ab, 3, 4, (float) v.get(0, 3), (float) v.get(1, 3),
                      (float) v.get(2, 3), (float) v.get(3, 3));
    }

    private UniformBinding verifyUniform(String name, UniformType[] validTypes) {
        if (name == null) {
            throw new NullPointerException("Uniform name cannot be null");
        }
        UniformBinding u = (uniformBindings != null ? uniformBindings.get(name) : null);
        if (u == null) {
            return null;
        }

        UniformType type = u.uniform.uniform.getType();
        if (type == UniformType.UNSUPPORTED) {
            return null;
        }

        boolean valid = false;
        for (int i = 0; i < validTypes.length; i++) {
            if (validTypes[i] == type) {
                valid = true;
                break;
            }
        }

        if (!valid) {
            String validTypeStr = Arrays.toString(validTypes);
            throw new IllegalArgumentException(
                    "Expected a type in " + validTypeStr + " but uniform " + name +
                    " is " + type);
        }
        return u;
    }

    @Override
    public void setUniform(String name, float val) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        if (!u.valuesValid || u.floatValues.get(0) != val) {
            u.floatValues.put(0, val);
            u.valuesValid = true;

            u.floatValues.rewind();
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    @Override
    public void setUniform(String name, float v1, float v2) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT2);
        if (u == null) {
            return; // ignore unsupported uniforms
        }
        if (!u.valuesValid || u.floatValues.get(0) != v1 || u.floatValues.get(1) != v2) {
            u.floatValues.put(0, v1);
            u.floatValues.put(1, v2);
            u.valuesValid = true;

            u.floatValues.rewind();
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    /**
     * Set the given uniform's values. The uniform could have any of the FLOAT_ types and
     * could possibly be an array. The buffer will have been rewound already.
     */
    protected abstract void glUniform(Uniform u, FloatBuffer values, int count);

    @Override
    public void setUniform(String name, float v1, float v2, float v3) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT3);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        if (!u.valuesValid || u.floatValues.get(0) != v1 || u.floatValues.get(1) != v2 ||
            u.floatValues.get(2) != v3) {
            u.floatValues.put(0, v1);
            u.floatValues.put(1, v2);
            u.floatValues.put(2, v3);
            u.valuesValid = true;

            u.floatValues.rewind();
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    @Override
    public void setUniform(String name, float v1, float v2, float v3, float v4) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT4);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        if (!u.valuesValid || u.floatValues.get(0) != v1 || u.floatValues.get(1) != v2 ||
            u.floatValues.get(2) != v3 || u.floatValues.get(3) != v4) {
            u.floatValues.put(0, v1);
            u.floatValues.put(1, v2);
            u.floatValues.put(2, v3);
            u.floatValues.put(3, v4);
            u.valuesValid = true;

            u.floatValues.rewind();
            glUniform(u.uniform, u.floatValues, 1);
        }
    }

    @Override
    public void setUniform(String name, @Const Matrix3 val) {
        if (val == null) {
            throw new NullPointerException("Matrix cannot be null");
        }
        UniformBinding u = verifyUniform(name, VALID_FLOAT_MAT3);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        val.get(u.floatValues, 0, false);
        u.floatValues.rewind();
        u.valuesValid = true;
        glUniform(u.uniform, u.floatValues, 1);
    }

    @Override
    public void setUniform(String name, @Const Matrix4 val) {
        if (val == null) {
            throw new NullPointerException("Matrix cannot be null");
        }
        UniformBinding u = verifyUniform(name, VALID_FLOAT_MAT4);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        val.get(u.floatValues, 0, false);
        u.floatValues.rewind();
        u.valuesValid = true;
        glUniform(u.uniform, u.floatValues, 1);
    }

    @Override
    public void setUniform(String name, @Const Vector3 v) {
        if (v == null) {
            throw new NullPointerException("Vector cannot be null");
        }
        setUniform(name, (float) v.x, (float) v.y, (float) v.z);
    }

    @Override
    public void setUniform(String name, @Const Vector4 v) {
        if (v == null) {
            throw new NullPointerException("Vector cannot be null");
        }
        setUniform(name, (float) v.x, (float) v.y, (float) v.z, (float) v.w);
    }

    @Override
    public void setUniform(String name, @Const ColorRGB color) {
        setUniform(name, color, false);
    }

    @Override
    public void setUniform(String name, @Const ColorRGB color, boolean isHDR) {
        if (color == null) {
            throw new NullPointerException("Color cannot be null");
        }

        if (isHDR) {
            setUniform(name, (float) color.redHDR(), (float) color.greenHDR(),
                       (float) color.blueHDR());
        } else {
            setUniform(name, (float) color.red(), (float) color.green(),
                       (float) color.blue());
        }
    }

    @Override
    public void setUniform(String name, float[] vals) {
        UniformBinding u = verifyUniform(name, VALID_FLOAT_ANY);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        GlslUniform uniform = u.uniform.uniform;
        int primitiveCount = uniform.getType().getPrimitiveCount();
        if (vals.length % primitiveCount != 0) {
            throw new IllegalArgumentException(
                    "Length does not align with primitive count of uniform " + name +
                    " with type " + uniform.getType());
        }

        int totalElements = vals.length / primitiveCount;
        if (totalElements != uniform.getLength()) {
            throw new IllegalArgumentException("Number of elements ( " + totalElements +
                                               ") does not equal the length of uniform " +
                                               name + " with " + uniform.getLength() +
                                               " elements");
        }

        // the float array is of the proper length, we assume that it is
        // laid out properly for the uniform's specific float type
        // - also, we don't verify that the array is equal since we expect
        // - the array equals check to take too long
        u.floatValues.rewind(); // must rewind because there is no absolute bulk put
        u.floatValues.put(vals);
        u.valuesValid = true;

        u.floatValues.rewind();
        glUniform(u.uniform, u.floatValues, totalElements);
    }

    @Override
    public void setUniform(String name, int val) {
        UniformBinding u = verifyUniform(name, VALID_INT);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        if (!u.valuesValid || u.intValues.get(0) != val) {
            u.intValues.put(0, val);
            u.valuesValid = true;

            u.intValues.rewind();
            glUniform(u.uniform, u.intValues, 1);
        }
    }

    /**
     * Set the given uniform's values. The uniform could have any of the INT_ types, the
     * BOOL type or any of the texture sampler types, and could possibly be an array.
     */
    protected abstract void glUniform(Uniform u, IntBuffer values, int count);

    @Override
    public void setUniform(String name, int v1, int v2) {
        UniformBinding u = verifyUniform(name, VALID_INT2);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        if (!u.valuesValid || u.intValues.get(0) != v1 || u.intValues.get(1) != v2) {
            u.intValues.put(0, v1);
            u.intValues.put(1, v2);
            u.valuesValid = true;

            u.intValues.rewind();
            glUniform(u.uniform, u.intValues, 1);
        }
    }

    @Override
    public void setUniform(String name, int v1, int v2, int v3) {
        UniformBinding u = verifyUniform(name, VALID_INT3);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        if (!u.valuesValid || u.intValues.get(0) != v1 || u.intValues.get(1) != v2 ||
            u.intValues.get(2) != v3) {
            u.intValues.put(0, v1);
            u.intValues.put(1, v2);
            u.intValues.put(2, v3);
            u.valuesValid = true;

            u.intValues.rewind();
            glUniform(u.uniform, u.intValues, 1);
        }
    }

    @Override
    public void setUniform(String name, int v1, int v2, int v3, int v4) {
        UniformBinding u = verifyUniform(name, VALID_INT4);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        if (!u.valuesValid || u.intValues.get(0) != v1 || u.intValues.get(1) != v2 ||
            u.intValues.get(2) != v3 || u.intValues.get(3) != v4) {
            u.intValues.put(0, v1);
            u.intValues.put(1, v2);
            u.intValues.put(2, v3);
            u.intValues.put(3, v4);
            u.valuesValid = true;

            u.intValues.rewind();
            glUniform(u.uniform, u.intValues, 1);
        }
    }

    @Override
    public void setUniform(String name, int[] vals) {
        if (vals == null) {
            throw new NullPointerException("Values array cannot be null");
        }

        UniformBinding u = verifyUniform(name, VALID_INT_ANY);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        GlslUniform uniform = u.uniform.uniform;
        int primitiveCount = uniform.getType().getPrimitiveCount();
        if (vals.length % primitiveCount != 0) {
            throw new IllegalArgumentException(
                    "Length does not align with primitive count of uniform " + name +
                    " with type " + uniform.getType());
        }

        int totalElements = vals.length / primitiveCount;
        if (totalElements != uniform.getLength()) {
            throw new IllegalArgumentException("Number of elements ( " + totalElements +
                                               ") does not equal the length of uniform " +
                                               name + " with " + uniform.getLength() +
                                               " elements");
        }

        // the int array is of the proper length, we assume that it is
        // laid out properly for the uniform's specific float type
        // - also, we don't verify that the array is equal since we expect
        // - the array equals check to take too long
        u.intValues.rewind(); // must rewind because there is no absolute bulk put
        u.intValues.put(vals);
        u.valuesValid = true;

        u.intValues.rewind();
        glUniform(u.uniform, u.intValues, totalElements);
    }

    @Override
    public void setUniform(String name, boolean val) {
        UniformBinding u = verifyUniform(name, VALID_BOOL);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        int translated = (val ? 1 : 0);
        if (!u.valuesValid || u.intValues.get(0) != translated) {
            u.intValues.put(0, translated);
            u.valuesValid = true;

            u.intValues.rewind();
            glUniform(u.uniform, u.intValues, 1);
        }
    }

    @Override
    public void setUniform(String name, boolean[] vals) {
        UniformBinding u = verifyUniform(name, VALID_BOOL);
        if (u == null) {
            return; // ignore unsupported uniforms
        }

        GlslUniform uniform = u.uniform.uniform;
        if (uniform.getLength() != vals.length) {
            throw new IllegalArgumentException("Number of elements ( " + vals.length +
                                               ") does not equal the length of uniform " +
                                               name + " with " + uniform.getLength() +
                                               " elements");
        }

        // convert the boolean array into an integer array
        for (int i = 0; i < vals.length; i++) {
            u.intValues.put(i, (vals[i] ? 1 : 0));
        }
        u.valuesValid = true;

        u.intValues.rewind();
        glUniform(u.uniform, u.intValues, vals.length);
    }

    // FIXME this method can be cleaned up, broken into multiple methods
    // to improve readability
    @Override
    public void setUniform(String name, Texture texture) {
        if (texture != null) {
            UniformType[] validTypes = null;
            switch (texture.getTarget()) {
            case T_1D:
                validTypes = VALID_T1D;
                break;
            case T_2D:
                validTypes = VALID_T2D;
                break;
            case T_3D:
                validTypes = VALID_T3D;
                break;
            case T_CUBEMAP:
                validTypes = VALID_TCM;
                break;
            }

            UniformBinding u = verifyUniform(name, validTypes);
            if (u == null) {
                return; // ignore unsupported uniforms
            }
            if (u.uniform.uniform.getType() == UniformType.SHADOW_MAP &&
                texture.getFormat() != TextureFormat.DEPTH) {
                throw new IllegalArgumentException(
                        "Shadow map uniforms must be depth textures, not: " +
                        texture.getFormat());
            }

            int oldUnit = -1;
            Target oldTarget = null;
            if (u.isTextureBinding) {
                // if u.isTextureBinding is true, we can assume that handle is not null
                TextureBinding oldBinding = textureBindings[u.intValues.get(0)];
                if (oldBinding.texture == texture) {
                    return; // no change is needed
                }

                // remove uniform's reference to tex unit, since we'll be
                // changing the uniform's binding
                oldBinding.referenceCount--;
                if (oldBinding.referenceCount == 0) {
                    // remember bind point for later, we might need to unbind
                    // the texture if the new texture doesn't just overwrite
                    oldUnit = oldBinding.texUnit;
                    oldTarget = oldBinding.handle.target;

                    // unlock old texture
                    resourceManager.unlock(oldBinding.texture);
                    oldBinding.texture = null;
                    oldBinding.handle = null;
                }
            }

            // search for an existing texture to share the binding
            int newUnit = -1;
            int firstEmpty = -1;
            for (int i = 0; i < textureBindings.length; i++) {
                if (textureBindings[i].texture == texture) {
                    newUnit = i;
                    break;
                } else if (textureBindings[i].texture == null && firstEmpty < 0) {
                    firstEmpty = i;
                }
            }

            boolean needsBind = false;
            if (newUnit < 0) {
                // use the first found empty unit if there is one
                if (firstEmpty >= 0) {
                    // must lock the texture to the unit
                    TextureHandle newHandle = (TextureHandle) resourceManager
                            .lock(context, texture);
                    if (newHandle != null) {
                        textureBindings[firstEmpty].texture = texture;
                        textureBindings[firstEmpty].handle = newHandle;
                        newUnit = firstEmpty;
                        needsBind = true;
                    }
                }
            }

            Target newTarget = (newUnit >= 0 && textureBindings[newUnit].texture != null
                                ? textureBindings[newUnit].handle.target : null);
            if ((oldTarget != null && oldTarget != newTarget) ||
                (oldUnit >= 0 && oldUnit != newUnit)) {
                // unbind old texture
                glBindTexture(oldUnit, oldTarget, null);
            }

            if (newUnit >= 0) {
                // found a bind point
                if (needsBind) {
                    glBindTexture(newUnit, newTarget, textureBindings[newUnit].handle);
                }
                textureBindings[newUnit].referenceCount++;

                u.intValues.put(0, newUnit);
                u.isTextureBinding = true;
                u.valuesValid = true;

                u.intValues.rewind();
                glUniform(u.uniform, u.intValues, 1);
            } else {
                // no bind point because no available texture unit could be found
                u.isTextureBinding = false;
                u.valuesValid = false;
            }
        } else {
            // uniform must be unbound from the texture
            UniformBinding u = verifyUniform(name, VALID_TEXTURE_ANY);
            if (u == null) {
                return;
            }

            if (u.isTextureBinding) {
                int oldTexUnit = u.intValues.get(0);
                textureBindings[oldTexUnit].referenceCount--;

                if (textureBindings[oldTexUnit].referenceCount == 0) {
                    // unlock texture too
                    glBindTexture(oldTexUnit, textureBindings[oldTexUnit].handle.target,
                                  null);
                    resourceManager.unlock(textureBindings[oldTexUnit].texture);
                    textureBindings[oldTexUnit].texture = null;
                    textureBindings[oldTexUnit].handle = null;
                }

                u.isTextureBinding = false;
                u.valuesValid = false;
            }
        }
    }

    /**
     * Bind the given texture provided by the ResourceHandle. If the <tt>handle</tt> is
     * null, unbind the texture currently bound to the given target. <tt>tex</tt>
     * represents the texture unit to bind or unbind the texture on, which starts at 0. If
     * the handle is not null, its target will equal the provided target.
     */
    protected abstract void glBindTexture(int tex, Target target, TextureHandle handle);

    /**
     * Enable the given generic vertex attribute to read in data from an attribute pointer
     * as last assigned by glAttributePointer().
     */
    protected abstract void glEnableAttribute(int attr, boolean enable);

    /**
     * Bind the given resource handle as the array vbo. If null, unbind the array vbo.
     */
    protected abstract void glBindArrayVbo(VertexBufferObjectHandle handle);

    /**
     * Invoke OpenGL commands to set the given attribute pointer. The resource will have
     * already been bound using glBindArrayVbo. If this is for a texture coordinate,
     * glActiveClientTexture will already have been called.
     */
    protected abstract void glAttributePointer(int attr, VertexBufferObjectHandle handle,
                                               int offset, int stride, int elementSize);

    private void bindArrayVbo(VertexBufferObject vbo, VertexBufferObjectHandle handle,
                              VertexBufferObject oldVboOnSlot) {
        if (vbo != arrayVboBinding) {
            glBindArrayVbo(handle);
            activeArrayVbos = 0;
            arrayVboBinding = vbo;

            // If we have to bind the vbo, then the last vbo bound to the slot doesn't
            // matter, since it wasn't counted in the activeArrayVbos counter
            oldVboOnSlot = null;
        }

        // Only update the count if the new vbo isn't replacing itself in the same slot
        if (oldVboOnSlot != vbo) {
            activeArrayVbos++;
        }
    }

    private void unbindArrayVboMaybe(VertexBufferObject vbo) {
        if (vbo == arrayVboBinding) {
            activeArrayVbos--;
            if (activeArrayVbos == 0) {
                glBindArrayVbo(null);
                arrayVboBinding = null;
            }
        }
    }

    // FIXME implement this at the same time I do the fixes to GlslRenderer,
    // like adding int attrs, cleaning up arbitrary array uniforms, etc.
    // FIXME should update the resource driver to have warnings, errors if
    // there are uniforms/attrs of unsupported types
    @Override
    public ContextState<GlslRenderer> getCurrentState() {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void setCurrentState(ContextState<GlslRenderer> state) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void reset() {
        // FIXME not fully correct yet
        delegate.setCurrentState(delegate.defaultState);

        // This unbinds the shader handle, all textures and vertex attributes.
        // It clears the uniform and attribute cached values, but does not
        // assign default values to them.
        setShader(null);
    }
}
