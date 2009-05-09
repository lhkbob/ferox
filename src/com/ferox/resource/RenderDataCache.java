package com.ferox.resource;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import com.ferox.renderer.Renderer;

/**
 * Utility class implementing fast caching for the setRenderData() in Effect,
 * Resource, and Geometry. It is recommended that this is used by all
 * implementations.
 * 
 * @author Michael Ludwig
 */
public class RenderDataCache {
	private static WeakReference<Renderer> frequentContext = null;
	private static int frequentId = -1;

	private static WeakHashMap<Renderer, Integer> idMap = new WeakHashMap<Renderer, Integer>();
	private static int idCounter = 0;

	private Object[] renderData;

	public RenderDataCache() {
		renderData = new Object[0];
	}

	/**
	 * <p>
	 * Set the Renderer specific data for this RenderDataCache, overwriting any
	 * previous value.
	 * </p>
	 * <p>
	 * Does nothing if the Renderer is null.
	 * </p>
	 * 
	 * @param render The Renderer key that data is associated with
	 * @param data The new object assigned to render
	 */
	public void setRenderData(Renderer render, Object data) {
		if (render == null)
			return;

		int id = getId(render);
		if (id >= renderData.length) {
			Object[] temp = new Object[id + 1];
			System.arraycopy(renderData, 0, temp, 0, renderData.length);
			renderData = temp;
		}
		renderData[id] = data;
	}

	/**
	 * <p>
	 * Get the Renderer specific data for this RenderDataCache Returns null if
	 * the Renderer never set any data, or if it set null.
	 * </p>
	 * <p>
	 * Returns null if the Renderer is null.
	 * </p>
	 * 
	 * @param render The Renderer to request its assigned data object
	 * @return Object cached for render
	 */
	public Object getRenderData(Renderer render) {
		if (render == null)
			return null;

		int id = getId(render);
		if (id >= renderData.length)
			return null;
		return renderData[id];
	}

	// internally manage and retrieve Renderer ids, assumes Renderer isn't null
	private static int getId(Renderer renderer) {
		if (frequentContext != null && frequentContext.get() == renderer)
			return frequentId;

		Integer id = idMap.get(renderer);
		if (id == null) {
			id = idCounter++;
			idMap.put(renderer, id);
		}

		frequentContext = new WeakReference<Renderer>(renderer);
		frequentId = id;
		return id;
	}
}
