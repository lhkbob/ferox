package com.ferox.renderer.impl.jogl;

import java.awt.DisplayMode;
import java.awt.Frame;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FullscreenSurface;

public class JoglFullscreenSurface extends JoglOnscreenSurface implements FullscreenSurface {
	private Frame frame;
	private DisplayMode mode;
	private GraphicsDevice gDev;
	
	protected JoglFullscreenSurface(JoglSurfaceFactory factory,	int id, DisplayOptions optionsRequest, int width, int height) {
		super(factory, id, optionsRequest, 0, 0, width, height, false, true);
	
		// get target device parameters and set the display mode
		this.gDev = GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice();
		this.gDev.setFullScreenWindow(this.frame);
		
		if (this.gDev.isFullScreenSupported()) {
			if (this.gDev.isDisplayChangeSupported()) {
				this.mode = chooseBestMode(this.gDev.getDisplayModes(), optionsRequest, width, height);
				this.gDev.setDisplayMode(this.mode);
			} else
				this.mode = this.gDev.getDisplayMode();	
		} else
			this.mode = this.gDev.getDisplayMode();
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

			if (weight < bestWeight) {
				bestWeight = weight;
				best = modes[i];
				System.out.println(weight + " " + best.getBitDepth() + " " + best.getWidth() + " " + best.getHeight() + " " + best.getRefreshRate());
			}
		}
		return best;
	}
	
	// closer to 0 represents better match
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
		super.destroySurface();
	}
}
