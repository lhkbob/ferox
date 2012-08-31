package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Matrix3;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.shape.ConvexShape;

public class MinkowskiShape {
    private static final double CONTACT_NORMAL_ACCURACY = .0001;

    private final Matrix3 rotationA;
    private final Vector3 translationA;
    private final ConvexShape shapeA;
    
    private final Matrix3 rotationB;
    private final Vector3 translationB;
    private final ConvexShape shapeB;
    
    private int numMargins;
    
    public MinkowskiShape(ConvexShape shapeA, @Const Matrix4 transformA,
                          ConvexShape shapeB, @Const Matrix4 transformB) {
        rotationA = new Matrix3().setUpper(transformA);
        translationA = new Vector3(transformA.m03, transformA.m13, transformA.m23);
        
        rotationB = new Matrix3().setUpper(transformB);
        translationB = new Vector3(transformB.m03, transformB.m13, transformB.m23);
        
        this.shapeA = shapeA;
        this.shapeB = shapeB;
        
        numMargins = 1;
    }
    
    public void setAppliedMargins(int num) {
        if (num < 0)
            throw new IllegalArgumentException("Applied margin count must be at least 0, not: " + num);
        numMargins = num;
    }
    
    public Vector3 getInitialGuess() {
        return new Vector3().sub(translationB, translationA);
    }
    
    public ClosestPair getClosestPair(Simplex2 simplex, @Const Vector3 zeroNormal) {
        Vector3 a = computePointOnA(simplex);
        Vector3 b = computePointOnB(simplex);
        
        double scale = 1.0; // no direction flip
        if (translationA.distanceSquared(b) < translationA.distanceSquared(a) ||
            translationB.distanceSquared(a) < translationB.distanceSquared(b)) {
            // shapes are intersecting, so flip everything
            scale = -1.0;
        }
        
        Vector3 normal = new Vector3().sub(b, a);
        double distance = normal.length() * scale;
        
        if (Math.abs(distance) < CONTACT_NORMAL_ACCURACY) {
            // special case for handling contacts that are very close
            if (zeroNormal != null) {
                normal.scale(zeroNormal, scale).normalize();
            } else {
                return null;
            }
        } else {
            // normalize and flip if intersecting
            normal.scale(1.0 / distance);
        }
        
        // update positions to be only a single margin away
        if (numMargins != 1) {
            // adjust a's point by moving N margins along the contact normal
            a.add(new Vector3().scale(normal, (1 - numMargins) * shapeA.getMargin()));
            
            // compute how the contact depth changes
            double delta = scale * Math.abs(numMargins - 1) * (shapeA.getMargin() + shapeB.getMargin());
            if ((numMargins == 0 && delta < 0.0) || (numMargins > 1 && delta > 0.0)) {
                // moving to one margin increases distance
                distance += delta;
            } else {
                // moving to one margin decreases distance
                distance -= delta;
            }
        }
        
        return new ClosestPair(a, normal, distance);
    }
    
    private Vector3 computePointOnA(Simplex2 simplex) {
        Vector3 a = new Vector3();
        Vector3 s = new Vector3();
        for (int i = 0; i < simplex.getRank(); i++) {
            support(shapeA, rotationA, translationA, simplex.getInput(i), false, s);
            a.add(s.scale(simplex.getWeight(i)));
        }
        return a;
    }
    
    private Vector3 computePointOnB(Simplex2 simplex) {
        Vector3 b = new Vector3();
        Vector3 s = new Vector3();
        for (int i = 0; i < simplex.getRank(); i++) {
            support(shapeB, rotationB, translationB, simplex.getInput(i), true, s);
            b.add(s.scale(simplex.getWeight(i)));
        }
        return b;
    }
    
    public Vector3 getSupport(@Const Vector3 dir, Vector3 result) {
        Vector3 a = new Vector3();
        Vector3 b = new Vector3();
        
        support(shapeA, rotationA, translationA, dir, false, a);
        support(shapeB, rotationB, translationB, dir, true, b);
        
        if (result == null)
            result = new Vector3();
        return result.sub(a, b);
    }
    
    private void support(ConvexShape shape, @Const Matrix3 r, @Const Vector3 t,
                         @Const Vector3 d, boolean negate, Vector3 result) {
        // FIXME optimize by replacing with cached instances
        Vector3 transformedDir = (negate ? new Vector3().scale(d, -1.0) : new Vector3(d));
        transformedDir.mul(transformedDir, r);

        shape.computeSupport(transformedDir, result);
        if (numMargins > 0) {
            transformedDir.scale(numMargins * shape.getMargin());
            result.add(transformedDir);
        }
        
        result.mul(r, result).add(t);
    }
}
