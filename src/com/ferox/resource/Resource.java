package com.ferox.resource;

import com.ferox.renderer.RendererAware;

/**
 * Interface that represents some type of data stored on the graphics card. A
 * resource is fairly abstract so there many things can be represented (assuming
 * there is some hardware capabilities supporting it). The only paradigm is that
 * changes are made to the resource, a resource manager tells the renderer to
 * update it, and then the resource is used during the rest of the rendering.
 * 
 * If a resource is updated, used, then changed and used again before it is
 * updated, then those changes will not be visible.
 * 
 * Resources should not be States. The pattern to use is to have a state wrap a
 * resource, allowing for some dynamic property changes: texture wraps texture
 * image, providing environment variables.
 * 
 * Changes to variables/attributes of implementations will not be visible by the
 * Renderer until they have been updated (either by using the renderer's default
 * resource manager, or an application specific one). Related to this, a
 * resource cannot be used, and will not be implicitly updated, if it's
 * referenced by a state.
 * 
 * Ex: if an Appearance has a [Multi]Texture state, all of the associated
 * texture images must have been updated. If not, those texture units will
 * behave as if there is no texture bound.
 * 
 * @author Michael Ludwig
 * 
 */
public interface Resource extends RendererAware {
	/**
	 * Return an object that describes what regions of the resource are dirty.
	 * Implementations should document what type of object is returned. The
	 * returned dirty descriptor must be an immutable object (according to its
	 * public interface).
	 * 
	 * If null is returned, then Renderers should assume that the entire
	 * Resource is dirty, for lack of a better alternative. The descriptor is
	 * the minimal set of values needed to be updated. Renderers should not
	 * update less than what is described by the object.
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
