package com.ferox.physics.controller;

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.ExplicitEulerIntegrator;
import com.ferox.physics.dynamics.Gravity;
import com.ferox.physics.dynamics.Integrator;
import com.ferox.physics.dynamics.RigidBody;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.SimpleController;

public class ForcesController extends SimpleController {
    private Integrator integrator;
    private final Vector3 defaultGravity;
    
    public ForcesController() {
        defaultGravity = new Vector3(0, -10, 0);
        setIntegrator(new ExplicitEulerIntegrator());
    }
    
    public void setGravity(@Const Vector3 gravity) {
        defaultGravity.set(gravity);
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
        Vector3 inertia = new Vector3();
        Vector3 force = new Vector3();
        Matrix3 rotation = new Matrix3();
        
        RigidBody rb = getEntitySystem().createDataInstance(RigidBody.ID);
        CollisionBody cb = getEntitySystem().createDataInstance(CollisionBody.ID);
        Gravity g = getEntitySystem().createDataInstance(Gravity.ID);
        
        ComponentIterator it = new ComponentIterator(getEntitySystem());
        it.addRequired(rb);
        it.addRequired(cb);
        it.addOptional(g);
        
        while(it.next()) {
            Matrix4 transform = cb.getTransform();

            // compute the body's new inertia tensor 
            // FIXME this might not be the best place to do this computation
            Matrix3 tensor = rb.getInertiaTensorInverse();
            
            cb.getShape().getInertiaTensor(rb.getMass(), inertia);
            inertia.set(1.0 / inertia.x, 1.0 / inertia.y, 1.0 / inertia.z);
            
            rotation.setUpper(transform);
            tensor.mulDiagonal(rotation, inertia).mulTransposeRight(rotation);
            rb.setInertiaTensorInverse(tensor);
            
            // add gravity force
            if (g.isEnabled()) {
                force.scale(g.getGravity(), rb.getMass());
            } else {
                force.scale(defaultGravity, rb.getMass());
            }
            
            rb.addForce(force, null);
            
            // integrate and apply forces to the body's velocity
            Vector3 lv = rb.getVelocity();
            integrator.integrateLinearAcceleration(force.scale(rb.getTotalForce(), rb.getInverseMass()), dt, lv);
            rb.setVelocity(lv);
            
            Vector3 la = rb.getAngularVelocity();
            integrator.integrateAngularAcceleration(force.mul(tensor, rb.getTotalTorque()), dt, la);
            rb.setAngularVelocity(la);
            
            rb.clearForces();
        }
    }
}
