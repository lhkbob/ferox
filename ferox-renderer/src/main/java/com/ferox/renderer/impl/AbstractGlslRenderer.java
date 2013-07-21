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
import com.ferox.renderer.*;
import com.ferox.renderer.impl.resources.BufferImpl;
import com.ferox.renderer.impl.resources.ShaderImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashSet;
import java.util.Set;

/**
 * <p/>
 * The AbstractGlslRenderer is an abstract implementation of {@link GlslRenderer}. It uses a {@link
 * RendererDelegate} to handle implementing the methods exposed by {@link Renderer}. The AbstractGlslRenderer
 * tracks the current state, and when necessary, delegate to protected abstract methods which have the
 * responsibility of actually making OpenGL calls.
 * <p/>
 * It makes a best-effort attempt to preserve the texture, vertex attribute, and shader state when resource
 * deadlocks must be resolved. It is possible that a texture must be unbound or will have its data changed
 * based on the actions of another render task. Additionally, the preserving a shader's state when it
 * undergoes concurrent updates is quite complicated, so it is possible that it cannot be preserved if
 * uniforms or attributes are changed or reordered in the shader definition.
 *
 * @author Michael Ludwig
 */
public abstract class AbstractGlslRenderer extends AbstractRenderer implements GlslRenderer {
    protected final ShaderOnlyState state;
    protected final ShaderOnlyState defaultState;

    /**
     * Create a new glsl renderer for the given context.
     *
     * @param context             The context using the renderer
     * @param delegate            The delegate completing the implementation
     * @param numVertexAttributes The number of vertex attributes to support
     */
    public AbstractGlslRenderer(OpenGLContext context, RendererDelegate delegate, int numVertexAttributes) {
        super(context, delegate);
        state = new ShaderOnlyState(numVertexAttributes);
        defaultState = new ShaderOnlyState(state);
    }

    @Override
    public ContextState<GlslRenderer> getCurrentState() {
        return new ShaderState(new SharedState(delegate.state), new ShaderOnlyState(state));
    }

    @Override
    public void setCurrentState(ContextState<GlslRenderer> state) {
        ShaderState s = (ShaderState) state;
        setCurrentState(s.shaderState, s.sharedState);
    }

    @Override
    public void reset() {
        setCurrentState(defaultState, delegate.defaultState);
    }

    private void setCurrentState(ShaderOnlyState shaderState, SharedState sharedState) {
        // this will bind the expected shader and the textures, so that will be in a consistent state
        // with respect to sampler uniforms pointing to the correct texture unit
        delegate.setCurrentState(sharedState);

        // set all attributes
        for (int i = 0; i < shaderState.attributes.length; i++) {
            ShaderOnlyState.AttributeState a = shaderState.attributes[i];
            if (a.vbo != null) {
                // configure a pointer, we assume it properly matches the attribute type
                bindAttributeHandle(state.attributes[i], a.vbo, a.offset, a.stride, a.elementSize);
            } else {
                if (a.dataType == DataType.FLOAT) {
                    bindAttribute(state.attributes[i], 4, a.floatAttrValues[0], a.floatAttrValues[1],
                                  a.floatAttrValues[2], a.floatAttrValues[3]);
                } else if (a.dataType == DataType.UNSIGNED_INT) {
                    bindAttribute(state.attributes[i], 4, true, a.intAttrValues[0], a.intAttrValues[1],
                                  a.intAttrValues[2], a.intAttrValues[3]);
                } else {
                    bindAttribute(state.attributes[i], 4, false, a.intAttrValues[0], a.intAttrValues[1],
                                  a.intAttrValues[2], a.intAttrValues[3]);
                }
            }
        }
    }

    @Override
    public void setShader(Shader shader) {
        if (shader == null) {
            context.bindShader(null);
        } else {
            context.bindShader(((ShaderImpl) shader).getHandle());

            // perform maintenance on bound texture state to make it line up with
            // what the texture uniforms are expecting
            int unit = 0;
            Set<Integer> reservedUnits = new HashSet<>();
            Set<ShaderImpl.UniformImpl> needsInit = new HashSet<>();

            for (Shader.Uniform u : shader.getUniforms()) {
                if (u.getType().getPrimitiveType() == null) {
                    // sampler types have a null primitive type
                    ShaderImpl.UniformImpl ui = (ShaderImpl.UniformImpl) u;
                    if (ui.initialized) {
                        // mark unit as reserved and bind the last assigned sampler
                        reservedUnits.add(ui.intValues.get(0));
                        context.bindTexture(ui.intValues.get(0), ui.texture);
                    } else {
                        needsInit.add(ui);
                    }
                }
            }

            // complete initialization now that reservedUnits holds all used texture units
            for (ShaderImpl.UniformImpl u : needsInit) {
                // search for an unreserved unit
                while (reservedUnits.contains(unit)) {
                    unit++;
                }

                // mark it as reserved and configure the uniform
                reservedUnits.add(unit);
                u.intValues.put(0, unit);
                u.initialized = true;
                glUniform(u, u.intValues);

                // bind a null texture
                context.bindTexture(unit, null);
            }
        }
    }

