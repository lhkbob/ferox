package com.ferox.renderer.impl.jogl;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.awt.GLCanvas;

import com.ferox.input.AWTEventAdapter;
import com.ferox.input.KeyListener;
import com.ferox.input.MouseListener;
import com.ferox.renderer.DisplayMode;
import com.ferox.renderer.DisplayMode.PixelFormat;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.OnscreenSurfaceOptions.AntiAliasMode;
import com.ferox.renderer.OnscreenSurfaceOptions.DepthFormat;
import com.ferox.renderer.OnscreenSurfaceOptions.StencilFormat;
import com.ferox.renderer.impl.AbstractFramework;
import com.ferox.renderer.impl.AbstractOnscreenSurface;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.RendererProvider;

/**
 * JOGLAWTSurface is an AWT implementation of OnscreenSurface that uses a
 * GLCanvas as its rendering surface.
 * 
 * @author Michael Ludwig
 */
public class JoglAWTSurface extends AbstractOnscreenSurface implements WindowListener {
    private final Frame frame;
    private final GLCanvas canvas;
    private final JOGLContext context;
    
    private final AWTEventAdapter adapter;
    
    private volatile OnscreenSurfaceOptions options;
    private boolean optionsNeedVerify;
    
    private final Object surfaceLock; // guards editing properties of this surface
    
    private boolean vsync;
    private boolean vsyncNeedsUpdate;
    private boolean closable;

    
    public JoglAWTSurface(AbstractFramework framework, final JoglSurfaceFactory factory, 
                               OnscreenSurfaceOptions options, JOGLContext shareWith,
                               RendererProvider provider) {
        super(framework);
        surfaceLock = new Object();
        
        if (options == null)
            options = new OnscreenSurfaceOptions();
        
        if (options.getFullscreenMode() != null)
            options = options.setFullscreenMode(chooseCompatibleDisplayMode(options.getFullscreenMode(), factory.getAvailableDisplayModes()));
        
        DisplayMode fullscreen = options.getFullscreenMode();
        if (fullscreen != null) {
            options = options.setResizable(false)
                             .setUndecorated(true)
                             .setWidth(fullscreen.getWidth())
                             .setHeight(fullscreen.getHeight())
                             .setX(0)
                             .setY(0);
        }
        this.options = options;
        optionsNeedVerify = true;
        
        canvas = new PaintDisabledGLCanvas(chooseCapabilities(factory, options), 
                                           new DefaultGLCapabilitiesChooser(),
                                           (shareWith == null ? null : shareWith.getGLContext()), null);
        canvas.setAutoSwapBufferMode(false);
        
        frame = new Frame();
        Utils.invokeOnAWTThread(new Runnable() {
            @Override
            public void run() {
                OnscreenSurfaceOptions options = JOGLAWTSurface.this.options;
                frame.setResizable(options.isResizable());
                frame.setUndecorated(options.isUndecorated());
                frame.setBounds(options.getX(), options.getY(), options.getWidth(), options.getHeight());
                
                frame.add(canvas);
                
                frame.setVisible(true);
                canvas.requestFocusInWindow();
                
                frame.setIgnoreRepaint(true);
                canvas.setIgnoreRepaint(true);
                
                if (options.getFullscreenMode() != null) {
                    // attempt fullscreen mode
                    GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
                    device.setFullScreenWindow(frame);
                    
                    if (device.isFullScreenSupported()) {
                        if (device.isDisplayChangeSupported()) {
                            // perform display mode change
                            device.setDisplayMode(factory.getAWTDisplayMode(options.getFullscreenMode()));
                        } else {
                            // must switch back to default display mode
                            JOGLAWTSurface.this.options = options.setFullscreenMode(factory.getDefaultDisplayMode());
                        }
                    } else {
                        // must not claim fullscreen window anymore
                        JOGLAWTSurface.this.options = options.setFullscreenMode(null);
                    }
                }
            }
        }, true);
        
        frame.addWindowListener(this);
        context = new JOGLContext(factory, canvas.getContext(), provider);
        vsync = false;
        vsyncNeedsUpdate = true;
        closable = true;
        
        adapter = new AWTEventAdapter(this);
        adapter.attach(canvas, true);
    }
    
    @Override
    public OpenGLContext getContext() {
        return context;
    }

    @Override
    public void flush(OpenGLContext context) {
        canvas.swapBuffers();
    }

