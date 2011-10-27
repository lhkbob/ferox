package com.ferox.physics.dynamics.constraint;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.physics.dynamics.RigidBody;

public class LinearConstraint {
    public interface ImpulseListener {
        public void onApplyImpulse(LinearConstraint lc);
    }
    
    float taX, taY, taZ, tbX, tbY, tbZ;
    float nX, nY, nZ, laX, laY, laZ, lbX, lbY, lbZ;
    float aaX, aaY, aaZ, abX, abY, abZ;
    
    float jacobianDiagInverse;
    
    SolverBody bodyA;
    SolverBody bodyB;
    
    float rhs; // right-hand-side of differential equation being solved
    float cfm; // constraint force mixing for soft constraints
   
    float appliedImpulse;
    
    private float lowerLimit;
    private float upperLimit;
    
    private ImpulseListener listener;
    private LinearConstraint dynamicLimitConstraint;
    private float dynamicLimitFactor;
    
    public MutableVector3f getTorqueAxisA(MutableVector3f result) {
        return getVector(taX, taY, taZ, result);
    }
    
    public MutableVector3f getTorqueAxisB(MutableVector3f result) {
        return getVector(tbX, tbY, tbZ, result);
    }
    
    public MutableVector3f getConstraintAxis(MutableVector3f result) {
        return getVector(nX, nY, nZ, result);
    }
    
    private MutableVector3f getVector(float x, float y, float z, MutableVector3f result) {
        if (result == null)
            return new Vector3f(x, y, z);
        else
            return result.set(x, y, z);
    }
    
    public float getJacobianInverse() {
        return jacobianDiagInverse;
    }
    
    public RigidBody getRigidBodyA() {
        return (bodyA == null ? null : bodyA.body);
    }
    
    public RigidBody getRigidBodyB() {
        return (bodyB == null ? null : bodyB.body);
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
    
    public void setConstraintAxis(ReadOnlyVector3f normal, ReadOnlyVector3f torqueA, ReadOnlyVector3f torqueB) {
        nX = normal.getX(); nY = normal.getY(); nZ = normal.getZ();
        taX = torqueA.getX(); taY = torqueA.getY(); taZ = torqueA.getZ();
        tbX = torqueB.getX(); tbY = torqueB.getY(); tbZ = torqueB.getZ();

        Vector3f t = new Vector3f();
        if (bodyA != null) {
            laX = nX * bodyA.inverseMass;
            laY = nY * bodyA.inverseMass;
            laZ = nZ * bodyA.inverseMass;
            
            bodyA.body.getInertiaTensorInverse().mul(torqueA, t);
            aaX = t.getX(); aaY = t.getY(); aaZ = t.getZ();
        }
        
        if (bodyB != null) {
            lbX = nX * bodyB.inverseMass;
            lbY = nY * bodyB.inverseMass;
            lbZ = nZ * bodyB.inverseMass;
            
            bodyB.body.getInertiaTensorInverse().mul(torqueB, t);
            abX = t.getX(); abY = t.getY(); abZ = t.getZ();
        }
    }
    
    public void setSolutionParameters(float rhs, float cfm) {
        this.rhs = rhs;
        this.cfm = cfm;
    }
    
    public void set(RigidBody bodyA, RigidBody bodyB, SolverBodyPool pool) {
        setRigidBodies(bodyA, bodyB, pool);
        
        nX = 0f; nY = 0f; nZ = 0f; 
        laX = 0f; laY = 0f; laZ = 0f; 
        lbX = 0f; lbY = 0f; lbZ = 0f;
        
        taX = 0f; taY = 0f; taZ = 0f; 
        tbX = 0f; tbY = 0f; tbZ = 0f;
        
        aaX = 0f; aaY = 0f; aaZ = 0f; 
        abX = 0f; abY = 0f; abZ = 0f;
        
        jacobianDiagInverse = 0f;
        rhs = 0f;
        cfm = 0f;
        
        lowerLimit = -Float.MAX_VALUE;
        upperLimit = Float.MAX_VALUE;
        dynamicLimitConstraint = null;
        dynamicLimitFactor = 0f;
        
        appliedImpulse = 0f;
        listener = null;
    }
    
    public void setImpulseListener(ImpulseListener listener) {
        this.listener = listener;
    }
    
    public void addDeltaImpulse(float delta) {
        appliedImpulse += delta;
        
        if (bodyA != null) {
            bodyA.dlX += delta * laX;
            bodyA.dlY += delta * laY;
            bodyA.dlZ += delta * laZ;
            
            bodyA.daX += delta * aaX;
            bodyA.daY += delta * aaY;
            bodyA.daZ += delta * aaZ;
        }
        
        if (bodyB != null) {
            bodyB.dlX -= delta * lbX;
            bodyB.dlY -= delta * lbY;
            bodyB.dlZ -= delta * lbZ;
            
            bodyB.daX -= delta * abX;
            bodyB.daY -= delta * abY;
            bodyB.daZ -= delta * abZ;
        }
        
        if (listener != null)
            listener.onApplyImpulse(this);
    }
    
    private void setRigidBodies(RigidBody a, RigidBody b, SolverBodyPool pool) {
        if (a == null && b == null)
            throw new NullPointerException("Both RigidBodies cannot be null");
        bodyA = (a == null ? null : pool.get(a));
        bodyB = (b == null ? null : pool.get(b));
    }
    
    @Override
    public String toString() {
        return String.format("Constraint: [%.6f, %.6f, %.6f], [%.6f, %.6f, %.6f], [%.6f, %.6f, %.6f], %.6f, %.6f, %.6f, %.6f, %.6f",
                             nX, nY, nZ, aaX, aaY, aaZ, abX, abY, abZ, rhs, cfm, getLowerLimit(), getUpperLimit(), appliedImpulse);
    }
}
