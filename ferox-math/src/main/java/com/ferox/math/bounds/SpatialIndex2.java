package com.ferox.math.bounds;

import java.util.Iterator;

import com.ferox.math.ReadOnlyRay3f;

// FIXME: it makes more sense to have specialized index's for triangles
// while this is a general index for other objects. Triangle structures
// will want to pack the triangles in an array, and have nodes reference them
// by index.  Most likely they will not need to be live updated as well.

// This means that ray queries are not a valuable part of this interface,
// So I can remove that query, and get rid of the ugly ray types.

// I've also been thinking more heavily about the problems with entreri's shared
// properties and caching, as well as the math library. If I move the caching
// to the component instance level, the setters will be responsible for looking
// up the canonical instance and clearing its cache.

// The math library might be better to move to read-only annotations or 
// tuples or something
public interface SpatialIndex2<T extends Intersectable> extends Iterable<T> {

    public void query(Frustum f, QueryCallback<T> callback);
    
    public void query(ReadOnlyAxisAlignedBox aabb, QueryCallback<T> callback);
    
    // FIXME: this should have a special callback to provide the intersection 
    // information
    public void query(ReadOnlyRay3f ray, QueryCallback<T> callback);
    
    // FIXME: is this the right way to do things? what is the best way to
    // organize objects as well as triangles? Is it even good to use the
    // same exact implementation for those different use cases?
    
    // Should I hold onto the bounds, I will have to think how to do updates
    // without holding onto the old bounds. That requires the notebook. 
    public Object add(T obj);
    
    public void update(T obj, Object key);
    
    public void remove(Object key);
    
    public int size();
    
    public Iterator<T> iterator();
}
