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

import com.ferox.renderer.builder.*;
import com.ferox.renderer.impl.resources.AbstractResource;
import com.ferox.renderer.impl.resources.BufferImpl;
import com.ferox.renderer.impl.resources.ShaderImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

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
