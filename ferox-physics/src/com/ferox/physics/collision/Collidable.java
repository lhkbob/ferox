package com.ferox.physics.collision;

import java.util.BitSet;

import com.ferox.math.Matrix4f;
import com.ferox.math.bounds.AxisAlignedBox;

public class Collidable {
    private final Matrix4f worldTransform;
    private final Matrix4f predictedWorldTransform;
    
    private final AxisAlignedBox localAabb;
    private final AxisAlignedBox sweptWorldAabb;
    
    private final BitSet collisionGroups;
    private final BitSet collisionMask;
    
    private ConvexShape bounds;
    
    public Collidable() {
        
    }
    
    public Matrix4f getWorldTransform() {
        
    }
    
    public Matrix4f getPredictedWorldTransform() {
        
    }
}
