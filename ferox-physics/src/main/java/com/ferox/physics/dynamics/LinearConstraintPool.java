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

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Vector3;

import java.util.Arrays;

/**
 * LinearConstraintPool is a packed data structure for storing many linear constraints in a representation
 * usable by {@link LinearConstraintSolver}. Its capacity grows automatically as more constraints are added.
 *
 * @author Michael Ludwig
 */
public class LinearConstraintPool {
    private int count; // current number of valid constraints in the pool

    /*
     * Packed primitive structures for a linear constraint
     */
    private double[] constraintForceMixes = new double[0]; // produces soft constraints
    private double[] solutions = new double[0]; // current solution of the diff eq. to solve

    private double[] jacobianDiagInverses = new double[0]; // inverse of jacobian's diagonal

    private double[] directions = new double[0]; // (Vector3) constraint direction
    private double[] torqueAs = new double[0]; // (Vector3) angular torque on A about direction
    private double[] torqueBs = new double[0]; // (Vector3) angular torque on B about direction

    private double[] linearDirAs = new double[0]; // (Vector3) constraint direction scaled by A's inverse mass
    private double[] linearDirBs = new double[0]; // (Vector3) constraint direction scaled by B's inverse mass
    private double[] angleDirAs = new double[0]; // (Vector3) angular torque transformed by A's inertia tensor inverse
    private double[] angleDirBs = new double[0]; // (Vector3) angular torque transformed by B's inertia tensor inverse

    private double[] appliedImpulses = new double[0]; // intermediate soln. during iterative solving
    private double[] warmstartImpulses = new double[0]; // partial impulse from prior solution
    private double[] upperLimits = new double[0]; // upper limit on any change in impulse
    private double[] lowerLimits = new double[0]; // lower limit on any change in impulse

    // this int[] stores indexes back into this pool of constraints
    private int[] dynamicLimits = new int[0]; // dynamically limit impulse by impulse of other constraint
    private double[] dynamicScaleFactors = new double[0]; // scale factor applied to dynamic limits
    private LinearConstraintPool dynamicPool;

    // these two int[] store indexes into the RigidBody table in the system
    private int[] bodyAs = new int[0];
    private int[] bodyBs = new int[0];

    private final Vector3 direction = new Vector3();
    private final Vector3 torqueA = new Vector3();
    private final Vector3 torqueB = new Vector3();
    private final Vector3 linearA = new Vector3();
    private final Vector3 linearB = new Vector3();
    private final Vector3 angularA = new Vector3();
    private final Vector3 angularB = new Vector3();

    private final Matrix3 inertiaA = new Matrix3();
    private final Matrix3 inertiaB = new Matrix3();

    /**
     * Create a new constraint pool that is optionally linked with {@code linkedPool}. If the linked pool is
     * not null, then this new pool can have dynamic force limits based on the forces in the linked pool.
     *
     * @param linkedPool The optional linked pool
     */
    public LinearConstraintPool(LinearConstraintPool linkedPool) {
        count = 0;
        dynamicPool = linkedPool;
        setCapacity(10);
    }

    /**
     * Manually set the capacity to {@code newCapacity}. If the capacity is smaller than the current capacity,
     * it will be truncated and the extra constraints discarded.
     *
     * @param newCapacity The new capacity
     */
    public void setCapacity(int newCapacity) {
        constraintForceMixes = Arrays.copyOf(constraintForceMixes, newCapacity);
        solutions = Arrays.copyOf(solutions, newCapacity);
        jacobianDiagInverses = Arrays.copyOf(jacobianDiagInverses, newCapacity);
        directions = Arrays.copyOf(directions, 3 * newCapacity);
        torqueAs = Arrays.copyOf(torqueAs, 3 * newCapacity);
        torqueBs = Arrays.copyOf(torqueBs, 3 * newCapacity);
        linearDirAs = Arrays.copyOf(linearDirAs, 3 * newCapacity);
        linearDirBs = Arrays.copyOf(linearDirBs, 3 * newCapacity);
        angleDirAs = Arrays.copyOf(angleDirAs, 3 * newCapacity);
        angleDirBs = Arrays.copyOf(angleDirBs, 3 * newCapacity);
        appliedImpulses = Arrays.copyOf(appliedImpulses, newCapacity);
        warmstartImpulses = Arrays.copyOf(warmstartImpulses, newCapacity);
        upperLimits = Arrays.copyOf(upperLimits, newCapacity);
        lowerLimits = Arrays.copyOf(lowerLimits, newCapacity);
        dynamicScaleFactors = Arrays.copyOf(dynamicScaleFactors, newCapacity);
        dynamicLimits = Arrays.copyOf(dynamicLimits, newCapacity);
        bodyAs = Arrays.copyOf(bodyAs, newCapacity);
        bodyBs = Arrays.copyOf(bodyBs, newCapacity);

        count = Math.min(newCapacity, count);
    }

