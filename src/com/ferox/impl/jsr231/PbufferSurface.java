package com.ferox.impl.jsr231;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;

import com.ferox.core.system.DisplayOptions;
import com.ferox.core.system.HeadlessRenderSurface;
import com.ferox.core.util.FeroxException;

class PbufferSurface extends HeadlessRenderSurface {
	private GLPbuffer pbuffer;
	DisplayOptions params;
	
	public PbufferSurface(GLCapabilities caps, DisplayOptions original) {
		if (GLDrawableFactory.getFactory().canCreateGLPbuffer()) {
			this.pbuffer = GLDrawableFactory.getFactory().createGLPbuffer(caps, null, original.getWidth(), original.getHeight(), null);
		} else
			throw new FeroxException("Platform doesn't support pbuffers, can't create a headless render surface");
		this.params = original;
	}
	
	public GLPbuffer getGLPbuffer() {
		return this.pbuffer;
	}
	
	@Override
	public DisplayOptions getDisplayOptions() {
		return this.params;
	}
}
