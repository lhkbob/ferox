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
    /**
     * <p>
     * Vertex is a utility class for Simplex that encapsulates the data used to
     * create a vertex within the simplex. It contains the vertex location, the
     * input vector used in the support function, and an assignable weight.
     * </p>
     * <p>
     * Each vertex's weight in a simplex must be at least 0, and there is the
     * assumed contract that they sum to 1.
     * </p>
     */
    public static class Vertex {
        final Vector3f input;
        final Vector3f vertex;
        private float weight;
        
        public Vertex() {
            input = new Vector3f();
            vertex = new Vector3f();
            weight = 0f;
        }
        
        /**
         * @return The input vector used to evaluate the support function
         */
        public ReadOnlyVector3f getInputVector() {
            return input;
        }
        
        /**
         * @return The vertex of the simplex
         */
        public ReadOnlyVector3f getVertex() {
            return vertex;
        }
        
        /**
         * @return The current weight of the simplex
         */
        public float getWeight() {
            return weight;
        }

        /**
         * Assign a new weight to the vertex. It is the callers responsibility
         * to preserve the invariant that all vertices within a simplex have
         * weights summing to 1.
         * 
         * @param w The new weight
         * @throws IllegalArgumentException if w is less than 0
         */
        public void setWeight(float w) {
            if (w < 0f)
                throw new IllegalArgumentException("Weight must be positive, not: " + w);
            weight = w;
        }
    }
    
    private final Vertex[] vertices;
    private int rank; // 0 -> 3
    
    private int mask;
    // for use with SimplexUtil
    private final int[] maskCache;
    private final float[] weightCache;
    
    /**
     * Create a Simplex that is initially empty and has a rank of 0.
     */
    public Simplex() {
        vertices = new Vertex[4];
        rank = 0;
        
        maskCache = new int[1];
        weightCache = new float[4];
    }

    /**
     * Create a Simplex that uses the given vertice's as its three
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
    public Simplex(Vertex c1, Vertex c2, Vertex c3) {
        if (c1 == null || c2 == null || c3 == null)
            throw new NullPointerException("SupportSamples cannot be null");
        vertices = new Vertex[] { c1, c2, c3, null};
        rank = 3;
        
        maskCache = new int[1];
        weightCache = new float[4];
    }
    
    public void clear() {
        rank = 0;
    }

    /**
     * Return the vertex at the given index within this Simplex.
     * @param i The vertex to return, assumed to be in [0, rank - 1]
     * @return The SupportSample instance holding the support data for the given
     *         vertex index
     * @throws IndexOutOfBoundsException if i is invalid
     */
    public Vertex getVertex(int i) {
        if (i < 0 || i >= rank)
            throw new IndexOutOfBoundsException("Invalid index: " + i);
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
            MutableVector3f d = vertices[1].vertex.sub(vertices[0].vertex, null);
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
            MutableVector3f n = vertices[1].vertex.sub(vertices[0].vertex, null).cross(vertices[2].vertex.sub(vertices[0].vertex, null));
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
            if (Math.abs(SimplexUtil.det(vertices[0].vertex.sub(vertices[3].vertex, null),
                                         vertices[1].vertex.sub(vertices[3].vertex, null),
                                         vertices[2].vertex.sub(vertices[3].vertex, null))) > 0f)
                return true;
            break; }
        }
        return false;
    }
    
    private void orient() {
        if (SimplexUtil.det(vertices[0].vertex.sub(vertices[3].vertex, null),
                            vertices[1].vertex.sub(vertices[3].vertex, null),
                            vertices[2].vertex.sub(vertices[3].vertex, null)) < 0f) {
            Vector3f t = new Vector3f();
            
            t.set(vertices[0].input);
            vertices[0].input.set(vertices[1].input);
            vertices[1].input.set(t);
            
            t.set(vertices[0].vertex);
            vertices[0].vertex.set(vertices[1].vertex);
            vertices[1].vertex.set(t);
            
            float tt = vertices[0].weight;
            vertices[0].weight = vertices[1].weight;
            vertices[1].weight = tt;
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
        if (rank == 4)
            throw new IllegalStateException("Cannot add a vertex to a full simplex");
        if (vertices[rank] == null)
            vertices[rank] = new Vertex();
        Vertex v = vertices[rank++];
        
        v.weight = 0f;
        if (negate)
            d.scale(-1f, v.input).normalize();
        else
            d.normalize(v.input);
        s.getSupport(v.input, v.vertex);
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
            int m = mask;
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
                        vertices[low].vertex.set(vertices[i].vertex);
                        vertices[low].weight = vertices[i].weight;
                        
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
    
    /*
     * Update the weights and mask to perform the actual math needed by reduce()
     */
    private boolean projectOrigin() {
        Arrays.fill(weightCache, 0f);
        maskCache[0] = 0;
        
        float sqdist = 0f;
        switch(rank) {
        case 2:
            sqdist = simplexUtil.get().projectOrigin2(vertices[0].vertex, vertices[1].vertex, 
                                                      weightCache, maskCache);
            break;
        case 3:
            sqdist = simplexUtil.get().projectOrigin3(vertices[0].vertex, vertices[1].vertex,
                                                      vertices[2].vertex, weightCache, maskCache);
            break;
        case 4:
            sqdist = simplexUtil.get().projectOrigin4(vertices[0].vertex, vertices[1].vertex, 
                                                      vertices[2].vertex, vertices[3].vertex, weightCache, maskCache);
            break;
        }
        
        if (sqdist >= 0f) {
            // copy weights back into member variables
            for (int i = 0; i < rank; i++)
                vertices[i].weight = weightCache[i];
            mask = maskCache[0];
            return true;
        } else
            return false;
    }
    
    // because projecting takes up so much extra storage, we cache a SimplexUtil per thread
    private static final ThreadLocal<SimplexUtil> simplexUtil = new ThreadLocal<SimplexUtil>() {
        @Override
        protected SimplexUtil initialValue() {
            return new SimplexUtil();
        }
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
                    return d.scaleAdd((float) t, a).lengthSquared();
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
                    n3.scale((float) (d / l), p3);
                    
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