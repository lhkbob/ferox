package com.ferox.renderer.impl.jogl;

import java.awt.EventQueue;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.Threading;

import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.Action;

/**
 * Utility class to implement the guts of FrameworkGLEventListener.
 * 
 * @author Michael Ludwig
 */
public class FrameworkGLEventListenerImpl {
	/*
	 * Various supported render modes. There are 3 varieties, of 2 different
	 * types. The first type is forcing JOGL rendering onto another thread (the
	 * AWT EDT). The second is active rendering, which renders on the current
	 * thread.
	 * 
	 * At the moment, use JOGL_STANDARD because: -On Mac, there have been odd
	 * visual glitches with ACTIVE -On Windows, there are context sharing
	 * crashes with ACTIVE -AWT_WAIT is essentially a manual JOGL_STANDARD, so
	 * we should just do that
	 */
	private static final int RENDER_MODE_JOGL_STANDARD = 0;
	private static final int RENDER_MODE_AWT_WAIT = 1;
	private static final int RENDER_MODE_ACTIVE = 2;

	private static final int renderMode = RENDER_MODE_JOGL_STANDARD;

	/* Variables used for each render mode. */
	private Action actions;
	private Exception caughtEDTException;

	/* Variables used for AWT_WAIT. */
	private InvokeLaterRenderAction invokeLaterAction;
	private volatile boolean actionFinished; // signal back to other thread
	private Object lock;

	public FrameworkGLEventListenerImpl() {
	}

	/**
	 * Matches the display(drawable) method of the
	 * FrameworkGLEventListener.
	 */
	public void display(GLAutoDrawable drawable) {
		try {
			Action c = actions;
			while(c != null) {
				c.perform();
				c = c.next();
			}

			// flush everything that has been issued
			drawable.getGL().glFlush();
		} catch (Exception e) {
			caughtEDTException = e;
			return;
		}
	}

	/**
	 * Matches the drawable.render() method of the
	 * FrameworkGLEventListener.
	 */
	public void render(final GLAutoDrawable drawable, Action actions) throws RenderException {
		try {
			caughtEDTException = null;
			this.actions = actions;
			
			switch (renderMode) {
			case RENDER_MODE_ACTIVE:
				renderActive(drawable);
				break;
			case RENDER_MODE_AWT_WAIT:
				renderAwtWait(drawable);
				break;
			case RENDER_MODE_JOGL_STANDARD:
				renderJoglStandard(drawable);
				break;
			}

			Exception c = caughtEDTException;
			if (c != null)
				throw new RenderException(c);
		} finally {
			// unwind every action so some can be garbage collected
			Action c = actions;
			Action n;
			while(c != null) {
				n = c.next();
				c.setNext(null);
				c = n;
			}
			
			actions = null;
			caughtEDTException = null;
		}
	}

	/* Render operation for the RENDER_MODE_JOGL_STANDARD. */
	private void renderJoglStandard(GLAutoDrawable drawable) {
		drawable.display();
	}

	/* Render operation for the RENDER_MODE_AWT_WAIT. */
	private void renderAwtWait(GLAutoDrawable drawable) {
		if (lock == null) {
			// lock and invokeLaterAction are null/not-null at the same time
			lock = new Object();
			invokeLaterAction = new InvokeLaterRenderAction();
		}

		// reset for rendering this drawable
		actionFinished = false;
		invokeLaterAction.drawable = drawable;

		// run the action
		EventQueue.invokeLater(invokeLaterAction);

		// wait to be notified that it has finished
		try {
			synchronized (lock) {
				while (!actionFinished)
					lock.wait();
			}
		} catch (InterruptedException ie) {
			// Do nothing ??
		}
	}

	/* Render operation for the RENDER_MODE_ACTIVE. */
	private void renderActive(GLAutoDrawable drawable) {
		// must disable single threading
		if (Threading.isSingleThreaded())
			Threading.disableSingleThreading();

		// swap buffers manually
		if (drawable.getAutoSwapBufferMode())
			drawable.setAutoSwapBufferMode(false);

		GLContext context = drawable.getContext();
		context.makeCurrent();
		display(drawable);
		context.release();
		drawable.swapBuffers();
	}

	/* Internal class used for renderAwtWait(). */
	private class InvokeLaterRenderAction implements Runnable {
		GLAutoDrawable drawable;

		public void run() {
			synchronized (lock) {
				drawable.display();

				// notify calling thread that we're done
				actionFinished = true;
				lock.notifyAll();
			}
		}
	}
}
