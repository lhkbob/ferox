package com.ferox.renderer.impl.lwjgl;

import java.awt.Graphics;
import java.awt.GraphicsDevice;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.awt.GLCanvas;

/**
 * <p>
 * PaintDisabledGLCanvas is a GLCanvas that listens for when it is added and removed
 * from the windowing system and properly controls its LWJGLContext with respect
 * to those events. Additionally it overrides the 'feature' of calling
 * {@link #display()} every time the canvas's {@link #paint(Graphics)} method is
 * invoked.
 * </p>
 * <p>
 * This is necessary because the LWJGLFramework and ContextManager manage when a
 * context can be current very carefully, and exceptions are thrown when the
 * default GLCanvas attempts to invoke display() from the AWT thread when the
 * JoglRenderManager is using it at the same time.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class PaintDisabledGLCanvas extends GLCanvas {
    private static final long serialVersionUID = 1L;

    public PaintDisabledGLCanvas() {
        super();
    }

    public PaintDisabledGLCanvas(GLCapabilities capabilities) {
        super(capabilities);
    }

    public PaintDisabledGLCanvas(GLCapabilities capabilities, GLCapabilitiesChooser chooser,
                                 GLContext shareWith, GraphicsDevice device) {
        super(capabilities, chooser, shareWith, device);
    }
    
    @Override
    public void paint(Graphics g) {
        // do nothing, DO NOT call super.paint() since that invokes display
    }
    
    @Override
    public Graphics getGraphics() {
        // Must return a bogus graphics object because things are painted once,
        // even if they have ignoreRepaint() set to true, and getGraphics() can
        // block for some reason with JOGL's GLCanvas impl.
        return new NullGraphics();
    }
}
