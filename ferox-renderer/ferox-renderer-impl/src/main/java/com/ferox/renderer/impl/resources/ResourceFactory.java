package com.ferox.renderer.impl.resources;

import com.ferox.renderer.builder.*;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

/**
 *
 */
public interface ResourceFactory {
    public VertexBufferBuilder newVertexBufferBuilder(FrameworkImpl framework);

    public ElementBufferBuilder newElementBufferBuilder(FrameworkImpl framework);

    public ShaderBuilder newShaderBuilder(FrameworkImpl framework);

    public Texture2DBuilder newTexture2DBuilder(FrameworkImpl framework);

    public Texture2DArrayBuilder newTexture2DArrayBuilder(FrameworkImpl framework);

    public Texture1DBuilder newTexture1DBuilder(FrameworkImpl framework);

    public Texture1DArrayBuilder newTexture1DArrayBuilder(FrameworkImpl framework);

    public Texture3DBuilder newTexture3DBuilder(FrameworkImpl framework);

    public TextureCubeMapBuilder newTextureCubeMapBuilder(FrameworkImpl framework);

    public DepthMap2DBuilder newDepthMap2DBuilder(FrameworkImpl framework);

    public DepthCubeMapBuilder newDepthCubeMapBuilder(FrameworkImpl framework);

    public void deleteVBO(OpenGLContext context, BufferImpl.BufferHandle vbo);

    public void deleteShader(OpenGLContext context, ShaderImpl.ShaderHandle shader);

    public void deleteTexture(OpenGLContext context, TextureImpl.TextureHandle texture);

    public void refresh(OpenGLContext context, AbstractResource<?> resource);
}
