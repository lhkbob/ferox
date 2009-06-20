package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;

import com.ferox.math.Color4f;
import com.ferox.renderer.AbstractRenderSurface;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.Framework;
import com.ferox.renderer.DisplayOptions.DepthFormat;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.DisplayOptions.StencilFormat;
import com.ferox.renderer.impl.jogl.record.FramebufferRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.PixelOpRecord;

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
public abstract class JoglRenderSurface extends AbstractRenderSurface implements
	RenderSurface, AttachableSurfaceGLEventListener {
	private boolean destroyed;

	private boolean renderedOnce;
	private Runnable renderAction;

	private final AttachableSurfaceImplHelper helper;

	protected final JoglContextManager factory;

	protected JoglRenderSurface(JoglContextManager factory) {
		this.factory = factory;
		destroyed = false;

		helper = new AttachableSurfaceImplHelper();

		renderAction = null;
		renderedOnce = false;
	}

	/**
	 * <p>
	 * Return the GLAutoDrawable associated with this surface. Return null if
	 * there is no GLAutoDrawable for the surface (at the moment this only
	 * applies to JoglTextureSurfaces using an FboDelegate).
	 * </p>
	 * <p>
	 * If null is returned, the surface will be attached to the shadow context
	 * or another surface to be rendered during their drawable's display()
	 * method.
	 * </p>
	 * <p>
	 * When null is not returned, the drawable will have its display() method
	 * executed during the factory's renderFrame() method.
	 * </p>
	 */
	public abstract GLAutoDrawable getGLAutoDrawable();

	/**
	 * All JoglRenderSurfaces must use a JoglStateRecord. Returns the
	 * JoglStateRecord that's associated with the drawable from
	 * getGLAutoDrawable(). If this is null, the effective state record for the
	 * surface is the state record of the current surface.
	 */
	public abstract JoglStateRecord getStateRecord();

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
	 * Called after the JoglContextManager has been notified that this is the
	 * active surface, and after resources have been potentially managed, but
	 * before the surface has been cleared, and before the render action has
	 * been executed.
	 */
	protected abstract void preRenderAction();

	/**
	 * <p>
	 * Called right after the render action has completed, while there is still
	 * a valid gl context active.
	 * </p>
	 * <p>
	 * If the next surface is null it means there will be no more surfaces
	 * rendered on the active context and it will be released.
	 * </p>
	 */
	protected abstract void postRenderAction(JoglRenderSurface next);

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
	public void assignResourceAction(Runnable runnable) {
		helper.assignResourceAction(runnable);
	}

	@Override
	public void attachRenderSurface(JoglRenderSurface surface) {
		helper.attachRenderSurface(surface);
	}

	@Override
	public Framework getRenderer() {
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

	/** Requirement of the ContextManager */
	public void setRenderAction(Runnable action) {
		renderAction = action;
	}

	@Override
	public void render() throws RenderException {
		GLAutoDrawable gd = getGLAutoDrawable();
		if (gd == null)
			throw new RenderException(
				"Cannot render a surface that must be attached to GL context");

		helper.render(gd);
	}

	@Override
	public final void display(GLAutoDrawable drawable) {
		helper.display(drawable);
	}

	@Override
	public final void displayChanged(GLAutoDrawable drawable,
		boolean modeChanged, boolean deviceChanged) {
		// do nothing
	}

	@Override
	public final void init(GLAutoDrawable drawable) {
		GL gl = drawable.getGL();
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT
			| GL.GL_STENCIL_BUFFER_BIT);
	}

	@Override
	public final void reshape(GLAutoDrawable drawable, int x, int y, int width,
		int height) {
		// do nothing
	}

	/**
	 * Utility method to run this surfaces renderAction and call all of its
	 * callbacks: init(), preRenderAction(), and postRenderAction(). This must
	 * only be called from within the display() method of an
	 * AttachableSurfaceGLEventListner.
	 */
	public void displaySurface(JoglRenderSurface next) {
		if (!renderedOnce) {
			this.init();
			renderedOnce = true;
		}

		preRenderAction();

		clearBuffers();
		renderAction.run();

		postRenderAction(next);
	}

	private void clearBuffers() {
		FramebufferRecord fr = getStateRecord().frameRecord;
		PixelOpRecord pr = getStateRecord().pixelOpRecord;

		GL gl = factory.getGL();

		// disable this to make sure color clearing happens over whole surface
		gl.glDisable(GL.GL_SCISSOR_TEST);
		pr.enableScissorTest = false;

		Color4f clearColor = getClearColor();
		if (!clearColor.equals(fr.clearColor)) {
			gl.glClearColor(clearColor.getRed(), clearColor.getGreen(),
				clearColor.getBlue(), clearColor.getAlpha());
			JoglUtil.get(clearColor, fr.clearColor);
		}
		float clearDepth = getClearDepth();
		if (fr.clearDepth != clearDepth) {
			gl.glClearDepth(clearDepth);
			fr.clearDepth = clearDepth;
		}
		int clearStencil = getClearStencil();
		if (fr.clearStencil != clearStencil) {
			gl.glClearStencil(clearStencil);
			fr.clearStencil = clearStencil;
		}

		int clearBits = 0;
		if (isColorBufferCleared())
			clearBits |= GL.GL_COLOR_BUFFER_BIT;
		if (isDepthBufferCleared())
			clearBits |= GL.GL_DEPTH_BUFFER_BIT;
		if (isStencilBufferCleared())
			clearBits |= GL.GL_STENCIL_BUFFER_BIT;

		if (clearBits != 0)
			gl.glClear(clearBits);

		// these aren't covered by any drivers at the moment, so make sure
		// they're
		// enabled correctly
		gl.glEnable(GL.GL_SCISSOR_TEST);
		pr.enableScissorTest = true;

		// this isn't part of the state record, but overhead should be minimal
		gl.glEnable(GL.GL_RESCALE_NORMAL);
	}
}
