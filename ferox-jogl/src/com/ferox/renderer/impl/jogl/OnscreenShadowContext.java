package com.ferox.renderer.impl.jogl;

import java.awt.Frame;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

public class OnscreenShadowContext extends JoglContext {
	private Frame frame;
	
	private OnscreenShadowContext(JoglFramework framework, Frame frame, GLCanvas surface) {
		super(framework, surface, surface.getContext());
		this.frame = frame;
	}
	
	@Override
	public void destroy() {
		if (frame != null) {
			Utils.invokeOnAwtThread(new Runnable() {
				public void run() {
					frame.setVisible(false);
					frame.dispose();
				}
			});
			frame = null;
		}
		
		super.destroy();
	}

	public static OnscreenShadowContext create(JoglFramework framework, GLProfile profile) {
		final GLCanvas canvas = new PaintDisabledGLCanvas(new GLCapabilities(profile));
		final Frame frame = new Frame();
		Utils.invokeOnAwtThread(new Runnable() {
			public void run() {
				frame.setSize(1, 1);
				frame.setResizable(false);
				frame.setUndecorated(true);
				frame.setTitle("");
				frame.add(canvas);
				frame.setVisible(true);
			}
		});
		
		return new OnscreenShadowContext(framework, frame, canvas);
	}
}
