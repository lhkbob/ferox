package com.ferox.physics.dynamics;

import com.ferox.math.Matrix3f;
import com.ferox.math.MutableQuat4f;
import com.ferox.math.MutableVector3f;
import com.ferox.math.Quat4f;
import com.ferox.math.ReadOnlyMatrix3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.Shape;

public class RigidBody extends Collidable {
    private static final float ANGULAR_MOTION_THRESHOLD = (float) Math.PI / 4f;
    private static final float MAX_ANGULAR_VELOCITY = (float) Math.PI  / 2f;
    
    private final Transform predictedTransform;

    private float inverseMass; // 1 / mass
    private final Matrix3f inertiaTensorWorldInverse;

    private ReadOnlyVector3f explicitGravity; // null if gravity is taken from world

    private final Vector3f velocity;
    private final Vector3f angularVelocity;
    private final Vector3f totalForce;
    private final Vector3f totalTorque;

    private final Vector3f deltaLinearVelocity;
    private final Vector3f deltaAngularVelocity;
    
    public RigidBody(ReadOnlyMatrix4f t, Shape shape, float mass) {
        super(t, shape);

        velocity = new Vector3f();
        angularVelocity = new Vector3f();
        inertiaTensorWorldInverse = new Matrix3f();
        deltaLinearVelocity = new Vector3f();
        deltaAngularVelocity = new Vector3f();

        totalForce = new Vector3f();
        totalTorque = new Vector3f();
        
        predictedTransform = new Transform();

        setMass(mass);
    }
    
    public ReadOnlyVector3f getDeltaLinearVelocity() {
        return deltaLinearVelocity;
    }
    
    public ReadOnlyVector3f getDeltaAngularVelocity() {
        return deltaAngularVelocity;
    }

    public void addDeltaImpulse(ReadOnlyVector3f linear, ReadOnlyVector3f angular, float magnitude) {
        // FIXME: don't do anything if kinematic
        linear.scaleAdd(magnitude, deltaLinearVelocity, deltaLinearVelocity);
        angular.scaleAdd(magnitude, deltaAngularVelocity, deltaAngularVelocity);
    }
    
    public void applyDeltaImpulse() {
        // FIXME: dont' do anything for kinematic
        velocity.add(deltaLinearVelocity);
        angularVelocity.add(deltaAngularVelocity);
        
        deltaLinearVelocity.set(0f, 0f, 0f);
        deltaAngularVelocity.set(0f, 0f, 0f);
    }
    
    @Override
    public void setWorldTransform(ReadOnlyMatrix4f t) {
        super.setWorldTransform(t);
        
        // FIXME: what about kinematic objects? they don't get inertia really
        ReadOnlyVector3f inertia = getShape().getInertiaTensor(getMass(), temp3.get());
        Transform toWorld = getWorldTransform();
        toWorld.getRotation().mulDiagonal(inertia, inertiaTensorWorldInverse).mulTransposeRight(toWorld.getRotation());
    }
    
    public ReadOnlyMatrix4f getPredictedTransform() {
        return predictedTransform;
    }
    
    public ReadOnlyMatrix3f getInertiaTensorInverse() {
        return inertiaTensorWorldInverse;
    }
    
