package com.ferox.physics.dynamics.constraint;

import java.util.Collection;

import com.ferox.math.Vector3f;
import com.ferox.physics.dynamics.RigidBody;
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
    
    @Override
    public void solve(Collection<Constraint> solve, float dt) {
        constraints.clear(true);
        for (Constraint c: solve) {
            if (c instanceof NormalizableConstraint) {
                // normalize constraint to be solved uniformly later
                ((NormalizableConstraint) c).normalize(dt, constraints, constraintPool);
            } else {
                // don't know how to solve it, will rely on its solve() method
                c.solve(dt);
            }
        }
     
        solveLinearConstraints();
    }

    @Override
    public void solve(Constraint c, float dt) {
        if (c instanceof NormalizableConstraint) {
            constraints.clear(true);
            ((NormalizableConstraint) c).normalize(dt, constraints, constraintPool);
            solveLinearConstraints();
        } else
            c.solve(dt);
    }
    
    private void solveSingleConstraint(LinearConstraint c) {
        RigidBody ba = c.getRigidBodyA();
        RigidBody bb = c.getRigidBodyB();
        
        if (Float.isNaN(c.getRightHandSide()) || Float.isNaN(c.getAppliedImpulse()) || Float.isNaN(c.getConstraintForceMix())) {
            return; 
        }
        
        float deltaImpulse = c.getRightHandSide() - c.getAppliedImpulse() * c.getConstraintForceMix();
        float deltaVelADotN = 0f;
        float deltaVelBDotN = 0f;
        
        if (ba != null)
            deltaVelADotN = c.getConstraintAxis().dot(ba.getDeltaLinearVelocity()) + c.getTorqueAxisA().dot(ba.getDeltaAngularVelocity());
        if (bb != null)
            deltaVelBDotN = -c.getConstraintAxis().dot(bb.getDeltaLinearVelocity()) + c.getTorqueAxisB().dot(bb.getDeltaAngularVelocity());
        
        if (Float.isNaN(deltaVelADotN) || Float.isNaN(deltaVelBDotN)) {
            return;
        }
        
        deltaImpulse -= deltaVelADotN * c.getJacobianInverse();
        deltaImpulse -= deltaVelBDotN * c.getJacobianInverse();
        
        float sum = c.getAppliedImpulse() + deltaImpulse;
        if (sum < c.getLowerLimit()) {
            // clamp to lower
            deltaImpulse = c.getLowerLimit() - c.getAppliedImpulse();
        } else if (sum > c.getUpperLimit()) {
            // clamp to upper
            deltaImpulse = c.getUpperLimit() - c.getAppliedImpulse();
        }
        
        /*if (deltaImpulse > 0 && (first == null || ba == first)) {
            System.out.println("Moved: " + ba.hashCode());
            System.out.println(deltaImpulse);
            System.out.println(ba.getDeltaLinearVelocity() + " " + ba.getDeltaAngularVelocity());
            first = ba;
        }*/
        c.addDeltaImpulse(deltaImpulse);
    }
    RigidBody first = null;
    
    private void solveLinearConstraints() {
        int ct = constraints.size();
        for (int i = 0; i < internalIterations; i++) {
//            System.err.println("------- STARTING ITER: " + i + " ---------");
            for (int j = 0; j < ct; j++)
                solveSingleConstraint(constraints.get(j));
        }
        
        // return constraints to the pool
        for (int i = 0; i < ct; i++)
            constraintPool.add(constraints.get(i));
        constraints.clear(true);
    }
}
