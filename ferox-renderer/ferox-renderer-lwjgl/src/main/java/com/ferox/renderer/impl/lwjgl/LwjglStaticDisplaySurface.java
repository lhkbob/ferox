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
import com.ferox.renderer.DisplayMode.PixelFormat;
import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.OnscreenSurfaceOptions.DepthFormat;
import com.ferox.renderer.OnscreenSurfaceOptions.MultiSampling;
import com.ferox.renderer.OnscreenSurfaceOptions.StencilFormat;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.impl.AbstractOnscreenSurface;
import com.ferox.renderer.impl.FrameworkImpl;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.RendererProvider;
import org.lwjgl.LWJGLException;
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
public class LwjglStaticDisplaySurface extends AbstractOnscreenSurface implements WindowListener {
    // parentFrame and glCanvas are null when Display.setParent() was not used
    private final Frame parentFrame;
    private final Canvas glCanvas;

    private final LwjglContext context;
    private final MouseKeyEventDispatcher dispatcher;
    private final LwjglInputEventAdapter adapter;

    private volatile OnscreenSurfaceOptions options;
    private boolean optionsNeedVerify;

    private final Object surfaceLock; // guards editing properties of this surface

    private boolean vsync;
    private boolean vsyncNeedsUpdate;
    private boolean closable;

    public LwjglStaticDisplaySurface(FrameworkImpl framework, LwjglSurfaceFactory factory,
                                     OnscreenSurfaceOptions options, LwjglContext shareWith,
                                     RendererProvider provider) {
        super(framework);

        if (Display.isCreated()) {
            throw new SurfaceCreationException(
                    "Static LWJGL Display is already in use, cannot create another surface");
        }

        surfaceLock = new Object();

        if (options == null) {
            options = new OnscreenSurfaceOptions();
        }

        org.lwjgl.opengl.PixelFormat format = factory.choosePixelFormat(options);
        Drawable realShare = (shareWith == null ? null : shareWith.getDrawable());

        boolean fullscreen = false;
        if (options.getFullscreenMode() != null) {
            // try to configure this as a fullscreen window without an AWT parent
            DisplayMode bestMode = factory.chooseCompatibleDisplayMode(options.getFullscreenMode());
            org.lwjgl.opengl.DisplayMode lwjglMode = factory.getLWJGLDisplayMode(bestMode);

            if (lwjglMode != null) {
                try {
                    Display.setDisplayModeAndFullscreen(lwjglMode);
                    Display.create(format, realShare);

                    // By default, create() makes the surface current on the calling thread,
                    // which is incorrect behavior in this case.
                    Display.releaseContext();
                } catch (LWJGLException e) {
                    if (Display.isCreated()) {
                        Display.destroy();
                    }
                    throw new SurfaceCreationException("Unable to create static display", e);
                }

                // update options to reflect successful fullscreen support
                options = options.setResizable(false).setUndecorated(true).setFullscreenMode(bestMode)
                                 .setWidth(bestMode.getWidth()).setHeight(bestMode.getHeight()).setX(0)
                                 .setY(0);

                fullscreen = true;
            }

            // fall through to parent'ed AWT display
        }

        this.options = options;
        optionsNeedVerify = true;

        if (!fullscreen) {
            // not a fullscreen window
            glCanvas = new PaintDisabledCanvas();

            parentFrame = new Frame();
            Utils.invokeOnAWTThread(new Runnable() {
                @Override
                public void run() {
                    OnscreenSurfaceOptions options = LwjglStaticDisplaySurface.this.options;
                    parentFrame.setResizable(options.isResizable());
                    parentFrame.setUndecorated(options.isUndecorated());
                    parentFrame.setBounds(options.getX(), options.getY(), options.getWidth(),
                                          options.getHeight());

                    parentFrame.add(glCanvas);

                    parentFrame.setVisible(true);
                    glCanvas.requestFocusInWindow(); // We use LWJGL's input system, but just in case

                    parentFrame.setIgnoreRepaint(true);
                    glCanvas.setIgnoreRepaint(true);
                }
            }, true);

            try {
                Display.setParent(glCanvas);
                Display.create(format, realShare);

                // By default, create() makes the surface current on the calling thread,
                // which is incorrect behavior in this case.
                Display.releaseContext();
            } catch (LWJGLException e) {
                if (Display.isCreated()) {
                    Display.destroy();
                }
                throw new SurfaceCreationException("Unable to create static display", e);
            }
        } else {
            // have to null out the parent
            glCanvas = null;
            parentFrame = null;
        }

        context = new LwjglContext(factory, Display.getDrawable(), provider);

        vsync = false;
        vsyncNeedsUpdate = true;
        closable = true;

        dispatcher = new MouseKeyEventDispatcher(this);
        adapter = new LwjglInputEventAdapter(dispatcher);
    }

