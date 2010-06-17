package com.ferox.renderer.impl.jogl;

import java.awt.DisplayMode;
import java.awt.EventQueue;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import com.ferox.renderer.OnscreenSurface;
import com.ferox.renderer.OnscreenSurfaceOptions;
import com.ferox.renderer.OnscreenSurfaceOptions.AntiAliasMode;
import com.ferox.renderer.OnscreenSurfaceOptions.DepthFormat;
import com.ferox.renderer.OnscreenSurfaceOptions.PixelFormat;
import com.ferox.renderer.OnscreenSurfaceOptions.StencilFormat;
import com.ferox.renderer.impl.AbstractSurface;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.Context;
import com.ferox.renderer.impl.jogl.PaintDisabledGLCanvas;
import com.ferox.renderer.impl.jogl.Utils;

public class JoglOnscreenSurface extends AbstractSurface implements OnscreenSurface, WindowListener {
    private static AtomicBoolean fullscreenActive;
    
    private final JoglContext context;
    private final GLCanvas canvas;
    private final Frame frame;
    
    private final GraphicsDevice graphicsDevice;
    private final DisplayMode[] availableModes;
    private final DisplayMode original;
    private DisplayMode selected;
    
    private boolean fullscreen;
    
    private volatile OnscreenSurfaceOptions options;
    private volatile boolean iconified;
    
    private volatile boolean enableVSync;
    private volatile boolean updateVSync;
    
    public JoglOnscreenSurface(JoglFramework framework, OnscreenSurfaceOptions options) {
        super(framework);
        
        final OnscreenSurfaceOptions finalOptions = (options == null ? new OnscreenSurfaceOptions() : options);
        this.options = finalOptions; // will be updated during init
        
        JoglContext shareWith = (JoglContext) framework.getResourceManager().getContext();
        canvas = new PaintDisabledGLCanvas(chooseCapabilities(framework.getProfile(), options), 
                                           new DefaultGLCapabilitiesChooser(),
                                           (shareWith == null ? null : shareWith.getGLContext()), null);
        canvas.setAutoSwapBufferMode(false);
        
        frame = new Frame();
        Utils.invokeOnAwtThread(new Runnable() {
            public void run() {
                frame.setResizable(finalOptions.isResizable());
                frame.setUndecorated(finalOptions.isUndecorated());
                frame.setBounds(finalOptions.getX(), finalOptions.getY(), 
                                finalOptions.getWidth(), finalOptions.getHeight());

                frame.add(canvas);

                frame.setVisible(true);
                canvas.requestFocusInWindow();
                
                frame.setIgnoreRepaint(true);
                canvas.setIgnoreRepaint(true);
            }
        });
        frame.addWindowListener(this);
        
        context = new JoglContext(framework, canvas.getContext(), lock);
        enableVSync = false;
        updateVSync = true;
        
        // fullscreen support
        graphicsDevice = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        availableModes = pruneDisplayModes(graphicsDevice.getDisplayModes(), finalOptions);
        original = graphicsDevice.getDisplayMode();
        
        if (finalOptions.getFullscreenMode() != null) {
            // find best display mode match
            DisplayMode bestMode = null;
            int bestMatch = Integer.MAX_VALUE;
            for (int i = 0; i < availableModes.length; i++) {
                int diff = Math.abs(availableModes[i].getWidth() - finalOptions.getWidth()) +
                           Math.abs(availableModes[i].getHeight() - finalOptions.getHeight());
                if (diff < bestMatch) {
                    bestMode = availableModes[i];
                    bestMatch = diff;
                }
            }
            
            // active fullscreen mode using best match
            selected = bestMode;
            setFullscreen(true);
        } else
            selected = null;
    }
    
    @Override
    public Context getContext() {
        return context;
    }
    
    @Override
    protected void destroyImpl() {
        frame.removeWindowListener(this);
        setFullscreen(false);
        context.destroy();
        Utils.invokeOnAwtThread(new Runnable() {
           public void run() {
               frame.setVisible(false);
               frame.dispose();
           }
        });
    }

    @Override
    protected void init() {
        options = detectOptions(context.getGL(), options);
    }

    @Override
    protected void postRender(Action next) {
        canvas.swapBuffers();
    }

