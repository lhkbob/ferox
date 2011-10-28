package com.ferox.physics.dynamics.constraint;

import com.ferox.physics.dynamics.RigidBody;

public class SolverBody {
    public float dlX, dlY, dlZ; // delta linear velocity
    public float daX, daY, daZ; // delta angular velocity
    
    public float inverseMass; // cached inverse mass from rigid body
    public RigidBody body;
}
