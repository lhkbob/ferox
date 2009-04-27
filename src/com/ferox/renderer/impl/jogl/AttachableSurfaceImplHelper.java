package com.ferox.renderer.impl.jogl;

import java.awt.EventQueue;
import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLContext;
import javax.media.opengl.Threading;

import com.ferox.renderer.RenderException;

/** Utility class to implement the guts of AttachableSurfaceGLEventListener. 
 * 
 * @author Michael Ludwig
 *
 */
public class AttachableSurfaceImplHelper {
	/*
	 * Various supported render modes. There are 3 varieties, of 2 different
	 * types. The first type is forcing JOGL rendering onto another thread (the
	 * AWT EDT). The second is active rendering, which renders on the current
	 * thread.
	 * 
	 * The 1st has 2 varieties, the standard way, and an experimental way using
	 * invokeLater() instead. They both perform about the same. In small scenes
	 * they suffer from unstable high frame rates.
	 * 
	 * Active rendering is significantly faster, and has more stable framerates.
	 * Unfortunately, it suffers from rendering artifacts in Mac from other
	 * windows. I don't know why. It is still suitable for fullscreen rendering.
	 */
	private static final int RENDER_MODE_JOGL_STANDARD = 0;
	private static final int RENDER_MODE_AWT_WAIT = 1;
	private static final int RENDER_MODE_ACTIVE = 2;
	
	private static final int renderMode = RENDER_MODE_JOGL_STANDARD;
	
	/* Variables used for each render mode. */
	private List<JoglRenderSurface> attachedSurfaces;
	private Runnable resourceAction;
	private Exception caughtEDTException;
	
	/* Variables used for AWT_WAIT. */
	private InvokeLaterRenderAction invokeLaterAction;
	private volatile boolean actionFinished; // signal back to other thread
	private Object lock;
	
	public AttachableSurfaceImplHelper() {
		this.attachedSurfaces = new ArrayList<JoglRenderSurface>();
	}
	
	/** Matches the assignResourceAction() method of the AttachableSurfaceGLEventListener. */
	public void assignResourceAction(Runnable action) {
		this.resourceAction = action;
	}

	/** Matches the attachRenderSurface() method of the AttachableSurfaceGLEventListener. */
	public void attachRenderSurface(JoglRenderSurface surface) {
		this.attachedSurfaces.add(surface);
	}

	/** Matches the display(drawable) method of the AttachableSurfaceGLEventListener. */
	public void display(GLAutoDrawable drawable) {
		try {
			// execute the assigned resource action
			if (this.resourceAction != null) {
				this.resourceAction.run();
			}

			JoglRenderSurface curr;
			JoglRenderSurface next;

			int i;
			int size = this.attachedSurfaces.size();
			if (size > 0) {
				i = 1;
				curr = this.attachedSurfaces.get(0);
				next = (size > 1 ? this.attachedSurfaces.get(1) : null);

				while (curr != null) {
					curr.displaySurface(next);
					curr = next;

					i++;
					next = (i < size ? this.attachedSurfaces.get(i) : null);
				}
			}
			
			// flush everything that has been issued
			drawable.getGL().glFlush();
		} catch (Exception e) {
			this.caughtEDTException = e;
			return;
		}
	}
	
	/** Matches the render(drawable) method of the AttachableSurfaceGLEventListener. */
	public void render(final GLAutoDrawable drawable) throws RenderException {
		try {
			this.caughtEDTException = null;
			
			switch(renderMode) {
			case RENDER_MODE_ACTIVE:
				this.renderActive(drawable);
				break;
			case RENDER_MODE_AWT_WAIT:
				this.renderAwtWait(drawable);
				break;
			case RENDER_MODE_JOGL_STANDARD:
				this.renderJoglStandard(drawable);
				break;
			}
			
			Exception c = this.caughtEDTException;
			if (c != null)
				throw new RenderException(c);
		} finally {
			this.attachedSurfaces.clear();
			this.resourceAction = null;
			this.caughtEDTException = null;
		}
	}
	
	/* Render operation for the RENDER_MODE_JOGL_STANDARD. */
	private void renderJoglStandard(GLAutoDrawable drawable) {
		drawable.display();
	}
	
	/* Render operation for the RENDER_MODE_AWT_WAIT. */
	private void renderAwtWait(GLAutoDrawable drawable) {
		if (this.lock == null) {
			// lock and invokeLaterAction are null/not-null at the same time
			this.lock = new Object();
			this.invokeLaterAction = new InvokeLaterRenderAction();
		}
		
		// reset for rendering this drawable
		this.actionFinished = false;
		this.invokeLaterAction.drawable = drawable;
		
		// run the action
		EventQueue.invokeLater(this.invokeLaterAction);
		
		// wait to be notified that it has finished
		try {
			synchronized(this.lock) {
				while(!this.actionFinished)
					this.lock.wait();
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
		this.display(drawable);
		context.release();
		drawable.swapBuffers();
	}
	
	/* Internal class used for renderAwtWait(). */
	private class InvokeLaterRenderAction implements Runnable {
		GLAutoDrawable drawable;
		
		public void run() {
			synchronized(lock) {
				this.drawable.display();
				
				// notify calling thread that we're done
				actionFinished = true;
				lock.notifyAll();
			}
		}
	}
}