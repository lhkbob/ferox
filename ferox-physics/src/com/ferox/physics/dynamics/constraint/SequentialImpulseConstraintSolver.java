package com.ferox.physics.dynamics.constraint;

import java.util.Collection;

import com.ferox.util.Bag;

public class SequentialImpulseConstraintSolver implements ConstraintSolver {
    private final int internalIterations;
    private final LinearConstraintPool constraintPool;
    private final Bag<LinearConstraint> constraints;
    
    public SequentialImpulseConstraintSolver() {
        this(10);
    }
    
    public SequentialImpulseConstraintSolver(int numIters) {
        if (numIters <= 0)
            throw new IllegalArgumentException("Number of iterations must be at least 1, not: " + numIters);
        internalIterations = numIters;
        constraintPool = new LinearConstraintPool();
        constraints = new Bag<LinearConstraint>();
    }
    
    private void solveSingleConstraint(LinearConstraint c) {
        // FIXME: check for nullity of bodyA and bodyB for when one side is a Collidable and not a RigidBody
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

    @Override
    public void solve(Collection<Constraint> solve, float dt) {
        constraints.clear(true);
        for (Constraint c: solve) {
            if (c instanceof NormalizableConstraint) {
                // normalize constraint to be solved uniformly later
                ((NormalizableConstraint) c).normalize(constraints, constraintPool);
            } else {
                // don't know how to solve it, will rely on its solve() method
                c.solve(dt);
            }
        }
     
        solveLinearConstraints(constraints);
    }

    @Override
    public void solve(Constraint c, float dt) {
        if (c instanceof NormalizableConstraint) {
            constraints.clear(true);
            ((NormalizableConstraint) c).normalize(constraints, constraintPool);
            solveLinearConstraints(constraints);
        } else
            c.solve(dt);
    }
    
    private void solveLinearConstraints(Bag<LinearConstraint> constraints) {
        int ct = constraints.size();
        for (int i = 0; i < internalIterations; i++) {
            for (int j = 0; j < ct; j++)
                solveSingleConstraint(constraints.get(j));
        }
    }
}
