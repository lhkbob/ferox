package com.ferox.physics.dynamics;

import java.util.Random;

import com.ferox.math.Vector3;
import com.ferox.math.entreri.Vector3Property;

public class LinearConstraintSolver {
    public static int totalConstraints = 0;
    
    private final Random shuffler;
    
    private boolean shuffleConstraints;
    private boolean shuffleEachIteration;
    private int numIterations;
    
    // solver body access
    private Vector3Property deltaLinearImpulse;
    private Vector3Property deltaAngularImpulse;
    
    private final Vector3 linear = new Vector3();
    private final Vector3 angular = new Vector3();
    
    public LinearConstraintSolver() {
        shuffler = new Random();
        setShuffleConstraints(true);
        setShuffleEveryIteration(true);
        setIterationCount(10);
    }
    
    public void setDeltaLinearImpulseProperty(Vector3Property prop) {
        deltaLinearImpulse = prop;
    }
    
    public void setDeltaAngularImpulseProperty(Vector3Property prop) {
        deltaAngularImpulse = prop;
    }
    
    public void setShuffleConstraints(boolean shuffle) {
        shuffleConstraints = shuffle;
    }
    
    public void setShuffleEveryIteration(boolean shuffle) {
        shuffleEachIteration = shuffle;
    }
    
    public void setIterationCount(int numIters) {
        if (numIters <= 0)
            throw new IllegalArgumentException("Iteration count must be at least 1, not " + numIters);
        numIterations = numIters;
    }
    
    public boolean getShuffleConstraints() {
        return shuffleConstraints;
    }
    
    public boolean getShuffleEveryIteration() {
        return shuffleEachIteration;
    }
    
    public int getIterationCount() {
        return numIterations;
    }

    public static long warmstartTime = 0;
    public static long shuffleTime = 0;
    public static long solveTime = 0;
    
    public void solve(LinearConstraintPool... groups) {
        // handle warmstarting
        warmstartTime -= System.nanoTime();
        for (int i = 0; i < groups.length; i++) {
            applyWarmstarting(groups[i]);
        }
        warmstartTime += System.nanoTime();
        
        if (shuffleConstraints) {
            if (shuffleEachIteration) {
                solveShuffle(groups);
            } else {
                solveShuffleOnce(groups);
            }
        } else {
            solveNoShuffling(groups);
        }
    }
    
    private void solveNoShuffling(LinearConstraintPool[] groups) {
        solveTime -= System.nanoTime();
        // with no shuffling we don't need an array of indices
        int count;
        LinearConstraintPool group;
        for (int i = 0; i < numIterations; i++) {
            for (int j = 0; j < groups.length; j++) {
                group = groups[j];
                count = group.getConstraintCount();
                for (int k = 0; k < count; k++) {
                    solveSingleConstraint(group, k);
                }
            }
        }
        solveTime += System.nanoTime();
    }
    
    private void solveShuffleOnce(LinearConstraintPool[] groups) {
        shuffleTime -= System.nanoTime();
        // since we're shuffling, we have to allocate these arrays
        int[][] indices = createIndices(groups);
        // shuffle one time at the very start
        for (int i = 0; i < groups.length; i++) {
            shuffle(indices[i]);
        }
        shuffleTime += System.nanoTime();
        
        solveTime -= System.nanoTime();
        int[] shuffled;
        LinearConstraintPool group;
        for (int i = 0; i < numIterations; i++) {
            for (int j = 0; j < groups.length; j++) {
                shuffled = indices[j];
                group = groups[j];
                for (int k = 0; k < shuffled.length; k++) {
                    solveSingleConstraint(group, shuffled[k]);
                }
            }
        }
        solveTime += System.nanoTime();
    }
    
    private void solveShuffle(LinearConstraintPool[] groups) {
        shuffleTime -= System.nanoTime();
        // since we're shuffling, we have to allocate these arrays
        int[][] indices = createIndices(groups);
        shuffleTime += System.nanoTime();
        
        int[] shuffled;
        LinearConstraintPool group;
        for (int i = 0; i < numIterations; i++) {
            for (int j = 0; j < groups.length; j++) {
                shuffled = indices[j];
                group = groups[j];
                
                shuffleTime -= System.nanoTime();
                // shuffle the indices every iteration
                shuffle(shuffled);
                shuffleTime += System.nanoTime();
                
                solveTime -= System.nanoTime();
                for (int k = 0; k < shuffled.length; k++) {
                    solveSingleConstraint(group, shuffled[k]);
                }
                solveTime += System.nanoTime();
            }
        }
    }
    
