package com.ferox.math.bounds;

import com.ferox.math.ReadOnlyRay3f;

public interface Intersectable {
    public boolean intersects(ReadOnlyAxisAlignedBox aabb);
    
    public boolean intersects(Frustum f);
    
    public float intersects(ReadOnlyRay3f r);
    
    public ReadOnlyAxisAlignedBox getBounds();
}
