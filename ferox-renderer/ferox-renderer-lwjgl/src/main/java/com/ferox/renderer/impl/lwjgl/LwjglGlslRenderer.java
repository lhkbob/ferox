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

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.EnumSet;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL20;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.AbstractGlslRenderer;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.ResourceManager;
import com.ferox.renderer.impl.drivers.GlslShaderHandle;
import com.ferox.renderer.impl.drivers.GlslShaderHandle.Uniform;
import com.ferox.renderer.impl.drivers.TextureHandle;
import com.ferox.renderer.impl.drivers.VertexBufferObjectHandle;
import com.ferox.resource.Texture.Target;
import com.ferox.resource.VertexBufferObject.StorageMode;

public class LwjglGlslRenderer extends AbstractGlslRenderer {
    private boolean initialized;
    private EnumSet<Target> supportedTargets;

    public LwjglGlslRenderer(LwjglRendererDelegate delegate) {
        super(delegate);
    }

    // FIXME: must understand how to implement binding to a render target,
    //   especially across shader versions
    @Override
    public void bindRenderTarget(String fragmentVariable, int target) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void activate(AbstractSurface surface, OpenGLContext context,
                         ResourceManager manager) {
        super.activate(surface, context, manager);

        if (!initialized) {
            // detect caps
            RenderCapabilities caps = surface.getFramework().getCapabilities();
            supportedTargets = caps.getSupportedTextureTargets();

            initialized = true;
        }
    }

    @Override
    protected void glUseProgram(GlslShaderHandle shader) {
        int pid = (shader == null ? 0 : shader.programID);
        ((LwjglContext) context).bindGlslProgram(pid);
    }

    @Override
    protected void glAttributeValue(int attr, int rowCount, float v1, float v2, float v3,
                                    float v4) {
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
    protected void glUniform(Uniform u, FloatBuffer values, int count) {
        switch (u.uniform.getType()) {
        case FLOAT:
            GL20.glUniform1(u.index, values);
            break;
        case FLOAT_VEC2:
            GL20.glUniform2(u.index, values);
            break;
        case FLOAT_VEC3:
            GL20.glUniform3(u.index, values);
            break;
        case FLOAT_VEC4:
            GL20.glUniform4(u.index, values);
            break;
        case FLOAT_MAT2:
            GL20.glUniformMatrix2(u.index, false, values);
            break;
        case FLOAT_MAT3:
            GL20.glUniformMatrix3(u.index, false, values);
            break;
        case FLOAT_MAT4:
            GL20.glUniformMatrix4(u.index, false, values);
            break;
        default:
            throw new RuntimeException("Unsupported enum value: " + u.uniform.getType());
        }
    }

    @Override
    protected void glUniform(Uniform u, IntBuffer values, int count) {
        switch (u.uniform.getType()) {
        case BOOL:
        case INT:
        case SHADOW_MAP:
        case TEXTURE_1D:
        case TEXTURE_2D:
        case TEXTURE_3D:
        case TEXTURE_CUBEMAP:
            GL20.glUniform1(u.index, values);
            break;
        case INT_VEC2:
            GL20.glUniform2(u.index, values);
            break;
        case INT_VEC3:
            GL20.glUniform3(u.index, values);
            break;
        case INT_VEC4:
            GL20.glUniform4(u.index, values);
            break;
        default:
            throw new RuntimeException("Unsupported enum value: " + u.uniform.getType());
        }
    }

    @Override
    protected void glBindTexture(int tex, Target target, TextureHandle handle) {
        if (supportedTargets.contains(target)) {
            LwjglContext ctx = (LwjglContext) context;
            ctx.setActiveTexture(tex);
            ctx.bindTexture(Utils.getGLTextureTarget(target),
                            (handle == null ? 0 : handle.texID));
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
    protected void glBindArrayVbo(VertexBufferObjectHandle h) {
        LwjglContext ctx = (LwjglContext) context;

        if (h != null) {
            if (h.mode != StorageMode.IN_MEMORY) {
                // Must bind the VBO
                ctx.bindArrayVbo(h.vboID);
            } else {
                // Must unbind any old VBO, will grab the in-memory buffer during render call
                ctx.bindArrayVbo(0);
            }
        } else {
            // Must unbind the vbo
            ctx.bindArrayVbo(0);
        }
    }

    @Override
    protected void glAttributePointer(int attr, VertexBufferObjectHandle h, int offset,
                                      int stride, int elementSize) {
        int strideBytes = (elementSize + stride) * h.dataType.getByteCount();

        if (h.mode == StorageMode.IN_MEMORY) {
            h.inmemoryBuffer.clear().position(offset);
            GL20.glVertexAttribPointer(attr, elementSize, false, strideBytes,
                                       (FloatBuffer) h.inmemoryBuffer);
        } else {
            int vboOffset = offset * h.dataType.getByteCount();
            GL20.glVertexAttribPointer(attr, elementSize, GL11.GL_FLOAT, false,
                                       strideBytes, vboOffset);
        }
    }
}
