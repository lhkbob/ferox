package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLProfile;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FullscreenSurface;

/** 
 * A Jogl implementation that relies on AWT to display
 * a fullscreen window with a GLCanvas inside it.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglFullscreenSurface extends JoglOnscreenSurface implements FullscreenSurface {

	public JoglFullscreenSurface(JoglContextManager factory, GLProfile profile, DisplayOptions optionsRequest, 
								 final int width, final int height) {
		super(factory, profile, optionsRequest, 0, 0, width, height, false, true);

		window.setFullscreen(true);
	}

	@Override
	public void destroySurface() {
		if (window.isFullscreen())
			window.setFullscreen(false);
		super.destroySurface();
	}
}
