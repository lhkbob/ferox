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

import com.ferox.input.KeyListener;
import com.ferox.input.MouseKeyEventDispatcher;
import com.ferox.input.MouseListener;
import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.impl.AbstractOnscreenSurface;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;

import javax.media.nativewindow.WindowClosingProtocol;
import javax.media.opengl.*;

public class JoglNEWTSurface extends AbstractOnscreenSurface {
    private final JoglNEWTDestructible impl;

    private final int depthBits;
    private final int stencilBits;
    private final int sampleCount;

    private boolean vsync;
    private boolean vsyncNeedsUpdate;

    public JoglNEWTSurface(FrameworkImpl framework, JoglSurfaceFactory factory,
                           final OnscreenSurfaceOptions options, JoglContext shareWith) {
        GLCapabilities caps = chooseCapabilities(options, factory);

        Window window = NewtFactory.createWindow(factory.getScreen(), caps);
        window.setUndecorated(options.isUndecorated());
        window.setSize(options.getWidth(), options.getHeight());
        window.setDefaultCloseOperation(
                WindowClosingProtocol.WindowClosingMode.DO_NOTHING_ON_CLOSE); // we manage this ourselves

        window.setVisible(true);
        window.requestFocus();

        if (options.getFullscreenMode() != null) {
            // make the window fullscreen
            if (!window.getScreen()
                       .setCurrentScreenMode(factory.getScreenMode(options.getFullscreenMode()))) {
                window.destroy();
                throw new SurfaceCreationException("Unable to change screen mode");
            }
            if (!window.setFullscreen(true)) {
                // not successful
                window.destroy();
                throw new SurfaceCreationException("Unable to make window fullscreen");
            }
        }

        GLContext realShare = (shareWith == null ? null : shareWith.getGLContext());
        GLDrawable drawable = GLDrawableFactory.getFactory(factory.getGLProfile()).createGLDrawable(window);
        JoglContext context = new JoglContext(factory.getCapabilities(), drawable.createContext(realShare));
        drawable.setRealized(true);

        // Detect buffer config while the context is current
        context.makeCurrent();
        GL gl = context.getGLContext().getGL();

        int[] query = new int[1];
        gl.glGetIntegerv(GL.GL_STENCIL_BITS, query, 0);
        stencilBits = query[0];

        gl.glGetIntegerv(GL.GL_DEPTH_BITS, query, 0);
        depthBits = query[0];

        gl.glGetIntegerv(GL2GL3.GL_SAMPLES, query, 0);
        int samples = query[0];
        gl.glGetIntegerv(GL2GL3.GL_SAMPLE_BUFFERS, query, 0);
        int sampleBuffers = query[0];

        if (sampleBuffers != 0) {
            sampleCount = samples;
            gl.glEnable(GL2GL3.GL_MULTISAMPLE);
        } else {
            sampleCount = 0;
            gl.glDisable(GL2GL3.GL_MULTISAMPLE);
        }
        context.release();

        vsync = false;
        vsyncNeedsUpdate = true;

        impl = new JoglNEWTDestructible(framework, this, drawable, context, window,
                                        options.getFullscreenMode() != null);
    }

    private static GLCapabilities chooseCapabilities(OnscreenSurfaceOptions request,
                                                     JoglSurfaceFactory factory) {
        int pf;
        if (request.getFullscreenMode() != null) {
            pf = request.getFullscreenMode().getBitDepth();
        } else {
            pf = factory.getDefaultDisplayMode().getBitDepth();
        }

        boolean depthValid = false;
        for (int depth : factory.getCapabilities().getAvailableDepthBufferSizes()) {
            if (depth == request.getDepthBufferBits()) {
                depthValid = true;
                break;
            }
        }
        if (!depthValid) {
            throw new SurfaceCreationException(
                    "Invalid depth buffer bit count: " + request.getDepthBufferBits());
        }

        boolean stencilValid = false;
        for (int stencil : factory.getCapabilities().getAvailableStencilBufferSizes()) {
            if (stencil == request.getStencilBufferBits()) {
                stencilValid = true;
                break;
            }
        }
        if (!stencilValid) {
            throw new SurfaceCreationException(
                    "Invalid stencil buffer bit count: " + request.getStencilBufferBits());
        }

        boolean samplesValid = false;
        for (int sample : factory.getCapabilities().getAvailableSamples()) {
            if (sample == request.getSampleCount()) {
                samplesValid = true;
                break;
            }
        }
        if (!samplesValid) {
            throw new SurfaceCreationException("Invalid sample count: " + request.getSampleCount());
        }

        GLCapabilities caps = new GLCapabilities(factory.getGLProfile());
        caps.setStencilBits(request.getStencilBufferBits());
        caps.setDepthBits(request.getDepthBufferBits());
        if (request.getSampleCount() > 0) {
            caps.setSampleBuffers(true);
            caps.setNumSamples(request.getSampleCount());
        } else {
            caps.setSampleBuffers(false);
        }

        // common bit depth configurations
        if (pf == 32) {
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            caps.setAlphaBits(8);
        } else if (pf == 24) {
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            caps.setAlphaBits(0);
        } else if (pf == 16) {
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setAlphaBits(0);
        }

        return caps;
    }

    @Override
    public void onSurfaceActivate(OpenGLContext context) {
        super.onSurfaceActivate(context);

        synchronized (impl) {
            if (vsyncNeedsUpdate) {
                GL gl = ((JoglContext) context).getGLContext().getGL();
                gl.setSwapInterval(vsync ? 1 : 0);
                vsyncNeedsUpdate = false;
            }
        }
    }

