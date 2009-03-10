package com.ferox.renderer.impl;

import java.util.List;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.FullscreenSurface;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.TextureSurface;
import com.ferox.renderer.WindowSurface;
import com.ferox.resource.TextureImage.TextureTarget;

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
	
	/** Called by the AbstractRenderer before it has invalidated any of 
	 * the queued surfaces.  This method should be used to notify the Renderer
	 * of any surfaces that were implicitly destroyed. 
	 * 
	 * The provided list will be the order in which surfaces are made current
	 * during the rendering of the frame.  This can be used at the time of
	 * makeCurrent() to decide on which realized surface to use for an Fbo. */
	public void startFrame(List<ContextRecordSurface> queuedSurfaces);
	
	/** Make the shadow context current so that resource drivers may be used.
	 * Implementations of SurfaceFactory and ResourceDrivers must work together
	 * to get access to low-level contexts in situations such as this.
	 * 
	 * Throw an exception if the shadow context can't be made current.  After
	 * call to this method, getCurrentSurface() should still return null. */
	public void makeShadowContextCurrent() throws RenderException;
	
	/** Make the specified surface current on the calling thread so that it's
	 * state can be modified by low-level graphics calls, so that geometries
	 * can be rendered and resources updated/fetched, etc.
	 * 
	 * It can be assumed that the surface is non-null, hasn't been destroyed,
	 * and was created by this surface factory. 
	 * 
	 * This method is responsible for properly releasing any previously
	 * current surface.  To allow for ease of implementation, it can be assumed that
	 * the only current surfaces on the thread will have been from this 
	 * factory (because there should only ever be one active Renderer as well). 
	 * 
	 * Throw a RenderException if the specified surface can't be made current 
	 * for any reason. */
	public void makeCurrent(ContextRecordSurface surface) throws RenderException;
	
	/** Release the surface that was last made current on this thread.  
	 * If no context is current, this method should do nothing.
	 * 
	 * On some systems, it may be necessary for the surface to be released,
	 * so third party graphics systems can run without locking up. */
	public void release();
	
	/** Clear the buffers of the render surface that is current.
	 * Buffers should be cleared as specified by the current surface,
	 * with the clear values that it provides.
	 * 
	 * If no surface is current, this method should do nothing. */
	public void clearBuffers();
	
	/** Invoked by this factory's AbstractRenderer when all the rendering
	 * to the current surface has finished and it should have its 
	 * buffers swapped (if needed) so that they are visible to the user.
	 * 
	 * This may be a no-op for some implementations of onscreen or offscreen
	 * surfaces.  Also, do nothing if there is no current surface.
	 * 
	 * This method is not intended as a signal to release the current surface. */
	public void swapBuffers();
	
	/** Get the surface that is current.  Return null if no surface is current. */
	public ContextRecordSurface getCurrentSurface();
	
	/** In many certain OpenGL bindings, actual graphics and context works
	 * is intended to only execute on certain threads.  This allows
	 * the AbstractRenderer to provide the execution code without knowing
	 * the details of a specific opengl binding.
	 * 
	 * It can be assumed that the runner is not null.
	 * This method must block until the execution is finished on the other
	 * thread. 
	 * 
	 * If an exception is thrown by the Runnable, it must be caught
	 * and thrown from the calling thread. */
	public void runOnWorkerThread(Runnable runner);
}
