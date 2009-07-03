package com.ferox.resource;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import com.ferox.renderer.Framework;

/**
 * Utility class implementing fast caching for the setRenderData() in Effect,
 * Resource, and Geometry. It is recommended that this is used by all
 * implementations.
 * 
 * @author Michael Ludwig
 */
public class RenderDataCache {
	private static WeakReference<Framework> frequentContext = null;
	private static int frequentId = -1;

	private static WeakHashMap<Framework, Integer> idMap = new WeakHashMap<Framework, Integer>();
	private static int idCounter = 0;

	private Object[] renderData;

	public RenderDataCache() {
		renderData = new Object[0];
	}

	/**
	 * <p>
	 * Set the Framework specific data for this RenderDataCache, overwriting any
	 * previous value.
	 * </p>
	 * <p>
	 * Does nothing if the Framework is null.
	 * </p>
	 * 
	 * @param render The Framework key that data is associated with
	 * @param data The new object assigned to render
	 */
	public void setRenderData(Framework render, Object data) {
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
	 * Get the Framework specific data for this RenderDataCache Returns null if
	 * the Framework never set any data, or if it set null.
	 * </p>
	 * <p>
	 * Returns null if the Framework is null.
	 * </p>
	 * 
	 * @param render The Framework to request its assigned data object
	 * @return Object cached for render
	 */
	public Object getRenderData(Framework render) {
		if (render == null)
			return null;

		int id = getId(render);
		if (id >= renderData.length)
			return null;
		return renderData[id];
	}

	// internally manage and retrieve Framework ids, assumes Framework isn't
	// null
	private static int getId(Framework renderer) {
		if (frequentContext != null && frequentContext.get() == renderer)
			return frequentId;

		Integer id = idMap.get(renderer);
		if (id == null) {
			id = idCounter++;
			idMap.put(renderer, id);
		}

		frequentContext = new WeakReference<Framework>(renderer);
		frequentId = id;
		return id;
	}
}
