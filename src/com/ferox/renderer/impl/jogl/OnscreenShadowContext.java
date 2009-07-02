package com.ferox.renderer.impl.jogl;

import java.awt.Frame;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLContext;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;

/**
 * OnscreenShadowContext provides a GLContext from a GLCanvas, and when
 * necessary controls making the canvas visible. It attempts to keep the canvas
 * hidden unless GL calls actually need to be made (in which case, it makes it
 * visible only when necessary). This is intended to be a fall-back shadow
 * context when a PbufferShadowContext can't be used.
 * 
 * @author Michael Ludwig
 */
public class OnscreenShadowContext extends AbstractShadowContext {
	// to be executed on the EDT before the canvas has display() called
	private final Runnable showFrame;
	private final Runnable hideFrame;

	private Frame frame;
	private final GLCanvas canvas;

	private JoglStateRecord record;
	private final RenderCapabilities caps;

	public OnscreenShadowContext(RenderCapabilities caps) {
		canvas = new GLCanvas();
		canvas.addGLEventListener(this);

		this.caps = caps;
		record = new JoglStateRecord(caps); // init here just so we won't return
		// a null state record ever

		frame = null;

		showFrame = new Runnable() {
			public void run() {
				if (frame == null) {
					Frame f = new Frame();
					f.setSize(1, 1);
					f.setResizable(false);
					f.setUndecorated(true);
					f.setTitle("");
					f.add(canvas);

					frame = f;
				}

				frame.setVisible(true);
			}
		};

		hideFrame = new Runnable() {
			public void run() {
				frame.setVisible(false);
			}
		};
	}

	@Override
	public void render() throws RenderException {
		// must make the frame visible so that the context is valid
		// for gl execution (instead of just context sharing)
		JoglUtil.invokeOnAwtThread(showFrame);

		record = new JoglStateRecord(caps);

		try {
			super.render();
		} finally {
			// must always hide the frame, even when an exception is thrown
			JoglUtil.invokeOnAwtThread(hideFrame);
		}
	}

	@Override
	public void destroy() {
		if (frame != null) {
			JoglUtil.invokeOnAwtThread(new Runnable() {
				public void run() {
					frame.setVisible(false);
					frame.dispose();
				}
			});
			frame = null;
		}

		canvas.getContext().destroy();
	}

	@Override
	public GLContext getContext() {
		return canvas.getContext();
	}

	@Override
	public GLAutoDrawable getGLAutoDrawable() {
		return canvas;
	}

	@Override
	public JoglStateRecord getStateRecord() {
		return record;
	}
}
