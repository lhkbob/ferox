package com.ferox.renderer.impl.jogl;

import javax.swing.SwingUtilities;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.WindowSurface;

/**
 * The Jogl implementation of a WindowSurface. It uses a Frame that has a single
 * GLCanvas child that contains everything.
 * 
 * @author Michael Ludwig
 */
public class JoglWindowSurface extends JoglOnscreenSurface implements WindowSurface {
	public JoglWindowSurface(JoglContextManager factory, DisplayOptions optionsRequest, 
							 int x, int y, int width, int height, 
							 boolean resizable, boolean undecorated) {
		super(factory, optionsRequest, x, y, width, height, 
			  resizable, undecorated);
	}

	@Override
	public boolean isResizable() {
		return frame.isResizable();
	}

	@Override
	public boolean isUndecorated() {
		return frame.isUndecorated();
	}

	@Override
	public void setSize(final int width, final int height) {
		if (width <= 0 || height <= 0)
			return;

		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JoglWindowSurface.this.frame.setSize(width, height);
			}
		});
	}

	@Override
	public int getX() {
		return frame.getX();
	}

	@Override
	public int getY() {
		return frame.getY();
	}

	@Override
	public void setLocation(final int x, final int y) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				JoglWindowSurface.this.frame.setLocation(x, y);
			}
		});
	}
}