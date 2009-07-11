package com.ferox.renderer.impl.jogl;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/**
 * Is a shadow context that uses 1x1 pbuffer so that everything is still
 * offscreen, even when executing the resource action. This is the recommended
 * shadow context to use, assuming that pbuffers are supported.
 * 
 * @author Michael Ludwig
 */
public class PbufferShadowContext extends AbstractShadowContext {
	private final GLPbuffer pbuffer;
	private final JoglStateRecord record;

	/** Assumes that pbuffers are supported on the current hardware. */
	public PbufferShadowContext(GLProfile profile, RenderCapabilities caps) {
		GLCapabilities glCaps = new GLCapabilities(profile);
		pbuffer = GLDrawableFactory.getFactory(profile).createGLPbuffer(glCaps, 
																 		new DefaultGLCapabilitiesChooser(), 
																 		1, 1, null);
		pbuffer.addGLEventListener(this);

		record = new JoglStateRecord(caps);
	}

	@Override
	public void destroy() {
		pbuffer.destroy();
	}

	@Override
	public GLContext getContext() {
		return pbuffer.getContext();
	}

	@Override
	public GLAutoDrawable getGLAutoDrawable() {
		return pbuffer;
	}

	@Override
	public JoglStateRecord getStateRecord() {
		return record;
	}
}
