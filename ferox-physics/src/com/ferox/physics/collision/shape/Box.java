package com.ferox.physics.collision.shape;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.physics.collision.ConvexShape;

public class Box implements ConvexShape {
    private final Vector3f halfExtents;
    private final AxisAlignedBox aabb;
    
    public Box(float xExtent, float yExtent, float zExtent) {
        halfExtents = new Vector3f(xExtent / 2f, yExtent / 2f, zExtent / 2f);
        aabb = new AxisAlignedBox();
        aabb.getMin().set(halfExtents).scale(-1f);
        aabb.getMax().set(halfExtents);
    }
    
    @Override
    public Vector3f computeSupport(ReadOnlyVector3f v, Vector3f result) {
        float x = (v.getX() < 0f ? -halfExtents.getX() : halfExtents.getX());
        float y = (v.getY() < 0f ? -halfExtents.getY() : halfExtents.getY());
        float z = (v.getZ() < 0f ? -halfExtents.getZ() : halfExtents.getZ());
        
        if (result == null)
            result = new Vector3f();
        return result.set(x, y, z);
    }

    @Override
    public AxisAlignedBox getBounds() {
        return aabb;
    }
}
