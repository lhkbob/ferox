package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLAutoDrawable;

import com.ferox.renderer.RenderException;

public abstract class AbstractShadowContext implements ShadowContext {
	private AttachableSurfaceImplHelper helper;

	public AbstractShadowContext() {
		this.helper = new AttachableSurfaceImplHelper();
	}
	
	@Override 
	public void render() throws RenderException {
		this.helper.render(this.getGLAutoDrawable());
	}

	@Override
	public void assignResourceAction(Runnable action) {
		this.helper.assignResourceAction(action);
	}

	@Override
	public void attachRenderSurface(JoglRenderSurface surface) {
		this.helper.attachRenderSurface(surface);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		this.helper.display(drawable);
	}

	@Override
	public void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
		// do nothing
	}

	@Override
	public void init(GLAutoDrawable drawable) {
		// do nothing
	}

	@Override
	public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// do nothing
	}
}
