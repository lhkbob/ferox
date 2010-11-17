package com.ferox.physics.dynamics.constraint;

import com.ferox.util.Bag;

public abstract class NormalizableConstraint implements Constraint {
    // FIXME: thread-safety
    private static SequentialImpulseConstraintSolver defaultSolver = null;
    
    // FIXME: change this interface to allow for a priority of constraints, or something like that
    public abstract void normalize(float dt, Bag<LinearConstraint> constraints, Bag<LinearConstraint> friction, LinearConstraintPool pool);

    @Override
    public void solve(float dt) {
        if (defaultSolver == null)
            defaultSolver = new SequentialImpulseConstraintSolver();
        defaultSolver.solve(this, dt);
    }
}
