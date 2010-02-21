package com.ferox.math.bounds;

import com.ferox.math.Frustum;
import com.ferox.util.Bag;

public interface SpatialHierarchy<T> {

	public Object insert(T item, BoundVolume bounds, Object key);
	
	public void remove(T item, Object key);
	
	public Bag<T> query(BoundVolume volume, Bag<T> results);
	
	public Bag<T> query(Frustum frustum, Bag<T> results);
}
