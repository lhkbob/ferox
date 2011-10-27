package com.ferox.physics.entity;

import com.ferox.entity2.TypedComponent;
import com.ferox.physics.collision.Shape;

// FIXME: really we should just expose the RigidBody here so that
// everything can be managed directly by custom controllers easier
public class PhysicsBody extends TypedComponent<PhysicsBody> {
    private float mass;
    private Shape shape;
    
    public PhysicsBody(Shape shape, float mass) {
        super(PhysicsBody.class);
        setMass(mass);
        setShape(shape);
    }

    public float getMass() {
        return mass;
    }
    
    public void setMass(float mass) {
        this.mass = mass;
    }
    
    public Shape getShape() {
        return shape;
    }
    
    public void setShape(Shape shape) {
        if (shape == null)
            throw new NullPointerException("Shape cannot be null");
        this.shape = shape;
    }
}
