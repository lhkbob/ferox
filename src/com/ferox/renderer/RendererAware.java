package com.ferox.renderer;

/**
 * <p>
 * Generic interface that lets Renderers attach internal data objects to this
 * for future fast access, without needing to a potentially expensive Map
 * look-up.
 * </p>
 * <p>
 * It is recommended that implementations use the RenderDataCache.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface RendererAware {
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
}