    @Override
    public void bindAttribute(Shader.Attribute attribute, VertexAttribute attr) {
        bindAttribute(attribute, 0, attr);
    }

    @Override
    public void bindAttribute(Shader.Attribute attribute, int column, VertexAttribute attr) {
        if (attribute == null) {
            throw new NullPointerException("Attribute can't be null");
        }
        if (attribute.getType().getColumnCount() <= column) {
            throw new IllegalArgumentException("GLSL attribute with a type of " + attribute.getType() +
                                               " cannot use " + (column + 1) + " columns");
        }

        ShaderOnlyState.AttributeState a = state.attributes[attribute.getIndex() + column];
        if (attr != null) {
            // validate buffer type consistent with attribute type
            switch (attribute.getType().getPrimitiveType()) {
            case FLOAT:
                if (!attr.getVBO().getDataType().isDecimalNumber()) {
                    throw new IllegalArgumentException(
                            "Floating point attributes must use buffers with decimal data");
                }
                break;
            case UNSIGNED_INT:
                if (attr.getVBO().getDataType().isDecimalNumber()) {
                    throw new IllegalArgumentException(
                            "Unsigned integer attributes cannot use buffers with decimal data");
                }
                if (attr.getVBO().getDataType().isSigned()) {
                    throw new IllegalArgumentException(
                            "Unsigned integer attributes cannot use buffers with signed data");
                }
                break;
            default: // INT
                if (attr.getVBO().getDataType().isDecimalNumber()) {
                    throw new IllegalArgumentException(
                            "Signed integer attributes cannot use buffers with decimal data");
                }
                if (!attr.getVBO().getDataType().isSigned()) {
                    throw new IllegalArgumentException(
                            "Signed integer attributes cannot use buffers with unsigned data");
                }
                break;
            }

            BufferImpl.BufferHandle newVBO = ((BufferImpl) attr.getVBO()).getHandle();
            bindAttributeHandle(a, newVBO, attr.getOffset(), attr.getStride(), attr.getElementSize());
        } else {
            if (a.vbo != null) {
                // set a good default attribute value
                switch (attribute.getType().getPrimitiveType()) {
                case FLOAT:
                    bindAttribute(a, attribute.getType().getRowCount(), 0f, 0f, 0f, 0f);
                    break;
                case UNSIGNED_INT:
                    bindAttribute(a, attribute.getType().getRowCount(), true, 0, 0, 0, 0);
                    break;
                default: // INT
                    bindAttribute(a, attribute.getType().getRowCount(), false, 0, 0, 0, 0);
                    break;
                }
            }
        }
    }

    private void bindAttributeHandle(ShaderOnlyState.AttributeState a, BufferImpl.BufferHandle handle,
                                     int offset, int stride, int elementSize) {
        boolean accessDiffers = (a.offset != offset ||
                                 a.stride != stride ||
                                 a.elementSize != elementSize);
        if (a.vbo != handle || accessDiffers) {
            if (a.vbo == null) {
                glEnableAttribute(a.index, true);
            }
            if (delegate.state.arrayVBO != handle) {
                context.bindArrayVBO(handle);
            }
            a.elementSize = elementSize;
            a.stride = stride;
            a.offset = offset;
            glAttributePointer(a.index, handle, a.offset, a.stride, a.elementSize);
        }
    }

    private void bindAttribute(ShaderOnlyState.AttributeState a, int rowCount, float v1, float v2, float v3,
                               float v4) {
        if (a.vbo != null) {
            // there was a previously bound vertex attribute, so unbind it
            glEnableAttribute(a.index, false);
            a.vbo = null;
        }

        // glVertexAttrib calls are fast, don't waste time checking the state
        a.floatAttrValues[0] = v1;
        a.floatAttrValues[1] = v2;
        a.floatAttrValues[2] = v3;
        a.floatAttrValues[3] = v4;
        a.dataType = DataType.FLOAT;
        glAttributeValue(a.index, rowCount, v1, v2, v3, v4);
    }