    @Override
    protected void destroyImpl() {
        adapter.detach();
        frame.removeWindowListener(this);
        
        Utils.invokeOnAWTThread(new Runnable() {
            @Override
            public void run() {
                if (getOptions().getFullscreenMode() != null) {
                    // fullscreen window
                    GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().setFullScreenWindow(null);
                }
                
                // This will also call context.destroy() when appropriate so we 
                // have to do it ourselves
                frame.setVisible(false);
                frame.dispose();
            }
        }, false);
    }
    
    @Override
    public void onSurfaceActivate(OpenGLContext context, int activeLayer) {
        super.onSurfaceActivate(context, activeLayer);
        GL2GL3 gl = ((JOGLContext) context).getGLContext().getGL().getGL2GL3();
        
        if (optionsNeedVerify) {
            detectOptions(gl);
            optionsNeedVerify = false;
        }
        
        synchronized(surfaceLock) {
            if (vsyncNeedsUpdate) {
                gl.setSwapInterval(vsync ? 1 : 0);
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
        synchronized(surfaceLock) {
            return vsync;
        }
    }

    @Override
    public void setVSyncEnabled(boolean enable) {
        synchronized(surfaceLock) {
            vsync = enable;
            vsyncNeedsUpdate = true;
        }
    }

    @Override
    public String getTitle() {
        synchronized(surfaceLock) {
            return frame.getTitle();
        }
    }

    @Override
    public void setTitle(final String title) {
        if (title == null)
            throw new NullPointerException("Title cannot be null");
        Utils.invokeOnAWTThread(new Runnable() {
            @Override
            public void run() {
                synchronized(surfaceLock) {
                    frame.setTitle(title);
                }
            }
        }, false);
    }

    @Override
    public void setLocation(final int x, final int y) {
        if (options.getFullscreenMode() != null)
            throw new IllegalStateException("Cannot call setWindowSize() on a fullscreen surface");
        
        Utils.invokeOnAWTThread(new Runnable() {
            @Override
            public void run() {
                synchronized(surfaceLock) {
                    frame.setLocation(x, y);
                }
            }
        }, false);
    }
    
    @Override
    public int getX() {
        synchronized(surfaceLock) {
            return frame.getX();
        }
    }

    @Override
    public int getY() {
        synchronized(surfaceLock) {
            return frame.getY();
        }
    }

    @Override
    public void setWindowSize(final int width, final int height) {
        if (width < 1 || height < 1)
            throw new IllegalArgumentException("Dimensions must be at least 1");
        if (options.getFullscreenMode() != null)
            throw new IllegalStateException("Cannot call setWindowSize() on a fullscreen surface");
        
        Utils.invokeOnAWTThread(new Runnable() {
            @Override
            public void run() {
                synchronized(surfaceLock) {
                    frame.setSize(width, height);
                }
            }
        }, false);
    }
    
    @Override
    public int getWidth() {
        // Use canvas width because frame.getWidth() includes decorations
        synchronized(surfaceLock) {
            return canvas.getWidth();
        }
    }

    @Override
    public int getHeight() {
        // Use canvas height because frame.getHeight() includes decorations
        synchronized(surfaceLock) {
            return canvas.getHeight();
        }
    }

    @Override
    public boolean isClosable() {
        synchronized(surfaceLock) {
            return closable;
        }
    }

    @Override
    public void setClosable(boolean userClosable) {
        synchronized(surfaceLock) {
            closable = userClosable;
        }
    }

    @Override
    public void addMouseListener(MouseListener listener) {
        adapter.addMouseListener(listener);
    }

    @Override
    public void removeMouseListener(MouseListener listener) {
        adapter.removeMouseListener(listener);
    }
    
    @Override
    public void addKeyListener(KeyListener listener) {
        adapter.addKeyListener(listener);
    }

    @Override
    public void removeKeyListener(KeyListener listener) {
        adapter.removeKeyListener(listener);
    }
    
    /*
     * WindowListener implementation, we only care about windowClosing, though
     */

    @Override
    public void windowClosing(WindowEvent e) {
        synchronized(surfaceLock) {
            // If the window is not user closable, we perform no action.
            // windowClosing() listeners are responsible for disposing the window
            if (!closable)
                return;
        }
        
        // just call destroy() and let it take care of everything
        destroy();
    }
    
    @Override
    public void windowOpened(WindowEvent e) { }

    @Override
    public void windowClosed(WindowEvent e) { }

    @Override
    public void windowIconified(WindowEvent e) { }

    @Override
    public void windowDeiconified(WindowEvent e) { }

    @Override
    public void windowActivated(WindowEvent e) { }

    @Override
    public void windowDeactivated(WindowEvent e) { }
    
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
        if (red == 8 && green == 8 && blue == 8 && alpha == 8)
            format = PixelFormat.RGBA_32BIT;
        else if (red == 8 && green == 8 && blue == 8 && alpha == 0)
            format = PixelFormat.RGB_24BIT;
        else if (red == 5 && green == 6 && blue == 5)
            format = PixelFormat.RGB_16BIT;
        
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

        AntiAliasMode aa = AntiAliasMode.NONE;
        if (sampleBuffers != 0) {
            switch (samples) {
            case 8:
                aa = AntiAliasMode.EIGHT_X;
                break;
            case 4:
                aa = AntiAliasMode.FOUR_X;
                break;
            case 2:
                aa = AntiAliasMode.TWO_X;
                break;
            default:
                aa = AntiAliasMode.UNKNOWN;
            }

            gl.glEnable(GL.GL_MULTISAMPLE);
        } else
            gl.glDisable(GL.GL_MULTISAMPLE);
        
        if (options.getFullscreenMode() != null) {
            DisplayMode fullscreen = options.getFullscreenMode();
            options = options.setFullscreenMode(new DisplayMode(fullscreen.getWidth(), fullscreen.getHeight(), format));
        }
        
        options = options.setAntiAliasMode(aa)
                         .setDepthFormat(df)
                         .setStencilFormat(sf);
    }
    
    private static DisplayMode chooseCompatibleDisplayMode(DisplayMode requested, DisplayMode[] available) {
        // we assume there is at least 1 (would be the default)
        DisplayMode best = available[0];
        int reqArea = requested.getWidth() * requested.getHeight();
        int bestArea = best.getWidth() * best.getHeight();
        for (int i = 1; i < available.length; i++) {
            int area = available[i].getWidth() * available[i].getHeight();
            if (Math.abs(area - reqArea) <= Math.abs(bestArea - reqArea)) {
                // available[i] has a better or same match with screen resolution,
                // now evaluate pixel format
                
                if (available[i].getPixelFormat() == requested.getPixelFormat()) {
                    // exact match on format, go with available[i]
                    best = available[i];
                    bestArea = area;
                } else {
                    // go with the highest bit depth pixel format
                    // PixelFormat's declared ordering is by bit depth so we can use compareTo
                    if (available[i].getPixelFormat().compareTo(best.getPixelFormat()) >= 0) {
                        best = available[i];
                        bestArea = area;
                    }
                }
            }
        }
        
        return best;
    }
    
    private static GLCapabilities chooseCapabilities(JoglSurfaceFactory factory, OnscreenSurfaceOptions request) {
        GLCapabilities caps = new GLCapabilities(factory.getGLProfile());
        
        // update the caps fields
        PixelFormat pf;
        if (request.getFullscreenMode() != null) {
            pf = request.getFullscreenMode().getPixelFormat();
        } else {
            pf = factory.getDefaultDisplayMode().getPixelFormat();
        }
        
        switch (pf) {
        case RGB_16BIT:
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setAlphaBits(0);
            break;
        case RGB_24BIT: case UNKNOWN:
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            caps.setAlphaBits(0);
            break;
        case RGBA_32BIT:
            caps.setRedBits(8);
            caps.setGreenBits(8);
            caps.setBlueBits(8);
            caps.setAlphaBits(8);
            break;
        }

        switch (request.getDepthFormat()) {
        case DEPTH_16BIT:
            caps.setDepthBits(16);
            break;
        case DEPTH_24BIT: case UNKNOWN:
            caps.setDepthBits(24);
            break;
        case DEPTH_32BIT:
            caps.setDepthBits(32);
            break;
        case NONE:
            caps.setDepthBits(0);
            break;
        }

        switch (request.getStencilFormat()) {
        case STENCIL_16BIT:
            caps.setStencilBits(16);
            break;
        case STENCIL_8BIT:
            caps.setStencilBits(8);
            break;
        case STENCIL_4BIT:
            caps.setStencilBits(4);
            break;
        case STENCIL_1BIT:
            caps.setStencilBits(1);
            break;
        case NONE: case UNKNOWN:
            caps.setStencilBits(0);
            break;
        }

        switch (request.getAntiAliasMode()) {
        case EIGHT_X:
            caps.setNumSamples(8);
            caps.setSampleBuffers(true);
            break;
        case FOUR_X:
            caps.setNumSamples(4);
            caps.setSampleBuffers(true);
            break;
        case TWO_X:
            caps.setNumSamples(2);
            caps.setSampleBuffers(true);
            break;
        case NONE: case UNKNOWN:
            caps.setNumSamples(0);
            caps.setSampleBuffers(false);
            break;
        }
        
        return caps;
    }
}
