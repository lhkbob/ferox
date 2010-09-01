package com.ferox.physics.collision;

import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;

public class MinkowskiDifference {
    private static final ThreadLocal<Vector3f> supportCache = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
    private static final ThreadLocal<Vector3f> marginCache = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
    private static final ThreadLocal<Vector4f> transformCache = new ThreadLocal<Vector4f>() {
        @Override
        protected Vector4f initialValue() { return new Vector4f(); }
    };
    
    private final ConvexShape shapeA;
    private final ConvexShape shapeB;
    
    private final ReadOnlyMatrix4f transA; // transforms a to world
    private final ReadOnlyMatrix4f transB; // transforms b to world
    
    private final float margin;
    
    public MinkowskiDifference(Collidable objA, Collidable objB, float margin) {
        shapeA = objA.getShape();
        transA = objA.getWorldTransform();
        
        shapeB = objB.getShape();
        transB = objB.getWorldTransform();
        
        this.margin = margin;
    }
    
    public Vector3f getClosestPointA(Simplex simplex, boolean useMargin, Vector3f result) {
        Vector3f t = supportCache.get();
        
        if (result == null)
            result = new Vector3f();
        result.set(0f, 0f, 0f);
        for (int i = 0; i < simplex.getRank(); i++) {
            // sum weighted supports from simplex
            getAffineSupport(shapeA, transA, simplex.getSupportInput(i), useMargin, t)
                            .scaleAdd(simplex.getWeight(i), result, result);
        }
        
        return result;
    }
    
    public Vector3f getClosestPointB(Simplex simplex, boolean useMargin, Vector3f result) {
        Vector3f t = supportCache.get();
        
        if (result == null)
            result = new Vector3f();
        result.set(0f, 0f, 0f);
        for (int i = 0; i < simplex.getRank(); i++) {
            // sum weighted supports from simplex
            getAffineSupport(shapeB, transB, simplex.getSupportInput(i).scale(-1f, t), useMargin, t)
                            .scaleAdd(simplex.getWeight(i), result, result);
        }
        
        return result;
    }
    
    public Vector3f getSupport(ReadOnlyVector3f d, Vector3f result) {
        Vector3f t = supportCache.get();
        
        Vector3f sa = getAffineSupport(shapeA, transA, d, true, result);
        Vector3f sb = getAffineSupport(shapeB, transB, d.scale(-1f, t), true, t);
        return sa.sub(sb);
    }
    
    public Vector3f getSupportA(ReadOnlyVector3f d, Vector3f result) {
        return getAffineSupport(shapeA, transA, d, true, result);
    }
    
    public Vector3f getSupportB(ReadOnlyVector3f d, Vector3f result) {
        return getAffineSupport(shapeB, transB, d, true, result);
    }
    
    private Vector3f getAffineSupport(ConvexShape shape, ReadOnlyMatrix4f t, ReadOnlyVector3f d, boolean useMargin, Vector3f result) {
        if (result == null)
            result = new Vector3f();
        
        Vector3f m = marginCache.get();
        Vector4f s = transformCache.get();
        
        // first step is to transform d by the transpose of the upper 3x3
        // we do this by wrapping d in a 4-vector and setting w = 0
        s.set(d.getX(), d.getY(), d.getZ(), 0f);
        t.mulPre(s);
        
        // second step is to compute the actual support
        result.set(s.getX(), s.getY(), s.getZ());
        shape.computeSupport(result, result);
        if (useMargin && margin > 0f)
            result.sub(result.normalize(m).scale(margin));
        
        // then transform that by the complete affine transform
        s.set(result.getX(), result.getY(), result.getZ(), 1f);
        t.mul(s);
        
        return result.set(s.getX(), s.getY(), s.getZ());
    }
}