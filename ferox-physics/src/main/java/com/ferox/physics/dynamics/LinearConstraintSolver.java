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

/**
 * LinearConstraintSolver is an iteration based constraint solver that estimates the impulses required to
 * solve the global solution. It does not guarantee perfect correctness, but as the iteration count is
 * increased its accuracy is improved. This is largely based on the sequential impulse constraint solver from
 * Bullet.
 *
 * @author Michael Ludwig
 */
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

    /**
     * Create a new LinearConstraintSolver that shuffles the constraints every iteration, for ten iterations.
     */
    public LinearConstraintSolver() {
        shuffler = new Random();
        setShuffleConstraints(true);
        setShuffleEveryIteration(true);
        setIterationCount(10);
    }

    /**
     * Set the decorated property used to hold the delta linear impulses that are accumulated for each
     * RigidBody. The vector property must be a decorated property applied to {@link RigidBody} on the
     * EntitySystem that this solver processes. This can be done with {@link
     * com.lhkbob.entreri.EntitySystem#decorate(Class, com.lhkbob.entreri.property.PropertyFactory)}.
     *
     * @param prop The linear impulse property
     */
    public void setDeltaLinearImpulseProperty(Vector3Property prop) {
        deltaLinearImpulse = prop;
    }

    /**
     * Set the decorated property used to hold the delta angular impulses that are accumulated for each
     * RigidBody. The vector property must be a decorated property applied to {@link RigidBody} on the
     * EntitySystem that this solver processes. This can be done with {@link
     * com.lhkbob.entreri.EntitySystem#decorate(Class, com.lhkbob.entreri.property.PropertyFactory)}.
     *
     * @param prop The angular impulse property
     */
    public void setDeltaAngularImpulseProperty(Vector3Property prop) {
        deltaAngularImpulse = prop;
    }

    /**
     * Set whether or not constraints should be shuffled. If this is set to false, then {@link
     * #getShuffleEveryIteration()} is ignored. If this is set to true, the order in which constraints are
     * iterated over is randomized, which can improve simulation stability. However, this comes at the expense
     * of performance since cache locality over the constraints is broken.
     *
     * @param shuffle Whether or not to shuffle the constraints during solving
     */
    public void setShuffleConstraints(boolean shuffle) {
        shuffleConstraints = shuffle;
    }

    /**
     * Set whether or not constraints should be shuffled every iteration, or only once at the start. This is
     * ignored if general constraint shuffling is disabled. When disabled, the primary performance improvement
     * is that calls to {@link java.util.Random#nextInt()} are reduced. Cache locality is still impacted by
     * the initial random ordering.
     *
     * @param shuffle Whether or not to shuffle each iteration or only once
     */
    public void setShuffleEveryIteration(boolean shuffle) {
        shuffleEachIteration = shuffle;
    }

    /**
     * Set the number of iterations used to estimate a solution to all constraints in the supplied pools.
     *
     * @param numIters The number of iterations
     *
     * @throws IllegalArgumentException if numIters is less than 1
     */
    public void setIterationCount(int numIters) {
        if (numIters <= 0) {
            throw new IllegalArgumentException("Iteration count must be at least 1, not " + numIters);
        }
        numIterations = numIters;
    }

    /**
     * @return True if constraints are shuffled
     */
    public boolean getShuffleConstraints() {
        return shuffleConstraints;
    }

    /**
     * @return True if constraints are shuffled every iteration, ignored if {@link #getShuffleConstraints()}
     * is false
     */
    public boolean getShuffleEveryIteration() {
        return shuffleEachIteration;
    }

    /**
     * @return The number of iterations
     */
    public int getIterationCount() {
        return numIterations;
    }

    /**
     * Solve the constraints provided in the list of pools. Within each iteration, the groups are solved in
     * the order provided.
     *
     * @param groups The linear constraints to solve
     */
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

            deltaImpulse -= jacobian * (group.getConstraintDirection(constraint).dot(linear) +
                                        group.getTorqueA(constraint).dot(angular));
        }

        if (bb >= 0) {
            deltaLinearImpulse.get(bb, linear);
            deltaAngularImpulse.get(bb, angular);

            deltaImpulse += jacobian * (group.getConstraintDirection(constraint).dot(linear) +
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
            deltaLinearImpulse.set(ba, linear);

            deltaAngularImpulse.get(ba, angular);
            angular.add(group.getAngularImpulseA(constraint, deltaImpulse));
            deltaAngularImpulse.set(ba, angular);
        }

        if (bb >= 0) {
            deltaLinearImpulse.get(bb, linear);
            linear.sub(group.getLinearImpulseB(constraint, deltaImpulse));
            deltaLinearImpulse.set(bb, linear);

            deltaAngularImpulse.get(bb, angular);
            angular.sub(group.getAngularImpulseB(constraint, deltaImpulse));
            deltaAngularImpulse.set(bb, angular);
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
                    deltaLinearImpulse.set(ba, linear);

                    deltaAngularImpulse.get(ba, angular);
                    angular.add(group.getAngularImpulseA(i, warmstart));
                    deltaAngularImpulse.set(ba, angular);
                }

                int bb = group.getBodyBIndex(i);
                if (bb >= 0) {
                    deltaLinearImpulse.get(bb, linear);
                    linear.sub(group.getLinearImpulseB(i, warmstart));
                    deltaLinearImpulse.set(bb, linear);

                    deltaAngularImpulse.get(bb, angular);
                    angular.sub(group.getAngularImpulseB(i, warmstart));
                    deltaAngularImpulse.set(bb, angular);
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
