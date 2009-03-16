package com.ferox.renderer.impl.jogl;

import java.awt.Frame;

import javax.swing.SwingUtilities;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.WindowSurface;

/** The Jogl implementation of a WindowSurface.  It uses a Frame that has a single
 * GLCanvas child that contains everything. 
 * 
 * @author Michael Ludwig
 *
 */
public class JoglWindowSurface extends JoglOnscreenSurface implements WindowSurface {
	private Frame frame;
	
	/** Expects as arguments, the factory that is currently handling a createWindowSurface()
	 * call, as well as the identically named arguments to that call. 
	 * 
	 * Makes sure that the created window is at least 1x1, wherever the window was created. */
	protected JoglWindowSurface(JoglSurfaceFactory factory,	int id, DisplayOptions optionsRequest, 
								final int x, final int y, final int width, final int height, 
								final boolean resizable,final  boolean undecorated) {
		super(factory, id, optionsRequest);
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					Frame f = new Frame();
					f.setResizable(resizable);
					f.setUndecorated(undecorated);

					f.add(JoglWindowSurface.this.getGLAutoDrawable());
					f.setBounds(x, y, Math.max(width, 1), Math.max(height, 1));
					f.setVisible(true);
					
					JoglWindowSurface.this.frame = f;
				}
			});
		} catch (Exception e) {
			throw new RenderException("Error creating JoglWindowSurface", e);
		}
		
		this.frame.addWindowListener(this);
	}

	protected Frame getFrame() {
		return this.frame;
	}
	
	@Override
	public void destroySurface() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					Frame f = JoglWindowSurface.this.frame;
					
					f.removeWindowListener(JoglWindowSurface.this);
					f.setVisible(false);
					f.dispose();
					
					JoglWindowSurface.this.frame = null;
				}
			});
		} catch (Exception e) {
			throw new RenderException("Error hiding JoglWindowSurface", e);
		}
		
		super.destroySurface();
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
