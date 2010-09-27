package com.ferox.physics.collision.algorithm;

import java.util.Arrays;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

/**
 * <p>
 * In the mathematical sense, a Simplex is the generalization of a triangle to
 * arbitrary dimensions. In other terms, it's the smallest convex set containing
 * a set of vertices: a 0-simplex is a point, a 1-simplex is a line, a 2-simplex
 * is a triangle, etc. This implementation of Simplex is tied to the details of
 * the {@link GJK} and {@link EPA} algorithms and provides the storage they need
 * to represent their solutions.
 * </p>
 * <p>
 * Because of this, Simplex is not heavily configurable. It is not possible to
 * add arbitrary vertices but vertices are added by evaluating the support of
 * the MinkowskiDifference involved in the algorithms' iterations.
 * </p>
 * <p>
 * Like the GJK and EPA classes, much of this code is a port and clean-up of the
 * code from "BulletCollision/NarrowPhase/btGjkEpa2.cpp" written by Nathanael
 * Presson for the Bullet physics engine.
 * </p>
 * 
 * @author Michael Ludwig
 * @author Nathanael Presson
 */
public class Simplex {
    static class SupportSample {
        final Vector3f input = new Vector3f();
        final Vector3f support = new Vector3f();
        
        @Override
        public String toString() {
            return "{" + input + ", " + support + "}";
        }
    }
    
    // because projecting takes up so much extra storage, we cache a SimplexUtil per thread
    private static final ThreadLocal<SimplexUtil> simplexUtil = new ThreadLocal<SimplexUtil>() {
        @Override
        protected SimplexUtil initialValue() {
            return new SimplexUtil();
        }
    };
    
    private final SupportSample[] vertices;
    private final float[] weights;
    private int rank; // 0 -> 3
    
    private int[] mask; // for use with SimplexUtil
    
    /**
     * Create a Simplex that is initially empty and has a rank of 0.
     */
    public Simplex() {
        vertices = new SupportSample[4];
        weights = new float[4];
        rank = 0;
        mask = new int[1];
        
        for (int i = 0; i < 4; i++)
            vertices[i] = new SupportSample();
    }

    /**
     * Create a Simplex that uses the given sample as its only vertex. It will
     * have a rank of 1, and the samples weight will be 1.
     * 
     * @param c1 The support sample used for the only vertex
     * @throws NullPointerException if c1 is null
     */
    Simplex(SupportSample c1) {
        if (c1 == null)
            throw new NullPointerException("SupportSample cannot be null");
        vertices = new SupportSample[] { c1, new SupportSample(), new SupportSample(), new SupportSample() };
        weights = new float[] { 1f, 0f, 0f, 0f };
        rank = 1;
    }

    /**
     * Create a Simplex that uses the given samples and weights as its three
     * vertices. It will have a rank of 3, and it is assumed that the three
     * provided weights sum to 1.
     * 
     * @param c1 The first vertex sample
     * @param c2 The second vertex sample
     * @param c3 The third vertex sample
     * @param w1 The first vertex's weight
     * @param w2 The second vertex's weight
     * @param w3 The third vertex's weight
     * @throws NullPointerException if c1, c2, or c3 are null
     */
    Simplex(SupportSample c1, SupportSample c2, SupportSample c3, float w1, float w2, float w3) {
        if (c1 == null || c2 == null || c3 == null)
            throw new NullPointerException("SupportSamples cannot be null");
        vertices = new SupportSample[] { c1, c2, c3, new SupportSample() };
        weights = new float[] { w1, w2, w3, 0f };
        rank = 3;
    }

    /**
     * @param i The sample to return, assumed to be in [0, rank - 1]
     * @return The SupportSample instance holding the support data for the given
     *         vertex index
     * @throws IndexOutOfBoundsException if i is invalid
     */
    SupportSample getSample(int i) {
        return vertices[i];
    }

    /**
     * Return the rank, or number of vertices, of the Simplex. The rank can
     * range from 0 to 4. A rank of 0 is an empty simplex, a rank of 1 is a
     * point, 2 is a line, 3 is a triangle, and 4 is a tetrahedron.
     * 
     * @return The current rank
     */
    public int getRank() {
        return rank;
    }

