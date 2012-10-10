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

import com.ferox.renderer.Context;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.RenderCapabilities;

/**
 * RendererProvider provides Renderer implementations for {@link OpenGLContext
 * OpenGLContexts}. They do not need to worry about the selection of a single
 * renderer, as is the case with {@link Context}. This logic is handled by the
 * actual Context implementation used by {@link HardwareAccessLayerImpl}.
 * 
 * @author Michael Ludwig
 */
public interface RendererProvider {
    /**
     * Return the FixedFunctionRenderer to use. This does not need to worry
     * about whether or not a GlslRenderer has already been requested. This
     * should always return the same instance per context.
     * 
     * @param caps The current RenderCapabilities
     * @return The FixedFunctionRenderer to use, or null
     * @throws NullPointerException if caps is null
     */
    public FixedFunctionRenderer getFixedFunctionRenderer(RenderCapabilities caps);

    /**
     * Return the GlslRenderer to use. This does not need to worry about whether
     * or not a FixedFunctionRenderer has already been requested. This should
     * always return the same instance per context.
     * 
     * @param caps The current RenderCapabilities
     * @return The GlslRenderer to use, or null
     * @throws NullPointerException if caps is null
     */
    public GlslRenderer getGlslRenderer(RenderCapabilities caps);
}
