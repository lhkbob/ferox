package com.ferox.scene.fx.impl;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;
import java.util.Map.Entry;

import com.ferox.math.Frustum;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.scene.Scene;
import com.ferox.scene.SceneElement;
import com.ferox.util.Bag;

public class SceneQueryCache {
	private static class Query<T> {
		private Class<? extends SceneElement> index;
		private T query;
		
		public Query(T query, Class<? extends SceneElement> index) {
			this.query = query;
			this.index = index;
		}
		
		@Override
		public boolean equals(Object o) {
			if (!(o instanceof Query))
				return false;
			Query<?> that = (Query<?>) o;
			return query == that.query && index.equals(that.index);
		}
	}
	
	private final Scene scene;
	
	private final Queue<Bag<SceneElement>> bagPool;
	private final Map<Query<BoundVolume>, Bag<SceneElement>> bvCache;
	private final Map<Query<Frustum>, Bag<SceneElement>> viewCache;
	
	// dummy objects used to access cache's to avoid object creation
	private final Query<BoundVolume> bvDummy;
	private final Query<Frustum> viewDummy;
	
	public SceneQueryCache(Scene scene) {
		if (scene == null)
			throw new NullPointerException("Must specify non-null Scene");
		this.scene = scene;
		
		bagPool = new ArrayDeque<Bag<SceneElement>>();
		bvCache = new HashMap<Query<BoundVolume>, Bag<SceneElement>>();
		viewCache = new HashMap<Query<Frustum>, Bag<SceneElement>>();
		
		bvDummy = new Query<BoundVolume>(null, null);
		viewDummy = new Query<Frustum>(null, null);
	}
	
	public void reset() {
		// reset access dummies
		bvDummy.index = null;
		bvDummy.query = null;
		
		viewDummy.index = null;
		viewDummy.query = null;
		
		// clear BoundVolume cache
		Iterator<Entry<Query<BoundVolume>, Bag<SceneElement>>> it = bvCache.entrySet().iterator();
		while(it.hasNext()) {
			bagPool.add(it.next().getValue());
			it.remove();
		}
		
		// clear Frustum cache
		Iterator<Entry<Query<Frustum>, Bag<SceneElement>>> it2 = viewCache.entrySet().iterator();
		while(it2.hasNext()) {
			bagPool.add(it2.next().getValue());
			it2.remove();
		}
	}

	public Bag<SceneElement> query(BoundVolume volume, Class<? extends SceneElement> index) {
		if (volume == null)
			throw new NullPointerException("Must query with a non-null BoundVolume");
		
		if (index == null)
			index = SceneElement.class;
		bvDummy.query = volume;
		bvDummy.index = index;
		
		Bag<SceneElement> cache = bvCache.get(bvDummy);
		if (cache == null) {
			cache = getBag();
			scene.query(volume, index, cache);
			bvCache.put(new Query<BoundVolume>(volume, index), cache);
		}
		
		return cache;
	}
	
	public Bag<SceneElement> query(Frustum frustum, Class<? extends SceneElement> index) {
		if (frustum == null)
			throw new NullPointerException("Must query with a non-null Frustum");
		
		if (index == null)
			index = SceneElement.class;
		viewDummy.query = frustum;
		viewDummy.index = index;
		
		Bag<SceneElement> cache = viewCache.get(viewDummy);
		if (cache == null) {
			cache = getBag();
			scene.query(frustum, index, cache);
			viewCache.put(new Query<Frustum>(frustum, index), cache);
		}
		
		return cache;
	}
	
	private Bag<SceneElement> getBag() {
		Bag<SceneElement> b = bagPool.poll();
		if (b == null)
			b = new Bag<SceneElement>();
		
		b.clear(true);
		return b;
	}
}