    private void bindAttribute(ShaderOnlyState.AttributeState a, int rowCount, boolean unsigned, int v1,
                               int v2, int v3, int v4) {
        if (a.vbo != null) {
            // there was a previously bound vertex attribute, so unbind it
            glEnableAttribute(a.index, false);
            a.vbo = null;
        }

        // glVertexAttrib calls are fast, don't waste time checking the state
        a.intAttrValues[0] = v1;
        a.intAttrValues[1] = v2;
        a.intAttrValues[2] = v3;
        a.intAttrValues[3] = v4;
        a.dataType = (unsigned ? DataType.UNSIGNED_INT : DataType.INT);
        glAttributeValue(a.index, rowCount, unsigned, v1, v2, v3, v4);
    }

    @Override
    public void bindAttribute(Shader.Attribute attr, double val) {
        if (attr == null) {
            throw new NullPointerException("Attribute cannot be null");
        }
        if (attr.getType() != Shader.VariableType.FLOAT) {
            throw new IllegalArgumentException("Attribute must have a type of FLOAT");
        }
        bindAttribute(state.attributes[attr.getIndex()], 1, (float) val, 0f, 0f, 0f);
    }

    @Override
    public void bindAttribute(Shader.Attribute attr, double v1, double v2) {
        if (attr == null) {
            throw new NullPointerException("Attribute cannot be null");
        }
        if (attr.getType() != Shader.VariableType.VEC2) {
            throw new IllegalArgumentException("Attribute must have a type of VEC2");
        }
        bindAttribute(state.attributes[attr.getIndex()], 2, (float) v1, (float) v2, 0f, 0f);
    }

    @Override
    public void bindAttribute(Shader.Attribute attr, @Const Vector3 v) {
        if (attr == null) {
            throw new NullPointerException("Attribute cannot be null");
        }
        if (attr.getType() != Shader.VariableType.VEC3) {
            throw new IllegalArgumentException("Attribute must have a type of VEC3");
        }
        bindAttribute(state.attributes[attr.getIndex()], 3, (float) v.x, (float) v.y, (float) v.z, 0f);
    }

    @Override
    public void bindAttribute(Shader.Attribute attr, @Const Vector4 v) {
        bindAttribute(attr, v.x, v.y, v.z, v.w);
    }

    @Override
    public void bindAttribute(Shader.Attribute attr, double m00, double m01, double m10, double m11) {
        if (attr == null) {
            throw new NullPointerException("Attribute cannot be null");
        }

        if (attr.getType() == Shader.VariableType.VEC4) {
            bindAttribute(state.attributes[attr.getIndex()], 4, (float) m00, (float) m01, (float) m10,
                          (float) m11);
        } else if (attr.getType() == Shader.VariableType.MAT2) {
            bindAttribute(state.attributes[attr.getIndex()], 2, (float) m00, (float) m01, 0f, 0f);
            bindAttribute(state.attributes[attr.getIndex() + 1], 2, (float) m10, (float) m11, 0f, 0f);
        } else {
            throw new IllegalArgumentException("Attribute must have a type of VEC4 or MAT2");
        }
    }

    @Override
    public void bindAttribute(Shader.Attribute attr, @Const Matrix3 v) {
        if (attr == null) {
            throw new NullPointerException("Attribute cannot be null");
        }
        if (attr.getType() != Shader.VariableType.MAT3) {
            throw new IllegalArgumentException("Attribute must have a type of MAT3");
        }

        bindAttribute(state.attributes[attr.getIndex()], 3, (float) v.m00, (float) v.m01, (float) v.m02, 0f);
        bindAttribute(state.attributes[attr.getIndex() + 1], 3, (float) v.m10, (float) v.m11, (float) v.m12,
                      0f);
        bindAttribute(state.attributes[attr.getIndex() + 2], 3, (float) v.m20, (float) v.m21, (float) v.m22,
                      0f);
    }

    @Override
    public void bindAttribute(Shader.Attribute attr, @Const Matrix4 v) {
        if (attr == null) {
            throw new NullPointerException("Attribute cannot be null");
        }
        if (attr.getType() != Shader.VariableType.MAT3) {
            throw new IllegalArgumentException("Attribute must have a type of MAT3");
        }

        bindAttribute(state.attributes[attr.getIndex()], 4, (float) v.m00, (float) v.m01, (float) v.m02,
                      (float) v.m03);
        bindAttribute(state.attributes[attr.getIndex() + 1], 4, (float) v.m10, (float) v.m11, (float) v.m12,
                      (float) v.m13);
        bindAttribute(state.attributes[attr.getIndex() + 2], 4, (float) v.m20, (float) v.m21, (float) v.m22,
                      (float) v.m23);
        bindAttribute(state.attributes[attr.getIndex() + 3], 4, (float) v.m30, (float) v.m31, (float) v.m32,
                      (float) v.m33);
    }

