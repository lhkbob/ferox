package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;

import com.ferox.renderer.AbstractRenderSurface;
import com.ferox.renderer.Framework;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.DisplayOptions.StencilFormat;
import com.ferox.renderer.impl.Action;

/**
 * <p>
 * All render surfaces used by the JoglContextManager must extend from
 * JoglRenderSurface, which provides the necessary methods that
 * JoglContextManager requires for its implementations.
 * </p>
 * <p>
 * The JoglRenderSurface implements GLEventListener, however, subclasses will
 * not override those methods for customizability. Instead, use the onX()
 * methods to achieve that. Also, any subclasses that use a GLAutoDrawable must
 * be sure to add themselves as a GLEventListener to function properly.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class JoglRenderSurface extends AbstractRenderSurface implements RenderSurface, FrameworkGLEventListener {
	private boolean destroyed;
	private boolean renderedOnce;

	private final PostRenderAction postAction;
	private final PreRenderAction preAction;
	
	private final FrameworkGLEventListenerImpl helper;
	protected final JoglContextManager factory;

	protected JoglRenderSurface(JoglContextManager factory) {
		this.factory = factory;
		destroyed = false;
		renderedOnce = false;
		
		postAction = new PostRenderAction();
		preAction = new PreRenderAction();

		helper = new FrameworkGLEventListenerImpl();
	}

	/**
	 * <p>
	 * Called by this surface's factory its destroy(surface) method is called.
	 * The surface's context will not be current, so you can't make opengl
	 * commands.
	 * </p>
	 * <p>
	 * Subclasses must override this method to destroy the surface's opengl
	 * context data, etc. They must also call super.onDestroySurface() at the
	 * end, so that destroyed is set to true properly.
	 * </p>
	 */
	public void destroySurface() {
		markDestroyed();
	}

	/**
	 * Called by the Action returned by getPreRenderAction() to perform
	 * the actual operations.
	 */
	protected abstract void preRenderAction();

	/**
	 * <p>
	 * Called by the Action returned by getPostRenderAction() to perform
	 * the actual operations.
	 * </p>
	 * <p>
	 * If the next action is null it means there will be no more actions
	 * rendered on the active context and it will be released.
	 * </p>
	 */
	protected abstract void postRenderAction(Action next);

	/**
	 * Called before preRenderAction() the first time this surface will be
	 * rendered. There is a suitable gl available from the factory.
	 */
	protected abstract void init();

	/**
	 * Must only be called by the surcace's factory when the surface is no
	 * longer usable. All this does is set the destroyed flag to true.
	 */
	final void markDestroyed() {
		destroyed = true;
	}

	@Override
	public Framework getFramework() {
		return factory.getFramework();
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

	@Override
	public void render(Action actions) throws RenderException {
		GLAutoDrawable gd = getGLAutoDrawable();
		if (gd == null)
			throw new RenderException("Cannot render a surface that must be attached to GL context");

		helper.render(gd, actions);
	}
	
	@Override
	public Action getPreRenderAction() {
		return preAction;
	}
	
	@Override
	public Action getPostRenderAction() {
		return postAction;
	}
	
	/* GLEventListener methods */

	@Override
	public final void display(GLAutoDrawable drawable) {
		helper.display(drawable);
	}

	@Override
	public final void displayChanged(GLAutoDrawable drawable, boolean modeChanged, boolean deviceChanged) {
		// do nothing
	}

	@Override
	public final void init(GLAutoDrawable drawable) {
		GL gl = drawable.getGL();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT | GL.GL_STENCIL_BUFFER_BIT);
	}

	@Override
	public final void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
		// do nothing
	}
	
	private class PreRenderAction extends Action {
		public PreRenderAction() {
			super(JoglRenderSurface.this);
		}

		@Override
		public void perform() {
			if (!renderedOnce) {
				init();
				renderedOnce = true;
			}
			
			preRenderAction();
		}
	}
	
	private class PostRenderAction extends Action {
		public PostRenderAction() {
			super(JoglRenderSurface.this);
		}

		@Override
		public void perform() {
			postRenderAction(next());
		}
	}
}
