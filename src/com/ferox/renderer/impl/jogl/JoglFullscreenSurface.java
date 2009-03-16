package com.ferox.renderer.impl.jogl;

import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import javax.swing.SwingUtilities;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FullscreenSurface;
import com.ferox.renderer.RenderException;

public class JoglFullscreenSurface extends JoglOnscreenSurface implements FullscreenSurface {
	private Frame frame;
	private DisplayMode mode;
	private GraphicsDevice gDev;
	
	protected JoglFullscreenSurface(JoglSurfaceFactory factory,	int id, DisplayOptions optionsRequest, int width, int height) {
		super(factory, id, optionsRequest);
		
		// get target device parameters
		this.gDev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		if (this.gDev.isFullScreenSupported()) {
			if (this.gDev.isDisplayChangeSupported())
				this.mode = chooseBestMode(this.gDev.getDisplayModes(), optionsRequest, width, height);
			else
				this.mode = this.gDev.getDisplayMode();	
		} else
			this.mode = this.gDev.getDisplayMode();
		
		// create the frame to use
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					Frame f = new Frame();
					f.setResizable(false);
					f.setUndecorated(true);
					
					f.add(JoglFullscreenSurface.this.getGLAutoDrawable());
					f.setVisible(true);
					
					JoglFullscreenSurface.this.frame = f;
				}
			});
		} catch (Exception e) {
			throw new RenderException("Error creating JoglWindowSurface", e);
		}
		this.frame.addWindowListener(this);
		
		// set it to be the fullscreen window and change the display mode
		this.gDev.setFullScreenWindow(this.frame);
		if (this.mode != this.gDev.getDisplayMode())
			this.gDev.setDisplayMode(this.mode);
	}

	private static DisplayMode chooseBestMode(DisplayMode[] modes, DisplayOptions options, int width, int height) {
		int desiredBitDepth;
		switch(options.getPixelFormat()) {
		case RGB_16BIT: desiredBitDepth = 16; break;
		case RGBA_32BIT: case RGBA_FLOAT: desiredBitDepth = 32; break;
		default: desiredBitDepth = 24; break;
		}
		
		DisplayMode best = modes[0];
		float bestWeight = getWeight(best, desiredBitDepth, width, height);
		float weight;
		for (int i = 1; i < modes.length; i++) {
			weight = getWeight(modes[i], desiredBitDepth, width, height);
			if (weight > bestWeight) {
				bestWeight = weight;
				best = modes[i];
			}
		}
		
		return best;
	}
	
	private static float getWeight(DisplayMode candidate, int bits, int width, int height) {
		int w = candidate.getWidth();
		int h = candidate.getHeight();
		int b = candidate.getBitDepth();
		
		if (b == DisplayMode.BIT_DEPTH_MULTI) {
			return ((Math.abs(width - w) / (float)(w + width)) + (Math.abs(height - h) / (float)(h + height))) / 2f;
		} else {
			return ((Math.abs(width - w) / (float)(w + width)) + (Math.abs(height - h) / (float)(h + height)) + (Math.abs(bits - b) / (float)(b + bits))) / 3f;
		}
	}
	
	@Override
	public void destroySurface() {
		if (this.gDev.getFullScreenWindow() == this.frame)
			this.gDev.setFullScreenWindow(null);
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					Frame f = JoglFullscreenSurface.this.frame;
					
					f.removeWindowListener(JoglFullscreenSurface.this);
					f.setVisible(false);
					f.dispose();
					
					JoglFullscreenSurface.this.frame = null;
				}
			});
		} catch (Exception e) {
			throw new RenderException("Error hiding JoglFullscreenSurface", e);
		}
		
		super.destroySurface();
	}

	@Override
	protected Frame getFrame() {
		return this.frame;
	}
}
