package com.ferox.physics.collision;

import java.util.BitSet;

import com.ferox.math.Matrix4f;
import com.ferox.math.ReadOnlyMatrix4f;

public class Collidable {
    private final Matrix4f worldTransform;
//    private final Matrix4f predictedWorldTransform;
    
//    private final AxisAlignedBox localAabb;
//    private final AxisAlignedBox sweptWorldAabb;
    
    private final BitSet collisionGroups;
    private final BitSet collisionMask;
    
    private Shape bounds;
    
    public Collidable(ReadOnlyMatrix4f t, Shape shape) {
        worldTransform = new Matrix4f(t);
        bounds = shape;
        
        collisionGroups = new BitSet();
        collisionMask = new BitSet();
        
        // every Collidable starts out in a group
        setMemberOfGroup(0, true);
        setCollidesWithGroup(0, true);
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
    
    public Shape getShape() {
        return bounds;
    }
    
//    public Matrix4f getPredictedWorldTransform() {
        
//    }
}
