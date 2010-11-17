package com.ferox.physics.collision;

import java.util.BitSet;

import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.Transform;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.physics.collision.shape.Shape;

public class Collidable {
    private final BitSet collisionGroups;
    private final BitSet collisionMask;

    private final Transform worldTransform;
    private final AxisAlignedBox worldAabb;
    private Shape bounds;
    
    private float restitution;
    private float friction;
    
    public Collidable(ReadOnlyMatrix4f t, Shape shape) {
        worldTransform = new Transform(t);
        bounds = shape;
        worldAabb = new AxisAlignedBox();
        
        collisionGroups = new BitSet();
        collisionMask = new BitSet();
        
        // every Collidable starts out in a group
        setMemberOfGroup(0, true);
        setCollidesWithGroup(0, true);
        setFriction(.5f);
        setRestitution(0f);
        updateBounds();
    }
    
    public float getFriction() {
        return friction;
    }
    
    public void setFriction(float friction) {
        // FIXME: what are the bounds on this, just > 0?
        this.friction = friction;
    }
    
    public float getRestitution() {
        return restitution;
    }
    
    public void setRestitution(float restitution) {
        // FIXME: what are the bounds on this?
        this.restitution = restitution;
    }
    
    public boolean canCollide(Collidable other) {
        if (other == null)
            throw new NullPointerException("Collidable cannot be null");
        
        return collisionGroups.intersects(other.collisionMask) ||
               collisionMask.intersects(other.collisionGroups);
    }
    
    public boolean isMemberOfGroup(int group) {
        return collisionGroups.get(group);
    }
    
    public boolean canCollideWithGroup(int group) {
        return collisionMask.get(group);
    }
    
    public void setMemberOfGroup(int group, boolean member) {
        collisionGroups.set(group, member);
    }
    
    public void setCollidesWithGroup(int group, boolean collide) {
        collisionMask.set(group, collide);
    }
    
    public ReadOnlyMatrix4f getWorldTransform() {
        return worldTransform;
    }
    
    public void setShape(Shape shape) {
        if (shape == null)
            throw new NullPointerException("Shape cannot be null");
        bounds = shape;
        updateBounds();
    }
    
    public Shape getShape() {
        return bounds;
    }
    
    public void setWorldTransform(ReadOnlyMatrix4f t) {
        if (t == null)
            throw new NullPointerException("Transform cannot be null");
        worldTransform.set(t);
        updateBounds();
    }
    
    public AxisAlignedBox getWorldBounds() {
        return worldAabb;
    }
    
    private void updateBounds() {
        bounds.getBounds().transform(worldTransform, worldAabb);
    }
}