    /**
     * Start the monitoring threads needed for event pulling and window management.
     */
    public void initialize() {
        ThreadGroup managedGroup = getFramework().getLifeCycleManager().getManagedThreadGroup();
        Thread eventMonitor = new Thread(managedGroup, new EventMonitor(), "lwjgl-event-monitor");
        eventMonitor.setDaemon(true);
        getFramework().getLifeCycleManager().startManagedThread(eventMonitor);

        if (parentFrame != null) {
            parentFrame.addWindowListener(this);
        }
    }

    @Override
    public void onSurfaceActivate(OpenGLContext context, int activeLayer) {
        super.onSurfaceActivate(context, activeLayer);

        if (optionsNeedVerify) {
            detectOptions();
            optionsNeedVerify = false;
        }

        synchronized (surfaceLock) {
            if (vsyncNeedsUpdate) {
                Display.setSwapInterval(vsync ? 1 : 0);
                vsyncNeedsUpdate = false;
            }
        }
    }

    @Override
    public OnscreenSurfaceOptions getOptions() {
        return options;
    }

    @Override
    public boolean isVSyncEnabled() {
        synchronized (surfaceLock) {
            return vsync;
        }
    }

    @Override
    public void setVSyncEnabled(boolean enable) {
        synchronized (surfaceLock) {
            vsync = enable;
            vsyncNeedsUpdate = true;
        }
    }

    @Override
    public String getTitle() {
        synchronized (surfaceLock) {
            if (parentFrame != null) {
                return parentFrame.getTitle();
            } else {
                return Display.getTitle();
            }
        }
    }

