package com.ferox.physics.dynamics;

import java.util.Arrays;

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Vector3;
import com.ferox.math.entreri.Matrix3Property;
import com.ferox.math.entreri.Vector3Property;
import com.lhkbob.entreri.property.DoubleProperty;

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
    private double[] upperLimits = new double[0]; // upper limit on any change in impulse
    private double[] lowerLimits = new double[0]; // lower limit on any change in impulse
    
    // this int[] stores indexes back into this pool of constraints
    private int[] dynamicLimits = new int[0]; // dynamically limit impulse by impulse of other constraint
    private double[] dynamicScaleFactors = new double[0]; // scale factor applied to dynamic limits
 
    // these two int[] store indexes into the RigidBody table in the system
    private int[] bodyAs = new int[0];
    private int[] bodyBs = new int[0];
    
    // accessed by values inside bodyAs and bodyBs
    private Matrix3Property inertiaTensorInverses;
    private DoubleProperty inverseMasses;
    private Vector3Property deltaLinearImpulses;
    private Vector3Property deltaAngularImpulses;
    
    private final Vector3 direction = new Vector3();
    private final Vector3 torqueA = new Vector3();
    private final Vector3 torqueB = new Vector3();
    private final Matrix3 tensor = new Matrix3();
    
    // FIXME we might be able to avoid this if we have the manifolds remember
    // the indices of the constraints they produce and as part of the final
    // update phase, they read back the total applied impulse
