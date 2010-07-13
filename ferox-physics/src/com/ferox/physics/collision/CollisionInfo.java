package com.ferox.physics.collision;

import com.ferox.math.Vector3f;

public class CollisionInfo {
    private final Collidable objA;
    private final Collidable objB;
    
    // FIXME: make these world space coordinates
    private final Vector3f contactNormalInA;
    private final Vector3f closestPointInA;
    private final float distance;
    
    public CollisionInfo(Collidable objA, Collidable objB) {
        
    }
    
    public Vector3f getContactNormal() {
        return contactNormalInA;
    }
    
    public Vector3f getClosestPointInA() {
        return closestPointInA;
    }
    
    public Vector3f getClosestPointInB() {
        // FIXME transform from A local space to B space
        Vector3f bInA = contactNormalInA.scaleAdd(distance, closestPointInA, null);
        return bInA;
    }
    
    public void recordPoint(Vector3f normalInWorld, Vector3f pointInWorld, float distance) {
        if (distance < this.distance) {
            // FIXME: update storage
        }
    }
}
