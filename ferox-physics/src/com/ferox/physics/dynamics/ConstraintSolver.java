package com.ferox.physics.dynamics;

import com.ferox.util.Bag;

public interface ConstraintSolver {
    // FIXME: how do I want to organize my constraints?
    // it would be nice if I could set up to be purely two objects with a vector
    // and either an equality or inequality along the vector -> not sure how this applies
    // to useful examples
    //
    // then the physics world has to generate constraints for friction and contacts
    public void solve(Bag<Constraint> constraints);
}
