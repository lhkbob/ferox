package com.ferox.renderer.impl.jogl;

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;

import com.ferox.input.KeyListener;
import com.ferox.input.MouseKeyEventDispatcher;
import com.ferox.input.MouseListener;
import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.DisplayMode.PixelFormat;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.OnscreenSurfaceOptions.DepthFormat;
import com.ferox.renderer.OnscreenSurfaceOptions.MultiSampling;
import com.ferox.renderer.OnscreenSurfaceOptions.StencilFormat;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.AbstractOnscreenSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.RendererProvider;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.WindowEvent;
import com.jogamp.newt.event.WindowListener;
import com.jogamp.newt.event.WindowUpdateEvent;

public class JoglNEWTSurface extends AbstractOnscreenSurface implements WindowListener {
    private final Window window;
    private final GLDrawable drawable;
    private final JoglContext context;

    private final NEWTEventAdapter adapter;
    private final MouseKeyEventDispatcher dispatcher;

    private volatile OnscreenSurfaceOptions options;
    private boolean optionsNeedVerify;

    private final Object surfaceLock; // guards editing properties of this surface

    private boolean vsync;
    private boolean vsyncNeedsUpdate;
    private boolean closable;

    public JoglNEWTSurface(AbstractFramework framework, final JoglSurfaceFactory factory,
                           OnscreenSurfaceOptions options, JoglContext shareWith,
                           RendererProvider provider) {
        super(framework);
        surfaceLock = new Object();

        if (options == null) {
            options = new OnscreenSurfaceOptions();
        }

        if (options.getFullscreenMode() != null) {
            options = options.setFullscreenMode(factory.chooseCompatibleDisplayMode(options.getFullscreenMode()));
        }

        DisplayMode fullscreen = options.getFullscreenMode();
        if (fullscreen != null) {
            options = options.setResizable(false).setUndecorated(true)
                             .setWidth(fullscreen.getWidth())
                             .setHeight(fullscreen.getHeight()).setX(0).setY(0);
        } else {
            // the NEWT windowing API does not allow us to make a window
            // a fixed-size
            options = options.setResizable(true);
        }

        GLCapabilities caps = factory.chooseCapabilities(options);
        window = NewtFactory.createWindow(factory.getScreen(), caps);
        window.setUndecorated(options.isUndecorated());
        window.setSize(options.getWidth(), options.getHeight());
        window.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE); // we manage this ourselves

        window.setVisible(true);
        window.requestFocus();

        if (options.getFullscreenMode() != null) {
            // make the window fullscreen
            if (!window.getScreen()
                       .setCurrentScreenMode(factory.getScreenMode(options.getFullscreenMode()))) {
                // not successful
                options = options.setFullscreenMode(factory.getDefaultDisplayMode());
            }
            if (!window.setFullscreen(true)) {
                // not successful
                options = options.setFullscreenMode(null);
            }
        }

        window.addWindowListener(this);

        GLContext realShare = (shareWith == null ? null : shareWith.getGLContext());
        drawable = GLDrawableFactory.getFactory(factory.getGLProfile())
                                    .createGLDrawable(window);
        context = new JoglContext(factory, drawable.createContext(realShare), provider);
        drawable.setRealized(true);

        vsync = false;
        vsyncNeedsUpdate = true;
        closable = true;

        dispatcher = new MouseKeyEventDispatcher(this);
        adapter = new NEWTEventAdapter(dispatcher);
        adapter.attach(window);

