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

import com.ferox.renderer.Capabilities;
import com.ferox.renderer.FrameworkException;
import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

/**
 * PbufferShadowContext is a special form of LWJGLContext that is suitable for use as a shadow context for a
 * LWJGLFramework. It uses pbuffers to maintain an offscreen GLContext.
 *
 * @author Michael Ludwig
 */
public class PbufferShadowContext extends LwjglContext {
    private PbufferShadowContext(Capabilities caps, Pbuffer surface) {
        super(caps, surface);
    }

    /**
     * Create a new PbufferShadowContext that will be returned by {@link LwjglSurfaceFactory#createOffscreenContext(com.ferox.renderer.impl.OpenGLContext)}
     * .
     *
     * @param creator   The LwjglSurfaceFactory that is creating the shadow context
     * @param shareWith The LWJGLContext to share object data with
     *
     * @return An PbufferShadowContext
     *
     * @throws NullPointerException if framework or profile is null
     */
    public static PbufferShadowContext create(LwjglSurfaceFactory creator, LwjglContext shareWith) {
        if (creator == null) {
            throw new NullPointerException(
                    "Cannot create a PbufferShadowContext with a null LwjglSurfaceFactory");
        }

        Drawable realShare = (shareWith == null ? null : shareWith.getDrawable());
        Pbuffer pbuffer;
        try {
            pbuffer = new Pbuffer(1, 1, new PixelFormat(), realShare);
        } catch (LWJGLException e) {
            throw new FrameworkException("Unable to create Pbuffer", e);
        }

        try {
            return new PbufferShadowContext(creator.getCapabilities(), pbuffer);
        } catch (RuntimeException re) {
            // extra cleanup if we never finished constructing the shadow context
            pbuffer.destroy();
            throw re;
        }
    }
}