//    private Object impulseListener;
    
    public LinearConstraintPool() {
        count = 0;
        setCapacity(10);
    }
    
    public void setInertiaTensorInverseProperty(Matrix3Property prop) {
        inertiaTensorInverses = prop;
    }
    
    public void setInverseMassProperty(DoubleProperty prop) {
        inverseMasses = prop;
    }
    
    public void setDeltaLinearImpulseProperty(Vector3Property prop) {
        deltaLinearImpulses = prop;
    }
    
    public void setDeltaAngularImpulseProperty(Vector3Property prop) {
        deltaAngularImpulses = prop;
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
        upperLimits = Arrays.copyOf(upperLimits, newCapacity);
        lowerLimits = Arrays.copyOf(lowerLimits, newCapacity);
        dynamicScaleFactors = Arrays.copyOf(dynamicScaleFactors, newCapacity);
        dynamicLimits = Arrays.copyOf(dynamicLimits, newCapacity);
        bodyAs = Arrays.copyOf(bodyAs, newCapacity);
        bodyBs = Arrays.copyOf(bodyBs, newCapacity);
        // FIXME allocate listeners too, unless we're not using them here
        
        count = Math.min(newCapacity, count);
    }
    
    public void clear() {
        count = 0;
    }

    public int addConstraint(int bodyA, int bodyB) {
        int i = count++;
        int veci = i * 3;
        if (i >= bodyAs.length) {
            // increase capacity (since capacity starts at 10, we know i != 0)
            setCapacity(i * 2);
        }
        
        bodyAs[i] = bodyA;
        bodyBs[i] = bodyB;
        
        // set vectors to 0
        // FIXME if 0'ing these values are too expensive, we can combine this
        // method with setConstraintAxis so we just overwrite with new data
        directions[veci] = 0; directions[veci + 1] = 0; directions[veci + 2] = 0;
        torqueAs[veci] = 0; torqueAs[veci + 1] = 0; torqueAs[veci + 2] = 0;
        torqueBs[veci] = 0; torqueBs[veci + 1] = 0; torqueBs[veci + 2] = 0;
        linearDirAs[veci] = 0; linearDirAs[veci + 1] = 0; linearDirAs[veci + 2] = 0;
        linearDirBs[veci] = 0; linearDirBs[veci + 1] = 0; linearDirBs[veci + 2] = 0;
        angleDirAs[veci] = 0; angleDirAs[veci + 1] = 0; angleDirAs[veci + 2] = 0;
        angleDirBs[veci] = 0; angleDirBs[veci + 1] = 0; angleDirBs[veci + 2] = 0;

        // zero out other values, too
        jacobianDiagInverses[i] = 0;
        solutions[i] = 0;
        constraintForceMixes[i] = 0;
        
        lowerLimits[i] = -Double.MAX_VALUE;
        upperLimits[i] = Double.MAX_VALUE;
        dynamicScaleFactors[i] = 0;
        dynamicLimits[i] = -1;
        
        appliedImpulses[i] = 0;
        
        return i;
    }
    
    public void addDeltaImpulse(int i, double deltaImpulse) {
        int veci = i * 3;
        appliedImpulses[i] += deltaImpulse;
        
        int ba = bodyAs[i];
        if (ba >= 0) {
            deltaLinearImpulses.get(ba, direction);
            direction.x += deltaImpulse * linearDirAs[veci];
            direction.y += deltaImpulse * linearDirAs[veci + 1];
            direction.z += deltaImpulse * linearDirAs[veci + 2];
            deltaLinearImpulses.set(direction, ba);
            
            deltaAngularImpulses.get(ba, direction);
            direction.x += deltaImpulse * angleDirAs[veci];
            direction.y += deltaImpulse * angleDirAs[veci + 1];
            direction.z += deltaImpulse * angleDirAs[veci + 2];
            deltaAngularImpulses.set(direction, ba);
        }
        
        int bb = bodyBs[i];
        if (bb >= 0) {
            deltaLinearImpulses.get(bb, direction);
            direction.x += deltaImpulse * linearDirBs[veci];
            direction.y += deltaImpulse * linearDirBs[veci + 1];
            direction.z += deltaImpulse * linearDirBs[veci + 2];
            deltaLinearImpulses.set(direction, bb);
            
            deltaAngularImpulses.get(bb, direction);
            direction.x += deltaImpulse * angleDirBs[veci];
            direction.y += deltaImpulse * angleDirBs[veci + 1];
            direction.z += deltaImpulse * angleDirBs[veci + 2];
            deltaAngularImpulses.set(direction, bb);
        }
    }
    
    public void setAppliedImpulse(int i, double impulse) {
        appliedImpulses[i] = impulse;
    }
    
    public void setConstraintAxis(int i, @Const Vector3 direction, @Const Vector3 torqueA, @Const Vector3 torqueB) {
        int veci = i * 3;
        
        direction.get(directions, veci);
        torqueA.get(torqueAs, veci);
        torqueB.get(torqueBs, veci);
        
        int ba = bodyAs[i];
        if (ba >= 0) {
            double imA = inverseMasses.get(ba);
            linearDirAs[veci] = direction.x * imA;
            linearDirAs[veci + 1] = direction.y * imA;
            linearDirAs[veci + 2] = direction.z * imA;
            
            this.torqueA.mul(inertiaTensorInverses.get(ba, tensor), torqueA);
            angleDirAs[veci] = this.torqueA.x;
            angleDirAs[veci + 1] = this.torqueA.y;
            angleDirAs[veci + 2] = this.torqueA.z;
        }
        
        int bb = bodyBs[i];
        if (bb >= 0) {
            double imB = inverseMasses.get(bb);
            linearDirBs[veci] = direction.x * imB;
            linearDirBs[veci + 1] = direction.y * imB;
            linearDirBs[veci + 2] = direction.z * imB;
            
            this.torqueB.mul(inertiaTensorInverses.get(bb, tensor), torqueB);
            angleDirBs[veci] = this.torqueB.x;
            angleDirBs[veci + 1] = this.torqueB.y;
            angleDirBs[veci + 2] = this.torqueB.z;
        }
    }
    
    public void setSolution(int i, double solution, double cfm) {
        solutions[i] = solution;
        constraintForceMixes[i] = cfm;
    }
    
    public void setJacobianDiagonalInverse(int i, double jacobian) {
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
        if (linkedConstraint < 0 || linkedConstraint > count || scale <= 0)
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
            return -dynamicScaleFactors[i] * appliedImpulses[dynamic];
        } else {
            return lowerLimits[i];
        }
    }
    
    public double getUpperImpulseLimit(int i) {
        int dynamic = dynamicLimits[i];
        if (dynamic >= 0) {
            return dynamicScaleFactors[i] * appliedImpulses[dynamic];
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
