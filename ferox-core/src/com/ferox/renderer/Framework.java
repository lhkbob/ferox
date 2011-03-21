package com.ferox.renderer;

import java.util.concurrent.Future;

import com.ferox.resource.Resource;
import com.ferox.resource.Texture;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Resource.UpdatePolicy;

/**
 * <p>
 * The Framework is the core component for rendering with Ferox. It controls the
 * creation of {@link Surface surfaces}, which store the final render outputs,
 * and provides {@link Context} implementations that allow actual use of
 * renderers and resources. A Framework acts as an advanced task execution
 * service that queues up {@link Task tasks} to run on internal threads that
 * have usable {@link Context contexts}. Most low-level graphics languages have
 * the concept of a context, where a thread needs an active context to be able
 * to communicate with the graphics hardware. The context provided by a
 * Framework are on a much higher level but have a similar scope in usability;
 * they only function on threads managed by the Framework that control the
 * low-level driver access.
 * </p>
 * <p>
 * A very important part of using a Framework is resource management. In the
 * simplest cases, no explicit management is needed. All Frameworks are required
 * to automatically clean up internal resource data when a resource is garbage
 * collected. Because resources start with an update policy of
 * {@link UpdatePolicy#ON_DEMAND ON_DEMAND}, resources will be automatically
 * updated as renderers use them. If more explicit control is needed by an
 * application, a resource can use the MANUAL update policy and tasks can be
 * queued that invoke {@link Context#update(Resource)} and
 * {@link Context#dispose(Resource)} as needed.
 * </p>
 * <p>
 * Another important part is using a Framework to create concrete Surface
 * objects. Surfaces can be onscreen windows or surfaces that render into
 * {@link Texture textures}. Surfaces are created with
 * {@link #createSurface(OnscreenSurfaceOptions)} and
 * {@link #createSurface(TextureSurfaceOptions)}. A computer's hardware may not
 * support TextureSurfaces, in which case the creation will fail. Surfaces can
 * be destroyed with {@link Surface#destroy()} or by destroying the Framework
 * that created them.
 * </p>
 * <p>
 * Framework implementations must be thread safe so they can be used by multiple
 * threads at the same time. All methods are intended to perform no effective
 * operation if they are called on a destroyed Framework. It is invalid behavior
 * to destroy a Framework from within a Task. See {@link Context} for more
 * details on what interaction is allowed by executing tasks.
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
     * @return The created surface, or null if the Framework has been destroyed
     * @throws SurfaceCreationException if the Framework cannot create the
     *             WindowSurface
     */
    public OnscreenSurface createSurface(OnscreenSurfaceOptions options);

    /**
     * <p>
     * Create a TextureSurface that can be used to render into textures. The
     * Framework will create new textures that can be retrieved by calling the
     * returned surface's {@link TextureSurface#getColorBuffer(int)} and
     * {@link TextureSurface#getDepthBuffer()}. The size and texture format of
     * the {@link Texture textures} used for the TextureSurface are determined
     * by the provided <tt>options</tt>.
     * </p>
     * <p>
     * If <tt>options</tt> is null or unsupported, the Framework is permitted to
     * choose options that allow it to create a valid TextureSurface.
     * </p>
     * 
     * @param options The requested options for configuring the created surface
     * @return The created surface, or null if the Framework has been destroyed
     * @throws SurfaceCreationException if the TextureSurface can't be created,
     *             because dimensions were invalid for the target, the layer was
     *             invalid (if not ignored), or if they are unsupported, etc.
     */
    public TextureSurface createSurface(TextureSurfaceOptions options);

    /**
     * <p>
     * Destroy all remaining Surfaces and cancel any pending Tasks. Any running
     * tasks are also canceled and interrupted. This will block until any
     * running tasks that did not respond to interruption have completed.
     * </p>
     * <p>
     * All internal resource data will be disposed of so it is not necessary to
     * manually dispose of every resource before calling this method.
     * </p>
     * <p>
     * This method will block until the Framework has been completely destroyed.
     * The Framework will act 'destroyed' to any other thread the moment this
     * method is invoked, however. Calling destroy() on an already destroyed
     * Framework does nothing.
     * </p>
     */
    public void destroy();
    
    /**
     * @return True if the Framework has been destroyed
     */
    public boolean isDestroyed();

    /**
     * <p>
     * Queue the given Task to be run as soon as possible by internal threads
     * managed by the Framework. The queued task will be part of the specified
     * task 'group' given by the string, <tt>group</tt>. All tasks in a group
     * will be guaranteed to run in the order queued (although if multiple
     * threads are queuing to the same group, there may not be an guaranteed
     * order anyway). Tasks in separate groups can run in parallel depending on
     * the number of threads used. It is likely, though not required, that tasks
     * in the same group will run on the same thread.
     * </p>
     * <p>
     * It is also possible for tasks with different groups to be interleaved and
     * run on the same thread. As an example, one thread could queue two tasks
     * with group 'A' and another thread could queue tasks as group 'B'. The
     * Framework could run these as [A1, A2, B1] or [A1, B1, A2] or [B1, A1,
     * A2]. A and B tasks can be interleaved on the same thread or in parallel.
     * Only tasks with the same group have any guarantee.
     * </p>
     * <p>
     * Generally, all operations needed for a frame of rendering will use a
     * single task group. Advanced applications may be able to parallelize their
     * rendering logic and synchronize at key points. It is strongly recommended
     * for Framework implementations to run tasks from different groups on
     * separate threads to allow for parallelism.
     * </p>
     * <p>
     * Of course, tasks in one group may block another group. Two tasks are not
     * allowed to use the same surface at the same time, and two tasks cannot
     * update or dispose of a resource while it is being used by another task.
     * In these cases, on thread will block until the other one releases the
     * surface or resource.
     * </p>
     * <p>
     * If the Framework is destroyed before a Task has started, its returned
     * Future will be canceled. Calls to this method are ignored if the
     * Framework is already destroyed.
     * </p>
     * 
     * @param <T> The return type of the Task
     * @param task The task to run with a valid context
     * @param group The task group this task is part of
     * @return A future linked to the given task
     * @throws NullPointerException if the task or group are null
     */
    public <T> Future<T> queue(Task<T> task, String group);

    /**
     * <p>
     * Convenience method to queue a Task that will update the given resource by
     * calling {@link Context#update(Resource)}. To simplify the use of this
     * method, the task group 'resource' is used. It is not recommended to use
     * this method if resources are being updated and disposed of with custom
     * Tasks.
     * </p>
     * <p>
     * This method does nothing if the Framework has been destroyed. Since the
     * Framework's destruction implies no resources can be used, this is not a
     * problem. This will unblock and return if the Framework is destroyed while
     * waiting for the update to complete.
     * </p>
     * 
     * @param resource The resource to update
     * @return The new Status for the resource, as would be returned by
     *         {@link #getStatus(Resource)}
     * @throws NullPointerException if resource is null
     */
    public Status update(Resource resource);

    /**
     * <p>
     * Convenience method to queue a Task that will dispose the given resource
     * by calling {@link Context#dispose(Resource)}. To simplify the use of this
     * method, the task group 'resource' is used. It is not recommended to use
     * this method if resources are being updated and disposed of with custom
     * Tasks.
     * </p>
     * <p>
     * This method does nothing if the Framework has been destroyed. Since the
     * Framework's destruction disposes any resource data anyway, this is not a
     * problem. This will unblock and return if the Framework is destroyed while
     * waiting for the dispose to complete.
     * </p>
     * 
     * @param resource The resource to dispose
     * @throws NullPointerException if resource is null
     */
    public void dispose(Resource resource);

    /**
     * <p>
     * Convenience method to queue a Task with the given group that will flush
     * the provided surface. If the flush is not being performed by already
     * queued tasks, this is needed to ensure that any rendering is made visible
     * to the surface. If the surface is a TextureSurface with multiple render
     * targets, all layers are flushed by this method. If finer control is
     * needed, a custom task will need to be queued instead. This method will
     * block until the flush has been completed, so it also acts as a
     * {@link #sync(String) sync} for the given group.
     * </p>
     * <p>
     * An exception is thrown if the surface is not owned by the Framework,
     * however. If the provided surface has been destroyed, this method will do
     * nothing.It is not best practice to queue or use surfaces that have been
     * destroyed, but this behavior is done to play nicely with onscreen
     * surfaces that can be closed by the user. If the Framework is destroyed,
     * this will do nothing and return immediately. This will also return as a
     * soon as a Framework is destroyed if this was actively blocking.
     * </p>
     * 
     * @param surface The surface to flush
     * @param group The task group to use for the generated task
     * @throws NullPointerException if surface or group are null
     * @throws IllegalArgumentException if the surface was not created by this
     *             Framework
     */
    public void flush(Surface surface, String group);

    /**
     * <p>
     * Synchronize the calling thread with the given group. This will block
     * until all queued tasks for the given group have completed. Tasks queued
     * to the group from other threads will not run until after sync() as
     * returned. See {@link #queue(Task, String)} for details about task groups.
     * </p>
     * <p>
     * If the Framework is destroyed, this will return immediately. This will
     * also return as soon as a Framework is destroyed if this was actively
     * blocking.
     * </p>
     * 
     * @param group The group to sync to
     * @throws NullPointerException if group is null
     */
    public void sync(String group);

    /**
     * <p>
     * Get the current status of the given resource. Return UNSUPPORTED if the
     * Framework cannot support the given Resource type. If the resource's
     * update policy is ON_DEMAND, this will NOT automatically update the
     * resource if needed. The automatic updates only occur when the resource is
     * used by a Renderer.
     * </p>
     * <p>
     * If ERROR is returned, the resource will be ignored when rendering. This
     * will also return DISPOSED if the Framework has been destroyed.
     * </p>
     * 
     * @param resource The Resource whose status is requested
     * @return The Status of resource
     * @throws NullPointerException if resource is null
     */
    public Status getStatus(Resource resource);

    /**
     * Get a Framework status message that is more informative about the given
     * resources's status. A null message is returned if the Framework has been
     * destroyed. If a Framework is not destroyed but has no message, the empty
     * string should be returned so that the null return value is unique.
     * 
     * @param resource The Resource whose status message is requested
     * @return The status message for resource
     * @throws NullPointerException if resource is null
     * @throws RenderException if the Framework has been destroyed
     */
    public String getStatusMessage(Resource resource);

    /**
     * Get the capabilities of this Framework. This is allowed to return null
     * after the Framework is destroyed although Frameworks might not behave
     * this way.
     * 
     * @return The RenderCapabilities for this Framework
     */
    public RenderCapabilities getCapabilities();
}
