package com.ferox.physics.dynamics;

import com.ferox.math.MutableMatrix3f;
import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;

public interface Integrator {
    public void integrateLinearAcceleration(ReadOnlyVector3f a, float dt, MutableVector3f velocity);
    
    public void integrateAngularAcceleration(ReadOnlyVector3f a, float dt, MutableVector3f angularVelocity);
    
    public void integrateLinearVelocity(ReadOnlyVector3f v, float dt, MutableVector3f position);
    
    public void integrateAngularVelocity(ReadOnlyVector3f v, float dt, MutableMatrix3f orientation);
}
