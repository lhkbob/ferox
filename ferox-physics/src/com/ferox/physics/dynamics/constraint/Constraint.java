package com.ferox.physics.dynamics.constraint;

import com.ferox.physics.dynamics.RigidBody;

public interface Constraint {
    public RigidBody getBodyA();
    
    public RigidBody getBodyB();
    
    public void solve(float dt);
}
