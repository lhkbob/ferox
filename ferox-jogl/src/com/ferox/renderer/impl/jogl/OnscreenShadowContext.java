package com.ferox.renderer.impl.jogl;

import java.awt.Frame;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

import com.ferox.renderer.impl.jogl.PaintDisabledGLCanvas;
import com.ferox.renderer.impl.jogl.Utils;

/**
 * OnscreenShadowContext is a special JoglContext that is intended for use as
 * the shadow context of a JoglFramework. To ensure that the underlying OpenGL
 * context always exists, this shadow context creates a 1x1 window without
 * decoration that contains a GLCanvas.
 * 
 * @author Michael Ludwig
 */
public class OnscreenShadowContext extends JoglContext {
    private Frame frame;
    
    private OnscreenShadowContext(JoglFramework framework, Frame frame, GLCanvas surface) {
        super(framework, surface.getContext(), null);
        this.frame = frame;
    }
    
    @Override
    public void destroy() {
        if (frame != null) {
            Utils.invokeOnAwtThread(new Runnable() {
                public void run() {
                    frame.setVisible(false);
                    frame.dispose();
                }
            });
            frame = null;
        }
        
        super.destroy();
    }

    /**
     * Create a new OnscreenShadowContext that will be used for the given
     * JoglFramework and will use the given GLProfile. The GLProfile must match
     * the profile that the JoglFramework will eventually report.
     * 
     * @param framework The JoglFramework using the returned
     *            OnscreenShadowContext
     * @param profile The GLProfile of the framework
     * @return An OnscreenShadowContext
     * @throws NullPointerException if framework or profile is null
     */
    public static OnscreenShadowContext create(JoglFramework framework, GLProfile profile) {
        if (framework == null || profile == null)
            throw new NullPointerException("Cannot create an OnscreenShadowContext with a null JoglFramework or GLProfile");
        final GLCanvas canvas = new PaintDisabledGLCanvas(new GLCapabilities(profile));
        final Frame frame = new Frame();
        
        // unfortunately we have to make the Frame visible before we
        // have access to the context
        Utils.invokeOnAwtThread(new Runnable() {
            public void run() {
                frame.setSize(1, 1);
                frame.setResizable(false);
                frame.setUndecorated(true);
                frame.setTitle("");
                frame.add(canvas);
                frame.setVisible(true);
            }
        });
        
        return new OnscreenShadowContext(framework, frame, canvas);
    }
}