    /**
     * Return the weight assigned to the given vertex of this simplex. The
     * weight is determined by the last call to {@link #setWeight(int, float)},
     * or can be updated automatically after a call to {@link #reduce()}. A
     * simplex's vertex inputs can be linearly combined with its weights to
     * construct the closest points between two hulls.
     * 
     * @param vertex The vertex index, from 0 to {@link #getRank()} - 1
     * @return The vertex weight
     * @throws IndexOutOfBoundsException if vertex is invalid
     */
    public float getWeight(int vertex) {
        if (vertex < 0 || vertex >= rank)
            throw new IndexOutOfBoundsException("Invalid index: " + vertex + ", must be in [0, " + (rank - 1) + "]");
        return weights[vertex];
    }

    /**
     * Forcibly assign the weight for a given vertex of this simplex. It is
     * assumed that the constraint that all weights sum to one is still met,
     * after all weights are updated.
     * 
     * @param vertex The vertex index, as in {@link #getWeight(int)}
     * @param weight The new vertex weight
     * @throws IndexOutOfBoundsException if vertex is invalid
     */
    void setWeight(int vertex, float weight) {
        if (vertex < 0 || vertex >= rank)
            throw new IndexOutOfBoundsException("Invalid index: " + vertex + ", must be in [0, " + (rank - 1) + "]");
        weights[vertex] = weight;
    }

    /**
     * <p>
     * Return the vertex of this Simplex. The returned vector is within the
     * coordinate space of the MinkowskiDifference used to generate the vertices
     * of the simplex (see
     * {@link #addVertex(MinkowskiDifference, ReadOnlyVector3f)}).
     * </p>
     * <p>
     * The returned vector is the support of the vector returned by
     * {@link #getSupportInput(int)} when using the original
     * MinkowskiDifference.
     * </p>
     * 
     * @param vertex The vertex index as in {@link #getWeight(int)}
     * @return The vertex at the given index
     * @throws IndexOutOfBoundsException if vertex is invalid
     */
    public ReadOnlyVector3f getVertex(int vertex) {
        if (vertex < 0 || vertex >= rank)
            throw new IndexOutOfBoundsException("Invalid index: " + vertex + ", must be in [0, " + (rank - 1) + "]");
        return vertices[vertex].support;
    }

    /**
     * Return the vector representing the input to
     * {@link MinkowskiDifference#getSupport(ReadOnlyVector3f, Vector3f)}. The
     * values within the vector are identical or the negation of the input
     * vector passed into
     * {@link #addVertex(MinkowskiDifference, ReadOnlyVector3f, boolean)} for
     * the newly added vertex.
     * 
     * @param vertex The vertex index
     * @return The support input passed to
     *         {@link #addVertex(MinkowskiDifference, ReadOnlyVector3f)}
     * @throws IndexOutOfBoundsException if vertex is invalid
     */
    public ReadOnlyVector3f getSupportInput(int vertex) {
        if (vertex < 0 || vertex >= rank)
            throw new IndexOutOfBoundsException("Invalid index: " + vertex + ", must be in [0, " + (rank - 1) + "]");
        return vertices[vertex].input;
    }

    /**
     * Manipulate this simplex so that its vertices will enclose the origin.
     * This is a necessary initial condition of the EPA algorithm after the GJK
     * algorithm has been applied. The specified MinkowskiDifference is assumed
     * to be the same shape that seeded the vertices of this Simplex.
     * 
     * @param shape The MinkowskiDifference controlling the support evaluation
     *            for this Simplex
     * @return True if the simplex could be modified to enclose the origin
     * @throws NullPointerException if shape is null
     */
    public boolean encloseOrigin(MinkowskiDifference shape) {
        if (shape == null)
            throw new NullPointerException("MinkowskiDifference cannot be null");
        
        if (encloseOriginImpl(shape)) {
            orient();
            return true;
        } else
            return false;
    }
    
