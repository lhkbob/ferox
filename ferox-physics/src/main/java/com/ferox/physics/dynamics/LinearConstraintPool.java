package com.ferox.physics.dynamics;

import java.util.Arrays;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

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
    
    public LinearConstraintPool(LinearConstraintPool linkedPool) {
        count = 0;
        dynamicPool = (linkedPool == null ? this : linkedPool);
        setCapacity(10);
    }
    
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
    
    public void clear() {
        count = 0;
    }
    
    public double getDynamicScaleFactor(int i) {
        return dynamicScaleFactors[i];
    }
    
    public int getDynamicLimitIndex(int i) {
        return dynamicLimits[i];
    }
    
    public int addConstraint(RigidBody bodyA, RigidBody bodyB, 
                              float taX, float taY, float taZ, 
                              float tbX, float tbY, float tbZ, 
                              float nX, float nY, float nZ, 
                              float laX, float laY, float laZ, 
                              float lbX, float lbY, float lbZ, 
                              float aaX, float aaY, float aaZ, 
                              float abX, float abY, float abZ) {
        int i = count++;
        int veci = i * 3;
        if (i >= bodyAs.length) {
            // increase capacity
            setCapacity((i + 1) * 2);
        }
        
        directions[veci] = nX; directions[veci + 1] = nY; directions[veci + 2] = nZ;
        torqueAs[veci] = taX; torqueAs[veci + 1] = taY; torqueAs[veci + 2] = taZ;
        torqueBs[veci] = tbX; torqueBs[veci + 1] = tbY; torqueBs[veci + 2] = tbZ;
        
        if (bodyA != null) {
            linearDirAs[veci] = laX; linearDirAs[veci + 1] = laY; linearDirAs[veci + 2] = laZ;
            angleDirAs[veci] = aaX; angleDirAs[veci + 1] = aaY; angleDirAs[veci + 2] = aaZ;
            bodyAs[i] = bodyA.getIndex();
        } else {
            bodyAs[i] = -1;
        }
        
        if (bodyB != null) {
            linearDirBs[veci] = lbX; linearDirBs[veci + 1] = lbY; linearDirBs[veci + 2] = lbZ;
            angleDirBs[veci] = abX; angleDirBs[veci + 1] = abY; angleDirBs[veci + 2] = abZ;
            bodyBs[i] = bodyB.getIndex();
        } else {
            bodyBs[i] = -1;
        }
        
        return i;
    }

    public int addConstraint(RigidBody bodyA, RigidBody bodyB, 
                             @Const Vector3 direction, @Const Vector3 torqueA, @Const Vector3 torqueB) {
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
            
            double imA = bodyA.getInverseMass();
            linearDirAs[veci] = direction.x * imA;
            linearDirAs[veci + 1] = direction.y * imA;
            linearDirAs[veci + 2] = direction.z * imA;
            
            this.torqueA.mul(bodyA.getInertiaTensorInverse(), torqueA);
            this.torqueA.get(angleDirAs, veci);
        } else {
            // assign negative id
            bodyAs[i] = -1;
        }
        
        if (bodyB != null) {
            bodyBs[i] = bodyB.getIndex();
            
            double imB = bodyB.getInverseMass();
            linearDirBs[veci] = direction.x * imB;
            linearDirBs[veci + 1] = direction.y * imB;
            linearDirBs[veci + 2] = direction.z * imB;
            
            this.torqueB.mul(bodyB.getInertiaTensorInverse(), torqueB);
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
    
    public @Const Vector3 getLinearImpulseA(int i, double impulse) {
        return linearA.set(linearDirAs, i * 3).scale(impulse);
    }
    
    public @Const Vector3 getLinearImpulseB(int i, double impulse) {
        return linearB.set(linearDirBs, i * 3).scale(impulse);
    }
    
    public @Const Vector3 getAngularImpulseA(int i, double impulse) {
        return angularA.set(angleDirAs, i * 3).scale(impulse);
    }
    
    public @Const Vector3 getAngularImpulseB(int i, double impulse) {
        return angularB.set(angleDirBs, i * 3).scale(impulse);
    }
    
    public void setAppliedImpulse(int i, double impulse) {
        appliedImpulses[i] = impulse;
    }
    
    public void setWarmstartImpulse(int i, double impulse) {
        warmstartImpulses[i] = impulse;
    }
    
    public void setSolution(int i, double solution, double cfm, double jacobian) {
        solutions[i] = solution;
        constraintForceMixes[i] = cfm;
        jacobianDiagInverses[i] = jacobian;
    }
    
    public void setStaticLimits(int i, double lower, double upper) {
        if (lower > upper)
            throw new IllegalArgumentException("Lower limit (" + lower + ") must be less than upper limit (" + upper + ")");
        lowerLimits[i] = lower;
        upperLimits[i] = upper;
        dynamicLimits[i] = -1; // clear dynamic limit
    }
    
    public void setDynamicLimits(int i, int linkedConstraint, double scale) {
        if (dynamicPool == null)
            throw new IllegalStateException("Pool does not have a linked pool");
        if (linkedConstraint < 0 || linkedConstraint > dynamicPool.count || scale <= 0)
            throw new IllegalArgumentException("Constraint index (" + linkedConstraint + ") is invalid, or scale (" + scale + ") is not positive");
        dynamicLimits[i] = linkedConstraint;
        dynamicScaleFactors[i] = scale;
    }
    
    public @Const Vector3 getConstraintDirection(int i) {
        return direction.set(directions, i * 3);
    }
    
    public @Const Vector3 getTorqueA(int i) {
        return torqueA.set(torqueAs, i * 3);
    }
    
    public @Const Vector3 getTorqueB(int i) {
        return torqueB.set(torqueBs, i * 3);
    }
    
    public double getJacobianDiagonalInverse(int i) {
        return jacobianDiagInverses[i];
    }
    
    public double getLowerImpulseLimit(int i) {
        int dynamic = dynamicLimits[i];
        if (dynamic >= 0) {
            return -dynamicScaleFactors[i] * dynamicPool.appliedImpulses[dynamic];
        } else {
            return lowerLimits[i];
        }
    }
    
    public double getUpperImpulseLimit(int i) {
        int dynamic = dynamicLimits[i];
        if (dynamic >= 0) {
            return dynamicScaleFactors[i] * dynamicPool.appliedImpulses[dynamic];
        } else {
            return upperLimits[i];
        }
    }
    
    public double getSolution(int i) {
        return solutions[i];
    }
    
    public double getConstraintForceMix(int i) {
        return constraintForceMixes[i];
    }
    
    public double getWarmstartImpulse(int i) {
        return warmstartImpulses[i];
    }
    
    public double getAppliedImpulse(int i) {
        return appliedImpulses[i];
    }
    
    public int getBodyAIndex(int i) {
        return bodyAs[i];
    }
    
    public int getBodyBIndex(int i) {
        return bodyBs[i];
    }
    
    public int getConstraintCount() {
        return count;
    }
}
