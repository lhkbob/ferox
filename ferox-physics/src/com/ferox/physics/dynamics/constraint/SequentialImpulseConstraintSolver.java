package com.ferox.physics.dynamics.constraint;

import java.util.Collection;
import java.util.Random;

import com.ferox.physics.dynamics.constraint.LinearConstraintAccumulator.ConstraintLevel;

public class SequentialImpulseConstraintSolver implements ConstraintSolver {
    private final int internalIterations;
    private final SolverBodyPool solverPool;
    private final LinearConstraintPool constraintPool;
    private final LinearConstraintAccumulator constraints;
    
    private final Random shuffler;
    
    public SequentialImpulseConstraintSolver() {
        this(10);
    }
    
    public SequentialImpulseConstraintSolver(int numIters) {
        if (numIters <= 0)
            throw new IllegalArgumentException("Number of iterations must be at least 1, not: " + numIters);
        internalIterations = numIters;
        solverPool = new SolverBodyPool();
        constraintPool = new LinearConstraintPool(solverPool);
        constraints = new LinearConstraintAccumulator();
        shuffler = new Random();
    }
    
    @Override
    public void solve(Collection<Constraint> solve, float dt) {
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
            ((NormalizableConstraint) c).normalize(dt, constraints, constraintPool);
            solveLinearConstraints();
        } else
            c.solve(dt);
    }
    
    private void solveSingleConstraint(LinearConstraint c) {
        SolverBody ba = c.bodyA;
        SolverBody bb = c.bodyB;
        
        float deltaImpulse;// = c.rhs;
        float deltaVelADotN = 0f;
        float deltaVelBDotN = 0f;
        
        if (ba != null)
            deltaVelADotN = -c.jacobianDiagInverse * (c.nX * ba.dlX + c.nY * ba.dlY + c.nZ * ba.dlZ + c.taX * ba.daX + c.taY * ba.daY + c.taZ * ba.daZ);
        if (bb != null)
            deltaVelBDotN = c.jacobianDiagInverse * (c.nX * bb.dlX + c.nY * bb.dlY + c.nZ * bb.dlZ + c.tbX * bb.daX + c.tbY * bb.daY + c.tbZ * bb.daZ);
    
        deltaImpulse = c.rhs + deltaVelADotN + deltaVelBDotN;
        
        float sum = c.appliedImpulse + deltaImpulse;
        if (sum < c.getLowerLimit())
            deltaImpulse = c.getLowerLimit() - c.appliedImpulse;
        else if (sum > c.getUpperLimit())
            deltaImpulse = c.getUpperLimit() - c.appliedImpulse;

        c.addDeltaImpulse(deltaImpulse);
    }
    
    private void solveLinearConstraints() {
        // fetch all constraints from the accumulator
        int genericCount = constraints.getConstraintCount(ConstraintLevel.GENERIC);
        int contactCount = constraints.getConstraintCount(ConstraintLevel.CONTACT);
        int frictionCount = constraints.getConstraintCount(ConstraintLevel.FRICTION);
        
        LinearConstraint[] gar = constraints.getConstraints(ConstraintLevel.GENERIC);
        LinearConstraint[] car = constraints.getConstraints(ConstraintLevel.CONTACT);
        LinearConstraint[] far = constraints.getConstraints(ConstraintLevel.FRICTION);
        
        // iterate over all constraints X times, each time they are shuffled
        int j;
        for (int i = 0; i < internalIterations; i++) {
            shuffle(gar, genericCount);
            shuffle(car, contactCount);
            shuffle(far, frictionCount);

            for (j = genericCount - 1; j >= 0; j--)
                solveSingleConstraint(gar[j]);
            for (j = contactCount - 1; j >= 0; j--)
                solveSingleConstraint(car[j]);
            for (j = frictionCount - 1; j >= 0; j--)
                solveSingleConstraint(far[j]);
        }
        
        // return constraints to the pool
        constraints.clear(constraintPool);
        solverPool.updateRigidBodies();
    }
    
    private void shuffle(LinearConstraint[] array, int size) {
        for (int i = size; i >= 1; i--) {
            swap(array, i - 1, shuffler.nextInt(i));
        }
    }
    
    private void swap(LinearConstraint[] elements, int a, int b) {
        LinearConstraint t = elements[a];
        elements[a] = elements[b];
        elements[b] = t;
    }
}
