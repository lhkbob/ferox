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
package com.ferox.renderer.impl.lwjgl;

import com.ferox.renderer.Shader;
import com.ferox.renderer.impl.AbstractGlslRenderer;
import com.ferox.renderer.impl.resources.BufferImpl;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class LwjglGlslRenderer extends AbstractGlslRenderer {
    public LwjglGlslRenderer(LwjglContext context, LwjglRendererDelegate delegate, int numVertexAttribs) {
        super(context, delegate, numVertexAttribs);
    }

    @Override
    protected void glAttributeValue(int attr, int rowCount, float v1, float v2, float v3, float v4) {
        switch (rowCount) {
        case 1:
            GL20.glVertexAttrib1f(attr, v1);
            break;
        case 2:
            GL20.glVertexAttrib2f(attr, v1, v2);
            break;
        case 3:
            GL20.glVertexAttrib3f(attr, v1, v2, v3);
            break;
        case 4:
            GL20.glVertexAttrib4f(attr, v1, v2, v3, v4);
            break;
        }
    }

    @Override
    protected void glAttributeValue(int attr, int rowCount, boolean unsigned, int v1, int v2, int v3,
                                    int v4) {
        if (unsigned) {
            switch (rowCount) {
            case 1:
                GL30.glVertexAttribI1ui(attr, v1);
                break;
            case 2:
                GL30.glVertexAttribI2ui(attr, v1, v2);
                break;
            case 3:
                GL30.glVertexAttribI3ui(attr, v1, v2, v3);
                break;
            case 4:
                GL30.glVertexAttribI4ui(attr, v1, v2, v3, v4);
                break;
            }
        } else {
            switch (rowCount) {
            case 1:
                GL30.glVertexAttribI1i(attr, v1);
                break;
            case 2:
                GL30.glVertexAttribI2i(attr, v1, v2);
                break;
            case 3:
                GL30.glVertexAttribI3i(attr, v1, v2, v3);
                break;
            case 4:
                GL30.glVertexAttribI4i(attr, v1, v2, v3, v4);
                break;
            }
        }
    }

    @Override
    protected void glUniform(int index, Shader.VariableType type, FloatBuffer values) {
        switch (type) {
        case FLOAT:
            GL20.glUniform1(index, values);
            break;
        case VEC2:
            GL20.glUniform2(index, values);
            break;
        case VEC3:
            GL20.glUniform3(index, values);
            break;
        case VEC4:
            GL20.glUniform4(index, values);
            break;
        case MAT2:
            GL20.glUniformMatrix2(index, false, values);
            break;
        case MAT3:
            GL20.glUniformMatrix3(index, false, values);
            break;
        case MAT4:
            GL20.glUniformMatrix4(index, false, values);
            break;
        default:
            throw new RuntimeException("Unexpected enum value: " + type);
        }
    }

    @Override
    protected void glUniform(int index, Shader.VariableType type, IntBuffer values) {
        switch (type) {
        case INT:
        case BOOL:
        case SAMPLER_1D:
        case SAMPLER_2D:
        case SAMPLER_3D:
        case SAMPLER_CUBE:
        case SAMPLER_1D_SHADOW:
        case SAMPLER_2D_SHADOW:
        case SAMPLER_CUBE_SHADOW:
        case SAMPLER_1D_ARRAY:
        case SAMPLER_2D_ARRAY:
        case USAMPLER_1D:
        case USAMPLER_2D:
        case USAMPLER_3D:
        case USAMPLER_CUBE:
        case USAMPLER_1D_ARRAY:
        case USAMPLER_2D_ARRAY:
        case ISAMPLER_1D:
        case ISAMPLER_2D:
        case ISAMPLER_3D:
        case ISAMPLER_CUBE:
        case ISAMPLER_1D_ARRAY:
        case ISAMPLER_2D_ARRAY:
            GL20.glUniform1(index, values);
            break;
        case UINT:
            GL30.glUniform1u(index, values);
            break;
        case IVEC2:
        case BVEC2:
            GL20.glUniform2(index, values);
            break;
        case IVEC3:
        case BVEC3:
            GL20.glUniform3(index, values);
            break;
        case IVEC4:
        case BVEC4:
            GL20.glUniform4(index, values);
            break;
        case UVEC2:
            GL30.glUniform2u(index, values);
            break;
        case UVEC3:
            GL30.glUniform3u(index, values);
            break;
        case UVEC4:
            GL30.glUniform4u(index, values);
            break;
        default:
            throw new RuntimeException("Unexpected enum value: " + type);
        }
    }

    @Override
    protected void glEnableAttribute(int attr, boolean enable) {
        if (enable) {
            GL20.glEnableVertexAttribArray(attr);
        } else {
            GL20.glDisableVertexAttribArray(attr);
        }
    }

    @Override
    protected void glAttributePointer(int attr, BufferImpl.BufferHandle handle, int offset, int stride,
                                      int elementSize) {
        int strideBytes = (elementSize + stride) * handle.type.getByteCount();
        int vboOffset = offset * handle.type.getByteCount();

        if (handle.inmemoryBuffer != null) {
            handle.inmemoryBuffer.clear().position(vboOffset);
            if (!handle.type.isDecimalNumber()) {
                GL30.glVertexAttribIPointer(attr, elementSize, Utils.getGLType(handle.type), strideBytes,
                                            handle.inmemoryBuffer);
            } else {
                // always specify the type as normalized, since any non-normalized integer type will never
                // fall into this if block, and the parameter is ignored for already floating-point data
                GL20.glVertexAttribPointer(attr, elementSize, Utils.getGLType(handle.type), true, strideBytes,
                                           handle.inmemoryBuffer);
            }
        } else {
            if (!handle.type.isDecimalNumber()) {
                GL30.glVertexAttribIPointer(attr, elementSize, Utils.getGLType(handle.type), strideBytes,
                                            vboOffset);
            } else {
                GL20.glVertexAttribPointer(attr, elementSize, Utils.getGLType(handle.type), true, strideBytes,
                                           vboOffset);
            }
        }
    }
}
