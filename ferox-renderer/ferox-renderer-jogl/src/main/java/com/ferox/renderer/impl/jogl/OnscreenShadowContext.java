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

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;

import com.ferox.renderer.impl.RendererProvider;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;

/**
 * OnscreenShadowContext is a special JoglContext that is intended for use as
 * the shadow context of a JoglFramework. To ensure that the underlying OpenGL
 * context always exists, this shadow context creates a 1x1 window without
 * decoration that contains a GLCanvas.
 * 
 * @author Michael Ludwig
 */
public class OnscreenShadowContext extends JoglContext {
    private final Window frame;

    private OnscreenShadowContext(JoglSurfaceFactory creator, Window frame,
                                  GLContext context, RendererProvider provider) {
        super(creator, context, provider);
        this.frame = frame;
    }

    @Override
    public void destroy() {
        // we need to clear the thread's interrupted status because NEWT's
        // EDT thread locking is a little crazy
        Thread.interrupted();

        frame.setVisible(false);
        super.destroy();
        frame.destroy();
    }

    /**
     * Create a new OnscreenShadowContext that will be returned by
     * {@link JoglSurfaceFactory#createOffscreenContext(com.ferox.renderer.impl.OpenGLContext)}
     * 
     * @param creator The JoglSurfaceFactory that is creating the shadow context
     * @param shareWith The JoglContext to share object data with
     * @param ffp The FixedFunctionRenderer to use with the context
     * @param glsl The GlslRenderer to use with the context
     * @return An OnscreenShadowContext
     * @throws NullPointerException if framework or profile is null
     */
    public static OnscreenShadowContext create(JoglSurfaceFactory creator,
                                               JoglContext shareWith,
                                               RendererProvider provider) {
        if (creator == null) {
            throw new NullPointerException("Cannot create an OnscreenShadowContext with a null JoglSurfaceFactory");
        }

        GLCapabilities caps = new GLCapabilities(creator.getGLProfile());
        Window window = NewtFactory.createWindow(creator.getScreen(), caps);
        window.setUndecorated(true);
        window.setSize(1, 1);
        window.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE); // we manage this ourselves

        window.setVisible(true);

        GLDrawable drawable = GLDrawableFactory.getFactory(creator.getGLProfile())
                                               .createGLDrawable(window);
        GLContext context = drawable.createContext(shareWith == null ? null : shareWith.getGLContext());

        return new OnscreenShadowContext(creator, window, context, provider);
    }
}
