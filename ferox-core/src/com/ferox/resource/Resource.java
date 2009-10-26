package com.ferox.resource;

import com.ferox.renderer.Framework;

/**
 * <p>
 * Interface that represents some type of data stored on the graphics card. A
 * resource is fairly abstract so there many things can be represented (assuming
 * there are hardware capabilities supporting it). Some examples include
 * TextureImage, Geometry, and GlslProgram.
 * </p>
 * <p>
 * There are multiple ways that a Resource can be managed with a Framework. A
 * Resource cannot be used until its been updated by a Framework. There are
 * multiple ways that a Resource can be updated, some of which are automatic:
 * <ol>
 * <li>Implement a ResourceManager to call update() and cleanUp() with the
 * necessary Resources and a Renderer from the Framework</li>
 * <li>Use a Framework's requestUpdate() and requestCleanUp() methods to
 * schedule an update/cleanup for the next frame</li>
 * <li>Rely on the Framework automatically updating a Resource if it's never
 * seen the Resource before, or if the Resource has a non-null dirty descriptor</li>
 * </ol>
 * Although the Resource can be automatically updated by a Framework, it must be
 * manually cleaned-up. A Framework that's destroyed will have any remaining
 * Resource's internal data cleaned up, too.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface Resource {
	/**
	 * <p>
	 * Get the renderer specific data that has been assigned to this Effect.
	 * This object should not be modified unless it's by the Framework that
	 * created it.
	 * </p>
	 * <p>
	 * Undefined behavior occurs if it's changed.
	 * </p>
	 * 
	 * @param renderer Framework to fetch data for, will not be null
	 * @return The previously assigned data for the renderer, or null
	 */
	public Object getRenderData(Framework renderer);

	/**
	 * <p>
	 * Assign the renderer specific data for this object. This should not be
	 * called directly, it is to be used by renderers to attach implementation
	 * specific information needed for successful operation.
	 * </p>
	 * <p>
	 * Undefined behavior occurs if this is set by something other than the
	 * Framework.
	 * </p>
	 * 
	 * @param renderer Framework to assign data to
	 * @param data Object to return from getRenderData
	 */
	public void setRenderData(Framework renderer, Object data);

	/**
	 * <p>
	 * Return an object that describes what regions of the Resource are dirty.
	 * When this returns a non-null instance, and the Resource is used in a
	 * frame, then the Framework should automatically update the Resource based
	 * on the returned dirty descriptor. If null is returned, then this Resource
	 * has not be modified (or marked as modified).
	 * </p>
	 * <p>
	 * Implementations should document what type of object is returned, and
	 * override the return type, too. The returned dirty descriptor must be an
	 * immutable object (according to its public interface).
	 * </p>
	 * <p>
	 * Because there is only one dirty descriptor per Resource, a
	 * ResourceManager is generally required when using multiple Frameworks.
	 * </p>
	 * <p>
	 * The descriptor is the minimal set of values needed to be updated.
	 * Renderers should not update less than what is described by the object. If
	 * a Resource is manually updated and it's descriptor is null, the entire
	 * Resource should be updated entirely, for lack of a better alternative.
	 * </p>
	 * 
	 * @return Implementations specific object describing what parts of the
	 *         Resource are dirty
	 */
	public Object getDirtyDescriptor();

	/**
	 * <p>
	 * Should only be called by Renderer implementations when an update is
	 * completed and the Resource is no longer deemed dirty from the Renderer's
	 * point of view.
	 * </p>
	 * <p>
	 * After a call to this, getDirtyDescriptor() should return null.
	 * </p>
	 */
	public void clearDirtyDescriptor();

	/**
	 * Each resource will have a status with the active renderer. A Resource is
	 * usable if it has a status of READY. Resources that are CLEANED will be
	 * auto-updated when used. A Resource that has a status of ERROR is unusable
	 * until it's been repaired.
	 */
	public static enum Status {
		/** The resource has been updated successfully and is ready to use. */
		READY,
		/**
		 * The Framework has tried to update the resource and there may be
		 * internal data for the Resource, but something is wrong and the
		 * Resource isn't usable.
		 */
		ERROR,
		/**
		 * The Framework has no internal representations of the Resource (never
		 * updated, or it's been cleaned).
		 */
		CLEANED
	}
}
