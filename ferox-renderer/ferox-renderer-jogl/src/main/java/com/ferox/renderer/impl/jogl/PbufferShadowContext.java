package com.ferox.renderer.impl.jogl;

import javax.media.nativewindow.AbstractGraphicsDevice;
import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

import com.ferox.renderer.impl.RendererProvider;

/**
 * PbufferShadowContext is a special form of JoglContext that is suitable for
 * use as a shadow context for a JoglFramework. It uses pbuffers to maintain an
 * offscreen GLContext.
 * 
 * @author Michael Ludwig
 */
public class PbufferShadowContext extends JoglContext {
    private final GLPbuffer pbuffer;

    private PbufferShadowContext(JoglSurfaceFactory creator, GLPbuffer surface,
                                 RendererProvider provider) {
        super(creator, surface.getContext(), provider);
        pbuffer = surface;
    }

    @Override
    public void destroy() {
        super.destroy();
        pbuffer.destroy();
    }

    /**
     * Create a new PbufferShadowContext that will be returned by
     * {@link JoglSurfaceFactory#createOffscreenContext(com.ferox.renderer.impl.OpenGLContext)}
     * .
     * 
     * @param creator The JoglSurfaceFactory that is creating the shadow context
     * @param shareWith The JoglContext to share object data with
     * @param ffp The FixedFunctionRenderer to use with the context
     * @param glsl The GlslRenderer to use with the context
     * @return An PbufferShadowContext
     * @throws NullPointerException if framework or profile is null
     */
    public static PbufferShadowContext create(JoglSurfaceFactory creator,
                                              JoglContext shareWith,
                                              RendererProvider provider) {
        if (creator == null) {
            throw new NullPointerException("Cannot create a PbufferShadowContext with a null JoglSurfaceFactory");
        }

        GLContext realShare = (shareWith == null ? null : shareWith.getGLContext());
        GLCapabilities glCaps = new GLCapabilities(creator.getGLProfile());
        AbstractGraphicsDevice device = GLProfile.getDefaultDevice();
        GLPbuffer pbuffer = GLDrawableFactory.getFactory(creator.getGLProfile())
                                             .createGLPbuffer(device,
                                                              glCaps,
                                                              new DefaultGLCapabilitiesChooser(),
                                                              1, 1, realShare);
        try {
            return new PbufferShadowContext(creator, pbuffer, provider);
        } catch (RuntimeException re) {
            // extra cleanup if we never finished constructing the shadow context
            pbuffer.destroy();
            throw re;
        }
    }
}
