package com.ferox.renderer;

import java.util.concurrent.Future;

import com.ferox.resource.Resource;
import com.ferox.resource.Texture;
import com.ferox.resource.Resource.Status;

/**
 * <p>
 * The Framework is the core component for rendering with Ferox. It controls the
 * creation of Surfaces, which store the final render outputs, organizes
 * the Resources and, internally, handles all necessary operations for rendering
 * queued surfaces.
 * </p>
 * <p>
 * The lifecycle of a Framework often fits the following pattern:
 * <ol>
 * <li>Initialize the Framework and create the Surfaces necessary</li>
 * <li>Render some number of frames, which involves queuing the Surfaces
 * and then invoking {@link #render()}</li>
 * <li>At the same time as above, Resources may be updated and cleaned up based
 * on need of the application</li>
 * <li>The Framework is destroyed and is no longer usable</li>
 * </ol>
 * Another variant might represent a 3D editor where the Surfaces are
 * created and destroyed more dynamically in response to user action.
 * </p>
 * <p>
 * Framework implementations must be thread-safe such that all methods can be
 * invoked from any thread at any time. Depending on the type of action
 * performed by the action, the methods may return immediately with the result,
 * block until capable of performing the action, or schedule some future task
 * that will be performed automatically. Generally, blocking methods are methods
 * that are called infrequently such as Surface creation and destruction,
 * or Framework destruction. Methods that return immediately are queries to
 * immutable objects or the queuing of RenderPasses. Methods with future tasks are those that are performed
 * frequently and benefit most from the ability of multi-threading, such as
 * managing resources and rendering frames.
 * </p>
 * <p>
 * When queuing Surfaces for a frame, each Thread maintains its own
 * Surface queue. This is so that the queues from one Thread do not
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
 * 
 * @author Michael Ludwig
 */
public interface Framework {
    /**
     * <p>
     * Create a OnscreenSurface with the given options.. These parameters are
     * requests to the underlying Framework, which will try its best to follow
     * them. When the window surface is returned, it will be visible and on
     * screen.
     * </p>
     * <p>
     * If options is null, or any of the other parameters have unsupported
     * values, the Framework may change them to successfully create a surface.
     * </p>
     * 
     * @param options Requested pixel format and initial configuration of the
     *            surface
     * @throws SurfaceCreationException if the Framework cannot create the
     *             WindowSurface
     * @throws RenderException if the Framework has been destroyed
     */
	public OnscreenSurface createSurface(OnscreenSurfaceOptions options);

    /**
     * <p>
     * Create a TextureSurface that can be used to render into textures. The
     * Framework will create new textures that can be retrieved by calling the
     * returned surface's {@link TextureSurface#getColorBuffer(int)} and
     * {@link TextureSurface#getDepthBuffer()}. The size and texture format of
     * the {@link Texture textures} used for the TextureSurface are
     * determined by the provided <tt>options</tt>.
     * </p>
     * <p>
     * If <tt>options</tt> is null or unsupported, the Framework is permitted to
     * choose options that allow it to create a valid TextureSurface.
     * </p>
     * 
     * @param options The requested options for configuring the created surface
     * @throws SurfaceCreationException if the TextureSurface can't be created,
     *             because dimensions were invalid for the target, the layer was
     *             invalid (if not ignored), or if they are unsupported, etc.
     * @throws RenderException if the Framework has already been destroyed
     */
	public TextureSurface createSurface(TextureSurfaceOptions options);

