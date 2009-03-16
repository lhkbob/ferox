package com.ferox.renderer.impl.jogl;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/** Is a shadow context that uses 1x1 pbuffer so that everything
 * is still offscreen, even when executing the resource action. 
 * This is the recommended shadow context to use, assuming that pbuffers
 * are supported.
 * 
 * @author Michael Ludwig
 *
 */
public class PbufferShadowContext extends AbstractShadowContext {
	private GLPbuffer pbuffer;
	private JoglStateRecord record;
	
	/** Assumes that pbuffers are supported on the current hardware. */
	public PbufferShadowContext(RenderCapabilities caps) {
		this.pbuffer = GLDrawableFactory.getFactory().createGLPbuffer(new GLCapabilities(), new DefaultGLCapabilitiesChooser(), 1, 1, null);
		this.pbuffer.addGLEventListener(this);
		
		this.record = new JoglStateRecord(caps);
	}
	
	@Override
	public void destroy() {
		this.pbuffer.destroy();
	}

	@Override
	public GLContext getContext() {
		return this.pbuffer.getContext();
	}

	@Override
	public GLAutoDrawable getGLAutoDrawable() {
		return this.pbuffer;
	}

	@Override
	public JoglStateRecord getStateRecord() {
		return this.record;
	}
}
