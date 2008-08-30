package com.ferox.impl.jsr231;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLJPanel;

import com.ferox.core.system.DisplayOptions;
import com.ferox.core.system.OnscreenRenderSurface;

class GLJPanelSurface extends OnscreenRenderSurface {
	private GLJPanel canvas;

	DisplayOptions params;
	
	public GLJPanelSurface(GLCapabilities caps, DisplayOptions original) {
		super();
		this.canvas = new GLJPanel(caps);
		
		this.params = original;
	}
	
	@Override
	public Object getRenderSurface() {
		return this.getGLJPanel();
	}

	public GLJPanel getGLJPanel() {
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
