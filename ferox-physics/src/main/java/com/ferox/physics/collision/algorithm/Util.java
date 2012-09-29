package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

public class Util {

    public static double tripleProduct(@Const Vector3 a, @Const Vector3 b, @Const Vector3 c) {
        return a.y * b.z * c.x + a.z * b.x * c.y -
                a.x * b.z * c.y - a.y * b.x * c.z +
                a.x * b.y * c.z - a.z * b.y * c.x;
    }

    public static Vector3 normal(@Const Vector3 va, @Const Vector3 vb, @Const Vector3 vc, Vector3 result) {
        // inline subtraction of 2 vectors
        double e1x = vb.x - va.x;
        double e1y = vb.y - va.y;
        double e1z = vb.z - va.z;

        double e2x = vc.x - va.x;
        double e2y = vc.y - va.y;
        double e2z = vc.z - va.z;

        if (result == null) {
            result = new Vector3();
        }

        // compute the cross-product of e1 and e2
        return result.set(e1y * e2z - e2y * e1z,
                          e1z * e2x - e2z * e1x,
                          e1x * e2y - e2x * e1y);
    }
}
