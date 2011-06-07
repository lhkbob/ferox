package com.ferox.renderer.impl.jogl;

import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.awt.GLCanvas;

import com.ferox.renderer.impl.RendererProvider;

/**
 * OnscreenShadowContext is a special JoglContext that is intended for use as
 * the shadow context of a JoglFramework. To ensure that the underlying OpenGL
 * context always exists, this shadow context creates a 1x1 window without
 * decoration that contains a GLCanvas.
 * 
 * @author Michael Ludwig
 */
public class OnscreenShadowContext extends JoglContext {
    private final Frame frame;
    
    private OnscreenShadowContext(JoglSurfaceFactory creator, Frame frame, GLCanvas surface, RendererProvider provider) {
        super(creator, surface.getContext(), provider);
        this.frame = frame;
    }
    
    @Override
    public void destroy() {
        super.destroy();

        Utils.invokeOnAWTThread(new Runnable() {
            public void run() {
                frame.setVisible(false);
                frame.dispose();
            }
        }, false);
    }

    /**
     * Create a new OnscreenShadowContext that will be returned by
     * {@link JoglSurfaceFactory#createShadowContext(com.ferox.renderer.impl.OpenGLContext)}
     * 
     * @param creator The JoglSurfaceFactory that is creating the shadow context
     * @param shareWith The JoglContext to share object data with
     * @param ffp The FixedFunctionRenderer to use with the context
     * @param glsl The GlslRenderer to use with the context
     * @return An OnscreenShadowContext
     * @throws NullPointerException if framework or profile is null
     */
    public static OnscreenShadowContext create(JoglSurfaceFactory creator, JoglContext shareWith, RendererProvider provider) {
        if (creator == null)
            throw new NullPointerException("Cannot create an OnscreenShadowContext with a null JoglSurfaceFactory");
        
        GLContext realShare = (shareWith == null ? null : shareWith.getGLContext());
        GraphicsDevice device = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
        final GLCanvas canvas = new PaintDisabledGLCanvas(new GLCapabilities(creator.getGLProfile()),
                                                          new DefaultGLCapabilitiesChooser(),
                                                          realShare, device);
        final Frame frame = new Frame();

        // unfortunately we have to make the Frame visible before we
        // have access to the context
        Utils.invokeOnAWTThread(new Runnable() {
            public void run() {
                frame.setSize(1, 1);
                frame.setResizable(false);
                frame.setUndecorated(true);
                frame.setTitle("");
                frame.add(canvas);
                frame.setVisible(true);
            }
        }, true);
        
        try {
            return new OnscreenShadowContext(creator, frame, canvas, provider);
        } catch(RuntimeException re) {
            // last minute cleanup
            canvas.destroy();
            Utils.invokeOnAWTThread(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.dispose();
                }
            }, false);
            
            throw re;
        }
    }
}
