package com.ferox.renderer;

import com.ferox.resource.BufferData;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.Resource.UpdatePolicy;

/**
 * The HardwareAccessLayer is the direct interface Tasks use to access graphics
 * hardware. It has two broad capabilities: working with Resource's and the
 * hardware layer data needed to use them, and rendering into Surfaces. Each
 * Task that is run by a Framework is provided a HardwareAccessLayer to utilize
 * in whatever manner they see fit. The HardwareAccessLayer is tied to the
 * thread executing the Task so it (and the related {@link Context}) must not be
 * held onto outside of the Task's execution period.</p>
 * <p>
 * Resources can be manually updated and disposed of by using the
 * HardwareAccessLayer. Because the hardware level data for a Resource is shared
 * across all threads and surfaces in a Framework, it does not matter which task
 * group or HardwareAccessLayer instance is used to perform an update or
 * dispose, the change will by Framework-wide.
 * </p>
 * Surfaces can be rendered into by first activating them with
 * setActiveSurface(), and then using one of the Renderer's from the Context
 * associated with the active surface. In order for a user to see the actual
 * rendered content, the surface must be {@link Context#flush() flushed}.</p>
 * 
 * @see Context
 * @author Michael Ludwig
 */
public interface HardwareAccessLayer {
    /**
     * <p>
     * Set the active Surface for rendering when using this hardware access
     * layer. A Surface must be active in order to use any Renderers (which are
     * provided by the returned Context). This may be called multiple times by
     * the same Task, although only one Surface will be active for the task at a
     * given time. A Surface cannot be active on more than one thread at a time.
     * This method will block until any other running task releases the surface
     * (by returning or activating a different surface).
     * </p>
     * <p>
     * The returned Context is only valid while the specified surface is active.
     * If this is called again with any surface (including this one), the old
     * Context must be discarded and the new Context should be used.
     * </p>
     * <p>
     * The update, dispose and reset operations provided by this access layer
     * can be used whether or not a surface is active and their high-level
     * behavior does not change if a Surface is active. Resource operations can
     * be interleaved with rendering operations.
     * </p>
     * <p>
     * Calling this method with a null surface is allowed. It deactivates any
     * current surface, allowing that surface to be activated by other Tasks. A
     * null surface will return a null context. Using a null surface effectively
     * restores the hardware access layer to its initial state when the Task
     * began executing.
     * </p>
     * <p>
     * If the provided Surface is a TextureSurface, the surface will be
     * activated using its activate layer or active depth plane depending on if
     * it has a cube map or 3D texture. Other texture types do not have multiple
     * layers to select so it does not matter.
     * </p>
     * <p>
     * If the surface has its {@link Surface#destroy()} method called before
     * this it is activated, a null Context is returned. A Surface cannot be
     * destroyed until it has been deactivated. The only exception to this is if
     * the Thread running the Task calls destroy. In this case, the Surface is
     * deactivated automatically and then destroyed.
     * </p>
     * <p>
     * Generally Surfaces should not be destroyed within a Task since it can
     * have unintended consequences. As an example, an OnscreenSurface might
     * still be used to provide a Context for a TextureSurface. Destroying the
     * OnscreenSurface while the TextureSurface is active would also have to
     * deactivate the TextureSurface. Essentially, destroying the active surface
     * while in a Task has well defined consequences; destroying other surfaces
     * in a Task may or may not also deactivate the currently active surface.
     * </p>
     * 
     * @param surface The Surface to activate, or null to deactivate the current
     *            surface
     * @return A Context to use for the activation lifetime of the given
     *         Surface, or null if surface was null was or destroyed
     */
    public Context setActiveSurface(Surface surface);

    /**
     * <p>
     * Set the active surface to be the given TextureSurface, overriding the
     * surface's active layer or depth plane. This functions identically to
     * {@link #setActiveSurface(Surface)} except that it overrides the
     * TextureSurface's active layer and does not allow the surface to be null.
     * Deactivating surfaces can only be done with the first method.
     * </p>
     * <p>
     * The layer argument is interpreted differently depending on the type of
     * Texture used by the TextureSurface. If the texture is a cube map, the
     * layer overrides {@link TextureSurface#getActiveLayer()}. If the texture
     * is a 3D texture, the layer overrides
     * {@link TextureSurface#getActiveDepthPlane()}. The layer argument is
     * validated in the same manner that setting the default active layer or
     * active depth plane is. For 1D and 2D textures, it must always be 0.
     * </p>
     * <p>
     * See {@link #setActiveSurface(Surface)} for details on how destroyed
     * Surfaces or destroying a Surface within a Task behaves.
     * </p>
     * 
     * @param surface The TextureSurface to activate, cannot be null
     * @param layer The texture layer or depth plane to render into, depending
     *            on texture type of the surface
     * @return A Context to use for the activation lifetime of the given surface
     * @throws NullPointerException if surface is null or destroyed
     * @throws IllegalArgumentException if the layer is invalid
     */
    public Context setActiveSurface(TextureSurface surface, int layer);

    /**
     * <p>
     * Push all changes to the given Resource to the graphics hardware. The
     * access layer is responsible for detecting these changes. In many
     * situations this is a fast operation. The {@link BufferData} type has a
     * built in mechanism for tracking changes to the arrays that do not require
     * full scans.
     * </p>
     * <p>
     * If a resource's update policy is {@link UpdatePolicy#ON_DEMAND}, the
     * Framework will call this method as needed when the resource is used in
     * rendering. However, it is perfectly valid to invoke this method with an
     * on demand resource. Manually-updated resources must be updated with this
     * method by a Task before they can be used by a renderer.
     * <p>
     * Calling {@link #reset(Resource)} resets the Framework's last known state
     * for the resource, so that the next update pushes everything to the
     * graphics card. This can be useful in advanced resource management
     * scenarios where almost total changes are made to a resource.
     * </p>
     * 
     * @param resource The resource to update
     * @return The new status of the resource
     * @throws NullPointerException if resource is null
     */
    public <R extends Resource> Status update(R resource);

    /**
     * Dispose of all hardware level resources tied to the given Resource. The
     * Resource's status will be set to {@link Status#DISPOSED}. If the
     * resource's status was ERROR, the resource data is still cleared from the
     * graphics hardware. Nothing is done if the status was DISPOSED or
     * UNSUPPORTED (since these won't have any graphic's resource).
     * 
     * @param resource The Resource to clean up
     * @throws NullPointerException if the resource is null
     * @throws IllegalStateException if the resource is a Texture that is owned
     *             by an undestroyed TextureSurface
     */
    public <R extends Resource> void dispose(R resource);

    /**
     * Reset the internal state this Framework keeps for the given Resource, so
     * that the next call to {@link #update(Resource)} will perform a full
     * update. This does not delete or change any hardware level data associated
     * with the Resource.
     * 
     * @param resource The resource to reset
     * @throws NullPointerException if resource is null
     */
    public <R extends Resource> void reset(R resource);
}