    @Override
    public void bindAttribute(Shader.Attribute var, int val) {
        if (var == null) {
            throw new NullPointerException("Attribute cannot be null");
        }

        if (var.getType() == Shader.VariableType.UINT) {
            bindAttribute(state.attributes[var.getIndex()], 1, true, val, 0, 0, 0);
        } else if (var.getType() == Shader.VariableType.INT) {
            bindAttribute(state.attributes[var.getIndex()], 1, false, val, 0, 0, 0);
        } else {
            throw new IllegalArgumentException("Attribute must have a type of INT or UINT");
        }
    }

    @Override
    public void bindAttribute(Shader.Attribute var, int v1, int v2) {
        if (var == null) {
            throw new NullPointerException("Attribute cannot be null");
        }

        if (var.getType() == Shader.VariableType.UVEC2) {
            bindAttribute(state.attributes[var.getIndex()], 2, true, v1, v2, 0, 0);
        } else if (var.getType() == Shader.VariableType.IVEC2) {
            bindAttribute(state.attributes[var.getIndex()], 2, false, v1, v2, 0, 0);
        } else {
            throw new IllegalArgumentException("Attribute must have a type of IVEC2 or UVEC2");
        }
    }

    @Override
    public void bindAttribute(Shader.Attribute var, int v1, int v2, int v3) {
        if (var == null) {
            throw new NullPointerException("Attribute cannot be null");
        }

        if (var.getType() == Shader.VariableType.UVEC3) {
            bindAttribute(state.attributes[var.getIndex()], 3, true, v1, v2, v3, 0);
        } else if (var.getType() == Shader.VariableType.IVEC3) {
            bindAttribute(state.attributes[var.getIndex()], 3, false, v1, v2, v3, 0);
        } else {
            throw new IllegalArgumentException("Attribute must have a type of IVEC3 or UVEC3");
        }
    }

    @Override
    public void bindAttribute(Shader.Attribute var, int v1, int v2, int v3, int v4) {
        if (var == null) {
            throw new NullPointerException("Attribute cannot be null");
        }

        if (var.getType() == Shader.VariableType.UVEC4) {
            bindAttribute(state.attributes[var.getIndex()], 4, true, v1, v2, v3, v4);
        } else if (var.getType() == Shader.VariableType.IVEC4) {
            bindAttribute(state.attributes[var.getIndex()], 4, false, v1, v2, v3, v4);
        } else {
            throw new IllegalArgumentException("Attribute must have a type of IVEC4 or UVEC4");
        }
    }

    @Override
    public void setUniform(Shader.Uniform var, double val) {
        if (var == null) {
            throw new NullPointerException("Uniform cannot be null");
        }
        if (var.getType() != Shader.VariableType.FLOAT) {
            throw new IllegalArgumentException("Uniform must have a type of FLOAT");
        }

        ShaderImpl.UniformImpl u = (ShaderImpl.UniformImpl) var;
        if (!u.initialized || u.floatValues.get(0) != val) {
            u.initialized = true;
            u.floatValues.put(0, (float) val);
            glUniform(u, u.floatValues);
        }
    }

    @Override
    public void setUniform(Shader.Uniform var, double v1, double v2) {
        if (var == null) {
            throw new NullPointerException("Uniform cannot be null");
        }
        if (var.getType() != Shader.VariableType.VEC2) {
            throw new IllegalArgumentException("Uniform must have a type of VEC2");
        }

        ShaderImpl.UniformImpl u = (ShaderImpl.UniformImpl) var;
        if (!u.initialized || u.floatValues.get(0) != v1 || u.floatValues.get(1) != v2) {
            u.initialized = true;
            u.floatValues.put(0, (float) v1);
            u.floatValues.put(1, (float) v2);
            glUniform(u, u.floatValues);
        }
    }

