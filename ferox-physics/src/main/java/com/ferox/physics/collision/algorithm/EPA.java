package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.physics.collision.algorithm.Simplex.Vertex;
import com.ferox.util.Bag;


/**
 * <p>
 * EPA is a low-level implementation of the Expanding Polytope Algorithm for
 * detecting the closest pairs between two intersecting convex hulls. The
 * algorithm was originally presented in
 * "Proximity Queries and Penetration Depth Computation on 3D Game Objects" by
 * Gino van den Bergen (as far as I can this was the first).
 * </p>
 * <p>
 * The code within this class is a Java port, refactoring and clean-up of the
 * EPA implementation contained in the Bullet physics engine. Specifically, the
 * code in "/BulletCollision/NarrowPhaseCollision/btGjkEpa2.cpp" by Nathanael
 * Presson.
 * </p>
 * 
 * @author Michael Ludwig
 * @author Nathanael Presson
 */
public class EPA {
    /**
     * The Status enum represents the various states that an evaluation of EPA
     * can take. Only the state VALID represents a correct run of the EPA
     * algorithm.
     */
    public static enum Status {
        VALID, DEGENERATED, NON_CONVEX, INVALID_HULL, FAILED
    }
    
    private static final int EPA_MAX_ITERATIONS = 255;
    private static final double EPA_ACCURACY = .00001;
    private static final double EPA_PLANE_EPS = .00001;
    private static final double EPA_INSIDE_EPS = .00001;
    
    private static final int[] I1M3 = new int[] { 1, 2, 0 };
    private static final int[] I2M3 = new int[] { 2, 0, 1 };
    

    private MinkowskiDifference shape;
    private Simplex gjkSimplex;
    
    private Vector3 normal;
    private Bag<Face> hull;
    
    private Simplex simplex;
    private double depth;
    private Status status;

    /**
     * Construct an EPA instance that will use the end result of the given GJK
     * to determine the correct intersection between the two convex hulls.
     * 
     * @param gjk GJK whose evaluated simplex is used to start the EPA algorithm
     * @throws NullPointerException if gjk is null
     */
    public EPA(GJK gjk) {
        this(gjk.getMinkowskiDifference(), gjk.getSimplex());
    }
    
    public EPA(MinkowskiDifference shape, Simplex gjk) {
        depth = 0f;
        this.shape = shape;
        this.gjkSimplex = gjk;
    }

    /**
     * Return the simplex containing the information needed to reconstruct the closest
     * pair, if the status returned by {@link #getStatus()} is VALID. Any other
     * status implies that the Simplex may be inaccurate or invalid.
     * 
     * @return The Simplex after running the EPA algorithm.
     */
    public Simplex getSimplex() {
        return simplex;
    }

    /**
     * Return the depth of penetration after {@link #evaluate(Vector3)}
     * is invoked. If the status of EPA is not VALID, the depth is invalid.
     * 
     * @return The penetration depth
     */
    public double getDepth() {
        return depth;
    }

    /**
     * Return the contact normal between the two intersecting convex hulls from
     * the last invocation of {@link #evaluate(Vector3)}. If the status
     * is not VALID, the returned normal is invalid and possibly null.
     * 
     * @return The contact normal from A to B
     */
    public @Const Vector3 getNormal() {
        return normal;
    }

