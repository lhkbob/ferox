package com.ferox.physics.controller;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.ferox.math.Vector3;
import com.ferox.math.entreri.Vector3Property;
import com.ferox.physics.dynamics.LinearConstraintPool;
import com.ferox.physics.dynamics.LinearConstraintSolver;
import com.ferox.physics.dynamics.RigidBody;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.Result;
import com.lhkbob.entreri.SimpleController;

public class ConstraintSolvingController extends SimpleController {
    private final LinearConstraintSolver solver;
    
    private List<LinearConstraintPool> groups;
    
    private Vector3Property deltaLinearImpulse;
    private Vector3Property deltaAngularImpulse;
    // FIXME need a way to get the tensor and mass properties
    
    public ConstraintSolvingController() {
        solver = new LinearConstraintSolver();
    }
    
    public LinearConstraintSolver getSolver() {
        return solver;
    }
    
    @Override
    public void preProcess(double dt) {
        groups = new ArrayList<LinearConstraintPool>();
    }
    
    @Override
    public void process(double dt) {
        Vector3 d = new Vector3();
        
        LinearConstraintPool[] asArray = groups.toArray(new LinearConstraintPool[groups.size()]);
        solver.solve(asArray);
        
        // now apply all of the delta impulses back to the rigid bodies
        Iterator<RigidBody> it = getEntitySystem().iterator(RigidBody.ID);
        while(it.hasNext()) {
            RigidBody b = it.next();
            
            // linear velocity
            deltaLinearImpulse.get(b.getIndex(), d);
            b.setVelocity(d.add(b.getVelocity()));
            
            // angular velocity
            deltaAngularImpulse.get(b.getIndex(), d);
            b.setAngularVelocity(d.add(b.getAngularVelocity()));
        }
    }
    
    @Override
    public void report(Result r) {
        if (r instanceof ConstraintResult) {
            groups.add(((ConstraintResult) r).getConstraints());
        }
    }
    
    @Override
    public void init(EntitySystem system) {
        super.init(system);
        deltaLinearImpulse = system.decorate(RigidBody.ID, new Vector3Property.Factory(new Vector3()));
        deltaAngularImpulse = system.decorate(RigidBody.ID, new Vector3Property.Factory(new Vector3()));
        
        solver.setDeltaLinearImpulseProperty(deltaLinearImpulse);
        solver.setDeltaAngularImpulseProperty(deltaAngularImpulse);
    }
    
    @Override
    public void destroy() {
        getEntitySystem().undecorate(RigidBody.ID, deltaLinearImpulse);
        getEntitySystem().undecorate(RigidBody.ID, deltaAngularImpulse);
        super.destroy();
    }
}
