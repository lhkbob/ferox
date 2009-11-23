package com.ferox.renderer.impl.jogl;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

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

	public static PbufferShadowContext create(JoglFramework framework, GLProfile profile) {
		GLCapabilities glCaps = new GLCapabilities(profile);
		GLPbuffer pbuffer = GLDrawableFactory.getFactory(profile).createGLPbuffer(glCaps, 
																				  new DefaultGLCapabilitiesChooser(), 
																				  1, 1, null);
		GLContext context = pbuffer.getContext();
		return new PbufferShadowContext(framework, pbuffer, context);
	}
}
