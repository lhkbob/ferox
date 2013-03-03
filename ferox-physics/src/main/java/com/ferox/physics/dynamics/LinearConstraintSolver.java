/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.physics.dynamics;

import com.ferox.math.Vector3;
import com.ferox.math.entreri.Vector3Property;

import java.util.Random;

public class LinearConstraintSolver {
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
        if (numIters <= 0) {
            throw new IllegalArgumentException(
                    "Iteration count must be at least 1, not " + numIters);
        }
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

    public void solve(LinearConstraintPool... groups) {
        // handle warmstarting
        for (int i = 0; i < groups.length; i++) {
            applyWarmstarting(groups[i]);
        }

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
    }

    private void solveShuffleOnce(LinearConstraintPool[] groups) {
        // since we're shuffling, we have to allocate these arrays
        int[][] indices = createIndices(groups);
        // shuffle one time at the very start
        for (int i = 0; i < groups.length; i++) {
            shuffle(indices[i]);
        }

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
    }

    private void solveShuffle(LinearConstraintPool[] groups) {
        // since we're shuffling, we have to allocate these arrays
        int[][] indices = createIndices(groups);

        int[] shuffled;
        LinearConstraintPool group;
        for (int i = 0; i < numIterations; i++) {
            for (int j = 0; j < groups.length; j++) {
                shuffled = indices[j];
                group = groups[j];

                // shuffle the indices every iteration
                shuffle(shuffled);

                for (int k = 0; k < shuffled.length; k++) {
                    solveSingleConstraint(group, shuffled[k]);
                }
            }
        }
    }

    private void solveSingleConstraint(LinearConstraintPool group, int constraint) {
        double jacobian = group.getJacobianDiagonalInverse(constraint);
        double deltaImpulse = group.getSolution(constraint);

        int ba = group.getBodyAIndex(constraint);
        int bb = group.getBodyBIndex(constraint);

        if (ba >= 0) {
            deltaLinearImpulse.get(ba, linear);
            deltaAngularImpulse.get(ba, angular);

            deltaImpulse -= jacobian *
                            (group.getConstraintDirection(constraint).dot(linear) +
                             group.getTorqueA(constraint).dot(angular));
        }

        if (bb >= 0) {
            deltaLinearImpulse.get(bb, linear);
            deltaAngularImpulse.get(bb, angular);

            deltaImpulse += jacobian *
                            (group.getConstraintDirection(constraint).dot(linear) +
                             group.getTorqueB(constraint).dot(angular));
        }

        double applied = group.getAppliedImpulse(constraint);
        double totalImpulse = applied + deltaImpulse;
        if (totalImpulse < group.getLowerImpulseLimit(constraint)) {
            totalImpulse = group.getLowerImpulseLimit(constraint);
            deltaImpulse = totalImpulse - applied; // really (lower - applied)
        } else if (totalImpulse > group.getUpperImpulseLimit(constraint)) {
            totalImpulse = group.getUpperImpulseLimit(constraint);
            deltaImpulse = totalImpulse - applied; // really (upper - applied)
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