    private boolean encloseOriginImpl(MinkowskiDifference shape) {
        switch(rank) {
        case 1: {
            Vector3f axis = new Vector3f();
            for (int i = 0; i < 3; i++) {
                addVertex(shape, axis.set(0f, 0f, 0f).set(i, 1f));
                if (encloseOrigin(shape))
                    return true;
                removeVertex();
                addVertex(shape, axis.scale(-1f));
                if (encloseOrigin(shape))
                    return true;
                removeVertex();
            }
            break; }
        case 2: {
            MutableVector3f d = vertices[1].support.sub(vertices[0].support, null);
            Vector3f axis = new Vector3f();
            
            for (int i = 0; i < 3; i++) {
                d.cross(axis.set(0f, 0f, 0f).set(i, 1f), axis);
                if (axis.lengthSquared() > 0) {
                    addVertex(shape, axis);
                    if (encloseOrigin(shape))
                        return true;
                    removeVertex();
                    addVertex(shape, axis.scale(-1f));
                    if (encloseOrigin(shape))
                        return true;
                    removeVertex();
                }
            }
            break; }
        case 3: {
            MutableVector3f n = vertices[1].support.sub(vertices[0].support, null).cross(vertices[2].support.sub(vertices[0].support, null));
            if (n.lengthSquared() > 0) {
                addVertex(shape, n);
                if (encloseOrigin(shape))
                    return true;
                removeVertex();
                addVertex(shape, n.scale(-1f));
                if (encloseOrigin(shape))
                    return true;
                removeVertex();
            }
            break; }
        case 4: {
            if (Math.abs(SimplexUtil.det(vertices[0].support.sub(vertices[3].support, null),
                                         vertices[1].support.sub(vertices[3].support, null),
                                         vertices[2].support.sub(vertices[3].support, null))) > 0f)
                return true;
            break; }
        }
        return false;
    }
    
    private void orient() {
        if (SimplexUtil.det(vertices[0].support.sub(vertices[3].support, null),
                            vertices[1].support.sub(vertices[3].support, null),
                            vertices[2].support.sub(vertices[3].support, null)) < 0f) {
            Vector3f t = new Vector3f();
            
            t.set(vertices[0].input);
            vertices[0].input.set(vertices[1].input);
            vertices[1].input.set(t);
            
            t.set(vertices[0].support);
            vertices[0].support.set(vertices[1].support);
            vertices[1].support.set(t);
            
            float tt = weights[0];
            weights[0] = weights[1];
            weights[1] = tt;
        }
    }

    /**
     * Add a new vertex to the Simplex by evaluating the support of the given
     * MinkowskiDifference with the input vector <tt>d</tt>. The new vertex is
     * added to the end and the simplex's rank is increased by 1.
     * 
     * @param s The MinkowskiDifference whose support function is evaluated
     * @param d The input to the support function
     * @throws NullPointerException if s or d are null
     */
    public void addVertex(MinkowskiDifference s, ReadOnlyVector3f d) {
        addVertex(s, d, false);
    }

    /**
     * Equivalent to {@link #addVertex(MinkowskiDifference, ReadOnlyVector3f)}
     * except that if <tt>negate</tt> is true, the negation of the input vector
     * is used to evaluate the support function.
     * 
     * @param s The MinkowskiDifference whose support function is evaluated
     * @param d The support function input
     * @param negate True if <tt>d</tt> should be negated before being passed to
     *            the support function
     */
    public void addVertex(MinkowskiDifference s, ReadOnlyVector3f d, boolean negate) {
        weights[rank] = 0f;
        if (negate)
            d.scale(-1f, vertices[rank].input).normalize();
        else
            d.normalize(vertices[rank].input);
        s.getSupport(vertices[rank].input, vertices[rank].support);
        
        rank++;
    }
    
    /**
     * Remove the last added vertex from the Simplex and reduce its rank by one.
     */
    public void removeVertex() {
        rank--;
    }
    
    /**
     * Update the Simplex so that it represents the smallest sub-simplex of its
     * vertices that still contains the last added vertex. If false is returned,
     * the simplex could not be reduced further.
     * 
     * @return True if the simplex was successfully reduced, i.e. can be used
     *         again in another iteration of the GJK algorithm
     */
    public boolean reduce() {
        if (projectOrigin()) {
            // the simplex is still valid, so compact it
            int m = mask[0];
            for (int i = 0; i < rank; i++) {
                if ((m & (1 << i)) != 0) {
                    // find lowest empty vertex slot
                    int low = -1;
                    for (int j = 0; j < i; j++) {
                        if ((m & (1 << j)) == 0) {
                            low = j;
                            break;
                        }
                    }
                    
                    if (low >= 0) {
                        // copy the ith vertex into low
                        vertices[low].input.set(vertices[i].input);
                        vertices[low].support.set(vertices[i].support);
                        weights[low] = weights[i];
                        
                        m |= (1 << low);
                        m &= ~(1 << i);
                    }
                }
            }
            
            // determine new rank after compaction
            rank = 0;
            for (int i = 3; i >= 0; i--) {
                if ((m & (1 << i)) != 0) {
                    rank = i + 1;
                    break;
                }
            }
            
            return true;
        } else
            return false;
    }
    
