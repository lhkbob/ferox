package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLAutoDrawable;

import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

public abstract class AbstractShadowContext implements ShadowContext {
	private final FrameworkGLEventListenerImpl helper;

	public AbstractShadowContext() {
		helper = new FrameworkGLEventListenerImpl();
	}

	@Override
	public void render(Action actions) throws RenderException {
		helper.render(getGLAutoDrawable(), actions);
	}

	@Override
	public void display(GLAutoDrawable drawable) {
		helper.display(drawable);
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

	@Override
	public Action getPostRenderAction() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Action getPreRenderAction() {
		return null;
	}

	@Override
	public JoglStateRecord getStateRecord() {
		return null;
	}
}
