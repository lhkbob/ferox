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
