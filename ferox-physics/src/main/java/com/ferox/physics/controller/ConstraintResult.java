package com.ferox.physics.controller;

import com.ferox.physics.dynamics.LinearConstraintPool;
import com.lhkbob.entreri.Result;

public class ConstraintResult implements Result {
    private final LinearConstraintPool group;
    
    public ConstraintResult(LinearConstraintPool group) {
        if (group == null)
            throw new NullPointerException("LinearConstraintPool cannot be null");
        this.group = group;
    }
    
    public LinearConstraintPool getConstraints() {
        return group;
    }
    
    @Override
    public boolean isSingleton() {
        return false;
    }
}
