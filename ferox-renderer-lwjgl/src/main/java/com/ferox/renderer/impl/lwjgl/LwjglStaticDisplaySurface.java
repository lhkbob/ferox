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

import com.ferox.input.KeyListener;
import com.ferox.input.MouseKeyEventDispatcher;
import com.ferox.input.MouseListener;
import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.impl.AbstractOnscreenSurface;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import org.lwjgl.LWJGLException;
import org.lwjgl.LWJGLUtil;
import org.lwjgl.opengl.Display;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;

/**
 * LwjglStaticDisplaySurface is a surface that uses LWJGL's static {@link Display} interface for window
 * management. There can be only one surface active at one time because of the static nature of the Display
 * API.
 *
 * @author Michael Ludwig
 */
public class LwjglStaticDisplaySurface extends AbstractOnscreenSurface {
    // implementation note, mutating the static Display must be done on the context thread on Windows, or it will lock up
    private final LwjglStaticDisplayDestructible impl;

    private final DisplayMode displayMode;
    private final boolean emulateVSync;
    private long lastSyncTime;

    private final int depthBits;
    private final int stencilBits;
    private final int sampleCount;

    private boolean vsync;
    private boolean vsyncNeedsUpdate;

    public LwjglStaticDisplaySurface(final FrameworkImpl framework, final LwjglSurfaceFactory factory,
                                     final OnscreenSurfaceOptions options, LwjglContext shareWith) {
        if (Display.isCreated()) {
            throw new SurfaceCreationException("Static LWJGL Display is already in use, cannot create another surface");
        }

        final org.lwjgl.opengl.PixelFormat format = choosePixelFormat(options, factory);
        final Drawable realShare = (shareWith == null ? null : shareWith.getDrawable());

        Canvas glCanvas = null;
        Frame parentFrame = null;

        if (options.getFullscreenMode() != null) {
            // configure this as a fullscreen window without an AWT parent
            boolean validDisplay = false;
            for (DisplayMode valid : framework.getCapabilities().getAvailableDisplayModes()) {
                if (valid.equals(options.getFullscreenMode())) {
                    validDisplay = true;
                    break;
                }
            }

            if (!validDisplay) {
                throw new SurfaceCreationException("Display mode is not available: " +
                                                   options.getFullscreenMode());
            }

            org.lwjgl.opengl.DisplayMode lwjglMode = factory.getLWJGLDisplayMode(options.getFullscreenMode());
            try {
                Display.setDisplayModeAndFullscreen(lwjglMode);
                Display.create(format, realShare, factory.getContextAttribs());
            } catch (LWJGLException e) {
                if (Display.isCreated()) {
                    Display.destroy();
                }
                throw new SurfaceCreationException("Unable to create static display", e);
            }
            displayMode = options.getFullscreenMode();
        } else {
            // not a fullscreen window
            boolean needCanvasParent = options.isResizable() || options.isUndecorated();
            if (needCanvasParent) {
                parentFrame = new Frame();
                glCanvas = new PaintDisabledCanvas();

                parentFrame.setResizable(options.isResizable());
                parentFrame.setUndecorated(options.isUndecorated());
                parentFrame.setBounds(0, 0, options.getWidth(), options.getHeight());
                parentFrame.add(glCanvas);

                parentFrame.setVisible(true);
                glCanvas.requestFocusInWindow(); // We use LWJGL's input system, but just in case

                parentFrame.setIgnoreRepaint(true);
                glCanvas.setIgnoreRepaint(true);
            }

            try {
                if (glCanvas != null) {
                    Display.setParent(glCanvas);
                } else {
                    Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(options.getWidth(),
                                                                            options.getHeight()));
                }

                Display.create(format, realShare, factory.getContextAttribs());
                if (glCanvas == null) {
                    Display.setLocation(0, 0);
                    Display.setTitle("");
                }
            } catch (LWJGLException e) {
                if (Display.isCreated()) {
                    Display.destroy();
                }
                throw new SurfaceCreationException("Unable to create static display", e);
            }
            displayMode = factory.getDefaultDisplayMode();
        }

        // OpenGL3.0 gets rid of STENCIL_BITS and DEPTH_BITS queries
        stencilBits = options.getStencilBufferBits();
        depthBits = options.getDepthBufferBits();

        // Detect buffer config while the context is current
        int samples = GL11.glGetInteger(GL13.GL_SAMPLES);
        int sampleBuffers = GL11.glGetInteger(GL13.GL_SAMPLE_BUFFERS);

        if (sampleBuffers != 0) {
            sampleCount = samples;
            GL11.glEnable(GL13.GL_MULTISAMPLE);
        } else {
            sampleCount = 0;
            GL11.glDisable(GL13.GL_MULTISAMPLE);
        }

