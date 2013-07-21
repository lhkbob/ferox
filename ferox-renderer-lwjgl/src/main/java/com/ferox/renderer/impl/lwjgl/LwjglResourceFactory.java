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

import com.ferox.renderer.ElementBuffer;
import com.ferox.renderer.Sampler;
import com.ferox.renderer.VertexBuffer;
import com.ferox.renderer.builder.*;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.resources.*;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL15;
import org.lwjgl.opengl.GL20;

/**
 *
 */
public class LwjglResourceFactory implements ResourceFactory {
    @Override
    public VertexBufferBuilder newVertexBufferBuilder(FrameworkImpl framework) {
        return new LwjglVertexBufferBuilder(framework);
    }

    @Override
    public ElementBufferBuilder newElementBufferBuilder(FrameworkImpl framework) {
        return new LwjglElementBufferBuilder(framework);
    }

    @Override
    public ShaderBuilder newShaderBuilder(FrameworkImpl framework) {
        return new LwjglShaderBuilder(framework);
    }

    @Override
    public Texture2DBuilder newTexture2DBuilder(FrameworkImpl framework) {
        return new LwjglTexture2DBuilder(framework);
    }

    @Override
    public Texture2DArrayBuilder newTexture2DArrayBuilder(FrameworkImpl framework) {
        return new LwjglTexture2DArrayBuilder(framework);
    }

    @Override
    public Texture1DBuilder newTexture1DBuilder(FrameworkImpl framework) {
        return new LwjglTexture1DBuilder(framework);
    }

    @Override
    public Texture1DArrayBuilder newTexture1DArrayBuilder(FrameworkImpl framework) {
        return new LwjglTexture1DArrayBuilder(framework);
    }

    @Override
    public Texture3DBuilder newTexture3DBuilder(FrameworkImpl framework) {
        return new LwjglTexture3DBuilder(framework);
    }

    @Override
    public TextureCubeMapBuilder newTextureCubeMapBuilder(FrameworkImpl framework) {
        return new LwjglTextureCubeMapBuilder(framework);
    }

    @Override
    public DepthMap2DBuilder newDepthMap2DBuilder(FrameworkImpl framework) {
        return new LwjglDepthMap2DBuilder(framework);
    }

    @Override
    public DepthCubeMapBuilder newDepthCubeMapBuilder(FrameworkImpl framework) {
        return new LwjglDepthCubeMapBuilder(framework);
    }

    @Override
    public void deleteVBO(OpenGLContext context, BufferImpl.BufferHandle vbo) {
        if (vbo.vboID > 0) {
            GL15.glDeleteBuffers(vbo.vboID);
        }
    }

    @Override
    public void deleteShader(OpenGLContext context, ShaderImpl.ShaderHandle shader) {
        GL20.glDeleteShader(shader.vertexShaderID);
        GL20.glDeleteShader(shader.fragmentShaderID);
        if (shader.geometryShaderID > 0) {
            GL20.glDeleteShader(shader.geometryShaderID);
        }
        GL20.glDeleteProgram(shader.programID);
    }

    @Override
    public void deleteTexture(OpenGLContext context, TextureImpl.TextureHandle texture) {
        GL11.glDeleteTextures(texture.texID);
    }

    @Override
    public void refresh(OpenGLContext context, AbstractResource<?> resource) {
        if (resource instanceof ElementBuffer) {
            LwjglElementBufferBuilder.refreshElementBuffer(context, (ElementBuffer) resource);
        } else if (resource instanceof VertexBuffer) {
            LwjglVertexBufferBuilder.refreshVertexBuffer(context, (VertexBuffer) resource);
        } else if (resource instanceof Sampler) {
            LwjglSamplerBuilder.refreshTexture(context, (Sampler) resource);
        }

        // otherwise the resource does not support refreshing its state
    }
}