    @Override
    public String toString() {
        StringBuilder b = new StringBuilder();
        b.append("(Simplex: rank=");
        b.append(rank);
        
        b.append(", vertices=");
        b.append(Arrays.toString(Arrays.copyOf(vertices, rank)));
        
        b.append(", weights=");
        b.append(Arrays.toString(Arrays.copyOf(weights, rank)));
        b.append(")");
        
        return b.toString();
    }
    
    /*
     * Update the weights and mask to perform the actual math needed by reduce()
     */
    private boolean projectOrigin() {
        float[] weights = weightCache.get();
        int[] mask = maskCache.get();
        
        Arrays.fill(weights, 0f);
        mask[0] = 0;
        
        float sqdist = 0f;
        switch(rank) {
        case 2:
            sqdist = simplexUtil.get().projectOrigin2(vertices[0].support, vertices[1].support, 
                                                      weights, mask);
            break;
        case 3:
            sqdist = simplexUtil.get().projectOrigin3(vertices[0].support, vertices[1].support,
                                                      vertices[2].support, weights, mask);
            break;
        case 4:
            sqdist = simplexUtil.get().projectOrigin4(vertices[0].support, vertices[1].support, 
                                                      vertices[2].support, vertices[3].support, weights, mask);
            break;
        }
        
        if (sqdist > 0f) {
            // copy weights back into member variables
            System.arraycopy(weights, 0, this.weights, 0, 4);
            this.mask[0] = mask[0];
            return true;
        } else
            return false;
    }
    
    private static final ThreadLocal<float[]> weightCache = new ThreadLocal<float[]>() {
        @Override
        protected float[] initialValue() { return new float[4]; }
    };
    private static final ThreadLocal<int[]> maskCache = new ThreadLocal<int[]>() {
        @Override
        protected int[] initialValue() { return new int[1]; }
    };
    
    private static class SimplexUtil {
        private static final int[] IMD3 = new int[] {1, 2, 0};

        // used only in projectOrigin2
        private final Vector3f p2 = new Vector3f(); 
        
        // used only in projectOrigin3
        private final ReadOnlyVector3f[] vt3 = new ReadOnlyVector3f[3];
        private final Vector3f[] dl3 = new Vector3f[] { new Vector3f(), new Vector3f(), new Vector3f() };
        private final Vector3f n3 = new Vector3f();
        private final Vector3f p3 = new Vector3f();
        
        private final float[] subw3 = new float[2];
        private final int[] subm3 = new int[1];
        
        // used only in projectOrigin4
        private final ReadOnlyVector3f[] vt4 = new ReadOnlyVector3f[4];
        private final Vector3f[] dl4 = new Vector3f[] { new Vector3f(), new Vector3f(), new Vector3f() };
        private final Vector3f n4 = new Vector3f();
        private final Vector3f p4 = new Vector3f();
        
        private final float[] subw4 = new float[3];
        private final int[] subm4 = new int[1];
        
        
        
        public float projectOrigin2(ReadOnlyVector3f a, ReadOnlyVector3f b, float[] weights, int[] mask) {
            MutableVector3f d = b.sub(a, p2);
            float l = d.lengthSquared();
            
            if (l > 0f) {
                float t = -a.dot(d) / l;
                if (t >= 1) {
                    weights[0] = 0f; 
                    weights[1] = 1f; 
                    mask[0] = 2; 
                    return b.lengthSquared();
                } else if (t <= 0) {
                    weights[0] = 1f; 
                    weights[1] = 0f;
                    mask[0] = 1; 
                    return a.lengthSquared();
                } else {
                    weights[0] = 1 - t; 
                    weights[1] = t; 
                    mask[0] = 3; 
                    return d.scaleAdd(t, a).lengthSquared();
                }
            } else
                return -1f;
        }
        
