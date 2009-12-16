package com.ferox.renderer.impl.jogl;

import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;

import com.ferox.renderer.Framework;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.DisplayOptions.StencilFormat;
import com.ferox.renderer.impl.AbstractRenderSurface;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.Context;

// DOWNLOAD: portal.acm.org/citation.cfm?id=300546
public abstract class JoglRenderSurface extends AbstractRenderSurface {
	private static final AtomicReferenceFieldUpdater<JoglRenderSurface, Boolean> casDestroyed =
		AtomicReferenceFieldUpdater.newUpdater(JoglRenderSurface.class, boolean.class, "destroyed");
	
	private final JoglFramework framework;
	
	private final Action postRenderAction;
	private final Action preRenderAction;
	
	private boolean renderedOnce;
	private volatile boolean destroyed;
	
	private final Object surfaceLock = new Object();
	
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
	
	public Object getLock() {
		return surfaceLock;
	}
	
	public Action getPreRenderAction() {
		return preRenderAction;
	}
	
	public Action getPostRenderAction() {
		return postRenderAction;
	}
	
	public boolean destroy() {
		return casDestroyed.compareAndSet(this, false, true);
	}
	
	public abstract JoglContext getContext();
	
	protected abstract void init();
	
	protected abstract void preRender();
	
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
