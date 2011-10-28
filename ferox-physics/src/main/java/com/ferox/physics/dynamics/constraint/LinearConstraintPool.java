package com.ferox.physics.dynamics.constraint;

import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.Bag;

public class LinearConstraintPool {
    private final SolverBodyPool solverPool;
    private Bag<LinearConstraint> constraints;
    
    public LinearConstraintPool(SolverBodyPool solverPool) {
        this.solverPool = solverPool;
        constraints = new Bag<LinearConstraint>();
    }
    
    public LinearConstraint get(RigidBody bodyA, RigidBody bodyB) {
        if (!constraints.isEmpty()) {
            LinearConstraint constraint = constraints.remove(constraints.size() - 1);
            constraint.set(bodyA, bodyB, solverPool);
            return constraint;
        } else {
            LinearConstraint lc = new LinearConstraint();
            lc.set(bodyA, bodyB, solverPool);
            return lc;
        }
    }
    
    public void add(LinearConstraint constraint) {
        constraint.setImpulseListener(null);
        constraint.setDynamicLimits(null, 0f);
        constraints.add(constraint);
    }
}
