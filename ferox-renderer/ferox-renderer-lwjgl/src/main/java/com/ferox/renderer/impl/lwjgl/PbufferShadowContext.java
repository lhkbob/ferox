package com.ferox.renderer.impl.lwjgl;

import org.lwjgl.LWJGLException;
import org.lwjgl.opengl.Drawable;
import org.lwjgl.opengl.Pbuffer;
import org.lwjgl.opengl.PixelFormat;

import com.ferox.renderer.FrameworkException;
import com.ferox.renderer.impl.RendererProvider;

/**
 * PbufferShadowContext is a special form of LWJGLContext that is suitable for
 * use as a shadow context for a LWJGLFramework. It uses pbuffers to maintain an
 * offscreen GLContext.
 * 
 * @author Michael Ludwig
 */
public class PbufferShadowContext extends LwjglContext {
    private PbufferShadowContext(LwjglSurfaceFactory creator, Pbuffer surface,
                                 RendererProvider provider) {
        super(creator, surface, provider);
    }

    /**
     * Create a new PbufferShadowContext that will be returned by
     * {@link LwjglSurfaceFactory#createOffscreenContext(com.ferox.renderer.impl.OpenGLContext)}
     * .
     * 
     * @param creator The LwjglSurfaceFactory that is creating the shadow
     *            context
     * @param shareWith The LWJGLContext to share object data with
     * @param ffp The FixedFunctionRenderer to use with the context
     * @param glsl The GlslRenderer to use with the context
     * @return An PbufferShadowContext
     * @throws NullPointerException if framework or profile is null
     */
    public static PbufferShadowContext create(LwjglSurfaceFactory creator,
                                              LwjglContext shareWith,
                                              RendererProvider provider) {
        if (creator == null) {
            throw new NullPointerException("Cannot create a PbufferShadowContext with a null LwjglSurfaceFactory");
        }

        Drawable realShare = (shareWith == null ? null : shareWith.getDrawable());
        Pbuffer pbuffer;
        try {
            pbuffer = new Pbuffer(1, 1, new PixelFormat(), realShare);
        } catch (LWJGLException e) {
            throw new FrameworkException("Unable to create Pbuffer", e);
        }

        try {
            return new PbufferShadowContext(creator, pbuffer, provider);
        } catch (RuntimeException re) {
            // extra cleanup if we never finished constructing the shadow context
            pbuffer.destroy();
            throw re;
        }
    }
}