    @Override
    public void setUniform(Shader.Uniform var, double v1, double v2, double v3, double v4) {
        if (var == null) {
            throw new NullPointerException("Uniform cannot be null");
        }
        if (var.getType() != Shader.VariableType.VEC4 && var.getType() != Shader.VariableType.MAT2) {
            throw new IllegalArgumentException("Uniform must have a type of VEC4 or MAT2");
        }

        ShaderImpl.UniformImpl u = (ShaderImpl.UniformImpl) var;
        if (!u.initialized || u.floatValues.get(0) != v1 || u.floatValues.get(1) != v1 ||
            u.floatValues.get(2) != v2 || u.floatValues.get(3) != v3) {
            u.initialized = true;
            u.floatValues.put(0, (float) v1);
            u.floatValues.put(1, (float) v2);
            u.floatValues.put(2, (float) v3);
            u.floatValues.put(3, (float) v4);
            glUniform(u, u.floatValues);
        }
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const Matrix3 val) {
        if (var == null) {
            throw new NullPointerException("Uniform cannot be null");
        }
        if (var.getType() != Shader.VariableType.MAT3) {
            throw new IllegalArgumentException("Uniform must have a type of MAT3");
        }

        ShaderImpl.UniformImpl u = (ShaderImpl.UniformImpl) var;
        val.get(u.floatValues, 0, false);
        u.initialized = true;
        glUniform(u, u.floatValues);
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const Matrix4 val) {
        if (var == null) {
            throw new NullPointerException("Uniform cannot be null");
        }
        if (var.getType() != Shader.VariableType.MAT4) {
            throw new IllegalArgumentException("Uniform must have a type of MAT4");
        }

        ShaderImpl.UniformImpl u = (ShaderImpl.UniformImpl) var;
        val.get(u.floatValues, 0, false);
        u.initialized = true;
        glUniform(u, u.floatValues);
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const Vector3 v) {
        setUniform(var, v.x, v.y, v.z);
    }

    private void setUniform(Shader.Uniform var, double x, double y, double z) {
        if (var == null) {
            throw new NullPointerException("Uniform cannot be null");
        }
        if (var.getType() != Shader.VariableType.VEC3) {
            throw new IllegalArgumentException("Uniform must have a type of VEC3");
        }

        ShaderImpl.UniformImpl u = (ShaderImpl.UniformImpl) var;
        if (!u.initialized || u.floatValues.get(0) != x || u.floatValues.get(1) != y ||
            u.floatValues.get(2) != z) {
            u.initialized = true;
            u.floatValues.put(0, (float) x);
            u.floatValues.put(1, (float) y);
            u.floatValues.put(2, (float) z);
            glUniform(u, u.floatValues);
        }
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const Vector4 v) {
        setUniform(var, v.x, v.y, v.z, v.w);
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const ColorRGB color) {
        setUniform(var, color, false);
    }

    @Override
    public void setUniform(Shader.Uniform var, @Const ColorRGB color, boolean isHDR) {
        if (isHDR) {
            setUniform(var, color.redHDR(), color.greenHDR(), color.blueHDR());
        } else {
            setUniform(var, color.red(), color.green(), color.blue());
        }
    }

    @Override
    public void setUniform(Shader.Uniform var, int val) {
        if (var == null) {
            throw new NullPointerException("Uniform cannot be null");
        }
        if (var.getType() != Shader.VariableType.INT ||
            var.getType() != Shader.VariableType.UINT ||
            var.getType() != Shader.VariableType.BOOL) {
            throw new IllegalArgumentException("Uniform must have a type of INT, UINT, or BOOL");
        }

        ShaderImpl.UniformImpl u = (ShaderImpl.UniformImpl) var;
        if (!u.initialized || u.intValues.get(0) != val) {
            u.initialized = true;
            u.intValues.put(0, val);
            glUniform(u, u.intValues);
        }
    }

    @Override
    public void setUniform(Shader.Uniform var, int v1, int v2) {
        if (var == null) {
            throw new NullPointerException("Uniform cannot be null");
        }
        if (var.getType() != Shader.VariableType.IVEC2 ||
            var.getType() != Shader.VariableType.UVEC2 ||
            var.getType() != Shader.VariableType.BVEC2) {
            throw new IllegalArgumentException("Uniform must have a type of IVEC2, UVEC2, or BVEC2");
        }

        ShaderImpl.UniformImpl u = (ShaderImpl.UniformImpl) var;
        if (!u.initialized || u.intValues.get(0) != v1 || u.intValues.get(1) != v2) {
            u.initialized = true;
            u.intValues.put(0, v1);
            u.intValues.put(1, v2);
            glUniform(u, u.intValues);
        }
    }