    private void solveSingleConstraint(LinearConstraintPool group, int constraint) {
        totalConstraints++;
        
        double jacobian = group.getJacobianDiagonalInverse(constraint);
        double deltaImpulse = group.getSolution(constraint);
        
        double deltaVelADotN = 0;
        double deltaVelBDotN = 0;
        
        int ba = group.getBodyAIndex(constraint);
        int bb = group.getBodyBIndex(constraint);
        
//        System.out.println("Adding impulse to (" + ba + ", " + bb + ")");
        if (ba >= 0) {
            deltaLinearImpulse.get(ba, linear);
            deltaAngularImpulse.get(ba, angular);
            
            deltaVelADotN = -jacobian * (group.getConstraintDirection(constraint).dot(linear) + group.getTorqueA(constraint).dot(angular));
//            System.out.println(" - dlA: " + linear + ", daA: " + angular);
        }
        
        if (bb >= 0) {
            deltaLinearImpulse.get(bb, linear);
            deltaAngularImpulse.get(bb, angular);
            
            deltaVelBDotN = jacobian * (group.getConstraintDirection(constraint).dot(linear) + group.getTorqueB(constraint).dot(angular));
//            System.out.println(" - dlB: " + linear + ", daB: " + angular);
        }
        
//        System.out.println(" - velADotN: " + deltaVelADotN + ", velBDotN: " + deltaVelBDotN);

        deltaImpulse = group.getSolution(constraint) + deltaVelADotN + deltaVelBDotN;
        
        double applied = group.getAppliedImpulse(constraint);
        double totalImpulse = applied + deltaImpulse;
        double lower = group.getLowerImpulseLimit(constraint);
        double upper = group.getUpperImpulseLimit(constraint);
        if (totalImpulse < lower) {
            deltaImpulse = lower - applied;
            totalImpulse = lower;
        } else if (totalImpulse > upper) {
            deltaImpulse = upper - applied;
            totalImpulse = upper;
        }
        
        if (ba >= 0) {
            deltaLinearImpulse.get(ba, linear);
            linear.add(group.getLinearImpulseA(constraint, deltaImpulse));
            deltaLinearImpulse.set(linear, ba);
            
            deltaAngularImpulse.get(ba, angular);
            angular.add(group.getAngularImpulseA(constraint, deltaImpulse));
            deltaAngularImpulse.set(angular, ba);
        }
        
        if (bb >= 0) {
            deltaLinearImpulse.get(bb, linear);
            linear.sub(group.getLinearImpulseB(constraint, deltaImpulse));
            deltaLinearImpulse.set(linear, bb);
            
            deltaAngularImpulse.get(bb, angular);
            angular.sub(group.getAngularImpulseB(constraint, deltaImpulse));
            deltaAngularImpulse.set(angular, bb);
        }
        
//        System.out.println(" - delta: " + deltaImpulse + ", total: " + totalImpulse + ", expected soln: " + group.getSolution(constraint));
//        System.out.println(" - constraint id: " + constraint);
        group.setAppliedImpulse(constraint, totalImpulse);
    }
    
    private void applyWarmstarting(LinearConstraintPool group) {
        int count = group.getConstraintCount();
        for (int i = 0; i < count; i++) {
            double warmstart = group.getWarmstartImpulse(i);
            if (warmstart >= 0) {
                int ba = group.getBodyAIndex(i);
                if (ba >= 0) {
                    deltaLinearImpulse.get(ba, linear);
                    linear.add(group.getLinearImpulseA(i, warmstart));
                    deltaLinearImpulse.set(linear, ba);
                    
                    deltaAngularImpulse.get(ba, angular);
                    angular.add(group.getAngularImpulseA(i, warmstart));
                    deltaAngularImpulse.set(angular, ba);
                }
                
                int bb = group.getBodyBIndex(i);
                if (bb >= 0) {
                    deltaLinearImpulse.get(bb, linear);
                    linear.sub(group.getLinearImpulseB(i, warmstart));
                    deltaLinearImpulse.set(linear, bb);
                    
                    deltaAngularImpulse.get(bb, angular);
                    angular.sub(group.getAngularImpulseB(i, warmstart));
                    deltaAngularImpulse.set(angular, bb);
                }
                
                group.setAppliedImpulse(i, warmstart);
            }
        }
    }
    
    private int[][] createIndices(LinearConstraintPool[] groups) {
        int[][] indices = new int[groups.length][];
        
        for (int i = 0; i < indices.length; i++) {
            int[] forGroup = new int[groups[i].getConstraintCount()];
            for (int j = 0; j < forGroup.length; j++) {
                forGroup[j] = j;
            }
            
            indices[i] = forGroup;
        }
        
        return indices;
    }
    
    private void shuffle(int[] indices) {
        for (int i = indices.length; i >= 1; i--) {
            int swap = shuffler.nextInt(i);
            int temp = indices[i - 1];
            indices[i - 1] = indices[swap];
            indices[swap] = temp;
        }
    }
}
