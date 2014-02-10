package com.ferox.renderer.geom;

import com.ferox.math.Vector3;
import com.ferox.math.Vector4;

/**
 *
 */
public final class Tangents {
    private Tangents() {
    }

    public static void compute(TriangleIterator ti) {
        // clone the iterator and add two temporary attributes to accumulate the tangent data
        TriangleIterator.Builder b = TriangleIterator.Builder.newBuilder().set(ti);
        b.createAttribute("tan", 3);
        b.createAttribute("bitan", 3);
        ti = b.build();

        Vector3 d1 = new Vector3();
        Vector3 d2 = new Vector3();
        Vector3 tan = new Vector3();
        Vector3 dTan = new Vector3();
        Vector4 invalid = new Vector4(0.0, 0.0, 0.0, Double.NaN);

        while (ti.next()) {
            d1.sub(ti.getVertex(1), ti.getVertex(0));
            d2.sub(ti.getVertex(2), ti.getVertex(0));

            double s1 = ti.getTextureCoordinateU(1) - ti.getTextureCoordinateU(0);
            double s2 = ti.getTextureCoordinateU(2) - ti.getTextureCoordinateU(0);
            double t1 = ti.getTextureCoordinateV(1) - ti.getTextureCoordinateV(0);
            double t2 = ti.getTextureCoordinateV(2) - ti.getTextureCoordinateV(0);

            //            double s1 = d1.x;
            //            double s2 = d2.x;
            //            double t1 = d1.y;
            //            double t2 = d2.y;

            double r = 1.0 / (s1 * t2 - s2 * t1);
            // primary tangent direction and accumulate
            dTan.scale(d1, t2).addScaled(-t1, d2).scale(r);
            ti.setAttribute("tan", 0, ti.getAttribute("tan", 0, tan).add(dTan));
            ti.setAttribute("tan", 1, ti.getAttribute("tan", 1, tan).add(dTan));
            ti.setAttribute("tan", 2, ti.getAttribute("tan", 2, tan).add(dTan));

            // secondary tangent direction and accumulate
            dTan.scale(d1, -s2).addScaled(s1, d2).scale(r);
            ti.setAttribute("bitan", 0, ti.getAttribute("bitan", 0, tan).add(dTan));
            ti.setAttribute("bitan", 1, ti.getAttribute("bitan", 1, tan).add(dTan));
            ti.setAttribute("bitan", 2, ti.getAttribute("bitan", 2, tan).add(dTan));

            // invalidate actual tangent vector
            ti.setTangent(0, invalid);
            ti.setTangent(1, invalid);
            ti.setTangent(2, invalid);
        }

        ti.reset();
        while (ti.next()) {
            // normalize and orthogonalize each vertex
            Vector4 t = ti.getTangent(0);
            if (Double.isNaN(t.w)) {
                Vector3 n = ti.getNormal(0);
                double l = ti.getAttribute("tan", 0, tan).ortho(n).length();
                if (l > 0.00001) {
                    tan.scale(1.0 / l);
                    t.set(tan.x, tan.y, tan.z,
                          d1.cross(n, tan).dot(ti.getAttribute("bitan", 0, d2)) < 0.0 ? -1.0 : 1.0);
                    ti.setTangent(0, t);
                } else {
                    // degenerate triangle
                    ti.setTangent(0, t.set(1.0, 1.0, 1.0, 1.0));
                }
            } // else already been processed

            t = ti.getTangent(1);
            if (Double.isNaN(t.w)) {
                Vector3 n = ti.getNormal(1);
                double l = ti.getAttribute("tan", 1, tan).ortho(n).length();
                if (l > 0.00001) {
                    tan.scale(1.0 / l);
                    t.set(tan.x, tan.y, tan.z,
                          d1.cross(n, tan).dot(ti.getAttribute("bitan", 1, d2)) < 0.0 ? -1.0 : 1.0);
                    ti.setTangent(1, t);

                } else {
                    // degenerate triangle
                    ti.setTangent(1, t.set(0.0, 0.0, 0.0, 0.0));
                }
            } // else already been processed

            t = ti.getTangent(2);
            if (Double.isNaN(t.w)) {
                Vector3 n = ti.getNormal(2);
                double l = ti.getAttribute("tan", 2, tan).ortho(n).length();
                if (l > 0.00001) {
                    tan.scale(1.0 / l);
                    t.set(tan.x, tan.y, tan.z,
                          d1.cross(n, tan).dot(ti.getAttribute("bitan", 2, d2)) < 0.0 ? -1.0 : 1.0);
                    ti.setTangent(2, t);

                } else {
                    // degenerate triangle
                    ti.setTangent(2, t.set(0.0, 0.0, 0.0, 0.0));
                }
            } // else already processed
        }
    }
}
