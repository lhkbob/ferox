package com.ferox.renderer.impl.jogl;

import java.awt.Graphics;
import java.awt.GraphicsDevice;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLCapabilitiesChooser;
import javax.media.opengl.GLContext;
import javax.media.opengl.awt.GLCanvas;

/**
 * <p>
 * PaintDisabledGLCanvas is a GLCanvas that overrides the 'feature' of calling
 * {@link #display()} every time the canvas's {@link #paint(Graphics)} method is
 * invoked.
 * </p>
 * <p>
 * This is necessary because the JoglRenderManager and JoglResourceManager
 * manage when a context can be current very carefully, and exceptions are
 * thrown when the default GLCanvas attempts to invoke display() from the AWT
 * thread when the JoglRenderManager is using it at the same time.
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
}