    @Override
    public void setUniform(Shader.Uniform var, int v1, int v2, int v3) {
        if (var == null) {
            throw new NullPointerException("Uniform cannot be null");
        }
        if (var.getType() != Shader.VariableType.IVEC3 ||
            var.getType() != Shader.VariableType.UVEC3 ||
            var.getType() != Shader.VariableType.BVEC3) {
            throw new IllegalArgumentException("Uniform must have a type of IVEC3, UVEC3, or BVEC3");
        }

        ShaderImpl.UniformImpl u = (ShaderImpl.UniformImpl) var;
        if (!u.initialized || u.intValues.get(0) != v1 || u.intValues.get(1) != v2 ||
            u.intValues.get(2) != v3) {
            u.initialized = true;
            u.intValues.put(0, v1);
            u.intValues.put(1, v2);
            u.intValues.put(2, v3);
            glUniform(u, u.intValues);
        }
    }

    @Override
    public void setUniform(Shader.Uniform var, int v1, int v2, int v3, int v4) {
        if (var == null) {
            throw new NullPointerException("Uniform cannot be null");
        }
        if (var.getType() != Shader.VariableType.IVEC4 ||
            var.getType() != Shader.VariableType.UVEC4 ||
            var.getType() != Shader.VariableType.BVEC4) {
            throw new IllegalArgumentException("Uniform must have a type of IVEC4, UVEC4, or BVEC4");
        }

        ShaderImpl.UniformImpl u = (ShaderImpl.UniformImpl) var;
        if (!u.initialized || u.intValues.get(0) != v1 || u.intValues.get(1) != v2 ||
            u.intValues.get(2) != v3 || u.intValues.get(3) != v4) {
            u.initialized = true;
            u.intValues.put(0, v1);
            u.intValues.put(1, v2);
            u.intValues.put(2, v3);
            u.intValues.put(3, v4);
            glUniform(u, u.intValues);
        }
    }

    @Override
    public void setUniform(Shader.Uniform var, boolean val) {
        setUniform(var, val ? 1 : 0);
    }

    @Override
    public void setUniform(Shader.Uniform var, boolean v1, boolean v2) {
        setUniform(var, v1 ? 1 : 0, v2 ? 1 : 0);
    }

    @Override
    public void setUniform(Shader.Uniform var, boolean v1, boolean v2, boolean v3) {
        setUniform(var, v1 ? 1 : 0, v2 ? 1 : 0, v3 ? 1 : 0);
    }

    @Override
    public void setUniform(Shader.Uniform var, boolean v1, boolean v2, boolean v3, boolean v4) {
        setUniform(var, v1 ? 1 : 0, v2 ? 1 : 0, v3 ? 1 : 0, v4 ? 1 : 0);
    }

