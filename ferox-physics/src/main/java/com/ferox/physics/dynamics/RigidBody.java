package com.ferox.physics.dynamics;

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Vector3;
import com.ferox.math.entreri.Matrix3Property;
import com.ferox.math.entreri.Vector3Property;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.DoubleProperty;

public class RigidBody extends ComponentData<RigidBody> {
    private DoubleProperty inverseMass;
    
    private Matrix3Property inertiaTensorWorldInverse;

    private Vector3Property velocity;
    private Vector3Property angularVelocity;
    
    private Vector3Property totalForce;
    private Vector3Property totalTorque;
    
    // This got pretty ugly pretty fast
    @Unmanaged private final Vector3 velocityCache = new Vector3();
    @Unmanaged private final Vector3 angularVelocityCache = new Vector3();
    @Unmanaged private final Vector3 forceCache = new Vector3();
    @Unmanaged private final Vector3 torqueCache = new Vector3();
    @Unmanaged private final Matrix3 tensorCache = new Matrix3();
    
    @Unmanaged private final Vector3 temp = new Vector3();
    
    private RigidBody() { }
    
    // FIXME depending on how many people need to use this, I can move it
    // into the set of decorated properties that are added to the rigid body
    // for contact solving support
    public @Const Matrix3 getInertiaTensorInverse() {
        return tensorCache;
    }
    
    /* FIXME move these functions into the internals of the physics controller
    private void updateInertiaTensor() {
        // FIXME: what about kinematic objects? they don't get inertia really
        // FIXME: the inertia tensor needs to be inverted (e.g. 1/x, 1/y, 1/z) here
        MutableVector3f inertia = getShape().getInertiaTensor(getMass(), temp3.get());
        inertia.set(1f / inertia.getX(), 1f / inertia.getY(), 1f / inertia.getZ());
        
        ReadOnlyMatrix3f rotation = getTransform().getUpperMatrix(); // since Collidable uses AffineTransform, this doesn't create an object
        rotation.mulDiagonal(inertia, inertiaTensorWorldInverse).mulTransposeRight(rotation);
    }
    
    public void applyForces(Integrator integrator, float dt) {
        if (dt <= 0f)
            throw new IllegalArgumentException("Time delta must be positive, not: " + dt);
        
        integrator.integrateLinearAcceleration(totalForce.scale(inverseMass), dt, velocity);
        integrator.integrateAngularAcceleration(inertiaTensorWorldInverse.mul(totalTorque), dt, angularVelocity);
        clearForces();
    }
    
    public void predictMotion(Integrator integrator, float dt, AffineTransform result) {
        if (dt <= 0f)
            throw new IllegalArgumentException("Time delta must be positive, not: " + dt);
        
        // FIXME what about kinematic objects?
        result.set(getTransform());
        integrator.integrateLinearVelocity(velocity, dt, result.getTranslation());
        integrator.integrateAngularVelocity(angularVelocity, dt, result.getRotation());
    }
    */
    
    public void addForce(@Const Vector3 force, @Const Vector3 relPos) {
        forceCache.add(force);
        totalForce.set(forceCache, getIndex());
        
        if (relPos != null) {
            torqueCache.add(temp.cross(relPos, force));
            totalTorque.set(torqueCache, getIndex());
        }
    }
    
    public void addImpulse(@Const Vector3 impulse, @Const Vector3 relPos) {
        velocityCache.add(temp.scale(impulse, getInverseMass()));
        velocity.set(velocityCache, getIndex());
        
        if (relPos != null) {
            temp.cross(relPos, impulse);
            temp.mul(tensorCache, temp);
            angularVelocityCache.add(temp);
            
            angularVelocity.set(angularVelocityCache, getIndex());
        }
    }
    
    public void setVelocity(@Const Vector3 vel) {
        velocityCache.set(vel);
        velocity.set(vel, getIndex());
    }
    
    public void setAngularVelocity(@Const Vector3 angVel) {
        angularVelocityCache.set(angVel);
        angularVelocity.set(angVel, getIndex());
    }
    
    public void clearForces() {
        forceCache.set(0.0, 0.0, 0.0);
        torqueCache.set(0.0, 0.0, 0.0);
        
        totalForce.set(forceCache, getIndex());
        totalTorque.set(torqueCache, getIndex());
    }

    public void setMass(double mass) {
        if (mass <= 0.0)
            throw new IllegalArgumentException("Mass must be positive");
        inverseMass.set(1.0 / mass, getIndex());
    }

    public double getMass() {
        return 1.0 / inverseMass.get(getIndex());
    }

    public double getInverseMass() {
        return inverseMass.get(getIndex());
    }

    public @Const Vector3 getVelocity() {
        return velocityCache;
    }

    public @Const Vector3 getAngularVelocity() {
        return angularVelocityCache;
    }
    
    @Override
    protected void onSet(int index) {
        // FIXME this might be a performance bottleneck if we don't need to
        // access every single vector object when processing a given rigid body
        angularVelocity.get(index, angularVelocityCache);
        velocity.get(index, velocityCache);
        inertiaTensorWorldInverse.get(index, tensorCache);
        totalForce.get(index, forceCache);
        totalTorque.get(index, torqueCache);
    }
}
