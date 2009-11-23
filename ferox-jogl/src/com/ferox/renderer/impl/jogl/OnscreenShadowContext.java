package com.ferox.renderer.impl.jogl;

import java.awt.Frame;

import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;
import javax.media.opengl.awt.GLCanvas;

public class OnscreenShadowContext extends JoglContext {
	private Frame frame;
	private final GLCanvas canvas;
	
	private OnscreenShadowContext(JoglFramework framework, GLCanvas surface, GLContext context) {
		super(framework, surface, context);
		canvas = surface;

		frame = new Frame();
		Utils.invokeOnAwtThread(new Runnable() {
			public void run() {
				Frame f = new Frame();
				f.setSize(1, 1);
				f.setResizable(false);
				f.setUndecorated(true);
				f.setTitle("");
				f.add(canvas);
				frame.setVisible(true);

				frame = f;
			}
		});
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
		GLCanvas canvas = new GLCanvas(new GLCapabilities(profile));
		return new OnscreenShadowContext(framework, canvas, canvas.getContext());
	}
}