    @Override
    public void setTitle(final String title) {
        if (parentFrame != null) {
            Utils.invokeOnAWTThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (surfaceLock) {
                        parentFrame.setTitle(title);
                    }
                }
            }, false);
        } else {
            synchronized (surfaceLock) {
                // Display.setTitle() does not require the display to be current.
                Display.setTitle(title);
            }
        }
    }

    @Override
    public int getX() {
        synchronized (surfaceLock) {
            if (parentFrame != null) {
                return parentFrame.getX();
            } else {
                return 0; // fullscreen
            }
        }
    }

    @Override
    public int getY() {
        synchronized (surfaceLock) {
            if (parentFrame != null) {
                return parentFrame.getY();
            } else {
                return 0; // fullscreen
            }
        }
    }

    @Override
    public void setWindowSize(final int width, final int height) {
        if (width < 1 || height < 1) {
            throw new IllegalArgumentException("Dimensions must be at least 1");
        }
        if (options.getFullscreenMode() != null) {
            throw new IllegalStateException("Cannot call setWindowSize() on a fullscreen surface");
        }

        if (parentFrame != null) {
            Utils.invokeOnAWTThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (surfaceLock) {
                        parentFrame.setSize(width, height);
                    }
                }
            }, false);
        } else {
            throw new IllegalStateException("Surface is fullscreen");
        }
    }

    @Override
    public void setLocation(final int x, final int y) {
        if (options.getFullscreenMode() != null) {
            throw new IllegalStateException("Cannot call setWindowSize() on a fullscreen surface");
        }

        if (parentFrame != null) {
            Utils.invokeOnAWTThread(new Runnable() {
                @Override
                public void run() {
                    synchronized (surfaceLock) {
                        parentFrame.setLocation(x, y);
                    }
                }
            }, false);
        } else {
            throw new IllegalStateException("Surface is fullscreen");
        }
    }

    @Override
    public boolean isClosable() {
        synchronized (surfaceLock) {
            return closable;
        }
    }

    @Override
    public void setClosable(boolean userClosable) {
        synchronized (surfaceLock) {
            closable = userClosable;
        }
    }

    @Override
    public int getWidth() {
        synchronized (surfaceLock) {
            if (parentFrame != null) {
                return glCanvas.getWidth();
            } else {
                return Display.getWidth();
            }
        }
    }

    @Override
    public int getHeight() {
        synchronized (surfaceLock) {
            if (parentFrame != null) {
                return glCanvas.getHeight();
            } else {
                return Display.getHeight();
            }
        }
    }

    @Override
    public void addMouseListener(MouseListener listener) {
        dispatcher.addMouseListener(listener);
    }

    @Override
    public void removeMouseListener(MouseListener listener) {
        dispatcher.removeMouseListener(listener);
    }

    @Override
    public void addKeyListener(KeyListener listener) {
        dispatcher.addKeyListener(listener);
    }

    @Override
    public void removeKeyListener(KeyListener listener) {
        dispatcher.removeKeyListener(listener);
    }

    @Override
    public OpenGLContext getContext() {
        return context;
    }

    @Override
    public void flush(OpenGLContext context) {
        try {
            // Just swap the buffers during flush(), we'll process messages
            // in another queued task
            Display.swapBuffers();
        } catch (LWJGLException e) {
            throw new FrameworkException("Error swapping Display's buffers", e);
        }
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
            parentFrame.removeWindowListener(this);

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
        synchronized (surfaceLock) {
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

    private void detectOptions() {
        int red, green, blue, alpha, stencil, depth;
        int samples, sampleBuffers;

        red = GL11.glGetInteger(GL11.GL_RED_BITS);
        green = GL11.glGetInteger(GL11.GL_GREEN_BITS);
        blue = GL11.glGetInteger(GL11.GL_BLUE_BITS);
        alpha = GL11.glGetInteger(GL11.GL_ALPHA_BITS);

        stencil = GL11.glGetInteger(GL11.GL_STENCIL_BITS);
        depth = GL11.glGetInteger(GL11.GL_DEPTH_BITS);

        samples = GL11.glGetInteger(GL13.GL_SAMPLES);
        sampleBuffers = GL11.glGetInteger(GL13.GL_SAMPLE_BUFFERS);

        PixelFormat format = PixelFormat.UNKNOWN;
        if (red == 8 && green == 8 && blue == 8 && alpha == 8) {
            format = PixelFormat.RGBA_32BIT;
        } else if (red == 8 && green == 8 && blue == 8 && alpha == 0) {
            format = PixelFormat.RGB_24BIT;
        } else if (red == 5 && green == 6 && blue == 5) {
            format = PixelFormat.RGB_16BIT;
        }

        DepthFormat df = DepthFormat.UNKNOWN;
        switch (depth) {
        case 16:
            df = DepthFormat.DEPTH_16BIT;
            break;
        case 24:
            df = DepthFormat.DEPTH_24BIT;
            break;
        case 32:
            df = DepthFormat.DEPTH_32BIT;
            break;
        case 0:
            df = DepthFormat.NONE;
            break;
        }

        StencilFormat sf = StencilFormat.UNKNOWN;
        switch (stencil) {
        case 16:
            sf = StencilFormat.STENCIL_16BIT;
            break;
        case 8:
            sf = StencilFormat.STENCIL_8BIT;
            break;
        case 4:
            sf = StencilFormat.STENCIL_4BIT;
            break;
        case 1:
            sf = StencilFormat.STENCIL_1BIT;
            break;
        case 0:
            sf = StencilFormat.NONE;
            break;
        }

        MultiSampling aa = MultiSampling.NONE;
        if (sampleBuffers != 0) {
            switch (samples) {
            case 8:
                aa = MultiSampling.EIGHT_X;
                break;
            case 4:
                aa = MultiSampling.FOUR_X;
                break;
            case 2:
                aa = MultiSampling.TWO_X;
                break;
            default:
                aa = MultiSampling.UNKNOWN;
            }

            GL11.glEnable(GL13.GL_MULTISAMPLE);
        } else {
            GL11.glDisable(GL13.GL_MULTISAMPLE);
        }

        if (options.getFullscreenMode() != null) {
            DisplayMode fullscreen = options.getFullscreenMode();
            options = options.setFullscreenMode(
                    new DisplayMode(fullscreen.getWidth(), fullscreen.getHeight(), format));
        }

        options = options.setMultiSampling(aa).setDepthFormat(df).setStencilFormat(sf);
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
                    getFramework().getContextManager().invokeOnContextThread(new InputTask(), false).get();
                    getFramework().getContextManager().invokeOnContextThread(new CloseRequestTask(), false)
                            .get();
                    blockedTime += System.nanoTime();

                    if (blockedTime < 1000000) {
                        // sleep if we haven't been waiting a full millisecond, so as
                        // not to flood the queue
                        Thread.sleep(1);
                    }
                } catch (InterruptedException e) {
                    // don't care
                } catch (CancellationException e) {
                    // don't care, this happens when we're shutting down
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    private class InputTask implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            adapter.poll();
            return null;
        }
    }

    private class CloseRequestTask implements Callable<Void> {
        @Override
        public Void call() throws Exception {
            boolean closeAllowed;
            synchronized (surfaceLock) {
                closeAllowed = closable;
            }

            if (closeAllowed && Display.isCloseRequested()) {
                destroy();
            }

            return null;
        }
    }
}
