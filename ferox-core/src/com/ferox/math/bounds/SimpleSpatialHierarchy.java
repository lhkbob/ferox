package com.ferox.math.bounds;

import com.ferox.math.bounds.Frustum.FrustumIntersection;
import com.ferox.util.Bag;

public class SimpleSpatialHierarchy<T> implements SpatialHierarchy<T> {
	private final Bag<SimpleKey<T>> elements;
	
	public SimpleSpatialHierarchy() {
		elements = new Bag<SimpleKey<T>>();
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public Object insert(T item, AxisAlignedBox bounds, Object key) {
		if (item == null)
			throw new NullPointerException("Item cannot be null");
		
		if (key instanceof SimpleKey) {
			SimpleKey sk = (SimpleKey) key;
			if (sk.owner == this) {
				 // key is valid and we already own it, update bounds
				sk.bounds = bounds;
				return sk;
			}
			// else bad key, fall through to perform an insert
		}
		
		int sz = elements.size();
		for (int i = 0; i < sz; i++) {
			if (elements.get(i).data == item) {
				// item already in hierarchy, update bounds
				elements.get(i).bounds = bounds;
				return elements.get(i); 
			}
		}
		
		// item is not in the hierarchy, insert a new key
		SimpleKey<T> newKey = new SimpleKey<T>(this);
		newKey.data = item;
		newKey.index = sz;
		newKey.bounds = bounds;
		
		elements.add(newKey);
		return newKey;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public void remove(T item, Object key) {
		if (item == null)
			throw new NullPointerException("Item cannot be null");
		
		if (key instanceof SimpleKey) {
			// remove quickly based on the key
			SimpleKey sk = (SimpleKey) key;
			if (sk.owner == this) {
				removeKey((SimpleKey<T>) sk);
				return;
			}
			// else bad key, fall through to perform a linear search
		}
		
		int sz = elements.size();
		for (int i = 0; i < sz; i++) {
			if (elements.get(i).data == item) {
				removeKey(elements.get(i));
				return;
			}
		}
		
		// item is not within the hierarchy
	}
	
	private void removeKey(SimpleKey<T> key) {
		elements.remove(key.index);
		if (key.index != elements.size()) {
			// update index of swapped item
			elements.get(key.index).index = key.index;
		}
	}

	@Override
	public Bag<T> query(AxisAlignedBox volume, Bag<T> results) {
		if (volume == null)
			throw new NullPointerException("Query bound volume cannot be null");
		
		if (results == null)
			results = new Bag<T>();
		
		SimpleKey<T> key;
		int sz = elements.size();
		for (int i = 0; i < sz; i++) {
			key = elements.get(i);
			if (key.bounds == null || key.bounds.intersects(volume))
				results.add(key.data);
		}

		return results;
	}

	@Override
	public Bag<T> query(Frustum frustum, Bag<T> results) {
		if (frustum == null)
			throw new NullPointerException("Query Frustum cannot be null");
		
		if (results == null)
			results = new Bag<T>();
		
		SimpleKey<T> key;
		int sz = elements.size();
		for (int i = 0; i < sz; i++) {
			key = elements.get(i);
			if (key.bounds == null || key.bounds.intersects(frustum, null) != FrustumIntersection.OUTSIDE)
				results.add(key.data);
		}

		return results;
	}

	private static class SimpleKey<T> {
		private T data;
		private AxisAlignedBox bounds;
		
		private int index;
		private final SimpleSpatialHierarchy<T> owner;
		
		public SimpleKey(SimpleSpatialHierarchy<T> owner) {
			this.owner = owner;
		}
	}
}
