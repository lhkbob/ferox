package com.ferox.renderer.util;

import java.lang.ref.WeakReference;
import java.util.WeakHashMap;

import com.ferox.renderer.RenderQueue;

/** 
 * Utility class implementing fast caching for the RenderAtom and influenceAtom
 * RenderQueue data methods.
 * 
 * @author Michael Ludwig
 *
 */
public class RenderQueueDataCache {
	private static WeakReference<RenderQueue> frequentContext = null;
	private static int frequentId = -1;
	
	private static WeakHashMap<RenderQueue, Integer> idMap = new WeakHashMap<RenderQueue, Integer>();
	private static int idCounter = 0;
	
	private Object[] renderQueueData;
	
	public RenderQueueDataCache() {
		this.renderQueueData = new Object[0];
	}
	
	/** Set the RenderQueue specific data for this RenderQueueDataCache, overwriting
	 * any previous value.
	 * 
	 * Does nothing if the RenderQueue is null. */
	public void setRenderQueueData(RenderQueue RenderQueue, Object data) {
		if (RenderQueue == null)
			return;
		
		int id = getId(RenderQueue);
		if (id >= this.renderQueueData.length) {
			Object[] temp = new Object[id + 1];
			System.arraycopy(this.renderQueueData, 0, temp, 0, this.renderQueueData.length);
			this.renderQueueData = temp;
		}
		this.renderQueueData[id] = data;
	}
	
	/** Get the RenderQueue specific data for this RenderQueueDataCache
	 * Returns null if the RenderQueue never set any data, or if it set null.
	 * 
	 * Returns null if the RenderQueue is null. */
	public Object getRenderQueueData(RenderQueue RenderQueue) {
		if (RenderQueue == null)
			return null;
		
		int id = getId(RenderQueue);
		if (id >= this.renderQueueData.length)
			return null;
		return this.renderQueueData[id];
	}
	
	// internally manage and retrieve RenderQueue ids, assumes RenderQueue isn't null
	private static int getId(RenderQueue renderQueue) {
		if (frequentContext != null && frequentContext.get() == renderQueue)
			return frequentId;
		
		Integer id = idMap.get(renderQueue);
		if (id == null) {
			id = idCounter++;
			idMap.put(renderQueue, id);
		}
		
		frequentContext = new WeakReference<RenderQueue>(renderQueue);
		frequentId = id;
		return id;
	}
}