	/**
	 * <p>
	 * Destroy the given Surface. After a call to this method, the surface
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
	 * @param surface The Surface to destroy
	 * @throws NullPointerException if surface is null
	 * @throws IllegalArgumentException if the surface wasn't created by this
	 *             Framework
	 * @throws RenderException if this Framework has been destroyed
	 */
	public void destroy(Surface surface);

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
	 * <p>
	 * The update action should use the DirtyState instance returned by
	 * getDirtyState() at the time this method is invoked. If a subsequent
	 * update is scheduled before this one has been processed, it should return
	 * a Future linked to the same task, and the task's DirtyState should be
	 * updated to represent the union of the current dirty state and the
	 * original dirty state.
	 * </p>
	 * <p>
	 * The correctness of the dirty state merging is dependent on
	 * {@link Resource#getDirtyState()} not being called elsewhere. Otherwise,
	 * an integral piece of dirtiness may be lost and the merged states will not
	 * reflect the actual picture. To prevent 'incomplete' updates when
	 * performing complex updates to a Resource, or using multiple Frameworks
	 * simultaneously, use <tt>forceFullUpdate</tt> set to true to ensure
	 * everything is completely up-to-date.
	 * </p>
	 * 
	 * @param resource The Resource to be updated
	 * @param forceFullUpdate Whether or not the update should ignore dirty
	 *            descriptions of the resource
	 * @return A Future that can be used to determine when the update completes,
	 *         the Status held by the Future is the new status of the Resource
	 * @throws NullPointerException if resource is null
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
	 * implicitly updated when needed, a resource should only be
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
	 * @throws IllegalArgumentException if the Resource is a Texture being
	 *             used by an undestroyed TextureSurface for this Framework
	 */
	public Future<Void> dispose(Resource resource);

    /**
     * <p>
     * Queue the given Surface so that the given RenderPass will be rendered
     * during the next call to render() from this Thread. RenderSurfaces and
     * their associated passes are rendered in the order that they were queued.
     * Therefore, it's possible to render one pass into surface A, then another
     * pass into surface B, and then render something else back into A.
     * </p>
     * <p>
     * When speaking of of the Surface queue, or performing a queue operation,
     * each Thread has its own queue so that multiple calls from various Threads
     * would not contaminate a single queue and create a non-deterministic
     * ordering.
     * </p>
     * <p>
     * Destroyed surfaces from this Framework are ignored; this applies to a
     * destroyed surface at queue time and render time. This is to make it
     * easier in a multi-threaded environment when the user explicitly closes
     * a window via OS interaction.
     * </p>
     * 
     * @param surface The Surface that the given RenderPass will be rendered to
     * @param pass The RenderPass that will be rendered
     * @throws NullPointerException if surface or pass are null
     * @throws IllegalArgumentException if surface wasn't created by this
     *             Framework
     * @throws RenderException if the Framework has been destroyed
     */
	public void queue(Surface surface, RenderPass pass);

    /**
     * <p>
     * Queue the given TextureSurface so that the provided RenderPass will be
     * rendered during the next call to render() on this Thread. This performs
     * the functionally equivalent operation to
     * {@link #queue(Surface, RenderPass)} except that <tt>layer</tt> and
     * <tt>atDepth</tt> override the active layer and active depth of the
     * TextureSurface. This queue operation can be used to easily render into
     * the various depths of a 3D texture, or into the 6 faces of a cube map.
     * </p>
     * <p>
     * When a TextureSurface is passed into {@link #queue(Surface, RenderPass)},
     * the behavior is equivalent to calling
     * <code>queue(surface, surface.getActiveLayer(), surface.getActiveDepth(), pass);</code>
     * . The layer and depth specified in this queue operation must be valid for
     * the TextureSurface being rendered into
     * </p>
     * 
     * @param surface The Surface that the given RenderPass will be rendered
     *            into
     * @param layer The layer of the Textures to be rendered into, overrides
     *            {@link TextureSurface#getActiveLayer()}
     * @param atDepth The depth value that the Textures are rendered into,
     *            overrides {@link TextureSurface#getActiveDepthPlane()}
     * @param pass The RenderPass that will be rendered
     * @throws NullPointerException if surface or pass are null
     * @throws IllegalArgumentException if surface wasn't created by this
     *             Framework, or if layer and atDepth would throw exceptions if
     *             they were used as the active layer and depth for the surface
     * @throws RenderException if the Framework has been destroyed
     */
	public void queue(TextureSurface surface, int layer, int atDepth, RenderPass pass);

	/**
	 * <p>
	 * Render a single frame for the queued RenderSurfaces of the calling
	 * Thread. For each queued (surface X pass), clear the surface based on the
	 * clear parameters when the surface was queued and render the pass. Queued
	 * surfaces that have been destroyed should be ignored.
	 * </p>
	 * <p>
	 * As specified in {@link #queue(Surface, RenderPass)}, each Thread
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
	 * Perform the equivalent operations as {@link #render()}, except that this
	 * method will block until the rendering is completed and return the
	 * FrameStatistics instead of a {@link Future}.
	 * 
	 * @return FrameStatistics information about the timing of the frame
	 * @throws RenderException if the Framework has been destroyed or if any
	 *             exception occurred while rendering that prevented the frame
	 *             from completing.
	 */
	public FrameStatistics renderAndWait();

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
