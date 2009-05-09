package com.ferox.resource;

import com.ferox.renderer.Renderer;

/**
 * <p>
 * Interface that represents some type of data stored on the graphics card. A
 * resource is fairly abstract so there many things can be represented (assuming
 * there is some hardware capabilities supporting it). The only paradigm is that
 * changes are made to the resource, a resource manager tells the renderer to
 * update it, and then the resource is used during the rest of the rendering.
 * </p>
 * <p>
 * If a resource is updated, used, then changed and used again before it is
 * updated, then those changes will not be visible.
 * </p>
 * <p>
 * Resources should not be Effects. The pattern to use is to have a state wrap a
 * resource, allowing for some dynamic property changes: texture wraps texture
 * image, providing environment variables.
 * </p>
 * <p>
 * Changes to variables/attributes of implementations will not be visible by the
 * Renderer until they have been updated (either by using the renderer's default
 * resource manager, or an application specific one). Related to this, a
 * resource cannot be used, and will not be implicitly updated, if it's
 * referenced by a state.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Resource {
	/**
	 * <p>
	 * Get the renderer specific data that has been assigned to this Effect.
	 * This object should not be modified unless it's by the Renderer that
	 * created it.
	 * </p>
	 * <p>
	 * Undefined behavior occurs if it's changed.
	 * </p>
	 * 
	 * @param renderer Renderer to fetch data for, will not be null
	 * @return The previously assigned data for the renderer, or null
	 */
	public Object getRenderData(Renderer renderer);
	
	/**
	 * <p>
	 * Assign the renderer specific data for this object. This should not be
	 * called directly, it is to be used by renderers to attach implementation
	 * specific information needed for successful operation.
	 * </p>
	 * <p>
	 * Undefined behavior occurs if this is set by something other than the
	 * Renderer.
	 * </p>
	 * 
	 * @param renderer Renderer to assign data to
	 * @param data Object to return from getRenderData
	 */
	public void setRenderData(Renderer renderer, Object data);
	
	/**
	 * <p>
	 * Return an object that describes what regions of the resource are dirty.
	 * Implementations should document what type of object is returned. The
	 * returned dirty descriptor must be an immutable object (according to its
	 * public interface).
	 * </p>
	 * <p>
	 * If null is returned, then Renderers should assume that the entire
	 * Resource is dirty, for lack of a better alternative. The descriptor is
	 * the minimal set of values needed to be updated. Renderers should not
	 * update less than what is described by the object.
	 * </p>
	 * 
	 * @return Implementations specific object describing what parts of the
	 *         Resource are dirty
	 */
	public Object getDirtyDescriptor();

	/**
	 * Should only be called by renderer implementations when an update is
	 * completed and the resource is no longer deemed dirty from the renderer's
	 * point of view.
	 */
	public void clearDirtyDescriptor();

	/**
	 * Each resource will have a status with the active renderer. A Resource is
	 * usable if it has a status of OK or DIRTY. It is likely to be ignored by
	 * ERROR or CLEANED (or some other Renderer dependent way of handling
	 * ignored resources).
	 */
	public static enum Status {
		/** The resource has been updated successfully and is ready to use. */
		OK,
		/**
		 * The renderer couldn't support the resource as is, but it was able to
		 * change it so that it can function as if it were OK.
		 */
		DIRTY,
		/**
		 * The renderer has tried to update the resource and there may be
		 * internal data for the resource, but something is wrong and the
		 * resource isn't usable.
		 */
		ERROR,
		/**
		 * The renderer has no internal representations of the resource (never
		 * updated, or it's been cleaned.
		 */
		CLEANED
	}
}