    /**
     * Quickly empty the pool of all currently filled constraints. This resets the pointer within the internal
     * arrays so old constraints will be overwritten.
     */
    public void clear() {
        count = 0;
    }

    /**
     * Add a new constraint to the pool between the two bodies, one of which may be null. The constraint will
     * be along the given linear direction and torque vectors. The solution parameters of the constraint are
     * reset. The index of the constraint for later configuration is returned.
     *
     * @param bodyA     The first body
     * @param bodyB     The second body
     * @param direction The linear direction of the constraint
     * @param torqueA   The torque vector for body A
     * @param torqueB   The torque vector for body B
     *
     * @return The index of the new constraint
     */
    public int addConstraint(RigidBody bodyA, RigidBody bodyB, @Const Vector3 direction,
                             @Const Vector3 torqueA, @Const Vector3 torqueB) {
        int i = count++;
        int veci = i * 3;
        if (i >= bodyAs.length) {
            // increase capacity
            setCapacity((i + 1) * 2);
        }

        // copy the three vectors into their arrays
        direction.get(directions, veci);
        torqueA.get(torqueAs, veci);
        torqueB.get(torqueBs, veci);

        if (bodyA != null) {
            bodyAs[i] = bodyA.getIndex();

            double imA = 1.0 / bodyA.getMass();
            linearDirAs[veci] = direction.x * imA;
            linearDirAs[veci + 1] = direction.y * imA;
            linearDirAs[veci + 2] = direction.z * imA;

            this.torqueA.mul(bodyA.getInertiaTensorInverse(inertiaA), torqueA);
            this.torqueA.get(angleDirAs, veci);
        } else {
            // assign negative id
            bodyAs[i] = -1;
        }

        if (bodyB != null) {
            bodyBs[i] = bodyB.getIndex();

            double imB = 1.0 / bodyB.getMass();
            linearDirBs[veci] = direction.x * imB;
            linearDirBs[veci + 1] = direction.y * imB;
            linearDirBs[veci + 2] = direction.z * imB;

            this.torqueB.mul(bodyB.getInertiaTensorInverse(inertiaB), torqueB);
            this.torqueB.get(angleDirBs, veci);
        } else {
            // assign negative id
            bodyBs[i] = -1;
        }

        // zero out solution parameters
        jacobianDiagInverses[i] = 0;
        solutions[i] = 0;
        constraintForceMixes[i] = 0;

        lowerLimits[i] = -Double.MAX_VALUE;
        upperLimits[i] = Double.MAX_VALUE;
        dynamicScaleFactors[i] = 0;
        dynamicLimits[i] = -1;

        appliedImpulses[i] = 0;
        warmstartImpulses[i] = 0;

        return i;
    }

    /**
     * @param i       The index of the constraint to access
     * @param impulse The impulse scalar
     *
     * @return The linear impulse to apply to A. This vector is reused if this method is invoked so the result
     * must be copied out
     */
    @Const
    public Vector3 getLinearImpulseA(int i, double impulse) {
        return linearA.set(linearDirAs, i * 3).scale(impulse);
    }

    /**
     * @param i       The index of the constraint to access
     * @param impulse The impulse scalar
     *
     * @return The linear impulse to apply to B. This vector is reused if this method is invoked so the result
     * must be copied out
     */
    @Const
    public Vector3 getLinearImpulseB(int i, double impulse) {
        return linearB.set(linearDirBs, i * 3).scale(impulse);
    }

    /**
     * @param i       The index of the constraint to access
     * @param impulse The impulse scalar
     *
     * @return The angular impulse to apply to A. This vector is reused if this method is invoked so the
     * result must be copied out
     */
    @Const
    public Vector3 getAngularImpulseA(int i, double impulse) {
        return angularA.set(angleDirAs, i * 3).scale(impulse);
    }

    /**
     * @param i       The index of the constraint to access
     * @param impulse The impulse scalar
     *
     * @return The angular impulse to apply to A. This vector is reused if this method is invoked so the
     * result must be copied out
     */
    @Const
    public Vector3 getAngularImpulseB(int i, double impulse) {
        return angularB.set(angleDirBs, i * 3).scale(impulse);
    }

    /**
     * Record the applied impulse for the given constraint
     *
     * @param i       The constraint to access
     * @param impulse The total applied impulse for the constraint
     */
    public void setAppliedImpulse(int i, double impulse) {
        appliedImpulses[i] = impulse;
    }

    /**
     * Record the warmstart impulse to apply to this constraint
     *
     * @param i       The constraint to modify
     * @param impulse The warmstart impulse
     */
    public void setWarmstartImpulse(int i, double impulse) {
        warmstartImpulses[i] = impulse;
    }