    private void validateSamplerType(Shader.VariableType type, Sampler texture) {
        switch (type) {
        case SAMPLER_1D:
            if (!(texture instanceof Texture1D)) {
                throw new IllegalArgumentException("SAMPLER_1D can only be used with Texture1D");
            }
            // INT_BIT_FIELD textures all get boiled down to decimal textures
            if (!texture.getDataType().isDecimalNumber() && texture.getDataType() != DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("SAMPLER_1D expects decimal texture formats");
            }
            break;
        case SAMPLER_1D_ARRAY:
            if (!(texture instanceof Texture1DArray)) {
                throw new IllegalArgumentException("SAMPLER_1D_ARRAY can only be used with Texture1DArray");
            }
            if (!texture.getDataType().isDecimalNumber() && texture.getDataType() != DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("SAMPLER_1D_ARRAY expects decimal texture formats");
            }
            break;
        case SAMPLER_1D_SHADOW:
            throw new UnsupportedOperationException("Ferox doesn't expose a way to create 1D depth maps");
        case SAMPLER_2D:
            if (!(texture instanceof Texture2D)) {
                if (texture instanceof DepthMap2D) {
                    if (((DepthMap2D) texture).getDepthComparison() != null) {
                        throw new IllegalArgumentException(
                                "Depth textures cannot have a depth comparison with SAMPLER_2D");
                    }
                } else {
                    throw new IllegalArgumentException(
                            "SAMPLER_2D can only be used with Texture2D or DepthMap2D");
                }
            }
            if (!texture.getDataType().isDecimalNumber() && texture.getDataType() != DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("SAMPLER_2D expects decimal texture formats");
            }
            break;
        case SAMPLER_2D_ARRAY:
            if (!(texture instanceof Texture2DArray)) {
                throw new IllegalArgumentException("SAMPLER_2D_Array can only be used with Texture2DArray");
            }
            if (!texture.getDataType().isDecimalNumber() && texture.getDataType() != DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("SAMPLER_2D_ARRAY expects decimal texture formats");
            }
            break;
        case SAMPLER_2D_SHADOW:
            if (!(texture instanceof DepthMap2D)) {
                throw new IllegalArgumentException("SAMPLER_2D_SHADOW can only be used with DepthMap2D");
            }
            if (((DepthMap2D) texture).getDepthComparison() == null) {
                throw new IllegalArgumentException("SAMPLER_2D_SHADOW requires a depth comparison");
            }
            break;
        case SAMPLER_3D:
            if (!(texture instanceof Texture3D)) {
                throw new IllegalArgumentException("SAMPLER_3D can only be used with Texture3D");
            }
            if (!texture.getDataType().isDecimalNumber() && texture.getDataType() != DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("SAMPLER_3D expects decimal texture formats");
            }
            break;
        case SAMPLER_CUBE:
            if (!(texture instanceof TextureCubeMap)) {
                if (texture instanceof DepthCubeMap) {
                    if (((DepthCubeMap) texture).getDepthComparison() != null) {
                        throw new IllegalArgumentException(
                                "Depth textures cannot have a depth comparison with SAMPLER_CUBE");
                    }
                } else {
                    throw new IllegalArgumentException(
                            "SAMPLER_CUBE can only be used with TextureCubeMap or DepthMapCubeMap");
                }
            }
            if (!texture.getDataType().isDecimalNumber() && texture.getDataType() != DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("SAMPLER_CUBE expects decimal texture formats");
            }
            break;
        case SAMPLER_CUBE_SHADOW:
            if (!(texture instanceof DepthCubeMap)) {
                throw new IllegalArgumentException("SAMPLER_CUBE_SHADOW can only be used with DepthCubeMap");
            }
            if (((DepthCubeMap) texture).getDepthComparison() == null) {
                throw new IllegalArgumentException("SAMPLER_CUBE_SHADOW requires a depth comparison");
            }
            break;
        case ISAMPLER_1D:
            if (!(texture instanceof Texture1D)) {
                throw new IllegalArgumentException("ISAMPLER_1D can only be used with Texture1D");
            }

            // INT_BIT_FIELD texture formats are never signed or unsigned integer textures
            if (texture.getDataType().isDecimalNumber() ||
                !texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("SAMPLER_1D expects signed integer texture formats");
            }
            break;
        case ISAMPLER_1D_ARRAY:
            if (!(texture instanceof Texture1DArray)) {
                throw new IllegalArgumentException("ISAMPLER_1D_ARRAY can only be used with Texture1DArray");
            }
            if (texture.getDataType().isDecimalNumber() ||
                !texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException(
                        "ISAMPLER_1D_ARRAY expects signed integer texture formats");
            }
            break;
        case ISAMPLER_2D:
            if (!(texture instanceof Texture2D)) {
                throw new IllegalArgumentException("ISAMPLER_2D can only be used with Texture2D");
            }
            if (texture.getDataType().isDecimalNumber() ||
                !texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("ISAMPLER_2D expects signed integer texture formats");
            }
            break;
        case ISAMPLER_2D_ARRAY:
            if (!(texture instanceof Texture2DArray)) {
                throw new IllegalArgumentException("ISAMPLER_2D_ARRAY can only be used with Texture2DArray");
            }
            if (texture.getDataType().isDecimalNumber() ||
                !texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException(
                        "ISAMPLER_2D_ARRAY expects signed integer texture formats");
            }
            break;
        case ISAMPLER_3D:
            if (!(texture instanceof Texture3D)) {
                throw new IllegalArgumentException("ISAMPLER_3D can only be used with Texture3D");
            }
            if (texture.getDataType().isDecimalNumber() ||
                !texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("ISAMPLER_3D expects signed integer texture formats");
            }
            break;
        case ISAMPLER_CUBE:
            if (!(texture instanceof TextureCubeMap)) {
                throw new IllegalArgumentException("ISAMPLER_CUBE can only be used with TextureCubeMap");
            }
            if (texture.getDataType().isDecimalNumber() ||
                !texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("ISAMPLER_CUBE expects signed integer texture formats");
            }
            break;
        case USAMPLER_1D:
            if (!(texture instanceof Texture1D)) {
                throw new IllegalArgumentException("USAMPLER_1D can only be used with Texture1D");
            }
            if (texture.getDataType().isDecimalNumber() ||
                texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("USAMPLER_1D expects unsigned integer texture formats");
            }
            break;
        case USAMPLER_1D_ARRAY:
            if (!(texture instanceof Texture1DArray)) {
                throw new IllegalArgumentException("USAMPLER_1D_ARRAY can only be used with Texture1DArray");
            }
            if (texture.getDataType().isDecimalNumber() ||
                texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException(
                        "USAMPLER_1D_ARRAY expects unsigned integer texture formats");
            }
            break;
        case USAMPLER_2D:
            if (!(texture instanceof Texture2D)) {
                throw new IllegalArgumentException("USAMPLER_2D can only be used with Texture2D");
            }
            if (texture.getDataType().isDecimalNumber() ||
                texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("USAMPLER_2D expects unsigned integer texture formats");
            }
            break;
        case USAMPLER_2D_ARRAY:
            if (!(texture instanceof Texture2DArray)) {
                throw new IllegalArgumentException("USAMPLER_2D_ARRAY can only be used with Texture2DArray");
            }
            if (texture.getDataType().isDecimalNumber() ||
                texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException(
                        "USAMPLER_2D_ARRAY expects unsigned integer texture formats");
            }
            break;
        case USAMPLER_3D:
            if (!(texture instanceof Texture3D)) {
                throw new IllegalArgumentException("USAMPLER_3D can only be used with Texture3D");
            }
            if (texture.getDataType().isDecimalNumber() ||
                texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("USAMPLER_3D expects unsigned integer texture formats");
            }
            break;
        case USAMPLER_CUBE:
            if (!(texture instanceof TextureCubeMap)) {
                throw new IllegalArgumentException("USAMPLER_CUBE can only be used with TextureCubeMap");
            }
            if (texture.getDataType().isDecimalNumber() ||
                texture.getDataType().isSigned() ||
                texture.getDataType() == DataType.INT_BIT_FIELD) {
                throw new IllegalArgumentException("USAMPLER_CUBE expects unsigned integer texture formats");
            }
            break;
        }
    }