    /**
     * <p>
     * Evaluate the EPA algorithm using the given vector as the initial guess
     * for contact normal between the two intersecting shapes. The EPA is
     * performed using the simplex of the GJK passed into the constructor. It is
     * assumed that said simplex has been left unmodified since the GJK was
     * evaluated.
     * </p>
     * <p>
     * After this method is invoked, {@link #getStatus()} returns the Status
     * enum specifying the correctness or validity of the EPA computaitons. If
     * it is not VALID, then the simplex returned by {@link #getSimplex()} is
     * invalid, otherwise it can be used to reconstruct the closest pair of
     * points on each convex hull.
     * </p>
     * 
     * @param guess The initial guess
     * @return The Status of the evaluation
     * @throws NullPointerException if guess is null
     */
    public Status evaluate(@Const Vector3 guess) {
        if (guess == null)
            throw new NullPointerException("Guess cannot be null");
        
        // we assume that the simplex of the GJK contains
        // the origin, otherwise behavior is undefined
        Simplex simplex = gjkSimplex;
        MinkowskiDifference function = shape;
        
        normal = new Vector3();
        hull = new Bag<Face>();
        status = Status.FAILED;
        
        if (simplex.getRank() > 1 && simplex.encloseOrigin(function)) {
            status = Status.VALID;
            
            // build initial hull
            Face f1 = new Face(simplex.getVertex(0), simplex.getVertex(1), simplex.getVertex(2), true);
            Face f2 = new Face(simplex.getVertex(1), simplex.getVertex(0), simplex.getVertex(3), true);
            Face f3 = new Face(simplex.getVertex(2), simplex.getVertex(1), simplex.getVertex(3), true);
            Face f4 = new Face(simplex.getVertex(0), simplex.getVertex(2), simplex.getVertex(3), true);
            
            if (hull.size() == 4) {
                Face best = findBest();
                Face outer = best;
                int pass = 0;
                
                bind(f1, 0, f2, 0);
                bind(f1, 1, f3, 0);
                bind(f1, 2, f4, 0);
                bind(f2, 1, f4, 2);
                bind(f2, 2, f3, 1);
                bind(f3, 2, f4, 1);
                
                Horizon horizon = new Horizon();
                boolean valid;
                double wdist;
                for (int iter = 0; iter < EPA_MAX_ITERATIONS; iter++) {
                    // reset the horizon for the next iteration
                    horizon.cf = null; 
                    horizon.ff = null; 
                    horizon.numFaces = 0;
                    
                    // calculate the next vertex to go into the hull
                    Vertex w = new Vertex();
                    valid = true;
                    best.pass = ++pass;
                    
                    w.getInputVector().normalize(best.normal);
                    function.getSupport(w.getInputVector(), w.getVertex());
                    wdist = best.normal.dot(w.getVertex()) - best.d;
                    
                    if (wdist > EPA_ACCURACY) {
                        for (int j = 0; j < 3 && valid; j++) {
                            valid &= expand(pass, w, best.adjacent[j], 
                                            best.faceIndex[j], horizon);
                        }
                        
                        if (valid && horizon.numFaces >= 3) {
                            bind(horizon.cf, 1, horizon.ff, 2);
                            best.remove();
                            best = findBest();
                                outer = best;
                        } else {
                            status = Status.INVALID_HULL;
                            break;
                        }
                    } else {
                        // accuracy reached, but the simplex should be valid
                        break;
                    }
                }
                
                Vector3 projection = new Vector3().scale(outer.normal, outer.d);
                
                normal.set(outer.normal);
                depth = outer.d;
                
                Vector3 t1 = new Vector3();
                Vector3 t2 = new Vector3();
                
                double w1 = t1.sub(outer.vertices[1].getVertex(), projection).cross(t2.sub(outer.vertices[2].getVertex(), projection)).length();
                double w2 = t1.sub(outer.vertices[2].getVertex(), projection).cross(t2.sub(outer.vertices[0].getVertex(), projection)).length();
                double w3 = t1.sub(outer.vertices[0].getVertex(), projection).cross(t2.sub(outer.vertices[1].getVertex(), projection)).length();

                double sum = w1 + w2 + w3;
                
                outer.vertices[0].setWeight(w1 / sum);
                outer.vertices[1].setWeight(w2 / sum);
                outer.vertices[2].setWeight(w3 / sum);
                this.simplex = new Simplex(outer.vertices[0], outer.vertices[1], outer.vertices[2]);
                return status;
            }
        }
        
        return Status.FAILED;
    }
        
