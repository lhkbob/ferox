package com.ferox.physics.dynamics.constraint;

import com.ferox.util.Bag;

public class LinearConstraintPool {
    private Bag<LinearConstraint> constraints;
    
    public LinearConstraintPool() {
        constraints = new Bag<LinearConstraint>();
    }
    
    public LinearConstraint get() {
        if (!constraints.isEmpty()) {
            LinearConstraint constraint = constraints.remove(constraints.size() - 1);
            reset(constraint);
            return constraint;
        } else
            return new LinearConstraint();
            
    }
    
    public void add(LinearConstraint constraint) {
        constraints.add(constraint);
    }
    
    private void reset(LinearConstraint constraint) {
        
    }
}
