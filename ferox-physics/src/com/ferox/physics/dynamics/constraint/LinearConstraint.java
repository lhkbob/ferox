package com.ferox.physics.dynamics.constraint;

import com.ferox.math.Vector3f;
import com.ferox.physics.dynamics.RigidBody;

public class LinearConstraint {
    public final Vector3f relposACrossNormal;
    public final Vector3f relposBCrossNormal;
    
    public final Vector3f constraintNormal;
    
    public final Vector3f angularComponentA;
    public final Vector3f angularComponentB;
    
    public float friction; // this is only needed for friction constraints with dynamic upper/lower limit recomputation per-step
    public float jacobianDiagInverse;
    
    public RigidBody bodyA;
    public RigidBody bodyB;
    
    public float rhs; // right-hand-side of differential equation being solved
    public float cfm; // constraint force mixing for soft constraints
    public float lowerLimit;
    public float upperLimit;
    
    public float appliedImpulse;
    
    public LinearConstraint() {
        relposACrossNormal = new Vector3f();
        relposBCrossNormal = new Vector3f();
        constraintNormal = new Vector3f();
        
        angularComponentA = new Vector3f();
        angularComponentB = new Vector3f();
    }
}
