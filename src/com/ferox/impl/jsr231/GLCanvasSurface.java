package com.ferox.impl.jsr231;

import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLCapabilities;

import com.ferox.core.system.DisplayOptions;
import com.ferox.core.system.OnscreenRenderSurface;

class GLCanvasSurface extends OnscreenRenderSurface {
	private GLCanvas canvas;

	DisplayOptions params;
	
	public GLCanvasSurface(GLCapabilities caps, DisplayOptions original) {
		super();
		this.canvas = new GLCanvas(caps);
		
		this.params = original;
	}
	
	@Override
	public Object getRenderSurface() {
		return this.getGLCanvas();
	}

	public GLCanvas getGLCanvas() {
		return this.canvas;
	}
	
	@Override
	public boolean isLightweight() {
		return false;
	}

	@Override
	public DisplayOptions getDisplayOptions() {
		return this.params;
	}
}