    @Override
    public void setUniform(Shader.Uniform var, Sampler texture) {
        if (var == null) {
            throw new NullPointerException("Uniform cannot be null");
        }
        if (var.getType().getPrimitiveType() != null) {
            throw new IllegalArgumentException("Uniform type must be a SAMPLER variety");
        }

        ShaderImpl.UniformImpl u = (ShaderImpl.UniformImpl) var;
        int textureUnit = u.intValues.get(0);
        if (texture != null) {
            TextureImpl.TextureHandle handle = ((TextureImpl) texture).getHandle();
            validateSamplerType(var.getType(), texture);

            if (handle != u.texture) {
                context.bindTexture(textureUnit, handle);
                u.texture = handle;
            }
        } else {
            if (u.texture != null) {
                context.bindTexture(textureUnit, null);
                u.texture = null;
            }
        }
    }

    /**
     * Set the given uniform's values. The uniform could have any of the INT_ types, the BOOL type or any of
     * the texture sampler types, and could possibly be an array.
     */
    protected abstract void glUniform(ShaderImpl.UniformImpl u, IntBuffer values);

    /**
     * Set the given uniform's values. The uniform could have any of the FLOAT_ types and could possibly be an
     * array. The buffer will have been rewound already.
     */
    protected abstract void glUniform(ShaderImpl.UniformImpl u, FloatBuffer values);

    /**
     * Enable the given generic vertex attribute to read in data from an attribute pointer as last assigned by
     * glAttributePointer().
     */
    protected abstract void glEnableAttribute(int attr, boolean enable);

    /**
     * Invoke OpenGL commands to set the given attribute pointer. The resource will have already been bound
     * using glBindArrayVbo. If this is for a texture coordinate, glActiveClientTexture will already have been
     * called.
     */
    protected abstract void glAttributePointer(int attr, BufferImpl.BufferHandle handle, int offset,
                                               int stride, int elementSize);

    /**
     * Set the generic vertex attribute at attr to the given vector marked by v1, v2, v3, and v4. Depending on
     * rowCount, certain vector values can be ignored (i.e. if rowCount is 3, v4 is meaningless).
     */
    protected abstract void glAttributeValue(int attr, int rowCount, float v1, float v2, float v3, float v4);

    protected abstract void glAttributeValue(int attr, int rowCount, boolean unsigned, int v1, int v2, int v3,
                                             int v4);

    private static class ShaderState implements ContextState<GlslRenderer> {
        private final ShaderOnlyState shaderState;
        private final SharedState sharedState;

        public ShaderState(SharedState shared, ShaderOnlyState shader) {
            shaderState = shader;
            sharedState = shared;
        }
    }
}
