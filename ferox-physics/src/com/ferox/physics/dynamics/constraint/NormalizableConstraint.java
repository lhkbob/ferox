package com.ferox.physics.dynamics.constraint;

import com.ferox.util.Bag;

public abstract class NormalizableConstraint implements Constraint {
    // FIXME: thread-safety
    private static SequentialImpulseConstraintSolver defaultSolver = null;
    
    public abstract void normalize(float dt, Bag<LinearConstraint> constraints, LinearConstraintPool pool);

    @Override
    public void solve(float dt) {
        if (defaultSolver == null)
            defaultSolver = new SequentialImpulseConstraintSolver();
        defaultSolver.solve(this, dt);
    }
}
