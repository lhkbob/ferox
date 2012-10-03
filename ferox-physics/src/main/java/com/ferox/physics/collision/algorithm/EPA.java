package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.util.Bag;

public class EPA {
    private static final int EPA_MAX_ITERATIONS = 255;
    private static final double EPA_ACCURACY = .00001;
    private static final double EPA_PLANE_EPS = .00001;

    private static final int[] I1_MAP = new int[] {1, 2, 0};
    private static final int[] I2_MAP = new int[] {2, 0, 1};

    public static int numEPA = 0;

    public static ClosestPair evaluate(Simplex simplex) {
        numEPA++;
        if (simplex.getRank() > 1 && simplex.encloseOrigin()) {
            Bag<Face> hull = new Bag<Face>();
            Face f1 = newFace(simplex, 0, 1, 2, hull);
            Face f2 = newFace(simplex, 1, 0, 3, hull);
            Face f3 = newFace(simplex, 2, 1, 3, hull);
            Face f4 = newFace(simplex, 0, 2, 3, hull);

            if (hull.size() == 4) {
                // we know that f1, f2, f3, f4 are not null
                Face best = findBest(hull);
                Face outer = best;

                bind(f1, 0, f2, 0);
                bind(f1, 1, f3, 0);
                bind(f1, 2, f4, 0);
                bind(f2, 1, f4, 2);
                bind(f2, 2, f3, 1);
                bind(f3, 2, f4, 1);

                Vector3 iw = new Vector3();
                Vector3 vw = new Vector3();
                Horizon horizon = new Horizon();

                for (int pass = 1; pass < EPA_MAX_ITERATIONS; pass++) {
                    horizon.reset();
                    best.pass = pass;

                    simplex.getShape().getSupport(iw.set(best.normal), vw);

                    double wdist = best.normal.dot(vw) - best.d;
                    boolean valid = true;
                    if (wdist > EPA_ACCURACY) {
                        for (int j = 0; j < 3 && valid; j++) {
                            valid &= expand(pass, iw, vw, best.adjacent[j],
                                            best.faceIndices[j], horizon, hull);
                        }

                        if (valid && horizon.numFaces >= 3) {
                            bind(horizon.cf, 1, horizon.ff, 2);
                            best.remove(hull);
                            best = findBest(hull);
                            outer = best;
                        } else {
                            // invalid hull
                            return null;
                        }
                    } else {
                        // accuracy reached
                        break;
                    }
                }

                // create new reduced simplex from hull
                Vector3 projection = new Vector3().scale(outer.normal, outer.d);
                simplex.setRank(outer.inputs.length);
                for (int j = 0; j < outer.inputs.length; j++) {
                    simplex.getInput(j).set(outer.inputs[j]);
                    simplex.getVertex(j).set(outer.vertices[j]);
                }

                Vector3 t = new Vector3();
                double w1 = Util.normal(projection, outer.vertices[1], outer.vertices[2],
                                        t).length();
                double w2 = Util.normal(projection, outer.vertices[2], outer.vertices[0],
                                        t).length();
                double w3 = Util.normal(projection, outer.vertices[0], outer.vertices[1],
                                        t).length();

                double sum = w1 + w2 + w3;
                simplex.setWeight(0, w1 / sum);
                simplex.setWeight(1, w2 / sum);
                simplex.setWeight(2, w3 / sum);

                // construct pair from simplex and normal
                return simplex.getShape().getClosestPair(simplex, projection);
            }
        }

        return null;
    }

    private static Face findBest(Bag<Face> hull) {
        Face minf = hull.get(0);
        double mind = minf.d * minf.d;
        int ct = hull.size();

        // FIXME this is an o(n) search through the hull
        // should we make the hull collection a queue?
        for (int i = 1; i < ct; i++) {
            Face f = hull.get(i);
            double sqd = f.d * f.d;
            if (sqd < mind) {
                mind = sqd;
                minf = f;
            }
        }

        return minf;
    }

    private static boolean expand(int pass, @Const Vector3 iw, @Const Vector3 vw, Face f,
                                  int e, Horizon horizon, Bag<Face> hull) {
        if (f.pass != pass) {
            int e1 = I1_MAP[e];
            if (f.normal.dot(vw) - f.d < -EPA_PLANE_EPS) {
                // If we need a new face, we clone iw and vw because they're
                // being reused in the EPA loop. The other vertices are already
                // in the hull and won't be modified anymore so we can share references.
                Face nf = newFace(f.inputs[e1], f.vertices[e1], f.inputs[e],
                                  f.vertices[e], new Vector3(iw), new Vector3(vw), hull,
                                  false);
                if (nf != null) {
                    bind(nf, 0, f, e);
                    if (horizon.cf != null) {
                        bind(horizon.cf, 1, nf, 2);
                    } else {
                        horizon.ff = nf;
                    }
                    horizon.cf = nf;
                    horizon.numFaces++;
                    return true;
                }
            } else {
                int e2 = I2_MAP[e];
                f.pass = pass;
                if (expand(pass, iw, vw, f.adjacent[e1], f.faceIndices[e1], horizon, hull) && expand(pass,
                                                                                                     iw,
                                                                                                     vw,
                                                                                                     f.adjacent[e2],
                                                                                                     f.faceIndices[e2],
                                                                                                     horizon,
                                                                                                     hull)) {
                    f.remove(hull);
                    return true;
                }
            }
        }

        return false;
    }

