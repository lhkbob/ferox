package com.ferox.renderer.impl.jogl;

import javax.swing.SwingUtilities;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.WindowSurface;

/** The Jogl implementation of a WindowSurface.  It uses a Frame that has a single
 * GLCanvas child that contains everything. 
 * 
 * @author Michael Ludwig
 *
 */
public class JoglWindowSurface extends JoglOnscreenSurface implements WindowSurface {
	/** Expects as arguments, the factory that is currently handling a createWindowSurface()
	 * call, as well as the identically named arguments to that call. 
	 * 
	 * Makes sure that the created window is at least 1x1, wherever the window was created. */
	protected JoglWindowSurface(JoglSurfaceFactory factory,	int id, DisplayOptions optionsRequest, 
								int x, int y, int width, int height, boolean resizable, boolean undecorated) {
		super(factory, id, optionsRequest, x, y, width, height, resizable, undecorated);
	}
	
	@Override
	public boolean isResizable() {
		return this.frame.isResizable();
	}

	@Override
	public boolean isUndecorated() {
		return this.frame.isUndecorated();
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
		return this.frame.getX();
	}

	@Override
	public int getY() {
		return this.frame.getY();
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
