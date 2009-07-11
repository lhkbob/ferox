package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.GLProfile;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.sun.javafx.newt.opengl.GLWindow;

/**
 * OnscreenShadowContext provides a GLContext from a GLCanvas, and when
 * necessary controls making the window visible. It attempts to keep the window
 * hidden unless GL calls actually need to be made (in which case, it makes it
 * visible only when necessary). This is intended to be a fall-back shadow
 * context when a PbufferShadowContext can't be used.
 * 
 * @author Michael Ludwig
 */
public class OnscreenShadowContext extends AbstractShadowContext {
	private final GLWindow window;

	private JoglStateRecord record;

	public OnscreenShadowContext(GLProfile profile, RenderCapabilities caps) {
		window = GLWindow.create();
		window.addGLEventListener(this);

		// init here just so we won't return a null record ever
		record = new JoglStateRecord(caps);
	}
	
	@Override
	public void render() throws RenderException {
		// must make the frame visible so that the context is valid
		window.setVisible(true);
		try {
			super.render();
		} finally {
			// must always hide the frame, even when an exception is thrown
			window.setVisible(false);
		}
	}

	@Override
	public void destroy() {
		window.destroy();
	}

	@Override
	public GLContext getContext() {
		return window.getContext();
	}

	@Override
	public GLAutoDrawable getGLAutoDrawable() {
		return window;
	}

	@Override
	public JoglStateRecord getStateRecord() {
		return record;
	}
}