    private static double edgeDistance(@Const Vector3 va, @Const Vector3 vb,
                                       @Const Vector3 normal) {
        Vector3 ba = new Vector3().sub(vb, va);
        Vector3 nab = new Vector3().cross(ba, normal); // outward facing edge normal direction on triangle plane

        double aDotNAB = va.dot(nab); // only care about sign to determine inside/outside, no normalization required

        if (aDotNAB < 0) {
            // outside of edge a->b
            double aDotBA = va.dot(ba);
            double bDotBA = vb.dot(ba);

            if (aDotBA > 0) {
                // pick distance to vertex a
                return va.length();
            } else if (bDotBA < 0) {
                // pick distance to vertex b
                return vb.length();
            } else {
                // pick distance to edge a->b
                double aDotB = va.dot(vb);
                double d2 = (va.lengthSquared() * vb.lengthSquared() - aDotB * aDotB) / ba.lengthSquared();
                return Math.sqrt(Math.max(d2, 0.0));
            }
        } else {
            return -1.0;
        }
    }

    private static Face newFace(Simplex simplex, int i1, int i2, int i3, Bag<Face> hull) {
        // the simplex will be later modified, so we clone the vertices here
        // so that we're not inadvertently editing the hull either
        return newFace(new Vector3(simplex.getInput(i1)),
                       new Vector3(simplex.getVertex(i1)),
                       new Vector3(simplex.getInput(i2)),
                       new Vector3(simplex.getVertex(i2)),
                       new Vector3(simplex.getInput(i3)),
                       new Vector3(simplex.getVertex(i3)), hull, true);
    }

    private static Face newFace(@Const Vector3 ia, @Const Vector3 va, @Const Vector3 ib,
                                @Const Vector3 vb, @Const Vector3 ic, @Const Vector3 vc,
                                Bag<Face> hull, boolean force) {
        Face face = new Face();
        face.vertices[0] = va;
        face.vertices[1] = vb;
        face.vertices[2] = vc;
        face.inputs[0] = ia;
        face.inputs[1] = ib;
        face.inputs[2] = ic;

        Util.normal(va, vb, vc, face.normal);

        double l = face.normal.length();
        boolean valid = l > EPA_ACCURACY;

        if (valid) {
            face.d = edgeDistance(va, vb, face.normal);
            if (face.d < 0) {
                face.d = edgeDistance(vb, vc, face.normal);
                if (face.d < 0) {
                    face.d = edgeDistance(vc, va, face.normal);
                    if (face.d < 0) {
                        // origin projects to the interior of the triangle,
                        // so use the distance to the triangle plane
                        face.d = va.dot(face.normal) / l;
                    }
                }
            }

            face.normal.scale(1 / l);
            if (force || face.d >= -EPA_PLANE_EPS) {
                hull.add(face);
                face.index = hull.size() - 1;
                return face;
            }
        }

        // invalid
        return null;
    }

    private static void bind(Face f1, int i1, Face f2, int i2) {
        f1.faceIndices[i1] = i2;
        f1.adjacent[i1] = f2;

        f2.faceIndices[i2] = i1;
        f2.adjacent[i2] = f1;
    }

    private static class Horizon {
        Face cf;
        Face ff;
        int numFaces;

        public void reset() {
            cf = null;
            ff = null;
            numFaces = 0;
        }
    }

    private static class Face {
        final Vector3 normal;
        double d;

        final Vector3[] vertices;
        final Vector3[] inputs;

        final Face[] adjacent;
        final int[] faceIndices;
        int index;

        int pass;

        public Face() {
            normal = new Vector3();
            vertices = new Vector3[3];
            inputs = new Vector3[3];

            adjacent = new Face[3];
            faceIndices = new int[3];

            pass = 0;
            index = 0;
            d = -1;
        }

        public void remove(Bag<Face> hull) {
            hull.remove(index);
            if (index != hull.size()) {
                // update swapped item's index
                hull.get(index).index = index;
            }
        }
    }
}
