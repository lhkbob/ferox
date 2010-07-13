package com.ferox.physics.dynamics;

import javax.vecmath.Quat4f;

import com.ferox.math.Vector3f;
import com.ferox.physics.collision.Collidable;
import com.ferox.util.Bag;

public class RigidBody extends Collidable {
    private final Vector3f velocity;
    private final Quat4f angularVelocity;
    
    private float inverseMass; // 1 / mass, or <= 0 for static objects
    
    // FIXME: in bullet, each constraint is a pair and they're stored
    // at a global level
    // doesn't it make more sense to store it that way?
    // or can it make sense to say each body has a # of constraints that
    // influence only their motion -> requires mirrored constraint to be
    // added to other body as needed -> could cause problems depending on ordering if
    // simultaneous solutions are required
    private final Bag<Constraint> constraints;
}