    @Override
    protected void preRender() {
        if (updateVSync) {
            GL2GL3 gl = context.getGL();
            if (enableVSync)
                gl.setSwapInterval(1);
            else
                gl.setSwapInterval(0);
            updateVSync = false;
        }
    }
    
    @Override
    public void setDisplayMode(com.ferox.renderer.DisplayMode mode) {
        lock.lock();
        try {
            if (mode == null)
                mode = new com.ferox.renderer.DisplayMode(original.getWidth(), original.getHeight());

            for (int i = 0; i < availableModes.length; i++) {
                if (availableModes[i].getWidth() == mode.getWidth() && availableModes[i].getHeight() == mode.getHeight()) {
                    // found a matching AWT display mode
                    selected = availableModes[i];
                    if (fullscreen && graphicsDevice.isDisplayChangeSupported())
                        graphicsDevice.setDisplayMode(selected);
                    return;
                }
            }
        } finally {
            lock.unlock();
        }
        
        throw new UnsupportedOperationException("Unavailable DisplayMode");
    }

    @Override
    public com.ferox.renderer.DisplayMode[] getAvailableDisplayModes() {
        com.ferox.renderer.DisplayMode[] modes = new com.ferox.renderer.DisplayMode[availableModes.length];
        for (int i = 0; i < availableModes.length; i++)
            modes[i] = new com.ferox.renderer.DisplayMode(availableModes[i].getWidth(), availableModes[i].getHeight());
        return modes;
    }

    @Override
    public com.ferox.renderer.DisplayMode getDisplayMode() {
        lock.lock();
        try {
            DisplayMode active = fullscreen ? selected : original;
            return new com.ferox.renderer.DisplayMode(active.getWidth(), active.getHeight());
        } finally {
            lock.unlock();
        }
    }
    
    @Override
    public boolean isFullscreen() {
        lock.lock();
        try {
            return fullscreen;
        } finally {
            lock.unlock();
        }
        
    }

    @Override
    public void setFullscreen(boolean fullscreen) {
        lock.lock();
        try {
            if (this.fullscreen != fullscreen) {
                if (fullscreen)
                    activeFullscreen();
                else
                    deactiveFullscreen();
            }
        } finally {
            lock.unlock();
        }
    }
    
    private void activeFullscreen() {
        if (!fullscreenActive.compareAndSet(false, true))
            throw new IllegalStateException("Another OnscreenSurface is already in fullscreen");
        
        if (selected == null)
            selected = original;
        graphicsDevice.setFullScreenWindow(frame);
        if (graphicsDevice.isDisplayChangeSupported()) {
            graphicsDevice.setDisplayMode(selected);
        } else
            selected = original;
        fullscreen = true;
    }
    
    private void deactiveFullscreen() {
        graphicsDevice.setFullScreenWindow(null);
        fullscreen = false;
        
        fullscreenActive.compareAndSet(true, false); // no fail condition
    }
    
    @Override
    public OnscreenSurfaceOptions getOptions() {
        return options;
    }

    @Override
    public String getTitle() {
        return frame.getTitle();
    }

    @Override
    public Object getWindowImpl() {
        return frame;
    }

    @Override
    public int getX() {
        return frame.getX();
    }

    @Override
    public int getY() {
        return frame.getY();
    }

    @Override
    public boolean isResizable() {
        return frame.isResizable();
    }

    @Override
    public boolean isUndecorated() {
        return frame.isUndecorated();
    }

    @Override
    public boolean isVSyncEnabled() {
        return enableVSync;
    }

    @Override
    public boolean isVisible() {
        return !isDestroyed() && !iconified;
    }

    @Override
    public void setLocation(final int x, final int y) {
        EventQueue.invokeLater(new Runnable() {
           public void run() {
               frame.setLocation(x, y);
           }
        });
    }

    @Override
    public void setTitle(final String title) {
        EventQueue.invokeLater(new Runnable() {
           public void run() {
               frame.setTitle(title == null ? "" : title);
           }
        });
    }

    @Override
    public void setVSyncEnabled(boolean enable) {
        enableVSync = enable;
        updateVSync = true;
    }

