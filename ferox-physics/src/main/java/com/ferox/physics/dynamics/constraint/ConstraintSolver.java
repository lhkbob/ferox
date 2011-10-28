package com.ferox.physics.dynamics.constraint;

import java.util.Collection;


public interface ConstraintSolver {
    public void solve(Collection<Constraint> solve, float dt);
    
    public void solve(Constraint c, float dt);
}
