package com.ferox.renderer.impl.jogl;

import com.ferox.renderer.Renderer;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.DisplayOptions.StencilFormat;
import com.ferox.renderer.impl.ContextRecordSurface;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.util.AbstractRenderSurface;

/** All render surfaces used by the JoglSurfaceFactory must extend from
 * JoglRenderSurface, which provides the necessary methods that JoglSurfaceFactory
 * requires for its implementations.
 * 
 * @author Michael Ludwig
 */
public abstract class JoglRenderSurface extends AbstractRenderSurface implements ContextRecordSurface {
	private boolean destroyed;
	private int id;
	
	private JoglSurfaceFactory factory;
	
	protected JoglRenderSurface(JoglSurfaceFactory factory, int id) {
		this.factory = factory;
		this.destroyed = false;
		this.id = id;
	}
	
	/** Return the JoglContext associated with this surface.  Return null if
	 * there is no actual context to associate with the surface.  When being
	 * made current, the surface factory will attempt to leverage the current
	 * surface, a realized surface and then the shadow context to provide 
	 * a current context for a surface with no true JoglContext.
	 * 
	 * This implies that null is only valid for surfaces that do not require their
	 * own context -> frame buffer objects for now. */
	public abstract JoglContext getContext();
	
	/** All JoglRenderSurfaces must use a JoglStateRecord. 
	 * Returns the JoglStateRecord that's returned with getContext().
	 * If getContext() returns null, then this returns the factory's 
	 * current state record. */
	public JoglStateRecord getStateRecord() {
		JoglContext c = this.getContext();
		if (c == null) {
			c = this.factory.getCurrentContext();
			return (c == null ? null : c.getStateRecord());
		} else
			return c.getStateRecord();
	}
	
	/** Called by this surface's factory has had its destroy(surface)
	 * method called.  The surface's context will not be current, so
	 * you can't make opengl commands.
	 * 
	 * Subclasses must override this method to destroy the surface's opengl
	 * context data, etc.  They must also call super.onDestroySurface() at 
	 * the end, so that destroyed is set to true properly. */
	public void onDestroySurface() {
		this.setDestroyed();
	}
	
	/** Called in response to the factory's swapBuffers() method.  It can
	 * be assumed that this surface's context is current. */
	public abstract void swapBuffers();
	
	/** Called after the context of this surface has been made current, allowing
	 * any actual gl code to be executed to complete context making-current process. */
	public abstract void onMakeCurrent();
	
	/** Called before the this surface will be released, allowing gl code to be
	 * executed that will 'release' any gl state needed for this surface.
	 * 
	 * If the next surface is null, or if the next surface uses a different
	 * gl context, then this surface's context will be released after a call to
	 * this method. */
	public abstract void onRelease(JoglRenderSurface next);
	
	/** Must only be called by the surcace's factory when the surface
	 * is no longer usable.  All this does is set the destroyed flag to true. */
	final void setDestroyed() {
		this.destroyed = true;
	}
	
	protected final JoglSurfaceFactory getFactory() {
		return this.factory;
	}
	
	@Override
	public int getSurfaceId() {
		return this.id;
	}

	@Override
	public Renderer getRenderer() {
		return this.factory.getRenderer();
	}

	@Override
	public boolean hasColorBuffer() {
		return this.getDisplayOptions().getPixelFormat() != PixelFormat.NONE;
	}

	@Override
	public boolean hasDepthBuffer() {
		return this.getDisplayOptions().getDepthFormat() != DepthFormat.NONE;
	}

	@Override
	public boolean hasStencilBuffer() {
		return this.getDisplayOptions().getStencilFormat() != StencilFormat.NONE;
	}

	@Override
	public boolean isDestroyed() {
		return this.destroyed;
	}
}