    @Override
    public void setWindowSize(final int width, final int height) {
        if (width <= 0 || height <= 0)
            throw new IllegalArgumentException("Invalid window dimensions: " + width + " x " + height);
        
        // FIXME: how does this play into the new fullscreen handling,
        // will it remember the correct size when it's no longer fullscreen?
        // what about correctly reported dimensions while running?
        EventQueue.invokeLater(new Runnable() {
           public void run() {
               frame.setSize(width, height);
           }
        });
    }

    @Override
    public int getHeight() {
        return canvas.getHeight();
    }

    @Override
    public int getWidth() {
        return canvas.getWidth();
    }
    
    /* WindowListener */

    @Override
    public void windowActivated(WindowEvent e) {
    }

    @Override
    public void windowClosed(WindowEvent e) {
    }

    @Override
    public void windowClosing(WindowEvent e) {
        framework.destroy(this);
    }

    @Override
    public void windowDeactivated(WindowEvent e) {
    }

    @Override
    public void windowDeiconified(WindowEvent e) {
        iconified = false;
    }

    @Override
    public void windowIconified(WindowEvent e) {
        iconified = true;
    }

    @Override
    public void windowOpened(WindowEvent e) {
    }
    
    /* Utilities */
    
    private static GLCapabilities chooseCapabilities(GLProfile profile, OnscreenSurfaceOptions request) {
        GLCapabilities caps = new GLCapabilities(profile);
        
        // try to update the caps fields
        switch (request.getPixelFormat()) {
        case RGB_16BIT:
            caps.setRedBits(5);
            caps.setGreenBits(6);
            caps.setBlueBits(5);
            caps.setAlphaBits(0);
            break;
        case RGB_24BIT:
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
        case DEPTH_24BIT:
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
        case NONE:
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
        case NONE:
            caps.setNumSamples(0);
            caps.setSampleBuffers(false);
            break;
        }
        
        return caps;
    }

    private static OnscreenSurfaceOptions detectOptions(GL2GL3 gl, OnscreenSurfaceOptions base) {
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

        PixelFormat format = PixelFormat.RGB_24BIT;
        switch (red + green + blue + alpha) {
        case 32:
            format = PixelFormat.RGBA_32BIT;
            break;
        case 24:
            format = PixelFormat.RGB_24BIT;
            break;
        case 16:
            format = PixelFormat.RGB_16BIT;
            break;
        }

        DepthFormat df = DepthFormat.NONE;
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
        }

        StencilFormat sf = StencilFormat.NONE;
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
            }

            gl.glEnable(GL.GL_MULTISAMPLE);
        } else
            gl.glDisable(GL.GL_MULTISAMPLE);
        
        return base.setPixelFormat(format)
                   .setDepthFormat(df)
                   .setStencilFormat(sf)
                   .setAntiAliasMode(aa);
    }
    
    private static DisplayMode[] pruneDisplayModes(DisplayMode[] available, OnscreenSurfaceOptions options) {
        // note that the ferox DisplayMode hashes and equals based on dimension
        int bitDepth = 0;
        switch(options.getPixelFormat()) {
        case RGB_16BIT: bitDepth = 16; break;
        case RGB_24BIT: bitDepth = 24; break;
        case RGBA_32BIT: bitDepth = 32; break;
        }
        Map<com.ferox.renderer.DisplayMode, DisplayMode> dimensionMap = new HashMap<com.ferox.renderer.DisplayMode, DisplayMode>();
        
        for (int i = 0; i < available.length; i++) {
            com.ferox.renderer.DisplayMode key = new com.ferox.renderer.DisplayMode(available[i].getWidth(),
                                                                                    available[i].getHeight());
            DisplayMode real = dimensionMap.get(key);
            if (real != null) {
                // choose the DisplayMode with the bit depth closest matching requested
                if (Math.abs(bitDepth - real.getBitDepth()) > Math.abs(bitDepth - available[i].getBitDepth()))
                    dimensionMap.put(key, available[i]);
            } else {
                // add in first mode with these dimensions
                dimensionMap.put(key, available[i]);
            }
        }
        
        return dimensionMap.values().toArray(new DisplayMode[dimensionMap.size()]);
    }
}