    public void predictMotion(float dt) {
        if (dt <= 0f)
            throw new IllegalArgumentException("Time delta must be positive, not: " + dt);
        
        if (inverseMass <= 0f) {
            // handle kinematic bodies differently
            // FIXME: get velocity from (transform - predTransform) / dt
            // then set predTransform = transform
            // and return
            predictedTransform.set(getWorldTransform());
        } else {
            // integrate accumulated forces into the velocity
            totalForce.scaleAdd(dt * inverseMass, velocity, velocity);
            
            // we can store transformed torque in totalTorque because
            // we're resetting the forces anyway
            angularVelocity.add(inertiaTensorWorldInverse.mul(totalTorque).scale(dt));
            
            // clamp angular velocity or collision detection can fail
            float angvel = angularVelocity.length() * dt;
            if (angvel > MAX_ANGULAR_VELOCITY)
                angularVelocity.scale(MAX_ANGULAR_VELOCITY / angvel);

            
            // reset all forces // FIXME: do we really want to do this here?
            clearForces();
            
            // now integrate velocities into predicted transform
            velocity.scaleAdd(dt, getWorldTransform().getTranslation(), predictedTransform.getTranslation());
            
            // angular velocity uses the exponential map method
            // "Practical Parametrization of Rotations Using the Exponential Map", F. Sebastian Grassia
            Vector3f axis = temp3.get();
            float fAngle = angularVelocity.length();
            // limit the angular motion
            if (fAngle * dt > ANGULAR_MOTION_THRESHOLD)
                fAngle = ANGULAR_MOTION_THRESHOLD / dt;
            
            if (fAngle < .001f) {
                // use Taylor's expansions of sync function
                angularVelocity.scale((.5f * dt) - (dt * dt * dt) * (.02083333333333f * fAngle * fAngle), axis);
            } else {
                // sync(fAngle) = sin(c * fAngle) / t
                angularVelocity.scale((float) Math.sin(.5f * fAngle * dt) / fAngle, axis);
            }
            
            MutableQuat4f newRot = tempq1.get().set(axis.getX(), axis.getY(), axis.getZ(), (float) Math.cos(.5f * fAngle * dt));
            MutableQuat4f oldRot = tempq2.get().set(getWorldTransform().getRotation());
            newRot.mul(oldRot).normalize();
            
            // store computed transform into predicted transform
            predictedTransform.getRotation().set(newRot);
        }
    }
    
    public void addForce(ReadOnlyVector3f force, ReadOnlyVector3f relPos) {
        if (force == null)
            throw new NullPointerException("Force vector cannot be null");
        totalForce.add(force);
        
        if (relPos != null)
            totalTorque.add(relPos.cross(force, temp3.get()));
    }
    
    public void addImpulse(ReadOnlyVector3f impulse, ReadOnlyVector3f relPos) {
        if (impulse == null)
            throw new NullPointerException("Force vector cannot be null");
        
        impulse.scaleAdd(inverseMass, velocity, velocity);
        
        if (relPos != null) {
            MutableVector3f torque = relPos.cross(impulse, temp3.get());
            angularVelocity.add(inertiaTensorWorldInverse.mul(torque));
        }
    }
    
    public void clearForces() {
        totalForce.set(0f, 0f, 0f);
        totalTorque.set(0f, 0f, 0f);
    }

    public ReadOnlyVector3f getExplicitGravity() {
        return explicitGravity;
    }

    public void setExplicitGravity(ReadOnlyVector3f gravity) {
        explicitGravity = gravity;
    }

    public void setMass(float mass) {
        if (mass <= 0f)
            inverseMass = -1f;
        else
            inverseMass = 1f / mass;
    }

    public float getMass() {
        if (inverseMass <= 0f)
            return -1f;
        else
            return 1f / inverseMass;
    }

    public float getInverseMass() {
        return inverseMass;
    }

    public ReadOnlyVector3f getVelocity() {
        return velocity;
    }

    public ReadOnlyVector3f getAngularVelocity() {
        return angularVelocity;
    }

    public boolean isKinematic() {
        return inverseMass <= 0f;
    }
    
    
    /* 
     * ThreadLocals for computations. 
     */
    
    private static final ThreadLocal<Vector3f> temp3 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
    private static final ThreadLocal<Quat4f> tempq1 = new ThreadLocal<Quat4f>() {
        @Override
        protected Quat4f initialValue() { return new Quat4f(); }
    };
    private static final ThreadLocal<Quat4f> tempq2 = new ThreadLocal<Quat4f>() {
        @Override
        protected Quat4f initialValue() { return new Quat4f(); }
    };
}
