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

import com.ferox.renderer.ElementBuffer;
import com.ferox.renderer.Sampler;
import com.ferox.renderer.VertexBuffer;
import com.ferox.renderer.builder.*;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.ResourceFactory;
import com.ferox.renderer.impl.resources.AbstractResource;
import com.ferox.renderer.impl.resources.BufferImpl;
import com.ferox.renderer.impl.resources.ShaderImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

import javax.media.opengl.GL2GL3;

/**
 *
 */
public class JoglResourceFactory implements ResourceFactory {
    @Override
    public VertexBufferBuilder newVertexBufferBuilder(FrameworkImpl framework) {
        return new JoglVertexBufferBuilder(framework);
    }

    @Override
    public ElementBufferBuilder newElementBufferBuilder(FrameworkImpl framework) {
        return new JoglElementBufferBuilder(framework);
    }

    @Override
    public ShaderBuilder newShaderBuilder(FrameworkImpl framework) {
        return new JoglShaderBuilder(framework);
    }

    @Override
    public Texture2DBuilder newTexture2DBuilder(FrameworkImpl framework) {
        return new JoglTexture2DBuilder(framework);
    }

    @Override
    public Texture2DArrayBuilder newTexture2DArrayBuilder(FrameworkImpl framework) {
        return new JoglTexture2DArrayBuilder(framework);
    }

    @Override
    public Texture1DBuilder newTexture1DBuilder(FrameworkImpl framework) {
        return new JoglTexture1DBuilder(framework);
    }

    @Override
    public Texture1DArrayBuilder newTexture1DArrayBuilder(FrameworkImpl framework) {
        return new JoglTexture1DArrayBuilder(framework);
    }

    @Override
    public Texture3DBuilder newTexture3DBuilder(FrameworkImpl framework) {
        return new JoglTexture3DBuilder(framework);
    }

    @Override
    public TextureCubeMapBuilder newTextureCubeMapBuilder(FrameworkImpl framework) {
        return new JoglTextureCubeMapBuilder(framework);
    }

    @Override
    public DepthMap2DBuilder newDepthMap2DBuilder(FrameworkImpl framework) {
        return new JoglDepthMap2DBuilder(framework);
    }

    @Override
    public DepthCubeMapBuilder newDepthCubeMapBuilder(FrameworkImpl framework) {
        return new JoglDepthCubeMapBuilder(framework);
    }

    private static GL2GL3 getGL(OpenGLContext context) {
        return ((JoglContext) context).getGLContext().getGL().getGL2GL3();
    }

    @Override
    public void deleteVBO(OpenGLContext context, BufferImpl.BufferHandle vbo) {
        if (vbo.vboID > 0) {
            getGL(context).glDeleteBuffers(1, new int[] { vbo.vboID }, 0);
        }
    }

    @Override
    public void deleteShader(OpenGLContext context, ShaderImpl.ShaderHandle shader) {
        getGL(context).glDeleteShader(shader.vertexShaderID);
        getGL(context).glDeleteShader(shader.fragmentShaderID);
        if (shader.geometryShaderID > 0) {
            getGL(context).glDeleteShader(shader.geometryShaderID);
        }
        getGL(context).glDeleteProgram(shader.programID);
    }

    @Override
    public void deleteTexture(OpenGLContext context, TextureImpl.TextureHandle texture) {
        getGL(context).glDeleteTextures(1, new int[] { texture.texID }, 0);
    }

    @Override
    public void refresh(OpenGLContext context, AbstractResource<?> resource) {
        if (resource instanceof ElementBuffer) {
            JoglElementBufferBuilder.refreshElementBuffer(context, (ElementBuffer) resource);
        } else if (resource instanceof VertexBuffer) {
            JoglVertexBufferBuilder.refreshVertexBuffer(context, (VertexBuffer) resource);
        } else if (resource instanceof Sampler) {
            JoglSamplerBuilder.refreshTexture(context, (Sampler) resource);
        }

        // otherwise the resource does not support refreshing its state
    }
}