    @Override
    public DisplayMode getDisplayMode() {
        return null;
    }

    @Override
    public int getMultiSamples() {
        return sampleCount;
    }

    @Override
    public int getDepthBufferBits() {
        return depthBits;
    }

    @Override
    public int getStencilBufferBits() {
        return stencilBits;
    }

    @Override
    public boolean isFullscreen() {
        return impl.isFullscreen;
    }

    @Override
    public boolean isVSyncEnabled() {
        synchronized (impl) {
            return vsync;
        }
    }

    @Override
    public void setVSyncEnabled(boolean enable) {
        synchronized (impl) {
            vsync = enable;
            vsyncNeedsUpdate = true;
        }
    }

    @Override
    public String getTitle() {
        synchronized (impl) {
            return impl.window.getTitle();
        }
    }

    @Override
    public void setTitle(final String title) {
        Utils.invokeOnContextThread(getFramework().getContextManager(), new Runnable() {
            @Override
            public void run() {
                synchronized (impl) {
                    impl.window.setTitle(title);
                }
            }
        }, false);
    }

    @Override
    public int getX() {
        synchronized (impl) {
            if (impl.isFullscreen) {
                return 0;
            } else {
                return impl.window.getX();
            }
        }
    }

    @Override
    public int getY() {
        synchronized (impl) {
            if (impl.isFullscreen) {
                return 0;
            } else {
                return impl.window.getY();
            }
        }
    }

    @Override
    public void setWindowSize(final int width, final int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Dimensions must be at least 1");
        }

        if (!impl.isFullscreen) {
            Utils.invokeOnContextThread(getFramework().getContextManager(), new Runnable() {
                @Override
                public void run() {
                    synchronized (impl) {
                        impl.window.setSize(width, height);
                    }
                }
            }, false);
        } else {
            throw new IllegalStateException("Surface is fullscreen");
        }
    }

    @Override
    public void setLocation(final int x, final int y) {
        if (!impl.isFullscreen) {
            Utils.invokeOnContextThread(getFramework().getContextManager(), new Runnable() {
                @Override
                public void run() {
                    synchronized (impl) {
                        impl.window.setPosition(x, y);
                    }
                }
            }, false);
        } else {
            throw new IllegalStateException("Surface is fullscreen");
        }
    }

    @Override
    public boolean isCloseable() {
        synchronized (impl) {
            return impl.closable;
        }
    }

    @Override
    public void setCloseable(boolean userClosable) {
        synchronized (impl) {
            impl.closable = userClosable;
        }
    }

    @Override
    public int getWidth() {
        synchronized (impl) {
            // FIXME what about window insets?
            return impl.drawable.getWidth();
        }
    }

    @Override
    public int getHeight() {
        synchronized (impl) {
            return impl.drawable.getHeight();
        }
    }

    @Override
    public void addMouseListener(MouseListener listener) {
        impl.dispatcher.addMouseListener(listener);
    }

    @Override
    public void removeMouseListener(MouseListener listener) {
        impl.dispatcher.removeMouseListener(listener);
    }

    @Override
    public void addKeyListener(KeyListener listener) {
        impl.dispatcher.addKeyListener(listener);
    }

    @Override
    public void removeKeyListener(KeyListener listener) {
        impl.dispatcher.removeKeyListener(listener);
    }

    @Override
    public SurfaceDestructible getSurfaceDestructible() {
        return impl;
    }

    @Override
    public void flush(OpenGLContext context) {
        impl.drawable.swapBuffers();
    }

    private static class JoglNEWTDestructible extends SurfaceDestructible implements WindowListener {
        private final Window window;
        private final GLDrawable drawable;
        private final JoglContext context;

        private final MouseKeyEventDispatcher dispatcher;
        private final NEWTEventAdapter adapter;

        private final boolean isFullscreen;

        private boolean closable;

        public JoglNEWTDestructible(FrameworkImpl framework, JoglNEWTSurface surface, GLDrawable drawable,
                                    JoglContext context, Window window, boolean fullscreen) {
            super(framework);
            this.window = window;
            this.context = context;
            this.drawable = drawable;
            isFullscreen = fullscreen;

            this.dispatcher = new MouseKeyEventDispatcher();
            adapter = new NEWTEventAdapter(surface, dispatcher);

            closable = true;
        }

        public void initialize() {
            window.addWindowListener(this);
            adapter.attach(window);
        }

        @Override
        public OpenGLContext getContext() {
            return context;
        }

        @Override
        protected void destroyImpl() {
            adapter.detach();
            dispatcher.shutdown();
            window.removeWindowListener(this);

            if (isFullscreen) {
                // restore original screen mode
                window.getScreen().setCurrentScreenMode(window.getScreen().getOriginalScreenMode());
            }

            window.setVisible(false);
            context.destroy();
            window.destroy();
        }

        @Override
        public void windowDestroyNotify(WindowEvent e) {
            synchronized (this) {
                // If the window is not user closable, we perform no action.
                // windowClosing() listeners are responsible for disposing the window
                if (!closable) {
                    return;
                }
            }

            // just call destroy() and let it take care of everything
            destroy();
        }

        /*
         * Window events that we do not care about
         */

        @Override
        public void windowDestroyed(WindowEvent e) {
        }

        @Override
        public void windowResized(WindowEvent e) {
        }

        @Override
        public void windowMoved(WindowEvent e) {
        }

        @Override
        public void windowGainedFocus(WindowEvent e) {
        }

        @Override
        public void windowLostFocus(WindowEvent e) {
        }

        @Override
        public void windowRepaint(WindowUpdateEvent e) {
        }

    }
}
