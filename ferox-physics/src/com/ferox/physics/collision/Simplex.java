package com.ferox.physics.collision;

import java.util.Arrays;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public class Simplex {
    static class SupportSample {
        final Vector3f input = new Vector3f();
        final Vector3f support = new Vector3f();
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
    
    public Simplex() {
        vertices = new SupportSample[4];
        weights = new float[4];
        rank = 0;
        mask = new int[1];
        
        for (int i = 0; i < 4; i++)
            vertices[i] = new SupportSample();
    }
    
    Simplex(SupportSample c1) {
        vertices = new SupportSample[] { c1, new SupportSample(), new SupportSample(), new SupportSample() };
        weights = new float[] { 1f, 0f, 0f, 0f };
        rank = 1;
    }
    
    Simplex(SupportSample c1, SupportSample c2, SupportSample c3, float w1, float w2, float w3) {
        vertices = new SupportSample[] { c1, c2, c3, new SupportSample() };
        weights = new float[] { w1, w2, w3, 0f };
        rank = 3;
    }
    
    SupportSample getSample(int i) {
        return vertices[i];
    }
    
    public int getRank() {
        return rank;
    }
    
    public float getWeight(int vertex) {
        return weights[vertex];
    }
    
    public void setWeight(int vertex, float weight) {
        weights[vertex] = weight;
    }
    
    public ReadOnlyVector3f getVertex(int vertex) {
        return vertices[vertex].support;
    }
    
    public ReadOnlyVector3f getSupportInput(int vertex) {
        return vertices[vertex].input;
    }
    
    public boolean encloseOrigin(MinkowskiDifference shape) {
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
            Vector3f d = vertices[1].support.sub(vertices[0].support, null);
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
            Vector3f n = vertices[1].support.sub(vertices[0].support, null).cross(vertices[2].support.sub(vertices[0].support, null));
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
    
    public void orient() {
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
    
    public void addVertex(MinkowskiDifference s, ReadOnlyVector3f d) {
        addVertex(s, d, false);
    }
    
    public void addVertex(MinkowskiDifference s, ReadOnlyVector3f d, boolean negate) {
        weights[rank] = 0f;
        if (negate)
            d.scale(-1f, vertices[rank].input).normalize();
        else
            d.normalize(vertices[rank].input);
        s.getSupport(vertices[rank].input, vertices[rank].support);
        
        rank++;
    }
    
    public void removeVertex() {
        rank--;
    }
    
    public boolean reduce() {
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
        
        if (sqdist >= 0f) {
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
    
    private static class SimplexUtil {
        private static final int[] IMD3 = new int[] {1, 2, 0};

        // used only in projectOrigin2
        private final Vector3f p1D = new Vector3f(); 
        
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
            Vector3f d = b.sub(a, p1D);
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