        optionsNeedVerify = true;
        this.options = options;
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
            return window.getTitle();
        }
    }

    @Override
    public void setTitle(final String title) {
        Utils.invokeOnContextThread(getFramework().getContextManager(), new Runnable() {
            @Override
            public void run() {
                synchronized (surfaceLock) {
                    window.setTitle(title);
                }
            }
        }, false);
    }

    @Override
    public int getX() {
        synchronized (surfaceLock) {
            if (window.isFullscreen()) {
                return 0;
            } else {
                return window.getX();
            }
        }
    }

    @Override
    public int getY() {
        synchronized (surfaceLock) {
            if (window.isFullscreen()) {
                return 0;
            } else {
                return window.getY();
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

        Utils.invokeOnContextThread(getFramework().getContextManager(), new Runnable() {
            @Override
            public void run() {
                synchronized (surfaceLock) {
                    window.setSize(width, height);
                }
            }
        }, false);
    }

    @Override
    public void setLocation(final int x, final int y) {
        if (options.getFullscreenMode() != null) {
            throw new IllegalStateException("Cannot call setWindowSize() on a fullscreen surface");
        }

        Utils.invokeOnContextThread(getFramework().getContextManager(), new Runnable() {
            @Override
            public void run() {
                synchronized (surfaceLock) {
                    window.setPosition(x, y);
                }
            }
        }, false);
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
            // FIXME On Mac's NEWT creates the window so that the insets are not included
            // in the window's content width, and that the requested size does not include
            // them either
            // - I should verify this behavior on Windows
            int insetWidth = 0;//window.getInsets().getLeftWidth() + window.getInsets().getRightWidth();
            return window.getWidth() - insetWidth;
        }
    }

    @Override
    public int getHeight() {
        synchronized (surfaceLock) {
            int insetHeight = 0;//window.getInsets().getBottomHeight() + window.getInsets().getTopHeight();
            return window.getHeight() - insetHeight;
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
        drawable.swapBuffers();
    }

    @Override
    public void onSurfaceActivate(OpenGLContext context, int activeLayer) {
        super.onSurfaceActivate(context, activeLayer);
        GL2GL3 gl = ((JoglContext) context).getGLContext().getGL().getGL2GL3();

        if (optionsNeedVerify) {
            detectOptions(gl);
            optionsNeedVerify = false;
        }

        synchronized (surfaceLock) {
            if (vsyncNeedsUpdate) {
                gl.setSwapInterval(vsync ? 1 : 0);
                vsyncNeedsUpdate = false;
            }
        }
    }

    @Override
    protected void destroyImpl() {
        adapter.detach();
        dispatcher.shutdown();
        window.removeWindowListener(this);

        if (options.getFullscreenMode() != null) {
            // restore original screen mode
            window.getScreen().setCurrentScreenMode(window.getScreen()
                                                          .getOriginalScreenMode());
        }

        window.setVisible(false);
        context.destroy();
        window.destroy();
    }

    private void detectOptions(GL2GL3 gl) {
        int[] t = new int[1];
        int red, green, blue, alpha, stencil, depth;
        int samples, sampleBuffers;

        gl.glGetIntegerv(GL.GL_RED_BITS, t, 0);
        red = t[0];
        gl.glGetIntegerv(GL.GL_GREEN_BITS, t, 0);
        green = t[0];
        gl.glGetIntegerv(GL.GL_BLUE_BITS, t, 0);
        blue = t[0];
        gl.glGetIntegerv(GL.GL_ALPHA_BITS, t, 0);
        alpha = t[0];

        gl.glGetIntegerv(GL.GL_STENCIL_BITS, t, 0);
        stencil = t[0];
        gl.glGetIntegerv(GL.GL_DEPTH_BITS, t, 0);
        depth = t[0];

        gl.glGetIntegerv(GL.GL_SAMPLES, t, 0);
        samples = t[0];
        gl.glGetIntegerv(GL.GL_SAMPLE_BUFFERS, t, 0);
        sampleBuffers = t[0];

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

            gl.glEnable(GL.GL_MULTISAMPLE);
        } else {
            gl.glDisable(GL.GL_MULTISAMPLE);
        }

        if (options.getFullscreenMode() != null) {
            DisplayMode fullscreen = options.getFullscreenMode();
            options = options.setFullscreenMode(new DisplayMode(fullscreen.getWidth(),
                                                                fullscreen.getHeight(),
                                                                format));
        }

        options = options.setMultiSampling(aa).setDepthFormat(df).setStencilFormat(sf);
    }

    @Override
    public void windowDestroyNotify(WindowEvent e) {
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

    /*
     * Window events that we do not care about
     */

    @Override
    public void windowDestroyed(WindowEvent e) {}

    @Override
    public void windowResized(WindowEvent e) {}

    @Override
    public void windowMoved(WindowEvent e) {}

    @Override
    public void windowGainedFocus(WindowEvent e) {}

    @Override
    public void windowLostFocus(WindowEvent e) {}

    @Override
    public void windowRepaint(WindowUpdateEvent e) {}
}
