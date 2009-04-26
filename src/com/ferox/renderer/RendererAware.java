package com.ferox.renderer;

/** Generic interface that lets Renderers attach internal data
 * objects to this for future fast access, without needing to
 * a potentially expensive Map look-up.
 * 
 * It is recommended that implementations use the RenderDataCache.
 * 
 * @author Michael Ludwig
 *
 */
public interface RendererAware {
	/**
	 * Get the renderer specific data that has been assigned to this Effect.
	 * This object should not be modified unless it's by the Renderer that
	 * created it.
	 * 
	 * Undefined behavior occurs if it's changed.
	 * 
	 * @param renderer Renderer to fetch data for, will not be null
	 * @return The previously assigned data for the renderer, or null
	 */
	public Object getRenderData(Renderer renderer);

	/**
	 * Assign the renderer specific data for this object. This should not be
	 * called directly, it is to be used by renderers to attach implementation
	 * specific information needed for successful operation.
	 * 
	 * Undefined behavior occurs if this is set by something other than the
	 * Renderer.
	 * 
	 * @param renderer Renderer to assign data to
	 * @param data Object to return from getRenderData
	 */
	public void setRenderData(Renderer renderer, Object data);
}
