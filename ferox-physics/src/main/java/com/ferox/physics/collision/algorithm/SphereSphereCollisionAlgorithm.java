package com.ferox.physics.collision.algorithm;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.Vector3f;
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
    public ClosestPair getClosestPair(Sphere shapeA, ReadOnlyMatrix4f transA, Sphere shapeB,
                                      ReadOnlyMatrix4f transB) {
        Vector3f ca = new Vector3f(transA.get(0, 3), transA.get(1, 3), transA.get(2, 3));
        Vector3f cb = new Vector3f(transB.get(0, 3), transB.get(1, 3), transB.get(2, 3));
        
        float ra = shapeA.getRadius() + shapeA.getMargin();
        float rb = shapeB.getRadius() + shapeB.getMargin();
        float dist = ca.distance(cb) - ra - rb;
        
        // FIXME: doesn't work if spheres are centered on each other
        MutableVector3f normal = cb.sub(ca).normalize();
        normal.scaleAdd(ra, ca, ca);

        if (normal.lengthSquared() > .000001f) {
            return new ClosestPair(ca, normal, dist);
        } else {
            // happens when spheres are perfectly centered on each other
            if (ra < rb) {
                // sphere a is inside sphere b
                normal.set(0f, 0f, -1f);
                ca.setZ(ca.getZ() + ra);
                return new ClosestPair(ca, normal, ra - rb);
            } else {
                // sphere b is inside sphere a
                normal.set(0f, 0f, 1f);
                ca.setZ(ca.getZ() + ra);
                return new ClosestPair(ca, normal, rb - ra);
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
