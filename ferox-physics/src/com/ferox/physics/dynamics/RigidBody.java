package com.ferox.physics.dynamics;

import com.ferox.math.Matrix3f;
import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyMatrix3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.shape.Shape;
import com.ferox.physics.dynamics.constraint.SolverBody;

public class RigidBody extends Collidable {
    private float inverseMass; // 1 / mass
    private final Matrix3f inertiaTensorWorldInverse;

    private ReadOnlyVector3f explicitGravity; // null if gravity is taken from world

    private final Vector3f velocity;
    private final Vector3f angularVelocity;
    private final Vector3f totalForce;
    private final Vector3f totalTorque;
    
    private final SolverBody solverBody;

    public RigidBody(ReadOnlyMatrix4f t, Shape shape, float mass) {
        super(t, shape);

        velocity = new Vector3f();
        angularVelocity = new Vector3f();
        inertiaTensorWorldInverse = new Matrix3f();

        totalForce = new Vector3f();
        totalTorque = new Vector3f();
        
        solverBody = new SolverBody();
        
        setMass(mass);
    }
    
    public SolverBody getSolverBody() {
        solverBody.inverseMass = inverseMass;
        solverBody.body = this;
        return solverBody;
    }
    
    public void updateFromSolverBody() {
        velocity.set(velocity.getX() + solverBody.dlX,
                     velocity.getY() + solverBody.dlY,
                     velocity.getZ() + solverBody.dlZ);
        angularVelocity.set(angularVelocity.getX() + solverBody.daX,
                            angularVelocity.getY() + solverBody.daY,
                            angularVelocity.getZ() + solverBody.daZ);
        
        solverBody.dlX = 0f; solverBody.dlY = 0f; solverBody.dlZ = 0f;
        solverBody.daX = 0f; solverBody.daY = 0f; solverBody.daZ = 0f;
    }
    
    @Override
    public void setWorldTransform(ReadOnlyMatrix4f t) {
        super.setWorldTransform(t);
        updateInertiaTensor();
    }
    
    private void updateInertiaTensor() {
        // FIXME: what about kinematic objects? they don't get inertia really
        // FIXME: the inertia tensor needs to be inverted (e.g. 1/x, 1/y, 1/z) here
        MutableVector3f inertia = getShape().getInertiaTensor(getMass(), temp3.get());
        inertia.set(1f / inertia.getX(), 1f / inertia.getY(), 1f / inertia.getZ());
        
        ReadOnlyMatrix3f rotation = getWorldTransform().getUpperMatrix(); // since Collidable uses Transform, this doesn't create an object
        rotation.mulDiagonal(inertia, inertiaTensorWorldInverse).mulTransposeRight(rotation);
    }
    
    public ReadOnlyMatrix3f getInertiaTensorInverse() {
        return inertiaTensorWorldInverse;
    }
    
    public void applyForces(Integrator integrator, float dt) {
        if (dt <= 0f)
            throw new IllegalArgumentException("Time delta must be positive, not: " + dt);
        
        integrator.integrateLinearAcceleration(totalForce.scale(inverseMass), dt, velocity);
        integrator.integrateAngularAcceleration(inertiaTensorWorldInverse.mul(totalTorque), dt, angularVelocity);
        clearForces();
    }
    
    public void predictMotion(Integrator integrator, float dt, Transform result) {
        if (dt <= 0f)
            throw new IllegalArgumentException("Time delta must be positive, not: " + dt);
        
        // FIXME what about kinematic objects?
        result.set(getWorldTransform());
        integrator.integrateLinearVelocity(velocity, dt, result.getTranslation());
        integrator.integrateAngularVelocity(angularVelocity, dt, result.getRotation());
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
            throw new IllegalArgumentException("Mass must be positive");
        inverseMass = 1f / mass;
        updateInertiaTensor();
    }

    public float getMass() {
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
    
    /* 
     * ThreadLocals for computations. 
     */
    
    private static final ThreadLocal<Vector3f> temp3 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
}