        public float projectOrigin3(ReadOnlyVector3f a, ReadOnlyVector3f b, ReadOnlyVector3f c, float[] weights, int[] mask) {
            vt3[0] = a; vt3[1] = b; vt3[2] = c;
            a.sub(b, dl3[0]);
            b.sub(c, dl3[1]);
            c.sub(a, dl3[2]);
            
            dl3[0].cross(dl3[1], n3);
            float l = n3.lengthSquared();
            
            if (l > 0f) {
                float minDist = -1f;
                subw3[0] = 0f; subw3[1] = 0f;
                subm3[0] = 0;
                
                for (int i = 0; i < 3; i++) {
                    if (vt3[i].dot(dl3[i].cross(n3, p3)) > 0) {
                        int j = IMD3[i];
                        float subd = projectOrigin2(vt3[i], vt3[j], subw3, subm3);
                        if (minDist < 0f || subd < minDist) {
                            minDist = subd;
                            mask[0] = ((subm3[0] & 1) != 0 ? (1 << i) : 0) + 
                                      ((subm3[0] & 2) != 0 ? (1 << j) : 0);
                            weights[i] = subw3[0];
                            weights[j] = subw3[1];
                            weights[IMD3[j]] = 0f;
                        }
                    }
                }
                
                if (minDist < 0f) {
                    float d = a.dot(n3);
                    float s = (float) Math.sqrt(l);
                    n3.scale(d / l, p3);
                    
                    minDist = p3.lengthSquared();
                    mask[0] = 7;
                    weights[0] = dl3[1].cross(b.sub(p3, n3)).length() / s; // at this point dl[1] and n are throwaway
                    weights[1] = dl3[2].cross(c.sub(p3, n3)).length() / s; // at this point dl[2] and n are throwaway
                    weights[2] = 1 - weights[0] - weights[1];
                }
                return minDist;
            } else
                return -1f;
        }
        
        public float projectOrigin4(ReadOnlyVector3f a, ReadOnlyVector3f b, ReadOnlyVector3f c, ReadOnlyVector3f d, float[] weights, int[] mask) {
            vt4[0] = a; vt4[1] = b; vt4[2] = c; vt4[3] = d;
            a.sub(d, dl4[0]);
            b.sub(d, dl4[1]);
            c.sub(d, dl4[2]);
            
            float vl = det(dl4[0], dl4[1], dl4[2]);
            boolean ng = (vl * a.dot(b.sub(c, n4).cross(a.sub(b, p4)))) <= 0f;
            
            if (ng && Math.abs(vl) > 0f) {
                float minDist = -1f;
                subw4[0] = 0f; subw4[1] = 0f; subw4[2] = 0f;
                subm4[0] = 0;
                
                for (int i = 0; i < 3; i++) {
                    int j = IMD3[i];
                    float s = vl * d.dot(dl4[i].cross(dl4[j], n4));
                    if (s > 0) {
                        float subd = projectOrigin3(vt4[i], vt4[j], d, subw4, subm4);
                        if (minDist < 0f || subd < minDist) {
                            minDist = subd;
                            mask[0] = ((subm4[0] & 1) != 0 ? (1 << i) : 0) +
                                      ((subm4[0] & 2) != 0 ? (1 << j) : 0) +
                                      ((subm4[0] & 4) != 0 ? 8 : 0);
                            weights[i] = subw4[0];
                            weights[j] = subw4[1];
                            weights[IMD3[j]] = 0f;
                            weights[3] = subw4[2];
                        }
                    }
                }
                
                if (minDist < 0f) {
                    minDist = 0f;
                    mask[0] = 15;
                    weights[0] = det(c, b, d) / vl;
                    weights[1] = det(a, c, d) / vl;
                    weights[2] = det(b, a, d) / vl;
                    weights[3] = 1 - weights[0] - weights[1] - weights[2];
                }
                return minDist;
            } else
                return -1f;
        }
        
        public static float det(ReadOnlyVector3f a, ReadOnlyVector3f b, ReadOnlyVector3f c) {
            return a.getY() * b.getZ() * c.getX() + a.getZ() * b.getX() * c.getY() -
                   a.getX() * b.getZ() * c.getY() - a.getY() * b.getX() * c.getZ() +
                   a.getX() * b.getY() * c.getZ() - a.getZ() * b.getY() * c.getX();
        }
    }
}