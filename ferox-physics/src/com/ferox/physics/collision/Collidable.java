package com.ferox.physics.collision;

import com.ferox.math.Matrix4f;
import com.ferox.math.ReadOnlyMatrix4f;

public class Collidable {
    private final Matrix4f worldTransform;
//    private final Matrix4f predictedWorldTransform;
    
//    private final AxisAlignedBox localAabb;
//    private final AxisAlignedBox sweptWorldAabb;
    
//    private final BitSet collisionGroups;
//    private final BitSet collisionMask;
    
    private ConvexShape bounds;
    
    public Collidable(ReadOnlyMatrix4f t, ConvexShape shape) {
        worldTransform = new Matrix4f(t);
        bounds = shape;
    }
    
    public ReadOnlyMatrix4f getWorldTransform() {
        return worldTransform;
    }
    
    public ConvexShape getShape() {
        return bounds;
    }
    
//    public Matrix4f getPredictedWorldTransform() {
        
//    }
}
