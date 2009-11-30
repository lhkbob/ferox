package com.ferox.renderer;

import java.util.concurrent.Future;

import com.ferox.math.Color4f;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * <p>
 * The Framework is the core component for rendering with Ferox. It controls the
 * creation of RenderSurfaces, which store the final render outputs, organizes
 * the Resources and, internally, handles all necessary operations for rendering
 * queued surfaces.
 * </p>
 * <p>
 * The lifecycle of a Framework often fits the following pattern:
 * <ol>
 * <li>Initialize the Framework and create the RenderSurfaces necessary</li>
 * <li>Render some number of frames, which involves queuing the RenderSurfaces
 * and then invoking {@link #render()}</li>
 * <li>At the same time as above, Resources may be updated and cleaned up based
 * on need of the application</li>
 * <li>The Framework is destroyed and is no longer usable</li>
 * </ol>
 * Another variant might represent a 3D editor where the RenderSurfaces are
 * created and destroyed more dynamically in response to user action.
 * </p>
 * <p>
 * Framework implementations must be thread-safe such that all methods can be
 * invoked from any thread at any time. Depending on the type of action
 * performed by the action, the methods may return immediately with the result,
 * block until capable of performing the action, or schedule some future task
 * that will be performed automatically. Generally, blocking methods are methods
 * that are called infrequently such as RenderSurface creation and destruction,
 * or Framework destruction. Methods that return immediately are queries to
 * immutable objects or are RenderSurface queues (which will be described in
 * more detail later). Methods with future tasks are those that are performed
 * frequently and benefit most from the ability of multi-threading, such as
 * managing resources and rendering frames.
 * </p>
 * <p>
 * When queuing RenderSurfaces for a frame, each Thread maintains its own
 * RenderSurface queue. This is so that the queues from one Thread do not
 * contaminate the ordering of the queue in another Thread. When a frame is
 * rendered from a Thread, it uses the queue for the calling Thread (even if the
 * rendering logic is executed on another Thread entirely).
 * </p>
 * <p>
 * There are three primary interfaces that Frameworks rely on to describe the
 * specifics of a scene:<br>
 * 1. Resource - fairly static data stored on the graphics card. <br>
 * 2. Geometry - describes the shape of something to be rendered (it's also a
 * Resource).<br>
 * 3. Effect - modifies how the Geometry is rendered (e.g. surface description).
 * </p>
 * <p>
 * Because these interfaces are not specific, a Framework implementation may not
 * support some types of implementations. Frameworks must document which types
 * they support, and how to add support for new types. To allow for efficient
 * caching and bookkeeping, these interfaces also provide methods allowing a
 * Framework to associate internal object with specific instances. <b>UNDER NO
 * CIRCUMSTANCES SHOULD THESE METHODS BE CALLED OR USED EXTERNALLY OF RENDERER
 * IMPLEMENTATIONS.</b>
 * </p>
 * <p>
 * Although the get/setRenderData() methods in Resource allow for multiple
 * simultaneous Frameworks, it is highly recommended that only one Framework be
 * used for efficiency purposes. It is unlikely that there should ever be a need
 * for multiple simultaneous Frameworks.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Framework {
	/**
	 * <p>
	 * Create a WindowSurface with the given options, and flags for resizable
	 * and undecorated. These parameters are requests to the underlying
	 * Framework, which will try its best to follow them. When the window
	 * surface is returned, it will be visible and on screen.
	 * </p>
	 * <p>
	 * The window is positioned at the given coordinates (relative to screen
	 * space) with the given dimensions. As stated in WindowSurface, these
	 * dimensions may not represent the drawable area, but can include the
	 * frame, border and insets used by the operating system.
	 * </p>
	 * <p>
	 * If options is null, or any of the other parameters have unsupported
	 * values, the Framework may change them to successfully create a surface.
	 * </p>
	 * <p>
	 * There are two types of onscreen surfaces supported by a Framework, either
	 * windows or fullscreen. There may be multiple window surfaces in use at
	 * the same time, but the Framework cannot mix window and fullscreen
	 * surfaces. Surfaces of one type must be destroyed before surfaces of the
	 * other type can be created successfully.
	 * </p>
	 * 
	 * @param options Requested display options
	 * @param x Requested x location of the window
	 * @param y Requested y location of the window
	 * @param width Requested width of the window
	 * @param height Requested height of the window
	 * @param resizable Whether or not the created window should be resizable
	 * @param undecorated Whether or not the created window is undecorated
	 * @throws SurfaceCreationException if the Framework cannot create the
	 *             WindowSurface, such as if there is an active
	 *             FullscreenSurface
	 * @throws RenderException if the Framework has been destroyed
	 */
	public WindowSurface createWindowSurface(DisplayOptions options, 
											 int x, int y, int width, int height, 
											 boolean resizable, boolean undecorated);

	/**
	 * <p>
	 * Create a surface that puts the application into exclusive fullscreen
	 * mode. The Framework will choose a screen resolution that is closest to
	 * the given dimensions in width/height.
	 * </p>
	 * <p>
	 * If options is null, or the parameters aren't directly supported, the
	 * Framework may change them to create a fullscreen surface.
	 * </p>
	 * <p>
	 * There may only be one fullscreen surface at any time. An exception will
	 * be thrown if another fullscreen surface exists from this Framework and
	 * that surface isn't destroyed. Similarly, the fullscreen surface can't be
	 * created until all window surfaces are destroyed.
	 * </p>
	 * 
	 * @param options The requested DisplayOptions
	 * @param width The requested screen width
	 * @param height The requested screen height
	 * @throws SurfaceCreationException if the Framework cannot create the
	 *             FullscreenSurface, such as if there's an active WindowSurface
	 *             already
	 * @throws RenderException if the Framework if has been destroyed
	 */
	public FullscreenSurface createFullscreenSurface(DisplayOptions options, 
													 int width, int height);

	/**
	 * <p>
	 * Create a TextureSurface that can be used to render into textures. The
	 * Framework will create new textures that can be retrieved by calling the
	 * returned surface's getColorBuffer(target) and getDepthBuffer() methods.
	 * The size of the texture is determined by the width, height, and depth
	 * values and its formatting and type is chosen by the DisplayOptions. In
	 * options, if the pixel format is NONE, then no textures will be created
	 * for the color buffers. Similarly, if the depth format is NONE, no texture
	 * is created for the depth buffer. (see about useDepthRenderBuffer below).
	 * </p>
	 * <p>
	 * Like the other surface creation methods, the Framework is free to change
	 * the parameters to make a surface create-able.
	 * </p>
	 * <p>
	 * The target value tells the Framework what type of texture to use (for all
	 * buffers of the surface). If the target is T_1D then height, depth and
	 * layer are ignored. If the target is T_CUBEMAP then height, and depth are
	 * ignored. If target is T_2D or T_RECT, then depth and layer are ignored.
	 * If target is T_3D, nothing is ignored.
	 * </p>
	 * <p>
	 * The layer value has the same meaning as it does in getLayer() of
	 * TextureSurface, which is why it's only used if target is T_3D or
	 * T_CUBEMAP.
	 * </p>
	 * <p>
	 * numColorTargets requests the surface to use the given number of color
	 * targets. For GLSL shader's, the textures returned by
	 * getColorBuffer(target) correspond to the low-level color targets
	 * supported by GLSL. If options has a pixel format of NONE, then
	 * numColorTargets is ignored and will be 0 for the returned surface. If
	 * numColorTargets is not ignored, then it will be clamped between [1,
	 * maxColorTargets].
	 * </p>
	 * <p>
	 * If options has a depth format of NONE, then normally no depth buffer
	 * would be allocated and no texture would be created for it. However, if
	 * useDepthRenderBuffer is true in this case, the Framework will allocate a
	 * depth buffer of its choosing for use with the surface. This allows
	 * correct z-ordering without the overhead of rendering into a texture. If a
	 * depth texture would be created, then useDepthRenderBuffer is ignored.
	 * </p>
	 * <p>
	 * Because T_CUBEMAP and T_3D textures do not support the DEPTH format, the
	 * created TextureSurface cannot have a depth buffer of the same texture
	 * type. If the requested depth format is not NONE in this case, then a
	 * Texture2D or TextureRectangle will be created that will be shared by each
	 * layer of the color buffers. If you need separate depth data for each
	 * layer, you must use some other solution (such as cube map "unrolling").
	 * </p>
	 * <p>
	 * All texture images created for the TextureSurface (and then returned by
	 * its getXBuffer() methods) will be ready to use (e.g. it's not necessary
	 * to update the textures unless you change the texture parameters of the
	 * image). Each texture image should return a null BufferData for its image
	 * data. It is illegal to dispose the TextureSurface's textures. When a
	 * TextureSurface is destroyed, it will dispose all of its associated
	 * textures (assuming no other TextureSurface references them).
	 * </p>
	 * 
	 * @param options The requested DisplayOptions, affects the chosen texture
	 *            formats
	 * @param target The type of color and depth TextureImages created
	 * @param width The width of the created image, must be valid as determined
	 *            by target
	 * @param height The height of the created image, must be valid as
	 *            determined by target. It is ignored for T_1D targets
	 * @param depth The depth of the created image, must be valid if target is
	 *            T_3D
	 * @param layer Integer parameter specifying 2D slice updated by this
	 *            surface. Only used if target is T_CUBEMAP or T_3D
	 * @param numColorTargets The requested number of color texture images to
	 *            render into simultaneously
	 * @param useDepthRenderBuffer If no depth image is used, true signals to
	 *            store depth information outside of a texture
	 * @throws SurfaceCreationException if the TextureSurface can't be created,
	 *             because dimensions were invalid for the target, the layer was
	 *             invalid (if not ignored), or if they are unsupported, etc.
	 * @throws RenderException if the Framework has already been destroyed
	 */
	public TextureSurface createTextureSurface(DisplayOptions options, TextureTarget target, 
											   int width, int height, int depth, int layer, 
											   int numColorTargets, boolean useDepthRenderBuffer);

	/**
	 * <p>
	 * Create a texture surface that uses the exact same textures as the given
	 * surface (e.g. its getXBuffer() methods return the same TextureImages).
	 * The new surface will have the same format, same number of color targets,
	 * and the same depth buffer (including hidden depth render buffers, if they
	 * were created for the original surface).
	 * </p>
	 * <p>
	 * The layer value can be used to tell the Framework to render into a
	 * different layer of the same textures. This allows 3d rendering, or
	 * dynamic reflections to easily be implemented (for targets of T_3D or
	 * T_CUBEMAP). If layer would be ignored in the other createTextureSurface()
	 * method, it is ignored here. In this case, another surface is created that
	 * references the exact same texture data which may cause undefined
	 * rendering results if the surfaces are used at the same time.
	 * </p>
	 * 
	 * @param share The TextureSurface whose images are attached to for
	 *            rendering purposes
	 * @param layer The new layer to use for this surface (e.g. a different cube
	 *            face)
	 * @throws SurfaceCreationException if share is null, it's already
	 *             destroyed, or it wasn't created by this Framework. Also fail
	 *             if layer is invalid for the share's target type
	 * @throws RenderException if the Framework has been destroyed
	 */
	public TextureSurface createTextureSurface(TextureSurface share, int layer);

	/**
	 * <p>
	 * Destroy the given RenderSurface. After a call to this method, the surface
	 * will be ignored by calls to any of the queue() methods, and its
	 * isDestroyed() method will return true. The behavior of other methods of
	 * the surface are undefined after a call to this method.
	 * </p>
	 * <p>
	 * OnscreenSurfaces will no longer be visible after they are destroyed. When
	 * a FullscreenSurface is destroyed, the monitor should be restored to the
	 * original resolution when the surface was first created.
	 * </p>
	 * <p>
	 * Invoking destroy(surface) on already destroyed surfaces is a no-op. If
	 * the given surface is being used in an actively rendering frame, this call
	 * will block until the frame has completed before it's destroyed.
	 * </p>
	 * 
	 * @param surface The RenderSurface to destroy
	 * @throws NullPointerException if surface is null
	 * @throws IllegalArgumentException if the surface wasn't created by this
	 *             Framework
	 * @throws RenderException if this Framework has been destroyed
	 */
	public void destroy(RenderSurface surface);

	/**
	 * <p>
	 * Destroy all remaining active RenderSurfaces created by this Framework.
	 * This will cancel any pending disposals and updates and any frames that are
	 * currently being rendered.  Even though the dispose Futures are canceled,
	 * the internal data will be properly discarded.
	 * </p>
	 * <p>
	 * There is no matching isDestroyed() method because it is assumed that the
	 * Framework will be discarded after it is destroyed.
	 * </p>
	 * <p>
	 * This method will block until all tasks have completed that can't be
	 * canceled.
	 * </p>
	 * 
	 * @throws RenderException if it's already been destroyed
	 */
	public void destroy();

	/**
	 * <p>
	 * Schedule an update for the given resource. Every resource must be updated
	 * before it can be used by the Framework (including Geometries). If the
	 * resource has never been updated before by this Framework, or if
	 * forceFullUpdate is true, the resource will ignore the object returned by
	 * getDirtyState(). Otherwise, the Framework must update at least what's
	 * specified by the dirty description.
	 * </p>
	 * <p>
	 * The update action should use the dirty state instance returned by
	 * getDirtyState() at the time this method is invoked. If a subsequent
	 * update is scheduled before this one has been processed, it should return
	 * a Future linked to the same task, and the task's dirty state should be
	 * updated to represent the union of the current dirty state and the
	 * original dirty state.
	 * </p>
	 * <p>
	 * An important note about the update/disposal process of the Framework: If
	 * a resource hasn't been used before or has been disposed, but is needed
	 * when rendering, it will be automatically updated. If the resource has a
	 * non-null dirty descriptor when it's needed, it will also be updated.
	 * </p>
	 * <p>
	 * An update will be guaranteed completed before a Resource must be used by
	 * by a Renderer. The scheduling of an update will cancel any disposal
	 * requests that haven't yet been processed. An update can be canceled
	 * manually, by scheduling a disposal, or when the Framework is destroyed.
	 * </p>
	 * 
	 * @param resource The Resource to be updated
	 * @param forceFullUpdate Whether or not the update should ignore dirty
	 *            descriptions of the resource
	 * @return A Future that can be used to determine when the update completes,
	 *         the Status held by the Future is the new status of the Resource
	 * @throws NullPointerException if resource is null
	 * @throws UnsupportedResourceException if the resource implementation is
	 *             unsupported by this Framework
	 */
	public Future<Status> update(Resource resource, boolean forceFullUpdate);

	/**
	 * <p>
	 * Dispose the low-level, graphics hardware related data for the given
	 * Resource. This is a request to the Framework to dispose the Resource, and
	 * the Framework will process it at some time near in the future. If the
	 * Resource has been scheduled for an update that has yet to occur, this
	 * will cancel the update.
	 * </p>
	 * <p>
	 * Unlike the destroy() methods, a resource is still usable after it has
	 * been cleaned. Its attributes and data can be modified; the only
	 * requirement is that it must be updated again before it can be used
	 * correctly for rendering (explicitly or implicitly). Because resources are
	 * implicitly updated if they're disposed, a resource should only be
	 * disposed if it's not to be used again for a long period of time.
	 * </p>
	 * <p>
	 * A disposal can be canceled by explicitly scheduling an update via
	 * {@link #update(Resource, boolean)}, or when the Framework has been
	 * destroyed, or by manually canceling the returned Future. An implicit
	 * update does not cancel the clean-up.
	 * </p>
	 * <p>
	 * This will do nothing if the resource has already been cleaned up or it
	 * was never updated by this Framework before. If a scheduled disposal has
	 * yet to be completed and dispose() is invoked again, this should return a
	 * Future linked to the same task.
	 * </p>
	 * <p>
	 * When a Resource is no longer strong referenced, and is capable of being
	 * garbage collected, the Framework should automatically schedule a disposal
	 * if the Resource's state is not UNSUPPORTED or DISPOSED already.
	 * </p>
	 * 
	 * @param resource The Resource to have its internal resources cleaned-up
	 * @return A Future that can be used to determine when the clean-up
	 *         completes, or if it is canceled by an update or Framework
	 *         destruction. The returned Future's get() method returns null.
	 * @throws NullPointerException if resource is null
	 * @throws IllegalArgumentException if the Resource is a TextureImage being
	 *             used by an undestroyed TextureSurface for this Framework
	 * @throws UnsupportedResourceException if the resource implementation is
	 *             unsupported by this Framework
	 */
	public Future<Object> dispose(Resource resource);

	/**
	 * <p>
	 * Queue the given RenderSurface so that the given RenderPass will be
	 * rendered during the next call to render() from this Thread.
	 * RenderSurfaces and their associated passes are rendered in the order that
	 * they were queued. Therefore, it's possible to render one pass into
	 * surface A, then another pass into surface B, and then render something
	 * else back into A.
	 * </p>
	 * <p>
	 * When speaking of of the RenderSurface queue, or performing a queue
	 * operation, each Thread has its own queue so that multiple calls from
	 * various Threads do contaminate a single queue with a non-deterministic
	 * ordering.
	 * </p>
	 * <p>
	 * If the RenderSurface is queued for the 1st time this frame, the surface
	 * will have its logical buffers cleared to its designated colors. If the
	 * RenderSurface is being re-queued with another pass (etc.), then the
	 * logical buffers will not be cleared again during the frame. The buffer
	 * clearing will not occur until render() is called, however.
	 * </p>
	 * <p>
	 * Destroyed surfaces from this Framework are ignored; this applies to a
	 * destroyed surface at queue time and render time. This is to make it
	 * easier in a multi-threaded environment.
	 * </p>
	 * <p>
	 * Implementations are strongly encouraged to batch sequential groups of
	 * RenderPasses that apply to the same RenderSurface to avoid unnecessary
	 * context switching.
	 * </p>
	 * 
	 * @param surface The RenderSurface that the given RenderPass will be
	 *            rendered to
	 * @param pass The RenderPass that will be rendered
	 * @throws NullPointerException if surface or pass are null
	 * @throws IllegalArgumentException if surface wasn't created by this
	 *             Framework
	 * @throws RenderException if the Framework has been destroyed
	 */
	public void queue(RenderSurface surface, RenderPass pass);

	/**
	 * <p>
	 * Perform the same operations as queue(RenderSurface, RenderPass) except
	 * this method allows you to manually override the buffer clear policy for
	 * each of the surface's logical buffers. This override is respected
	 * regardless of whether or not the surface is being queued for the first
	 * time or a subsequent time.
	 * </p>
	 * <p>
	 * If the RenderSurface does not have a logical buffer that matches one of
	 * the clear options, that clear option is ignored.
	 * </p>
	 * 
	 * @param surface The RenderSurface that the given RenderPass will be
	 *            rendered to
	 * @param pass The RenderPass that will be rendered
	 * @param clearColor True if the color buffer should be cleared
	 * @param clearDepth True if the depth buffer should be cleared
	 * @param clearStencil True if the stencil buffer should be cleared
	 * @throws NullPointerException if surface or pass are null
	 * @throws IllegalArgumentException if surface wasn't created by this
	 *             Framework
	 * @throws RenderException if the Framework is has been destroyed
	 */
	public void queue(RenderSurface surface, RenderPass pass, 
					  boolean clearColor, boolean clearDepth, boolean clearStencil);

	/**
	 * <p>
	 * Perform the same operations as queue(RenderSurface, RenderPass) except
	 * this method allows you to manually override the buffer clear policy for
	 * each of the surface's logical buffers. This override is respected
	 * regardless of whether or not the surface is being queued for the first
	 * time or a subsequent time.  In addition it also overrides the actual clear
	 * values for each of the buffers.
	 * </p>
	 * <p>
	 * If the RenderSurface does not have a logical buffer that matches one of
	 * the clear options, that clear option is ignored.
	 * </p>
	 * 
	 * @param surface The RenderSurface that the given RenderPass will be
	 *            rendered to
	 * @param pass The RenderPass that will be rendered
	 * @param clearColor True if the color buffer should be cleared
	 * @param clearDepth True if the depth buffer should be cleared
	 * @param clearStencil True if the stencil buffer should be cleared
	 * @param color The color that surface is cleared to, if null it clears to black
	 * @param depth The depth value that surface is cleared to, must be in [0, 1]
	 * @param stencil The stencil value that the surface is cleared to
	 * @throws NullPointerException if surface or pass are null
	 * @throws IllegalArgumentException if surface wasn't created by this
	 *             Framework, or if depth is outside of [0, 1]
	 * @throws RenderException if the Framework has been destroyed
	 */
	public void queue(RenderSurface surface, RenderPass pass, 
					  boolean clearColor, boolean clearDepth, boolean clearStencil, 
					  Color4f color, float depth, int stencil);

	/**
	 * <p>
	 * Render a single frame for the queued RenderSurfaces of the calling
	 * Thread. For each queued (surface X pass), clear the surface based on the
	 * clear parameters when the surface was queued and render the pass. Queued
	 * surfaces that have been destroyed should be ignored.
	 * </p>
	 * <p>
	 * As specified in {@link #queue(RenderSurface, RenderPass)}, each Thread
	 * maintains its own queue of RenderSurfaces for rendering. When render() is
	 * invoked, it uses the queue for the calling Thread. The actual rendering
	 * may be performed on this thread or on an internal thread, depending on
	 * the implementation. A RenderPass should be used in a synchronized manner
	 * so that it's only executed in Thread at a given time.
	 * </p>
	 * <p>
	 * If the frame is to be rendered on another Thread, the returned Future may
	 * not be completed when this returns. In this case it is cancel-able,
	 * leaving the frame unfinished. The state of the RenderSurfaces involved
	 * are not explicitly described here, but should be reasonable and must
	 * still be usable in subsequent renders without problems.
	 * </p>
	 * <p>
	 * If an exception is thrown, the rendering of the frame stops. Because of
	 * this, ResourceManagers, RenderPasses and other objects used in
	 * flushRenderer should be careful when they decide to throw an exception.
	 * The returned Future will then contain this Exception instead of a a
	 * FrameStatistics result.
	 * </p>
	 * 
	 * @return A Future that can be used to determine when a frame has
	 *         completed. Within it will contain FrameStatistics information
	 *         about the timing of the frame
	 * @throws RenderException if the Framework has been destroyed
	 */
	public Future<FrameStatistics> render();

	/**
	 * <p>
	 * Get the current status of the given resource. Return UNSUPPORTED if the
	 * Framework cannot support the given Resource type.
	 * </p>
	 * <p>
	 * If ERROR is returned, the resource will be ignored when rendering.
	 * </p>
	 * 
	 * @param resource The Resource whose status is requested
	 * @return The Status of resource
	 * @throws NullPointerException if resource is null
	 * @throws RenderException if the Framework has been destroyed
	 */
	public Status getStatus(Resource resource);

	/**
	 * Get a Framework status message that is more informative about the given
	 * resources's status.
	 * 
	 * @param resource The Resource whose status message is requested
	 * @return The status message for resource
	 * @throws NullPointerException if resource is null
	 * @throws RenderException if the Framework has been destroyed
	 */
	public String getStatusMessage(Resource resource);

	/**
	 * Get the capabilities of this Framework.
	 * 
	 * @return The RenderCapabilities for this Framework, will not be null
	 * @throws RenderException if the Framework has been destroyed
	 */
	public RenderCapabilities getCapabilities();
}
