package com.ferox.physics.dynamics;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.CollisionManager;
import com.ferox.physics.dynamics.constraint.Constraint;
import com.ferox.physics.dynamics.constraint.ConstraintSolver;

// FIXME: remove the physics world configuration and make the components of the physics world
// final, and to be configured via guice injection
// must also figure out thread-safety of components -> currently its mixed multithreaded and single-threaded
public interface PhysicsWorld {
    public void add(Collidable c);
    
    public void remove(Collidable c);
    
    public void step(float dt);
    
    public void add(Constraint c);
    
    public void remove(Constraint c);
    
    public Integrator getIntegrator();
    
    public void setIntegrator(Integrator integrator);
    
    public ConstraintSolver getConstraintSolver();
    
    public void setConstraintSolver(ConstraintSolver solver);
    
    public CollisionManager getCollisionManager();
    
    public void setCollisionManager(CollisionManager manager);
    
    public ReadOnlyVector3f getGravity();
    
    public void setGravity(ReadOnlyVector3f g);
}