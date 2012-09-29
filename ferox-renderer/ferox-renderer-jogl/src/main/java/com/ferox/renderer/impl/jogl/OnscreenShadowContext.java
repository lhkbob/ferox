package com.ferox.renderer.impl.jogl;

import javax.media.nativewindow.WindowClosingProtocol.WindowClosingMode;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawable;
import javax.media.opengl.GLDrawableFactory;

import com.ferox.renderer.impl.RendererProvider;
import com.jogamp.newt.NewtFactory;
import com.jogamp.newt.Window;

/**
 * OnscreenShadowContext is a special JoglContext that is intended for use as
 * the shadow context of a JoglFramework. To ensure that the underlying OpenGL
 * context always exists, this shadow context creates a 1x1 window without
 * decoration that contains a GLCanvas.
 * 
 * @author Michael Ludwig
 */
public class OnscreenShadowContext extends JoglContext {
    private final Window frame;

    private OnscreenShadowContext(JoglSurfaceFactory creator, Window frame, GLContext context, RendererProvider provider) {
        super(creator, context, provider);
        this.frame = frame;
    }

    @Override
    public void destroy() {
        // we need to clear the thread's interrupted status because NEWT's
        // EDT thread locking is a little crazy
        Thread.interrupted();

        frame.setVisible(false);
        super.destroy();
        frame.destroy();
    }

    /**
     * Create a new OnscreenShadowContext that will be returned by
     * {@link JoglSurfaceFactory#createOffscreenContext(com.ferox.renderer.impl.OpenGLContext)}
     * 
     * @param creator The JoglSurfaceFactory that is creating the shadow context
     * @param shareWith The JoglContext to share object data with
     * @param ffp The FixedFunctionRenderer to use with the context
     * @param glsl The GlslRenderer to use with the context
     * @return An OnscreenShadowContext
     * @throws NullPointerException if framework or profile is null
     */
    public static OnscreenShadowContext create(JoglSurfaceFactory creator, JoglContext shareWith, RendererProvider provider) {
        if (creator == null) {
            throw new NullPointerException("Cannot create an OnscreenShadowContext with a null JoglSurfaceFactory");
        }

        GLCapabilities caps = new GLCapabilities(creator.getGLProfile());
        Window window = NewtFactory.createWindow(creator.getScreen(), caps);
        window.setUndecorated(true);
        window.setSize(1, 1);
        window.setDefaultCloseOperation(WindowClosingMode.DO_NOTHING_ON_CLOSE); // we manage this ourselves

        window.setVisible(true);

        GLDrawable drawable = GLDrawableFactory.getFactory(creator.getGLProfile()).createGLDrawable(window);
        GLContext context = drawable.createContext(shareWith == null ? null : shareWith.getGLContext());

        return new OnscreenShadowContext(creator, window, context, provider);
    }
}
