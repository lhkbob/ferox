package com.ferox.physics.dynamics;

import com.ferox.util.Bag;

public class SequentialImpulseConstraintSolver implements ConstraintSolver {
    private final int internalIterations;
    
    public SequentialImpulseConstraintSolver() {
        this(10);
    }
    
    public SequentialImpulseConstraintSolver(int numIters) {
        if (numIters <= 0)
            throw new IllegalArgumentException("Number of iterations must be at least 1, not: " + numIters);
        internalIterations = numIters;
    }
    @Override
    public void solve(Bag<LinearNormalConstraint> linearNormalConstraints) {
        int constraintCount = linearNormalConstraints.size();
        for (int i = 0; i < internalIterations; i++) {
            for (int j = 0; j < constraintCount; j++) {
                solveSingleConstraint(linearNormalConstraints.get(j));
            }
        }
    }
    
    private void solveSingleConstraint(LinearNormalConstraint c) {
        float deltaImpulse = c.rhs - c.appliedImpulse * c.cfm;
        float deltaVelADotN = c.constraintNormal.dot(c.bodyA.getDeltaLinearVelocity()) + c.relposACrossNormal.dot(c.bodyA.getDeltaAngularVelocity());
        float deltaVelBDotN = c.constraintNormal.dot(c.bodyB.getDeltaLinearVelocity()) + c.relposBCrossNormal.dot(c.bodyB.getDeltaAngularVelocity());
        
        deltaImpulse -= deltaVelADotN * c.jacobianDiagInverse;
        deltaImpulse -= deltaVelBDotN * c.jacobianDiagInverse;
        
        float sum = c.appliedImpulse + deltaImpulse;
        if (sum < c.lowerLimit) {
            deltaImpulse = c.lowerLimit - c.appliedImpulse;
            c.appliedImpulse = c.lowerLimit;
        } else if (sum > c.upperLimit) {
            deltaImpulse = c.upperLimit - c.appliedImpulse;
            c.appliedImpulse = c.upperLimit;
        } else
            c.appliedImpulse = sum;
        
        c.bodyA.addDeltaImpulse(c.constraintNormal.scale(c.bodyA.getInverseMass(), null), c.angularComponentA, deltaImpulse);
        c.bodyB.addDeltaImpulse(c.constraintNormal.scale(c.bodyB.getInverseMass(), null), c.angularComponentB, deltaImpulse);
    }
}
