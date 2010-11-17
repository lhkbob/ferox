package com.ferox.physics.dynamics.constraint;

import java.util.Collection;

import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.Bag;

public class SequentialImpulseConstraintSolver implements ConstraintSolver {
    private final int internalIterations;
    private final LinearConstraintPool constraintPool;
    
    private final Bag<LinearConstraint> contacts;
    private final Bag<LinearConstraint> friction;
    
    public SequentialImpulseConstraintSolver() {
        this(10);
    }
    
    public SequentialImpulseConstraintSolver(int numIters) {
        if (numIters <= 0)
            throw new IllegalArgumentException("Number of iterations must be at least 1, not: " + numIters);
        internalIterations = numIters;
        constraintPool = new LinearConstraintPool();
        contacts = new Bag<LinearConstraint>();
        friction = new Bag<LinearConstraint>();
    }
    
    @Override
    public void solve(Collection<Constraint> solve, float dt) {
        contacts.clear(true);
        friction.clear(true);
        
        for (Constraint c: solve) {
            if (c instanceof NormalizableConstraint) {
                // normalize constraint to be solved uniformly later
                ((NormalizableConstraint) c).normalize(dt, contacts, friction, constraintPool);
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
            ((NormalizableConstraint) c).normalize(dt, contacts, friction, constraintPool);
            solveLinearConstraints();
        } else
            c.solve(dt);
    }
    
    private void solveSingleConstraint(LinearConstraint c) {
        RigidBody ba = c.getRigidBodyA();
        RigidBody bb = c.getRigidBodyB();
        
        float deltaImpulse = c.getRightHandSide() - c.getAppliedImpulse() * c.getConstraintForceMix();
        float deltaVelADotN = 0f;
        float deltaVelBDotN = 0f;
        
        if (ba != null)
            deltaVelADotN = c.getConstraintAxis().dot(ba.getDeltaLinearVelocity()) + c.getTorqueAxisA().dot(ba.getDeltaAngularVelocity());
        if (bb != null)
            deltaVelBDotN = c.getConstraintAxis().dot(bb.getDeltaLinearVelocity()) + c.getTorqueAxisB().dot(bb.getDeltaAngularVelocity());
        
        deltaImpulse -= deltaVelADotN * c.getJacobianInverse();
        deltaImpulse += deltaVelBDotN * c.getJacobianInverse();
        
        float sum = c.getAppliedImpulse() + deltaImpulse;
        if (sum < c.getLowerLimit()) {
            // clamp to lower
            deltaImpulse = c.getLowerLimit() - c.getAppliedImpulse();
        } else if (sum > c.getUpperLimit()) {
            // clamp to upper
            deltaImpulse = c.getUpperLimit() - c.getAppliedImpulse();
        }

        c.addDeltaImpulse(deltaImpulse);
    }
    
    private void solveLinearConstraints() {
        int ct = contacts.size();
        int ct2 = friction.size();
        for (int i = 0; i < internalIterations; i++) {
                contacts.shuffle();
                friction.shuffle();
            
            for (int j = 0; j < ct; j++)
                solveSingleConstraint(contacts.get(j));
            for (int j = 0; j < ct2; j++)
                solveSingleConstraint(friction.get(j));
        }
        
        // return constraints to the pool
        for (int i = 0; i < ct; i++)
            constraintPool.add(contacts.get(i));
        for (int i = 0; i < ct2; i++)
            constraintPool.add(friction.get(i));
        contacts.clear(true);
        friction.clear(true);
    }
}
