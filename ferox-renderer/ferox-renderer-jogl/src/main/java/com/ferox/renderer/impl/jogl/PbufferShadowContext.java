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

import com.ferox.renderer.impl.RendererProvider;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.*;

/**
 * PbufferShadowContext is a special form of JoglContext that is suitable for use as a
 * shadow context for a JoglFramework. It uses pbuffers to maintain an offscreen
 * GLContext.
 *
 * @author Michael Ludwig
 */
public class PbufferShadowContext extends JoglContext {
    private final GLPbuffer pbuffer;

    private PbufferShadowContext(JoglSurfaceFactory creator, GLPbuffer surface,
                                 RendererProvider provider) {
        super(creator, surface.getContext(), provider);
        pbuffer = surface;
    }

    @Override
    public void destroy() {
        super.destroy();
        pbuffer.destroy();
    }

    /**
     * Create a new PbufferShadowContext that will be returned by {@link
     * JoglSurfaceFactory#createOffscreenContext(com.ferox.renderer.impl.OpenGLContext)}
     * .
     *
     * @param creator   The JoglSurfaceFactory that is creating the shadow context
     * @param shareWith The JoglContext to share object data with
     * @param ffp       The FixedFunctionRenderer to use with the context
     * @param glsl      The GlslRenderer to use with the context
     *
     * @return An PbufferShadowContext
     *
     * @throws NullPointerException if framework or profile is null
     */
    public static PbufferShadowContext create(JoglSurfaceFactory creator,
                                              JoglContext shareWith,
                                              RendererProvider provider) {
        if (creator == null) {
            throw new NullPointerException(
                    "Cannot create a PbufferShadowContext with a null JoglSurfaceFactory");
        }

        GLContext realShare = (shareWith == null ? null : shareWith.getGLContext());
        GLCapabilities glCaps = new GLCapabilities(creator.getGLProfile());
        AbstractGraphicsDevice device = GLProfile.getDefaultDevice();
        GLPbuffer pbuffer = GLDrawableFactory.getFactory(creator.getGLProfile())
                                             .createGLPbuffer(device, glCaps,
                                                              new DefaultGLCapabilitiesChooser(),
                                                              1, 1, realShare);
        try {
            return new PbufferShadowContext(creator, pbuffer, provider);
        } catch (RuntimeException re) {
            // extra cleanup if we never finished constructing the shadow context
            pbuffer.destroy();
            throw re;
        }
    }
}
