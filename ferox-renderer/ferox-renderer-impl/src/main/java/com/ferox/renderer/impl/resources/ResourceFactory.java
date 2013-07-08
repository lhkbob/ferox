package com.ferox.renderer.impl.resources;

import com.ferox.renderer.builder.ElementBufferBuilder;
import com.ferox.renderer.builder.ShaderBuilder;
import com.ferox.renderer.builder.VertexBufferBuilder;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;

/**
 *
 */
public interface ResourceFactory {
    public VertexBufferBuilder newVertexBufferBuilder(FrameworkImpl framework);

    public ElementBufferBuilder newElementBufferBuilder(FrameworkImpl framework);

    public ShaderBuilder newShaderBuilder(FrameworkImpl framework);

    public void deleteVBO(OpenGLContext context, BufferImpl.BufferHandle vbo);

    public void deleteShader(OpenGLContext context, ShaderImpl.ShaderHandle shader);

    public void deleteTexture(OpenGLContext context, TextureImpl.TextureHandle texture);

    public void refresh(OpenGLContext context, AbstractResource<?> resource);
}
