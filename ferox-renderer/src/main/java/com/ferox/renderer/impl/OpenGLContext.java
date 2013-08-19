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

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.GlslRenderer;
import com.ferox.renderer.impl.resources.BufferImpl;
import com.ferox.renderer.impl.resources.ShaderImpl;
import com.ferox.renderer.impl.resources.TextureImpl;

/**
 * OpenGLContext is a wrapper around an OpenGL context that has been created by some low-level OpenGL wrapper
 * for Java (such as JOGL or LWJGL). An OpenGL context can be current on a single thread at a time, and a
 * context must be current in order to perform graphics operations. This adapter provides operations to make
 * the context current and to release it. These methods should not be used directly because contexts are
 * carefully organized by the {@link ContextManager}.
 *
 * @author Michael Ludwig
 */
public interface OpenGLContext {
    /**
     * Destroy this context. If the context is not shared with any other un-destroyed context, any graphics
     * resources that would be shared can be cleaned as well. This must be called on the OpenGL thread
     */
    public void destroy();

    /**
     * Make the context current on the calling thread. This must be called in a thread-safe environment, and
     * generally {@link ContextManager#ensureContext()} should be used instead. It is assumed that the context
     * is not current on any other thread, and there is no other context already current on this thread.
     */
    public void makeCurrent();

    /**
     * Release this context from the calling thread. It is assumed that the context is current on this thread.
     * This must be called in a thread-safe environment and should usually be left up to {@link
     * ContextManager} to manage (so use {@link ContextManager#forceRelease()} instead).
     */
    public void release();

    /**
     * @return Get the FFP renderer for this context. If the underlying OpenGL hardware no longer supports the
     *         deprecated FFP pipeline, a shader implementation must be provided to emulate this interface
     */
    public FixedFunctionRenderer getFixedFunctionRenderer();

    /**
     * @return Get the GLSL renderer for this context.
     */
    public GlslRenderer getGlslRenderer();

    /**
     * Bind the array vbo on this context. This will update its shared state. This is preferable to binding
     * and tracking state elsewhere. A non-null vbo with only an inmemory buffer is recorded in the state, but
     * will bind an actual OpenGL vbo of 0. It can be assumed the handle came from a {@link
     * com.ferox.renderer.VertexBuffer}
     *
     * @param vbo The vbo to bind, or null to clear it
     */
    public void bindArrayVBO(BufferImpl.BufferHandle vbo);

    /**
     * Bind the element vbo on this context. This will update its shared state. This is preferable to binding
     * and tracking state elsewhere. A non-null vbo with only an inmemory buffer is recorded in the state, but
     * will bind an actual OpenGL vbo of 0. It can be assumed the handle came from an {@link
     * com.ferox.renderer.ElementBuffer}
     *
     * @param vbo The vbo to bind, or null to clear it
     */
    public void bindElementVBO(BufferImpl.BufferHandle vbo);

    /**
     * Set the active shader on this context. If it is non-null, it is invalid to use the fixed-function
     * renderer and state. A null shader resets to using fixed-function.
     *
     * @param shader The new shader to use
     */
    public void bindShader(ShaderImpl.ShaderHandle shader);

    /**
     * Set the current texture on the given unit. Although OpenGL maintains multiple bindings, one for each
     * target, this should only have one texture of any target bound to a single unit at a time.  The context
     * is also responsible for tracking and switching the 'active texture unit' prior to binding the texture.
     * <p/>
     * Enabling a texture unit is not the responsibility of this method and only applies to fixed function
     * rendering.
     *
     * @param textureUnit The texture unit to bind to
     * @param texture     The texture handle, or null to reset the binding
     */
    public void bindTexture(int textureUnit, TextureImpl.TextureHandle texture);

    /**
     * @return Get the shared state that provides access to the bound resources
     */
    public SharedState getState();

    /**
     * Return an error message reported by OpenGL if it has an entered an erroneous state. If there is no
     * current error, then null should be returned.
     *
     * @return The current error message, or null
     */
    public String checkGLErrors();
}
