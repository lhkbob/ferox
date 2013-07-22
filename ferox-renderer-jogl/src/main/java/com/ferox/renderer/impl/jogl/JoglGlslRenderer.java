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
package com.ferox.renderer.impl.jogl;

import com.ferox.renderer.impl.AbstractGlslRenderer;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.resources.BufferImpl;
import com.ferox.renderer.impl.resources.ShaderImpl;

import javax.media.opengl.GL2GL3;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

public class JoglGlslRenderer extends AbstractGlslRenderer {
    private GL2GL3 gl;

    public JoglGlslRenderer(JoglContext context, JoglRendererDelegate delegate, int numVertexAttribs) {
        super(context, delegate, numVertexAttribs);
    }

    @Override
    public void activate(AbstractSurface surface) {
        gl = ((JoglContext) context).getGLContext().getGL().getGL2GL3();
        super.activate(surface);
    }

    @Override
    protected void glAttributeValue(int attr, int rowCount, float v1, float v2, float v3, float v4) {
        switch (rowCount) {
        case 1:
            gl.glVertexAttrib1f(attr, v1);
            break;
        case 2:
            gl.glVertexAttrib2f(attr, v1, v2);
            break;
        case 3:
            gl.glVertexAttrib3f(attr, v1, v2, v3);
            break;
        case 4:
            gl.glVertexAttrib4f(attr, v1, v2, v3, v4);
            break;
        }
    }

    @Override
    protected void glAttributeValue(int attr, int rowCount, boolean unsigned, int v1, int v2, int v3,
                                    int v4) {
        if (unsigned) {
            switch (rowCount) {
            case 1:
                gl.glVertexAttribI1ui(attr, v1);
                break;
            case 2:
                gl.glVertexAttribI2ui(attr, v1, v2);
                break;
            case 3:
                gl.glVertexAttribI3ui(attr, v1, v2, v3);
                break;
            case 4:
                gl.glVertexAttribI4ui(attr, v1, v2, v3, v4);
                break;
            }
        } else {
            switch (rowCount) {
            case 1:
                gl.glVertexAttribI1i(attr, v1);
                break;
            case 2:
                gl.glVertexAttribI2i(attr, v1, v2);
                break;
            case 3:
                gl.glVertexAttribI3i(attr, v1, v2, v3);
                break;
            case 4:
                gl.glVertexAttribI4i(attr, v1, v2, v3, v4);
                break;
            }
        }
    }

    @Override
    protected void glUniform(ShaderImpl.UniformImpl u, FloatBuffer values) {
        switch (u.getType()) {
        case FLOAT:
            gl.glUniform1fv(u.getIndex(), 1, values);
            break;
        case VEC2:
            gl.glUniform2fv(u.getIndex(), 1, values);
            break;
        case VEC3:
            gl.glUniform3fv(u.getIndex(), 1, values);
            break;
        case VEC4:
            gl.glUniform4fv(u.getIndex(), 1, values);
            break;
        case MAT2:
            gl.glUniformMatrix2fv(u.getIndex(), 1, false, values);
            break;
        case MAT3:
            gl.glUniformMatrix3fv(u.getIndex(), 1, false, values);
            break;
        case MAT4:
            gl.glUniformMatrix4fv(u.getIndex(), 1, false, values);
            break;
        default:
            throw new RuntimeException("Unexpected enum value: " + u.getType());
        }
    }

    @Override
    protected void glUniform(ShaderImpl.UniformImpl u, IntBuffer values) {
        switch (u.getType()) {
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
            gl.glUniform1iv(u.getIndex(), 1, values);
            break;
        case UINT:
            gl.glUniform1uiv(u.getIndex(), 1, values);
            break;
        case IVEC2:
        case BVEC2:
            gl.glUniform2iv(u.getIndex(), 1, values);
            break;
        case IVEC3:
        case BVEC3:
            gl.glUniform3iv(u.getIndex(), 1, values);
            break;
        case IVEC4:
        case BVEC4:
            gl.glUniform4iv(u.getIndex(), 1, values);
            break;
        case UVEC2:
            gl.glUniform2uiv(u.getIndex(), 1, values);
            break;
        case UVEC3:
            gl.glUniform3uiv(u.getIndex(), 1, values);
            break;
        case UVEC4:
            gl.glUniform4uiv(u.getIndex(), 1, values);
            break;
        default:
            throw new RuntimeException("Unexpected enum value: " + u.getType());
        }
    }

    @Override
    protected void glEnableAttribute(int attr, boolean enable) {
        if (enable) {
            gl.glEnableVertexAttribArray(attr);
        } else {
            gl.glDisableVertexAttribArray(attr);
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
                gl.glVertexAttribIPointer(attr, elementSize, Utils.getGLType(handle.type), strideBytes,
                                          handle.inmemoryBuffer);
            } else {
                // always specify the type as normalized, since any non-normalized integer type will
                // fall into this if blcok, and the parameter is ignored for already floating-point data
                gl.glVertexAttribPointer(attr, elementSize, Utils.getGLType(handle.type), true, strideBytes,
                                         handle.inmemoryBuffer);
            }
        } else {
            // FIXME there's no glVertexAttribIPointer that takes a VBO offset
            gl.glVertexAttribPointer(attr, elementSize, Utils.getGLType(handle.type),
                                     handle.type.isNormalized(), strideBytes, vboOffset);
        }
    }
}