    private Face findBest() {
        Face minf = hull.get(0);
        double mind = minf.d * minf.d;
        
        Face f;
        double sqd;
        int ct = hull.size();
        for (int i = 1; i < ct; i++) {
            f = hull.get(i);
            sqd = f.d * f.d;
            if (sqd < mind) {
                minf = f;
                mind = sqd;
            }
        }
        
        return minf;
    }
    
    private boolean expand(int pass, Vertex w, Face face, int index, Horizon horizon) {
        if (face.pass != pass) {
            int e1 = I1M3[index];
            if (face.normal.dot(w.getVertex()) - face.d < -EPA_PLANE_EPS) {
                Face nf = new Face(face.vertices[e1], face.vertices[index], w, false);
                if (nf.hullIndex >= 0) {
                    bind(nf, 0, face, index);
                    if (horizon.cf != null)
                        bind(horizon.cf, 1, nf, 2);
                    else
                        horizon.ff = nf;
                    
                    horizon.cf = nf;
                    horizon.numFaces++;
                    return true;
                }
            } else {
                int e2 = I2M3[index];
                face.pass = pass;
                if (expand(pass, w, face.adjacent[e1], face.faceIndex[e1], horizon) &&
                    expand(pass, w, face.adjacent[e2], face.faceIndex[e2], horizon)) {
                    face.remove();
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private static double edgeDistance(@Const Vector3 va, @Const Vector3 vb, @Const Vector3 normal) {
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
    
    private static class Horizon {
        Face cf;
        Face ff;
        int numFaces;
    }
    
    private class Face {
        final Vector3 normal;
        double d;
        double p;
        
        final Vertex[] vertices;
        final Face[] adjacent;
        final int[] faceIndex;
        
        int pass;
        int hullIndex;
        
        public Face(Vertex a, Vertex b, Vertex c, boolean force) {
            adjacent = new Face[3];
            faceIndex = new int[3];
            pass = 0;
            
            vertices = new Vertex[] { a, b, c };
            normal = new Vector3().sub(b.getVertex(), a.getVertex()).cross(new Vector3().sub(c.getVertex(), a.getVertex()));
            double l = normal.length();
            boolean v = l > EPA_ACCURACY;
            
            double invL = 1.0 / l;
            double d1 = a.getVertex().dot(new Vector3().cross(normal, new Vector3().sub(a.getVertex(), b.getVertex())));
            double d2 = b.getVertex().dot(new Vector3().cross(normal, new Vector3().sub(b.getVertex(), c.getVertex())));
            double d3 = c.getVertex().dot(new Vector3().cross(normal, new Vector3().sub(c.getVertex(), a.getVertex())));

            p = Math.min(Math.min(d1, d2), d3) * (v ? invL : 1.0);
            if (p >= -EPA_INSIDE_EPS)
                p = 0.0;
            
            hullIndex = -1;
            if (v) {
                d = edgeDistance(a.getVertex(), b.getVertex(), normal);
                if (d < 0) {
                    d = edgeDistance(b.getVertex(), c.getVertex(), normal);
                    if (d < 0) {
                        d = edgeDistance(c.getVertex(), a.getVertex(), normal);
                        if (d < 0) {
                            // origin projects to the interior of the triangle,
                            // so use the distance to the triangle plane
                            d = a.getVertex().dot(normal) / l;
                        }
                    }
                }
                
                normal.scale(1 / l);
                if (force || d >= -EPA_PLANE_EPS) {
                    hull.add(this);
                    hullIndex = hull.size() - 1;
                } else
                    status = Status.NON_CONVEX;
            } else
                status = Status.DEGENERATED;
        }
        
        public void remove() {
            hull.remove(hullIndex);
            if (hullIndex != hull.size())
                hull.get(hullIndex).hullIndex = hullIndex;
        }
    }
    
    private static void bind(Face f1, int i1, Face f2, int i2) {
        f1.faceIndex[i1] = i2;
        f1.adjacent[i1] = f2;
        
        f2.faceIndex[i2] = i1;
        f2.adjacent[i2] = f1;
    }
}
