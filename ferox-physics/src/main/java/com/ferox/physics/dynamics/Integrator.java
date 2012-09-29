package com.ferox.physics.dynamics;

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Vector3;

public interface Integrator {
    public void integrateLinearAcceleration(@Const Vector3 a, double dt, Vector3 velocity);

    public void integrateAngularAcceleration(@Const Vector3 a, double dt, Vector3 angularVelocity);

    public void integrateLinearVelocity(@Const Vector3 v, double dt, Vector3 position);

    public void integrateAngularVelocity(@Const Vector3 v, double dt, Matrix3 orientation);
}
