package com.ferox.renderer.impl.jogl;

import java.awt.DisplayMode;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FullscreenSurface;

public class JoglFullscreenSurface extends JoglOnscreenSurface implements
		FullscreenSurface {
	private DisplayMode mode;
	private final GraphicsDevice gDev;

	protected JoglFullscreenSurface(JoglContextManager factory,
			DisplayOptions optionsRequest, final int width, final int height) {
		super(factory, optionsRequest, 0, 0, width, height, false, true);

		// get target device parameters and set the display mode
		gDev = GraphicsEnvironment.getLocalGraphicsEnvironment()
				.getDefaultScreenDevice();
		gDev.setFullScreenWindow(frame);

		if (gDev.isFullScreenSupported()) {
			if (gDev.isDisplayChangeSupported()) {
				mode = chooseBestMode(gDev.getDisplayModes(), optionsRequest,
						width, height);
				gDev.setDisplayMode(mode);
			} else
				mode = gDev.getDisplayMode();
		} else
			mode = gDev.getDisplayMode();
	}

	private static DisplayMode chooseBestMode(DisplayMode[] modes,
			DisplayOptions options, int width, int height) {
		int desiredBitDepth;
		switch (options.getPixelFormat()) {
		case RGB_16BIT:
			desiredBitDepth = 16;
			break;
		case RGBA_32BIT:
		case RGBA_FLOAT:
			desiredBitDepth = 32;
			break;
		default:
			desiredBitDepth = 24;
			break;
		}

		DisplayMode best = modes[0];
		float bestWeight = getWeight(best, desiredBitDepth, width, height);
		float weight;
		for (int i = 1; i < modes.length; i++) {
			weight = getWeight(modes[i], desiredBitDepth, width, height);

			if (weight < bestWeight) {
				bestWeight = weight;
				best = modes[i];
			}
		}
		return best;
	}

	// closer to 0 represents better match
	private static float getWeight(DisplayMode candidate, int bits, int width,
			int height) {
		int w = candidate.getWidth();
		int h = candidate.getHeight();
		int b = candidate.getBitDepth();

		if (b == DisplayMode.BIT_DEPTH_MULTI)
			return ((Math.abs(width - w) / (float) (w + width)) + (Math
					.abs(height - h) / (float) (h + height))) / 2f;
		else
			return ((Math.abs(width - w) / (float) (w + width))
					+ (Math.abs(height - h) / (float) (h + height)) + (Math
					.abs(bits - b) / (float) (b + bits))) / 3f;
	}

	@Override
	public void destroySurface() {
		if (gDev.getFullScreenWindow() == frame)
			gDev.setFullScreenWindow(null);
		super.destroySurface();
	}
}
