package com.ferox.physics.dynamics;

import com.ferox.physics.collision.CollisionManager;
import com.ferox.physics.collision.SpatialHierarchyCollisionManager;
import com.ferox.physics.dynamics.constraint.ConstraintSolver;
import com.ferox.physics.dynamics.constraint.SequentialImpulseConstraintSolver;

public class PhysicsWorldConfiguration {
    private Integrator integrator;
    private ConstraintSolver solver;
    private CollisionManager manager;
    
    public PhysicsWorldConfiguration() {
        integrator = new ExplicitEulerIntegrator();
        solver = new SequentialImpulseConstraintSolver();
        manager = new SpatialHierarchyCollisionManager();
    }
    
    public Integrator getIntegrator() {
        return integrator;
    }
    
    public PhysicsWorldConfiguration setIntegrator(Integrator integrator) {
        if (integrator == null)
            throw new NullPointerException("Integrator cannot be null");
        this.integrator = integrator;
        return this;
    }
    
    public ConstraintSolver getConstraintSolver() {
        return solver;
    }
    
    public PhysicsWorldConfiguration setConstraintSolver(ConstraintSolver solver) {
        if (solver == null)
            throw new NullPointerException("ConstraintSolver cannot be null");
        this.solver = solver;
        return this;
    }
    
    public CollisionManager getCollisionManager() {
        return manager;
    }
    
    public PhysicsWorldConfiguration setCollisionManager(CollisionManager manager) {
        if (manager == null)
            throw new NullPointerException("CollisionManager cannot be null");
        this.manager = manager;
        return this;
    }
}
