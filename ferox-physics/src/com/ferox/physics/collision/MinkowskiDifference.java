package com.ferox.physics.collision;

import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;

public class MinkowskiDifference {
    private final ConvexShape shapeA;
    private final ConvexShape shapeB;
    
    private final ReadOnlyMatrix4f transA; // transforms a to world
    private final ReadOnlyMatrix4f transB; // transforms b to world
    
    private final Vector3f t1;
    private final Vector4f t2;
    
    public MinkowskiDifference(Collidable objA, Collidable objB) {
        shapeA = objA.getShape();
        transA = objA.getWorldTransform();
        
        shapeB = objB.getShape();
        transB = objB.getWorldTransform();
        
        t1 = new Vector3f();
        t2 = new Vector4f();
    }
    
    public Vector3f getSupport(ReadOnlyVector3f d, Vector3f result) {
        Vector3f sa = getAffineSupport(shapeA, transA, d, result);
        Vector3f sb = getAffineSupport(shapeB, transB, d.scale(-1f, t1), t1);
        return sa.sub(sb);
    }
    
    public Vector3f getSupportA(ReadOnlyVector3f d, Vector3f result) {
        return getAffineSupport(shapeA, transA, d, result);
    }
    
    public Vector3f getSupportB(ReadOnlyVector3f d, Vector3f result) {
        return getAffineSupport(shapeB, transB, d, result);
    }
    
    private Vector3f getAffineSupport(ConvexShape shape, ReadOnlyMatrix4f t, ReadOnlyVector3f d, Vector3f result) {
        if (result == null)
            result = new Vector3f();
        
        // first step is to transform d by the transpose of the upper 3x3
        // we avoid this by wrapping d and setting w = 0
        t2.set(d.getX(), d.getY(), d.getZ(), 0f);
        t.mulPre(t2);
        
        // second step is to compute the actual support
        result.set(t2.getX(), t2.getY(), t2.getZ());
        shape.computeSupport(result, result);
        
        // then transform that by the complete affine transform
        t2.set(result.getX(), result.getY(), result.getZ(), 1f);
        t.mul(t2, t2);
        
        return result.set(t2.getX(), t2.getY(), t2.getZ());
    }
}