    /**
     * Set the solution parameters of the constraint to inform the constraint solving process.
     *
     * @param i        The constraint to modify
     * @param solution The desired solution
     * @param cfm      The constraint force mix
     * @param jacobian The inverse of the Jacobian of the constraint
     */
    public void setSolution(int i, double solution, double cfm, double jacobian) {
        solutions[i] = solution;
        constraintForceMixes[i] = cfm;
        jacobianDiagInverses[i] = jacobian;
    }

    /**
     * Set the upper and lower limits of the applied impulse on this constraint.
     *
     * @param i     The constraint to modify
     * @param lower The lower bound on the applied impulse
     * @param upper The upper bound on the applied impulse
     */
    public void setStaticLimits(int i, double lower, double upper) {
        if (lower > upper) {
            throw new IllegalArgumentException("Lower limit (" + lower + ") must be less than upper limit (" +
                                               upper + ")");
        }
        lowerLimits[i] = lower;
        upperLimits[i] = upper;
        dynamicLimits[i] = -1; // clear dynamic limit
    }

    /**
     * Set the upper and lower limits of the applied impulse to be proportional to the {@code
     * linkedConstraint} in the configured linked constraint pool.
     *
     * @param i                The constraint to modify
     * @param linkedConstraint The linked constraint
     * @param scale            The scaling factor applied to the linked applied impulse
     */
    public void setDynamicLimits(int i, int linkedConstraint, double scale) {
        if (dynamicPool == null) {
            throw new IllegalStateException("Pool does not have a linked pool");
        }
        if (linkedConstraint < 0 || linkedConstraint > dynamicPool.count || scale <= 0) {
            throw new IllegalArgumentException("Constraint index (" + linkedConstraint +
                                               ") is invalid, or scale (" +
                                               scale + ") is not positive");
        }
        dynamicLimits[i] = linkedConstraint;
        dynamicScaleFactors[i] = scale;
    }

    /**
     * @param i The constraint index
     *
     * @return The constraint direction
     */
    @Const
    public Vector3 getConstraintDirection(int i) {
        return direction.set(directions, i * 3);
    }

    /**
     * @param i The constraint index
     *
     * @return The torque vector for body A
     */
    @Const
    public Vector3 getTorqueA(int i) {
        return torqueA.set(torqueAs, i * 3);
    }

    /**
     * @param i The constraint index
     *
     * @return The torque vector for body B
     */
    @Const
    public Vector3 getTorqueB(int i) {
        return torqueB.set(torqueBs, i * 3);
    }

    /**
     * @param i The constraint index
     *
     * @return The Jacobian inverse of the constraint
     */
    public double getJacobianDiagonalInverse(int i) {
        return jacobianDiagInverses[i];
    }

    /**
     * Get the lower limit on the applied impulse for this constraint. This may be a static lower limit or
     * dynamically computed from a linked constraint.
     *
     * @param i The constraint index
     *
     * @return The lower impulse limit
     */
    public double getLowerImpulseLimit(int i) {
        int dynamic = dynamicLimits[i];
        if (dynamic >= 0) {
            return -dynamicScaleFactors[i] * dynamicPool.appliedImpulses[dynamic];
        } else {
            return lowerLimits[i];
        }
    }

    /**
     * Get the upper limit on the applied impulse for this constraint. This may be a static upper limit or
     * dynamically computed from a linked constraint.
     *
     * @param i The constraint index
     *
     * @return The upper impulse limit
     */
    public double getUpperImpulseLimit(int i) {
        int dynamic = dynamicLimits[i];
        if (dynamic >= 0) {
            return dynamicScaleFactors[i] * dynamicPool.appliedImpulses[dynamic];
        } else {
            return upperLimits[i];
        }
    }

    /**
     * @param i The constraint index
     *
     * @return The desired constraint solution
     */
    public double getSolution(int i) {
        return solutions[i];
    }

    /**
     * @param i The constraint index
     *
     * @return The constraint force mix for the constraint
     */
    public double getConstraintForceMix(int i) {
        return constraintForceMixes[i];
    }

    /**
     * @param i The constraint index
     *
     * @return The warmstart impulse for the constraint
     */
    public double getWarmstartImpulse(int i) {
        return warmstartImpulses[i];
    }

    /**
     * @param i The constraint index
     *
     * @return The applied impulse for the constraint
     */
    public double getAppliedImpulse(int i) {
        return appliedImpulses[i];
    }

    /**
     * @param i The constraint index
     *
     * @return The component index of the first RigidBody, or -1 if the constraint didn't have a rigid body at
     * this attachment point
     */
    public int getBodyAIndex(int i) {
        return bodyAs[i];
    }

    /**
     * @param i The constraint index
     *
     * @return The component index of the second RigidBody, or -1 if the constraint didn't have a rigid body
     * at this attachment point
     */
    public int getBodyBIndex(int i) {
        return bodyBs[i];
    }

    /**
     * @return Get the number of constraints in the pool
     */
    public int getConstraintCount() {
        return count;
    }
}
