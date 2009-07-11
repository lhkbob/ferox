package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLProfile;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.WindowSurface;

/**
 * The Jogl implementation of a WindowSurface. It uses a Frame that has a single
 * GLCanvas child that contains everything.
 * 
 * @author Michael Ludwig
 */
public class JoglWindowSurface extends JoglOnscreenSurface implements WindowSurface {
	public JoglWindowSurface(JoglContextManager factory, GLProfile profile, DisplayOptions optionsRequest, 
							 int x, int y, int width, int height, 
							 boolean resizable, boolean undecorated) {
		super(factory, profile, optionsRequest, x, y, width, height, 
			  resizable, undecorated);
	}

	@Override
	public boolean isResizable() {
		// GLWindow's don't have an option to not be resizable
		return true;
	}

	@Override
	public boolean isUndecorated() {
		return window.isUndecorated();
	}

	@Override
	public void setSize(int width, int height) {
		if (width <= 0 || height <= 0)
			return;
		window.setSize(width, height);
	}

	@Override
	public int getX() {
		return window.getX();
	}

	@Override
	public int getY() {
		return window.getY();
	}

	@Override
	public void setLocation(int x, int y) {
		window.setPosition(x, y);
	}
}
