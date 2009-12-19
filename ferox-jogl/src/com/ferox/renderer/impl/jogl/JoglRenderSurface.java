package com.ferox.renderer.impl.jogl;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import com.ferox.renderer.Framework;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.DisplayOptions.StencilFormat;
import com.ferox.renderer.impl.AbstractRenderSurface;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.Context;

/**
 * JoglRenderSurface is the abstract RenderSurface that all createable
 * RenderSurfaces extend from for the JoglFramework.
 * 
 * @author Michael Ludwig
 */
public abstract class JoglRenderSurface extends AbstractRenderSurface {
	private static final AtomicReferenceFieldUpdater<JoglRenderSurface, Boolean> casDestroyed =
		AtomicReferenceFieldUpdater.newUpdater(JoglRenderSurface.class, Boolean.class, "destroyed");
	
	private final JoglFramework framework;
	
	private final Action postRenderAction;
	private final Action preRenderAction;
	
	private boolean renderedOnce;
	private volatile Boolean destroyed;
	
	private final Object surfaceLock = new Object();

	/**
	 * Create a new JoglRenderSurface that will be used by the given Framework.
	 * 
	 * @param framework the Framework that owns this surface
	 * @throws NullPointerException if framework is null
	 */
	public JoglRenderSurface(JoglFramework framework) {
		if (framework == null)
			throw new NullPointerException("Framework cannot be null");
		this.framework = framework;
		
		postRenderAction = new PostRenderAction();
		preRenderAction = new PreRenderAction();
		
		renderedOnce = false;
		destroyed = false;
	}

	@Override
	public Framework getFramework() {
		return framework;
	}
	
	@Override
	public boolean hasColorBuffer() {
		return getDisplayOptions().getPixelFormat() != PixelFormat.NONE;
	}

	@Override
	public boolean hasDepthBuffer() {
		return getDisplayOptions().getDepthFormat() != DepthFormat.NONE;
	}

	@Override
	public boolean hasStencilBuffer() {
		return getDisplayOptions().getStencilFormat() != StencilFormat.NONE;
	}

	@Override
	public boolean isDestroyed() {
		return destroyed;
	}

	/**
	 * @return A lock that must be synchronized on when rendering to a surface,
	 *         or destroying the surface
	 */
	public Object getLock() {
		return surfaceLock;
	}

	/**
	 * @return An Action to be invoked before any Actions that rely on this
	 *         surface. The JoglRenderManager should insert this into the
	 *         beginning of a batch to be rendered
	 */
	public Action getPreRenderAction() {
		return preRenderAction;
	}

	/**
	 * @return An Action to be invoked when all Actions that rely on this
	 *         surface have been completed. The JoglRenderManager should insert
	 *         this into the end of a batch to be rendered
	 */
	public Action getPostRenderAction() {
		return postRenderAction;
	}

	/**
	 * Destroy this JoglRenderSurface. Returns true if the the surface wasn't
	 * already destroyed and now is. Subclasses must override this method to
	 * actually destroy the surface.
	 * 
	 * @return True if the surface wasn't already destroyed
	 */
	public boolean destroy() {
		return casDestroyed.compareAndSet(this, false, true);
	}
	
	/**
	 * @return The JoglContext associated with this surface, may be null
	 */
	public abstract JoglContext getContext();

	/**
	 * This method is invoked the first time that the RenderSurface is rendered
	 * into.
	 */
	protected abstract void init();

	/**
	 * Invoked each time just before Actions associated with this RenderSurface
	 * are invoked.
	 */
	protected abstract void preRender();

	/**
	 * Invoked each time just after Actions associated with this RenderSurface
	 * are completed.
	 * 
	 * @param next The next Action to be rendered by the JoglRenderManager
	 */
	protected abstract void postRender(Action next);
	
	private class PreRenderAction extends Action {
		public PreRenderAction() {
			super(JoglRenderSurface.this);
		}

		@Override
		public void perform(Context context, Action next) {
			if (destroyed)
				return;
			
			if (!renderedOnce) {
				init();
				renderedOnce = true;
			}
			
			preRender();
		}
	}
	
	private class PostRenderAction extends Action {
		public PostRenderAction() {
			super(JoglRenderSurface.this);
		}

		@Override
		public void perform(Context context, Action next) {
			if (!destroyed)
				postRender(next);
		}
	}
}
