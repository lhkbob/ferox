package com.ferox.renderer.impl;

import java.util.List;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FullscreenSurface;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.SurfaceCreationException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.WindowSurface;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * <p>
 * ContextManager is one of the major backbones of an AbstractFramework
 * subclass. It provides a more specific interface for creating surfaces, as
 * well as providing some high-level method calls to allow their use when
 * rendering.
 * </p>
 * <p>
 * The resource model of AbstractFramework assumes that all created surfaces can
 * share the created resource objects (even if the surfaces have different state
 * records). A surface factory implementation must guarantee this, or throw an
 * exception when a surface creation would break the contract. Each surface
 * factory has a "shadow" context that doesn't have any surface associated with
 * it. This context must exist until destroy() is called. It is used if there
 * are no other surfaces that can be made current when managing resources.
 * </p>
 * <p>
 * It can be assumed that the methods defined here will only be used by a single
 * thread.
 * </p>
 * <p>
 * The relationship between ContextManager and AbstractFramework is 1-1, the
 * renderer must only have one factory and the factory can only be in use by one
 * renderer; their lifecycles are inextricably tied.
 * </p>
 * <p>
 * All RenderSurfaces created by the ContextManager must implement the following
 * method:
 * 
 * <pre>
 * /**
 * &lt;p&gt;
 * This method assigns this surface a Runnable to be executed each time the
 * surface is rendered. This action will properly handle flushing each of
 * the surface's render passes.
 * &lt;/p&gt;
 * &lt;p&gt;
 * It is the surface's responsibility to properly clear its buffers before
 * executing this, and to perform any actions afterwards necessary to
 * display the results correctly.
 * &lt;/p&gt;
 * &lt;p&gt;
 * This method will be called once right after the surface is created.
 * &lt;/p&gt;
 * 
 * &#064;param action The Runnable action for this surface, will not be null
 * /
 * 	private void setRenderAction(Runnable action);
 * </pre>
 * 
 * If they do not, exceptions will be thrown when the renderer creates a surface
 * using this ContextManager. The setRenderAction() should be private,
 * AbstractFramework will use reflection to invoke it and get around the
 * protections.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface ContextManager {
	/**
	 * This method must implement the identically named method in Framework,
	 * with additional rule: the returned surface must provide a
	 * setRenderAction() method. It can be assumed that the AbstractFramework
	 * has already made sure it's idle, so the ContextManager only needs to
	 * throw SurfaceCreationExceptions.
	 * 
	 * @param options The options originally passed to the Framework
	 * @param x The x originally passed to the Framework
	 * @param y The y originally passed to the Framework
	 * @param width The width originally passed to the Framework
	 * @param height The height originally passed to the Framework
	 * @param resizable The resizable boolean passed to the Framework
	 * @param undecorated The undecorated boolean passed to the Framework
	 * @return A concrete implementation of WindowSurface
	 * @throws SurfaceCreationException for any of the reasons specified in
	 *             Framework
	 */
	public WindowSurface createWindowSurface(DisplayOptions options, 
										     int x, int y, int width, int height, 
										     boolean resizable, boolean undecorated);

	/**
	 * This method must implement the identically named method in Framework,
	 * with additional rule: the returned surface must provide a
	 * setRenderAction() method. It can be assumed that the AbstractFramework
	 * has already made sure it's idle, so the ContextManager only needs to
	 * throw SurfaceCreationExceptions.
	 * 
	 * @param options The options originally passed to the Framework
	 * @param width The width originally passed to the Framework
	 * @param height The height originally passed to the Framework
	 * @return A concrete implementation of FullscreenSurface
	 * @throws SurfaceCreationException for any of the reasons specified in
	 *             Framework
	 */
	public FullscreenSurface createFullscreenSurface(DisplayOptions options, 
													 int width, int height) throws RenderException;

	/**
	 * This method must implement the identically named method in Framework,
	 * with additional rule: the returned surface must provide a
	 * setRenderAction() method. It can be assumed that the AbstractFramework
	 * has already made sure it's idle, so the ContextManager only needs to
	 * throw SurfaceCreationExceptions.
	 * 
	 * @param options The options originally passed to the Framework
	 * @param target The target originally passed to the Framework
	 * @param width The width originally passed to the Framework
	 * @param height The height originally passed to the Framework
	 * @param depth The depth originally passed to the Framework
	 * @param layer The layer passed to the Framework
	 * @param numColorTargets The numColorTargets value passed to the Framework
	 * @param useDepthRenderBuffer The useDepthRenderBuffer boolean passed to
	 *            the Framework
	 * @return A concrete implementation of TextureSurface
	 * @throws SurfaceCreationException for any of the reasons specified in
	 *             Framework
	 */
	public TextureSurface createTextureSurface(DisplayOptions options, TextureTarget target, 
											   int width, int height, int depth, int layer, 
											   int numColorTargets, boolean useDepthRenderBuffer) throws RenderException;

	/**
	 * <p>
	 * This method must implement the identically named method in Framework,
	 * with additional rule: the returned surface must provide a
	 * setRenderAction() method. It can be assumed that the AbstractFramework
	 * has already made sure it's idle, so the ContextManager only needs to
	 * throw SurfaceCreationExceptions.
	 * </p>
	 * <p>
	 * Additionally, share has already been validated by the AbstractFramework.
	 * </p>
	 * 
	 * @param share The TextureSurface to share with, it has already been
	 *            validated
	 * @param layer The layer to use for the new TextureSurface
	 * @return A concrete implementation of TextureSurface
	 * @throws SurfaceCreationException for any of the reasons specified in
	 *             Framework
	 */
	public TextureSurface createTextureSurface(TextureSurface share, int layer) throws RenderException;

	/**
	 * <p>
	 * Destroy the underlying resources associated with the given surface. After
	 * a call to this method, the given surface must return true from its
	 * isDestroyed() method.
	 * </p>
	 * <p>
	 * The ContextManager should not destroy any resource representations or
	 * otherwise clean up the shadow context necessary for sharing the
	 * resources. This will be accomplished in the destroy() method.
	 * </p>
	 * <p>
	 * In the event of the last surface being destroyed by this method, the
	 * resources must still remain allocated on the shadow context in case new
	 * surfaces are later created.
	 * </p>
	 * 
	 * @param surface The surface to destroy, it won't be null or already
	 *            destroyed
	 */
	public void destroy(RenderSurface surface);

	/**
	 * <p>
	 * Destroy the shadow context that is holding onto all of the resources.
	 * This method will only be called in response to the AbstractFramework's
	 * destroy() method. Afterwards, no other methods of the surface factory
	 * will be called.
	 * </p>
	 * <p>
	 * It can be assumed that all other surfaces have been destroyed by the
	 * AbstractFramework before this call.
	 * </p>
	 */
	public void destroy();

	/**
	 * <p>
	 * Perform the rendering operations on all the RenderSurfaces in the given
	 * list. Also perform the resourceAction before any rendering is performed.
	 * The resourceAction must be executed even if queuedSurface is empty or
	 * null. A null list must be treated like an empty list. If the
	 * resourceAction is executed on its own, it must still be executed on the
	 * graphics thread with a valid context.
	 * </p>
	 * <p>
	 * All thrown exceptions from the graphics thread should be caught (so it's
	 * not unwound) and re-thrown on the calling thread.
	 * </p>
	 * 
	 * @param queuedSurfaces The list of surfaces that need to be rendered
	 * @param resourceAction A Runnable that must be executed on a context
	 *            before any surfaces are rendered
	 */
	public void runOnGraphicsThread(List<RenderSurface> queuedSurfaces, 
									Runnable resourceAction);

	/**
	 * @return True if the calling thread can have low-level graphics operations
	 *         invoked on it.
	 */
	public boolean isGraphicsThread();

	/**
	 * Return a list of all un-destroyed RenderSurfaces that were created by
	 * this ContextManager. WindowSurfaces that are closed by user action (and
	 * thus implicitly destroyed) should not be included in this list.
	 * 
	 * @return The list of surfaces that are usable
	 */
	public List<RenderSurface> getActiveSurfaces();
}
