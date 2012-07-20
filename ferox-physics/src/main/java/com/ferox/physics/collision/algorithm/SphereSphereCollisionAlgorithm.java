package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.shape.Sphere;

/**
 * The SphereSphereCollisionAlgorithm is a CollisionAlgorithm optimized to
 * handle collision checks between two spheres.
 * 
 * @author Michael Ludwig
 */
public class SphereSphereCollisionAlgorithm implements CollisionAlgorithm<Sphere, Sphere> {
    @Override
    public ClosestPair getClosestPair(Sphere shapeA, @Const Matrix4 transA, 
                                      Sphere shapeB, @Const Matrix4 transB) {
        Vector3 ca = new Vector3(transA.m03, transA.m13, transA.m23);
        Vector3 cb = new Vector3(transB.m03, transB.m13, transB.m23);
        
        double ra = shapeA.getRadius() + shapeA.getMargin();
        double rb = shapeB.getRadius() + shapeB.getMargin();
        double dist = ca.distance(cb) - ra - rb;
        
        // FIXME: doesn't work if spheres are centered on each other
        Vector3 normal = new Vector3().sub(cb, ca).normalize();
        Vector3 pa = cb.scale(normal, ra).add(ca); // consumes cb

        if (normal.lengthSquared() > .000001f) {
            return new ClosestPair(pa, normal, dist);
        } else {
            // happens when spheres are perfectly centered on each other
            if (ra < rb) {
                // sphere a is inside sphere b
                normal.set(0, 0, -1);
                pa.z = pa.z + ra;
                return new ClosestPair(cb, normal, ra - rb);
            } else {
                // sphere b is inside sphere a
                normal.set(0, 0, 1);
                pa.z = pa.z + ra;
                return new ClosestPair(cb, normal, rb - ra);
            }
        }
    }

    @Override
    public Class<Sphere> getShapeTypeA() {
        return Sphere.class;
    }

    @Override
    public Class<Sphere> getShapeTypeB() {
        return Sphere.class;
    }
}
