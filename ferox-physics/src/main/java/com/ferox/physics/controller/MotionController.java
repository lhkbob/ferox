package com.ferox.physics.controller;

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.ExplicitEulerIntegrator;
import com.ferox.physics.dynamics.Integrator;
import com.ferox.physics.dynamics.RigidBody;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.SimpleController;

public class MotionController extends SimpleController {
    private Integrator integrator;
    
    public MotionController() {
        setIntegrator(new ExplicitEulerIntegrator());
    }
    
    public void setIntegrator(Integrator integrator) {
        if (integrator == null)
            throw new NullPointerException("Integrator can't be null");
        this.integrator = integrator; 
    }
    
    public Integrator getIntegrator() {
        return integrator;
    }
    
    @Override
    public void process(double dt) {
        Vector3 predictedPosition = new Vector3();
        Matrix3 predictedRotation = new Matrix3();
        
        RigidBody rb = getEntitySystem().createDataInstance(RigidBody.ID);
        CollisionBody cb = getEntitySystem().createDataInstance(CollisionBody.ID);
        
        ComponentIterator it = new ComponentIterator(getEntitySystem());
        it.addRequired(rb);
        it.addRequired(cb);
        
        while(it.next()) {
            Matrix4 transform = cb.getTransform();
            
            integrator.integrateLinearVelocity(rb.getVelocity(), dt, predictedPosition);
            integrator.integrateAngularVelocity(rb.getAngularVelocity(), dt, predictedRotation);
            
            // push values back into transform
            setTransform(predictedRotation, predictedPosition, transform);
            
            cb.setTransform(transform);
        }
    }
    
    private void setTransform(@Const Matrix3 r, @Const Vector3 p, Matrix4 t) {
        t.setUpper(r);
        
        t.m03 = p.x;
        t.m13 = p.y;
        t.m23 = p.z;
        
        // ensure this is still an affine transform
        t.m30 = 0.0; t.m31 = 0.0; t.m32 = 0.0; t.m33 = 1.0;
    }
}
