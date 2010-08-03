package com.ferox.physics.collision;

import com.ferox.math.ReadOnlyVector3f;

public class ClosestPair {
    private final ReadOnlyVector3f contactNormalFromA;
    private final ReadOnlyVector3f closestPointOnA;
    private final ReadOnlyVector3f closestPointOnB;

    private final float distance;
    
    public ClosestPair(ReadOnlyVector3f pointOnA, ReadOnlyVector3f contactNormal, float distance) {
        // FIXME: validate
        this.distance = distance;

        contactNormalFromA = contactNormal;
        closestPointOnA = pointOnA;
        closestPointOnB = contactNormal.scaleAdd(distance, pointOnA, null);
    }
    
    public ReadOnlyVector3f getContactNormal() {
        return contactNormalFromA;
    }
    
    public ReadOnlyVector3f getClosestPointOnA() {
        return closestPointOnA;
    }
    
    public ReadOnlyVector3f getClosestPointOnB() {
        return closestPointOnB;
    }
    
    public float getDistance() {
        return distance;
    }
    
    public boolean isIntersecting() {
        return distance <= 0f;
    }
}
