package com.ferox.renderer.impl;

import java.util.List;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FullscreenSurface;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.WindowSurface;
import com.ferox.resource.texture.TextureImage.TextureTarget;

/** SurfaceFactory is one of the major backbones of
 * an AbstractRenderer subclass. It provides a more specific
 * interface for creating surfaces, as well as providing some
 * high-level method calls to allow their use when rendering.
 * 
 * The resource model of AbstractRenderer assumes that all created
 * surfaces can share the created resource objects (even if the surfaces
 * have different state records).  A surface factory implementation
 * must guarantee this, or throw an exception when a surface creation would
 * break the contract.  Each surface factory has a "shadow" context that
 * doesn't have any surface associated with it.  This context must exist
 * until destroy() is called.  It is used if there are no other surfaces that
 * can be made current when managing resources.
 * 
 * It can be assumed that the methods defined here will only be used by
 * a single thread.
 * 
 * Because of the intimate relationship between surfaces
 * and textures, the surface factory is also the provider
 * of TextureImage drivers (instead of using a DriverFactory
 * like the other types if resources).
 * 
 * The relationship between SurfaceFactory and AbstractRenderer
 * is 1-1, the renderer must only have one factory and the factory
 * can only be in use by one renderer; their lifecycles are
 * inextricably tied.
 * 
 * Application programmers should not call any methods on
 * a surface factory as the renderer implementation that it's
 * attached to may likely get confused, and undefined results 
 * will ensue.  Ideally the visibility of a SurfaceFactory will
 * be completely internal to subclasses of AbstractRenderer.
 * 
 * @author Michael Ludwig
 *
 */
public interface SurfaceFactory {
	/** This method must implement the identically named method in Renderer, with additional
	 * rule: the returned surface must implement ContextRecordSurface, and the surface must
	 * respect the requirements of a valid context record surface implementation. */
	public WindowSurface createWindowSurface(DisplayOptions options, int x, int y, int width, int height,
											 boolean resizable, boolean undecorated) throws RenderException;
	
	/** This method must implement the identically named method in Renderer, with additional
	 * rule: the returned surface must implement ContextRecordSurface, and the surface must
	 * respect the requirements of a valid context record surface implementation. */
	public FullscreenSurface createFullscreenSurface(DisplayOptions options, int width, int height) throws RenderException;
	
	/** This method must implement the identically named method in Renderer, with additional
	 * rule: the returned surface must implement ContextRecordSurface, and the surface must
	 * respect the requirements of a valid context record surface implementation. */
	public TextureSurface createTextureSurface(DisplayOptions options, TextureTarget target, int width, int height, int depth, int layer,
											   int numColorTargets, boolean useDepthRenderBuffer) throws RenderException;
	
	/** This method must implement the identically named method in Renderer, with additional
	 * rule: the returned surface must implement ContextRecordSurface, and the surface must
	 * respect the requirements of a valid context record surface implementation. 
	 * 
	 * It can be assumed that the given surface was created by this factory and hasn't been 
	 * destroyed. */
	public TextureSurface createTextureSurface(TextureSurface share, int layer) throws RenderException;

	/** Destroy the underlying resources associated with the given surface.
	 * After a call to this method, the given surface must return true from
	 * its isDestroyed() method.  
	 * 
	 * The surface factory should not destroy any resource representations or
	 * otherwise clean up the shadow context necessary for sharing the resources.
	 * This will be accomplished in the destroy() method. 
	 * 
	 * In the event of the last surface being destroyed by this method, the 
	 * resources must still remain allocated on the shadow context in case new
	 * surfaces are later created. */
	public void destroy(ContextRecordSurface surface);
	
	/** Destroy the shadow context that is holding onto all of the resources.
	 * This method will only be called in response to the AbstractRenderer's
	 * destroy() method.  Afterwards, no other methods of the surface factory
	 * will be called.
	 * 
	 * It can be assumed that all other surfaces have been destroyed by the
	 * AbstractRenderer before this call.*/
	public void destroy();
	
	/** Called by the AbstractRenderer when it is time to invoke each of the
	 * given surface's render actions.  These surfaces should be processed
	 * in order, and do all actions necessary for the rendering to be visibly
	 * complete.
	 * 
	 * A Runnable resourceAction is also provided.  This must be executed on
	 * a valid context before any surface has its passes flushed.  If there
	 * are no queued surfaces, this action must be executed on an internal
	 * context. */
	public void renderFrame(List<ContextRecordSurface> queuedSurfaces, Runnable resourceAction);
	
	
	/** Return true if the calling thread can have low-level graphics operations invoked on it. */
	public boolean isGraphicsThread();
}
