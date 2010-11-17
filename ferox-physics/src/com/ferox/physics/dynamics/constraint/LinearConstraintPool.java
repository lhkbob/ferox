package com.ferox.physics.dynamics.constraint;

import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.Bag;

public class LinearConstraintPool {
    private Bag<LinearConstraint> constraints;
    
    public LinearConstraintPool() {
        constraints = new Bag<LinearConstraint>();
    }
    
    public LinearConstraint get(RigidBody bodyA, RigidBody bodyB) {
        if (!constraints.isEmpty()) {
            LinearConstraint constraint = constraints.remove(constraints.size() - 1);
            constraint.reset(bodyA, bodyB);
            return constraint;
        } else
            return new LinearConstraint(bodyA, bodyB);
            
    }
    
    public void add(LinearConstraint constraint) {
        constraint.setImpulseListener(null);
        constraint.setDynamicLimits(null, 0f);
        constraints.add(constraint);
    }
}
