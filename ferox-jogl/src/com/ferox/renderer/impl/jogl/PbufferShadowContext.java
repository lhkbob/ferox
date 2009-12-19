package com.ferox.renderer.impl.jogl;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

/**
 * PbufferShadowContext is a special form of JoglContext that is suitable for
 * use as a shadow context for a JoglFramework. It uses pbuffers to maintain an
 * offscreen GLContext.
 * 
 * @author Michael Ludwig
 */
public class PbufferShadowContext extends JoglContext {
	private GLPbuffer pbuffer;
	
	private PbufferShadowContext(JoglFramework framework, GLPbuffer surface, GLContext context) {
		super(framework, surface, context);
		this.pbuffer = surface;
	}
	
	@Override
	public synchronized void destroy() {
		super.destroy();
		// clean up the actual pbuffer, too
		pbuffer.destroy();
	}

	/**
	 * Create a new PbufferShadowContext that will be used for the given
	 * JoglFramework and will use the given GLProfile. The GLProfile must match
	 * the profile that the JoglFramework will eventually report.
	 * 
	 * @param framework The JoglFramework using the returned
	 *            PbufferShadowContext
	 * @param profile The GLProfile of the framework
	 * @return An PbufferShadowContext
	 * @throws NullPointerException if framework or profile is null
	 */
	public static PbufferShadowContext create(JoglFramework framework, GLProfile profile) {
		if (framework == null || profile == null)
			throw new NullPointerException("Cannot create a PbufferShadowContext with a null JoglFramework or null GLProfile");
		
		GLCapabilities glCaps = new GLCapabilities(profile);
		GLPbuffer pbuffer = GLDrawableFactory.getFactory(profile).createGLPbuffer(glCaps, 
																				  new DefaultGLCapabilitiesChooser(), 
																				  1, 1, null);
		GLContext context = pbuffer.getContext();
		return new PbufferShadowContext(framework, pbuffer, context);
	}
}