        GL11.glClearColor(0f, 0f, 0f, 1f);
        GL11.glClear(GL11.GL_COLOR_BUFFER_BIT);
        Display.update(false);

        try {
            // By default, create() makes the surface current on the calling thread,
            // which messes with our bookkeeping so we release the context here
            Display.releaseContext();
        } catch (LWJGLException e) {
            if (Display.isCreated()) {
                Display.destroy();
            }
            throw new SurfaceCreationException("Unable to create static display", e);
        }

        LwjglContext context = new LwjglContext(framework.getCapabilities(), Display.getDrawable());

        vsync = false;
        vsyncNeedsUpdate = true;
        // TODO test if other platforms support vsync with a parented window
        emulateVSync = LWJGLUtil.getPlatform() == LWJGLUtil.PLATFORM_MACOSX && parentFrame != null;
        lastSyncTime = -1L;

        impl = new LwjglStaticDisplayDestructible(framework, this, context, parentFrame, glCanvas);
    }

    private static org.lwjgl.opengl.PixelFormat choosePixelFormat(OnscreenSurfaceOptions request,
                                                                  LwjglSurfaceFactory factory) {
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
            throw new SurfaceCreationException("Invalid depth buffer bit count: " +
                                               request.getDepthBufferBits());
        }

        boolean stencilValid = false;
        for (int stencil : factory.getCapabilities().getAvailableStencilBufferSizes()) {
            if (stencil == request.getStencilBufferBits()) {
                stencilValid = true;
                break;
            }
        }
        if (!stencilValid) {
            throw new SurfaceCreationException("Invalid stencil buffer bit count: " +
                                               request.getStencilBufferBits());
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

        org.lwjgl.opengl.PixelFormat caps = new org.lwjgl.opengl.PixelFormat();
        return caps.withBitsPerPixel(pf).withDepthBits(request.getDepthBufferBits())
                   .withStencilBits(request.getStencilBufferBits()).withSamples(request.getSampleCount());
    }

    /**
     * Start the monitoring threads needed for event pulling and window management.
     */
    public void initialize() {
        impl.initialize();
    }

    @Override
    public void onSurfaceActivate(OpenGLContext context) {
        super.onSurfaceActivate(context);

        // FBO surfaces never unbind their framebuffer object, so make sure we're on the window this time
        ((LwjglContext) context).bindFbo(0);

        synchronized (impl) {
            if (vsyncNeedsUpdate && !emulateVSync) {
                Display.setSwapInterval(vsync ? 1 : 0);
                vsyncNeedsUpdate = false;
            }
        }
    }

    @Override
    public DisplayMode getDisplayMode() {
        return displayMode;
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
        return impl.parentFrame == null && Display.isFullscreen();
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
            if (impl.parentFrame != null) {
                return impl.parentFrame.getTitle();
            } else {
                return Display.getTitle();
            }
        }
    }

    @Override
    public void setTitle(final String title) {
        if (impl.parentFrame != null) {
            synchronized (impl) {
                impl.parentFrame.setTitle(title);
            }
        } else {
            getFramework().getContextManager().invokeOnContextThread(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    synchronized (impl) {
                        Display.setTitle(title);
                    }
                    return null;
                }
            }, false);
        }
    }

    @Override
    public int getX() {
        synchronized (impl) {
            if (impl.parentFrame != null) {
                return impl.parentFrame.getX();
            } else {
                return Display.getX();
            }
        }
    }

    @Override
    public int getY() {
        synchronized (impl) {
            if (impl.parentFrame != null) {
                return impl.parentFrame.getY();
            } else {
                return Display.getY();
            }
        }
    }

    @Override
    public void setWindowSize(final int width, final int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Dimensions must be at least 1");
        }

        if (impl.parentFrame != null) {
            synchronized (impl) {
                impl.parentFrame.setSize(width, height);
            }
        } else if (!Display.isFullscreen()) {
            getFramework().getContextManager().invokeOnContextThread(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    try {
                        Display.setDisplayMode(new org.lwjgl.opengl.DisplayMode(width, height));
                    } catch (LWJGLException e) {
                        throw new FrameworkException("Unexpected error changing window size", e);
                    }
                    return null;
                }
            }, false);
        } else {
            throw new IllegalStateException("Surface is fullscreen");
        }
    }

    @Override
    public void setLocation(final int x, final int y) {
            if (impl.parentFrame != null) {
                synchronized (impl) {
                    impl.parentFrame.setLocation(x, y);
                }
            } else if (!Display.isFullscreen()) {
                getFramework().getContextManager().invokeOnContextThread(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        Display.setLocation(x, y);
                        return null;
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
            if (impl.parentFrame != null) {
                return impl.glCanvas.getWidth();
            } else {
                // FIXME insets
                return Display.getWidth();
            }
        }
    }

    @Override
    public int getHeight() {
        synchronized (impl) {
            if (impl.parentFrame != null) {
                return impl.glCanvas.getHeight();
            } else {
                // FIXME insets
                return Display.getHeight();
            }
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
        // Just swap the buffers during flush(), we'll process messages
        // in another queued task
        Display.update(false);

        if (emulateVSync) {
            boolean vsync;
            synchronized (impl) {
                vsync = this.vsync;
            }

            if (vsync && lastSyncTime >= 0) {
                int freq = Display.getDisplayMode().getFrequency();
                if (freq <= 0) {
                    freq = 60; // default
                }

                long totalFrameTime = 1000 / freq;
                long renderedTime = System.currentTimeMillis() - lastSyncTime;
                while (totalFrameTime < renderedTime) {
                    totalFrameTime *= 2; // double the interval if we've missed it
                }

                try {
                    Thread.sleep(totalFrameTime - renderedTime);
                } catch (InterruptedException e) {
                    // ignore
                }
            }
        }
        lastSyncTime = System.currentTimeMillis();
    }

    private static class LwjglStaticDisplayDestructible extends SurfaceDestructible
            implements WindowListener {
        // parentFrame and glCanvas are null when Display.setParent() was not used
        private final Frame parentFrame;
        private final Canvas glCanvas;

        private final LwjglContext context;
        private final MouseKeyEventDispatcher dispatcher;
        private final LwjglInputEventAdapter adapter;

        private boolean closable;

        public LwjglStaticDisplayDestructible(FrameworkImpl framework, LwjglStaticDisplaySurface surface,
                                              LwjglContext context, Frame parentFrame, Canvas glCanvas) {
            super(framework);
            this.parentFrame = parentFrame;
            this.glCanvas = glCanvas;
            this.context = context;
            this.dispatcher = new MouseKeyEventDispatcher();
            adapter = new LwjglInputEventAdapter(surface, dispatcher);

            closable = true;
        }

        public void initialize() {
            ThreadGroup managedGroup = framework.getLifeCycleManager().getManagedThreadGroup();
            Thread eventMonitor = new Thread(managedGroup, new LwjglStaticDisplayDestructible.EventMonitor(),
                                             "lwjgl-event-monitor");
            eventMonitor.setDaemon(true);
            framework.getLifeCycleManager().startManagedThread(eventMonitor, false);

            if (parentFrame != null) {
                parentFrame.addWindowListener(this);
            }
        }

        @Override
        public OpenGLContext getContext() {
            return context;
        }

        @Override
        protected void destroyImpl() {
            dispatcher.shutdown();
            context.destroy();
            Display.destroy();

            try {
                Display.setParent(null);
            } catch (LWJGLException e) {
                // don't do anything
            }

            if (parentFrame != null) {
                Utils.invokeOnAWTThread(new Runnable() {
                    @Override
                    public void run() {
                        parentFrame.setVisible(false);
                        parentFrame.dispose();
                    }
                }, false);
            }
        }

        @Override
        public void windowClosing(WindowEvent e) {
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

        @Override
        public void windowOpened(WindowEvent e) {
        }

        @Override
        public void windowClosed(WindowEvent e) {
        }

        @Override
        public void windowIconified(WindowEvent e) {
        }

        @Override
        public void windowDeiconified(WindowEvent e) {
        }

        @Override
        public void windowActivated(WindowEvent e) {
        }

        @Override
        public void windowDeactivated(WindowEvent e) {
        }

        /*
         * Internal class that queues tasks to the gpu thread to periodically check
         * OS state to push input events, or close the window.
         */
        private class EventMonitor implements Runnable {
            @Override
            public void run() {
                while (!isDestroyed()) {
                    try {
                        long blockedTime = -System.nanoTime();
                        framework.getContextManager().invokeOnContextThread(new MaintenanceTask(), false)
                                 .get();
                        blockedTime += System.nanoTime();

                        if (blockedTime < 1000000) {
                            // sleep if we haven't been waiting a full millisecond, so as
                            // not to flood the queue
                            Thread.sleep(1);
                        }
                    } catch (InterruptedException | CancellationException e) {
                        // don't care
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        private class MaintenanceTask implements Callable<Void> {
            @Override
            public Void call() throws Exception {
                framework.getContextManager().ensureContext(context);
                adapter.poll();

                boolean closeAllowed;
                synchronized (LwjglStaticDisplayDestructible.this) {
                    closeAllowed = closable;
                }

                if (closeAllowed && Display.isCloseRequested()) {
                    destroy();
                }

                return null;
            }
        }
    }
}
