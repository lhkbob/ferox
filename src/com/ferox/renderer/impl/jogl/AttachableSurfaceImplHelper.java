package com.ferox.renderer.impl.jogl;

import java.util.ArrayList;
import java.util.List;

import javax.media.opengl.GLAutoDrawable;

import com.ferox.renderer.RenderException;

/** Utility class to implement the guts of AttachableSurfaceGLEventListener. 
 * 
 * @author Michael Ludwig
 *
 */
public class AttachableSurfaceImplHelper {
	private List<JoglRenderSurface> attachedSurfaces;
	private Runnable resourceAction;
	
	private Exception caughtEDTException;

	public AttachableSurfaceImplHelper() {
		this.attachedSurfaces = new ArrayList<JoglRenderSurface>();
	}
	
	/** Matches the render(drawable) method of the AttachableSurfaceGLEventListener. */
	public void render(GLAutoDrawable drawable) throws RenderException {
		try {
			this.caughtEDTException = null;
			drawable.display();
			
			Exception c = this.caughtEDTException;
			if (c != null)
				throw new RenderException(c);
		} finally {
			this.attachedSurfaces.clear();
			this.resourceAction = null;
			this.caughtEDTException = null;
		}
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
		} catch (Exception e) {
			this.caughtEDTException = e;
			return;
		}
	}
}
