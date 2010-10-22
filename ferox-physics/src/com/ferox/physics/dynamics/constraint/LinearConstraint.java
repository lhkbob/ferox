package com.ferox.physics.dynamics.constraint;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.physics.dynamics.RigidBody;

public class LinearConstraint {
    private final Vector3f relposACrossNormal;
    private final Vector3f relposBCrossNormal;
    
    private final Vector3f constraintNormal;
    
    private final Vector3f angularComponentA;
    private final Vector3f angularComponentB;
    
    private float jacobianDiagInverse;
    
    private RigidBody bodyA;
    private RigidBody bodyB;
    
    private float rhs; // right-hand-side of differential equation being solved
    private float cfm; // constraint force mixing for soft constraints
    private float lowerLimit;
    private float upperLimit;
    
    private LinearConstraint dynamicLimitConstraint;
    private float dynamicLimitFactor;
    
    private float appliedImpulse;
    
    public LinearConstraint(RigidBody bodyA, RigidBody bodyB) {
        setRigidBodies(bodyA, bodyB);
        
        relposACrossNormal = new Vector3f();
        relposBCrossNormal = new Vector3f();
        constraintNormal = new Vector3f();
        
        angularComponentA = new Vector3f();
        angularComponentB = new Vector3f();
    }
    
    public ReadOnlyVector3f getTorqueAxisA() {
        return relposACrossNormal;
    }
    
    public ReadOnlyVector3f getTorqueAxisB() {
        return relposBCrossNormal;
    }
    
    public ReadOnlyVector3f getConstraintAxis() {
        return constraintNormal;
    }
    
    public float getJacobianInverse() {
        return jacobianDiagInverse;
    }
    
    public RigidBody getRigidBodyA() {
        return bodyA;
    }
    
    public RigidBody getRigidBodyB() {
        return bodyB;
    }
    
    public float getRightHandSide() {
        return rhs;
    }
    
    public float getConstraintForceMix() {
        return cfm;
    }
    
    public float getUpperLimit() {
        if (dynamicLimitConstraint != null)
            return dynamicLimitConstraint.appliedImpulse * dynamicLimitFactor;
        else
            return upperLimit;
    }
    
    public float getLowerLimit() {
        if (dynamicLimitConstraint != null)
            return -dynamicLimitConstraint.appliedImpulse * dynamicLimitFactor;
        else
            return lowerLimit;
    }
    
    public float getAppliedImpulse() {
        return appliedImpulse;
    }
    
    public LinearConstraint getDynamicLimitConstraint() {
        return dynamicLimitConstraint;
    }
    
    public float getDynamicLimitFactor() {
        return dynamicLimitFactor;
    }
    
    public void setJacobianInverse(float diag) {
//        if (diag <= 0f)
//            throw new IllegalArgumentException("Jacobian is meant to be positive, semi-definate. Can't have a negative element: " + diag);
        jacobianDiagInverse = diag;
    }
    
    public void setAppliedImpulse(float impulseMagnitude) {
        if (impulseMagnitude < 0f)
            throw new IllegalArgumentException("Applied impulse cannot be negative");
        appliedImpulse = impulseMagnitude;
    }
    
    public void setLimits(float lower, float upper) {
        if (lower > upper)
            throw new IllegalArgumentException("Lower limit (" + lower + ") must be less than upper limit (" + upper + ")");
        lowerLimit = lower;
        upperLimit = upper;
        
        // clear dynamic limit
        dynamicLimitConstraint = null;
    }
    
    public void setDynamicLimits(LinearConstraint link, float scale) {
        if (link !=  null && scale <= 0f)
            throw new IllegalArgumentException("Dynamic limit scale must be positive if dynamic link is used, not: " + scale);
        
        dynamicLimitConstraint = link;
        dynamicLimitFactor = scale;
    }
    
    public void setTorqueAxis(ReadOnlyVector3f torqueA, ReadOnlyVector3f torqueB) {
        relposACrossNormal.set(torqueA);
        if (bodyA != null)
            bodyA.getInertiaTensorInverse().mul(torqueA, angularComponentA);
        else
            angularComponentA.set(0f, 0f, 0f);
        
        relposBCrossNormal.set(torqueB);
        if (bodyB != null)
            bodyB.getInertiaTensorInverse().mul(torqueB, angularComponentB);
        else
            angularComponentB.set(0f, 0f, 0f);
    }
    
    public void setConstraintAxis(ReadOnlyVector3f normal) {
        constraintNormal.set(normal);
    }
    
    public void setSolutionParameters(float rhs, float cfm) {
        if (rhs > 0 || cfm > 0) {
//            System.out.println("bad constraint: " + rhs);
        }
        this.rhs = rhs;
        this.cfm = cfm;
    }
    
    public void reset(RigidBody bodyA, RigidBody bodyB) {
        setRigidBodies(bodyA, bodyB);
        
        relposACrossNormal.set(0f, 0f, 0f);
        relposBCrossNormal.set(0f, 0f, 0f);
        constraintNormal.set(0f, 0f, 0f);
        angularComponentA.set(0f, 0f, 0f);
        angularComponentB.set(0f, 0f, 0f);
        
        jacobianDiagInverse = 0f;
        rhs = 0f;
        cfm = 0f;
        
        lowerLimit = -Float.MAX_VALUE;
        upperLimit = Float.MAX_VALUE;
        dynamicLimitConstraint = null;
        dynamicLimitFactor = 0f;
        
        appliedImpulse = 0f;
    }
    
    public void addDeltaImpulse(float delta) {
        appliedImpulse += delta;
        
        Vector3f t = temp.get();
        if (bodyA != null)
            bodyA.addDeltaImpulse(constraintNormal.scale(bodyA.getInverseMass(), t), angularComponentA, delta);
        if (bodyB != null)
            bodyB.addDeltaImpulse(constraintNormal.scale(-bodyB.getInverseMass(), t), angularComponentB, delta);
    }
    
    private void setRigidBodies(RigidBody a, RigidBody b) {
        if (a == null && b == null)
            throw new NullPointerException("Both RigidBodies cannot be null");
        bodyA = a;
        bodyB = b;
    }
    
    @Override
    public String toString() {
        return "Constraint along " + constraintNormal + " (A: " + angularComponentA + ", B: " + angularComponentB + "), jac: " + jacobianDiagInverse + ", rhs: " + rhs + ", applied: " + appliedImpulse;
    }
    
    private static final ThreadLocal<Vector3f> temp = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
}
