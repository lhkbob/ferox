package com.ferox.physics.collision.shape;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public class Box extends ConvexShape {
    private final Vector3f localTensorPartial;
    private final Vector3f halfExtents;
    
    public Box(float xExtent, float yExtent, float zExtent) {
        localTensorPartial = new Vector3f();
        halfExtents = new Vector3f();
        
        setExtents(xExtent, yExtent, zExtent);
    }
    
    public ReadOnlyVector3f getHalfExtents() {
        return halfExtents;
    }
    
    public ReadOnlyVector3f getExtents() {
        return halfExtents.scale(2f, null);
    }
    
    public void setExtents(float width, float height, float depth) {
        if (width <= 0)
            throw new IllegalArgumentException("Invalid width, must be greater than 0, not: " + width);
        if (height <= 0)
            throw new IllegalArgumentException("Invalid height, must be greater than 0, not: " + height);
        if (depth <= 0)
            throw new IllegalArgumentException("Invalid depth, must be greater than 0, not: " + depth);
        
        halfExtents.set(width / 2f, height / 2f, depth / 2f);
        localTensorPartial.set((height * height + depth * depth) / 12f,
                               (width * width + depth * depth) / 12f,
                               (width * width + height * height) / 12f);
        updateBounds();
    }
    
    public float getWidth() {
        return halfExtents.getX() * 2f;
    }
    
    public float getHeight() {
        return halfExtents.getY() * 2f;
    }
    
    public float getDepth() {
        return halfExtents.getZ() * 2f;
    }
    
    @Override
    public MutableVector3f computeSupport(ReadOnlyVector3f v, MutableVector3f result) {
        if (result == null)
            result = new Vector3f();
        
        float x = (v.getX() < 0f ? -halfExtents.getX() : halfExtents.getX());
        float y = (v.getY() < 0f ? -halfExtents.getY() : halfExtents.getY());
        float z = (v.getZ() < 0f ? -halfExtents.getZ() : halfExtents.getZ());
        
        return result.set(x, y, z);
    }

    @Override
    public MutableVector3f getInertiaTensor(float mass, MutableVector3f result) {
        return localTensorPartial.scale(mass, result);
    }
}