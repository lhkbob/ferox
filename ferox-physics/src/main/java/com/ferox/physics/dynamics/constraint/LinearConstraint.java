package com.ferox.physics.dynamics.constraint;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.physics.dynamics.RigidBody;

public class LinearConstraint {
    public interface ImpulseListener {
        public void onApplyImpulse(LinearConstraint lc);
    }
    
    // What classes of entity are there?
    // 1. rigid dynamic body, has mass, shape, position, and is physic'ed
    // 2. static object, has shape and position, but no mass and doesn't move
    // 3. kinematic object, has shape and position, no mass and is not physic'ed (moved externally)
    
    // The problem is that not everything has mass, and not everything moves,
    // but everything has a shape and position.
    // Do we classify non-moving things as having 0 mass, and only use one component type then?
    // or do we have two, where one does not provide position, etc.
    // Yes, let's do that
    
    // FIXME this should be converted into a collection of properties so that
    // all linear constraints are packed together in cache structures
    //
    // SolverBody can be handled as decorated properties onto the RigidBody components
    // I need two component types, one for Collidables, and another for making
    // them dynamic. (the only question is how to update dynamic body's position
    // dependent info, but since that only applies to rigid-body's, there's nothing
    // wrong with having the controller's handle that).
    //
    // One thing to wonder about is the need to shuffle the constraints. Do
    // they need to be shuffled every iteration? How much does that improve 
    // convergence, and is it a big performance hit. Is it easier to shuffle
    // an index array and iterate through that?
    //
    // That would make cache misses happen more, but the shuffling would be faster.
    // Shuffling the entire packed constraint arrays would involve lots of data 
    // movement, which could be slower.
    //
    //
    // The other structures that I would like to pack somehow are the contact
    // manifolds stored between pairs of objects. Since they are unique based
    // pair of collidable, I can't just store them in the entity system. I would
    // also need some fast way of accessing the index that would allow me to
    // pack the manifolds together.
    //
    // Then within a manifold, it can store up to 4 contact points, so its used
    // space needs to be somewhat flexible. That being said, since it has a final
    // max, we can just allocate the largest size for each block.
    
    // Constraint manifolds are reasonably heavy-weight objects, I should see
    // if I can simplify the collision model so that I can reduce storage somehow.
    // Otherwise I'll need to think of a fast indexing method that lets me
    // avoid using a standard collection, but allows me to easily lookup based
    // on two entity ids the manifold to use, since they have to be memoized
    // across frames.
    
    //
    // I might be able to store or link to them using decorator information,
    // but that would require having each collidable component have a list
    // of all other current collidables its in contact with, it couldn't just
    // be a single value. These structures would have to be mirrored in the
    // event that a pair was produced in flipped order during a subsequent update.
    
    // It might be that doing a loop over 4-8 ints is faster than a hashmap lookup,
    // and then we could just have that store a parallel offset into the packed
    // manifold structure. These could then be reclaimed, etc. to allow new
    // manifolds to take over the indices or get tacked to the end.
    // I feel like this will be a powerful strategy
    
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
