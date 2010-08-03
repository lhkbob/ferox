package com.ferox.physics.dynamics;

import com.ferox.math.Matrix3f;
import com.ferox.math.Quat4f;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.Collidable;

public class RigidBody extends Collidable {
    private final Vector3f velocity;
    private final Quat4f angularVelocity;
    
    private float inverseMass; // 1 / mass, or <= 0 for static objects
    private final Matrix3f inertiaTensor;
    
    private Vector3f explicitGravity;
    
    private final Vector3f accumulatedImpulse;
    private final Vector3f accumulatedAngularyImpulse;
    
    public RigidBody() {
        
    }
}
