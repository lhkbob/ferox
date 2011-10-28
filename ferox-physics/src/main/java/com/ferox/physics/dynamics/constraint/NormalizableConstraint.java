package com.ferox.physics.dynamics.constraint;


public abstract class NormalizableConstraint implements Constraint {
    private static final Object DEFAULT_SOLVER_LOCK = new Object();
    private static SequentialImpulseConstraintSolver defaultSolver = null;
    
    public abstract void normalize(float dt, LinearConstraintAccumulator accum, LinearConstraintPool pool);

    @Override
    public void solve(float dt) {
        synchronized(DEFAULT_SOLVER_LOCK) {
            if (defaultSolver == null)
                defaultSolver = new SequentialImpulseConstraintSolver();
            defaultSolver.solve(this, dt);
        }
    }
}
