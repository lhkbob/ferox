package com.ferox.physics.dynamics;

import com.ferox.math.MutableMatrix3f;
import com.ferox.math.MutableQuat4f;
import com.ferox.math.MutableVector3f;
import com.ferox.math.Quat4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public class ExplicitEulerIntegrator implements Integrator {
    private static final float MAX_ANGULAR_VELOCITY = (float) Math.PI  / 2f;
    private static final float ANGULAR_MOTION_THRESHOLD = (float) Math.PI / 4f;

    @Override
    public void integrateLinearAcceleration(ReadOnlyVector3f a, float dt, MutableVector3f velocity) {
        integrateVector(a, dt, velocity);
    }

    @Override
    public void integrateAngularAcceleration(ReadOnlyVector3f a, float dt,
                                             MutableVector3f angularVelocity) {
        integrateVector(a, dt, angularVelocity);
    }

    @Override
    public void integrateLinearVelocity(ReadOnlyVector3f v, float dt, MutableVector3f position) {
        integrateVector(v, dt, position);
    }

    @Override
    public void integrateAngularVelocity(ReadOnlyVector3f v, float dt, MutableMatrix3f orientation) {
        // clamp angular velocity
        Vector3f axis = temp3.get();

        float veclength = v.length();
        float angvel = veclength;
        if (angvel * dt > MAX_ANGULAR_VELOCITY) {
            // set axis to be linear velocity but with magnitude = MAX / dt
            axis.scale(MAX_ANGULAR_VELOCITY / (angvel * dt));
            angvel = MAX_ANGULAR_VELOCITY / dt;
        } else
            axis.set(v); // don't need to clamp so just set axis to angular velocity
        
        // angular velocity uses the exponential map method
        // "Practical Parameterization of Rotations Using the Exponential Map", F. Sebastian Grassia

        // limit the angular motion - but update the velocity vector
        float fAngle = angvel;
        if (angvel * dt > ANGULAR_MOTION_THRESHOLD)
            fAngle = ANGULAR_MOTION_THRESHOLD / dt;
        
        if (fAngle < .001f) {
            // use Taylor's expansions of sync function
            axis.scale((.5f * dt) - (dt * dt * dt) * (.02083333333333f * fAngle * fAngle));
        } else {
            // sync(fAngle) = sin(c * fAngle) / t
            axis.scale((float) Math.sin(.5f * fAngle * dt) / fAngle);
        }
        
        MutableQuat4f newRot = tempq1.get().set(axis.getX(), axis.getY(), axis.getZ(), (float) Math.cos(.5f * fAngle * dt));
        MutableQuat4f oldRot = tempq2.get().set(orientation);
        newRot.mul(oldRot).normalize();
        
        orientation.set(newRot);
    }
    
    private void integrateVector(ReadOnlyVector3f deriv, float dt, MutableVector3f func) {
        if (deriv == null || func == null)
            throw new NullPointerException("Arguments cannot be null");
        deriv.scaleAdd(dt, func, func);
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
