package com.ferox.physics.collision;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.Simplex.SupportSample;
import com.ferox.util.Bag;

public class EPA {
    public static enum Status {
        VALID, DEGENERATED, NON_CONVEX, INVALID_HULL,
        ACCURACY_REACHED, FALLBACK, FAILED
    }
    
    private static final int EPA_MAX_ITERATIONS = 255;
    private static final float EPA_ACCURACY = .0001f;
    private static final float EPA_PLANE_EPS = .00001f;
    private static final float EPA_INSIDE_EPS = .01f;
    
    private static final int[] I1M3 = new int[] { 1, 2, 0 };
    private static final int[] I2M3 = new int[] { 2, 0, 1 };
    
    private static final ThreadLocal<Vector3f> temp1 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
    private static final ThreadLocal<Vector3f> temp2 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
    private static final ThreadLocal<Vector3f> temp3 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); }
    };
    
    private final GJK gjk;
    private final Vector3f normal;
    private final Bag<Face> hull;
    
    private Simplex simplex;
    private float depth;
    private Status status;
    
    public EPA(GJK gjk) {
        if (gjk == null)
            throw new NullPointerException("GJK cannot be null");
        if (gjk.getStatus() != GJK.Status.INSIDE)
            throw new IllegalArgumentException("GJK must have a status of INSIDE");
        
        this.gjk = gjk;
        normal = new Vector3f();
        hull = new Bag<Face>();
        
        depth = 0f;
        status = Status.FAILED;
    }
    
    public GJK getGJK() {
        return gjk;
    }
    
    public Simplex getSimplex() {
        return simplex;
    }
    
    public float getDepth() {
        return depth;
    }
    
    public ReadOnlyVector3f getNormal() {
        return normal;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public void evaluate(ReadOnlyVector3f guess) {
        // we assume that the simplex of the GJK contains
        // the origin, otherwise behavior is undefined
        Simplex simplex = gjk.getSimplex();
        MinkowskiDifference shape = gjk.getMinkowskiDifference();
        
        if (simplex.getRank() > 1 && simplex.encloseOrigin(shape)) {
            status = Status.VALID;
            
            // orient simplex
            simplex.orient();
            
            // build initial hull
            Face f1 = new Face(simplex.getSample(0), simplex.getSample(1), simplex.getSample(2), true);
            Face f2 = new Face(simplex.getSample(1), simplex.getSample(0), simplex.getSample(3), true);
            Face f3 = new Face(simplex.getSample(2), simplex.getSample(1), simplex.getSample(3), true);
            Face f4 = new Face(simplex.getSample(0), simplex.getSample(2), simplex.getSample(3), true);
            
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
                float wdist;
                for (int iter = 0; iter < EPA_MAX_ITERATIONS; iter++) {
                    // reset the horizon for the next iteration
                    horizon.cf = null; 
                    horizon.ff = null; 
                    horizon.numFaces = 0;
                    
                    // calculate the next vertex to go into the hull
                    SupportSample w = new SupportSample();
                    valid = true;
                    best.pass = ++pass;
                    
                    best.normal.normalize(w.input);
                    shape.getSupport(w.input, w.support);
                    wdist = best.normal.dot(w.support) - best.d;
                    
                    if (wdist > EPA_ACCURACY) {
                        for (int j = 0; j < 3 && valid; j++) {
                            valid &= expand(pass, w, best.adjacent[j], 
                                            best.faceIndex[j], horizon);
                        }
                        
                        if (valid && horizon.numFaces >= 3) {
                            bind(horizon.cf, 1, horizon.ff, 2);
                            best.remove();
                            best = findBest();
                            if (best.p >= outer.p)
                                outer = best;
                        } else {
                            status = Status.INVALID_HULL;
                            break;
                        }
                    } else {
                        status = Status.ACCURACY_REACHED;
                        break;
                    }
                }
                
                Vector3f projection = outer.normal.scale(outer.d, temp1.get());
                normal.set(outer.normal);
                depth = outer.d;
                
                Vector3f t1 = temp2.get();
                Vector3f t2 = temp3.get();
                
                float w1 = outer.vertices[1].support.sub(projection, t1).cross(outer.vertices[2].support.sub(projection, t2)).length();
                float w2 = outer.vertices[2].support.sub(projection, t1).cross(outer.vertices[0].support.sub(projection, t2)).length();
                float w3 = outer.vertices[0].support.sub(projection, t1).cross(outer.vertices[1].support.sub(projection, t2)).length();
                
                float sum = w1 + w2 + w3;
                this.simplex = new Simplex(outer.vertices[0], outer.vertices[1], outer.vertices[2],
                                           w1 / sum, w2 / sum, w3 / sum);
                return;
            }
        }
        
        // fallback
        status = Status.FALLBACK;
        guess.scale(-1f, normal);
        float nl = normal.length();
        if (nl > 0)
            normal.scale(1f / nl);
        else
            normal.set(1f, 0f, 0f);
        depth = 0f;
        this.simplex = new Simplex(simplex.getSample(0));
    }
        
    private Face findBest() {
        Face minf = hull.get(0);
        float mind = minf.d * minf.d;
        float maxp = minf.p;
        
        Face f;
        float sqd;
        int ct = hull.size();
        for (int i = 1; i < ct; i++) {
            f = hull.get(i);
            sqd = f.d * f.d;
            if (f.p >= maxp && sqd < mind) {
                minf = f;
                mind = sqd;
                maxp = f.p;
            }
        }
        
        return minf;
    }
    
    private boolean expand(int pass, SupportSample w, Face face, int index, Horizon horizon) {
        if (face.pass != pass) {
            int e1 = I1M3[index];
            if (face.normal.dot(w.support) - face.d < -EPA_PLANE_EPS) {
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
    
    private static class Horizon {
        Face cf;
        Face ff;
        int numFaces;
    }
    
    private class Face {
        final Vector3f normal;
        float d;
        float p;
        
        final SupportSample[] vertices;
        final Face[] adjacent;
        final int[] faceIndex;
        
        int pass;
        int hullIndex;
        
        public Face(SupportSample a, SupportSample b, SupportSample c, boolean force) {
            Vector3f t = temp1.get();
            
            adjacent = new Face[3];
            faceIndex = new int[3];
            pass = 0;
            
            vertices = new SupportSample[] { a, b, c };
            normal = b.support.sub(a.support, null).cross(c.support.sub(a.support, t));
            float l = normal.length();
            boolean v = l > EPA_ACCURACY;
            
            float invL = 1f / l;
            float d1 = a.support.dot(normal.cross(a.support.sub(b.support, t), t));
            float d2 = b.support.dot(normal.cross(b.support.sub(c.support, t), t));
            float d3 = c.support.dot(normal.cross(c.support.sub(a.support, t), t));
            p = Math.min(Math.min(d1, d2), d3) * (v ? invL : 1f);
            if (p >= -EPA_INSIDE_EPS)
                p = 0;
            
            hullIndex = -1;
            if (v) {
                d = a.support.dot(normal) * invL;
                normal.scale(invL);
                if (force || d >= EPA_PLANE_EPS) {
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
