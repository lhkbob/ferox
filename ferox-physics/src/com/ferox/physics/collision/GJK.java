package com.ferox.physics.collision;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

public class GJK {
    public static final int GJK_MAX_ITERATIONS = 128;
    public static final float GJK_MIN_DISTANCE = .0001f;
    public static final float GJK_DUPLICATE_EPS = .0001f;
    public static final float GJK_ACCURACY = .0001f;
    
    public static enum Status {
        VALID, INSIDE, FAILED
    }
    
    private final MinkowskiDifference shape;
    private Simplex simplex;
    private float distance;
    private Status status;
    
    public GJK(MinkowskiDifference shape) {
        this.shape = shape;
        status = Status.FAILED;
        simplex = null;
        distance = 0f;
    }
    
    public MinkowskiDifference getMinkowskiDifference() {
        return shape;
    }
    
    public Status getStatus() {
        return status;
    }
    
    public Simplex getSimplex() {
        return simplex;
    }
    
    public float getDistance() {
        return distance;
    }
    
    public void evaluate(ReadOnlyVector3f guess) {
        Status status = Status.VALID;
        simplex = new Simplex();
        Vector3f ray = new Vector3f(guess);
        
        int iterations = 0;
        float alpha = 0f;
        
        simplex.addVertex(shape, ray, true);
        simplex.setWeight(0, 1f);
        ray.set(simplex.getVertex(0));
        
        // old support values are tracked so we can terminate when a duplicate is 
        // returned in subsequent iterations
        Vector3f[] oldSupports = new Vector3f[] { new Vector3f(ray), new Vector3f(ray), 
                                                  new Vector3f(ray), new Vector3f(ray) };
        int lastSupportIndex = 0;
        
        float rayLength;
        ReadOnlyVector3f support;
        while(status == Status.VALID) {
            rayLength = ray.length();
            if (rayLength < GJK_MIN_DISTANCE) {
                // touching or inside
                status = Status.INSIDE;
                break;
            }
            
            // add the new vertex
            simplex.addVertex(shape, ray, true);
            support = simplex.getVertex(simplex.getRank() - 1);
            
            // check for duplicates
            boolean duplicate = false;
            for (int i = 0; i < 4; i++) {
                if (support.distanceSquared(oldSupports[i]) < GJK_DUPLICATE_EPS) {
                    duplicate = true;
                    break;
                }
            }
            
            if (duplicate) {
                // return the old simplex since we didn't add any vertices
                simplex.removeVertex();
                break;
            } else {
                // update the old support storage
                lastSupportIndex = (lastSupportIndex + 1) & 3;
                oldSupports[lastSupportIndex].set(support);
            }
            
            // check for termination
            alpha = Math.max(ray.dot(support) / rayLength, alpha);
            if ((rayLength - alpha) - (GJK_ACCURACY * rayLength) <= 0f) {
                // error threshold is small enough so we can terminate
                simplex.removeVertex();
                break;
            }
            
            // reduce the simplex
            if (simplex.reduce()) {
                // the simplex is valid, compute next guess
                ray.set(0f, 0f, 0f);
                for (int i = 0; i < simplex.getRank(); i++)
                    simplex.getVertex(i).scaleAdd(simplex.getWeight(i), ray, ray);
                
                // terminate if the simplex is full
                if (simplex.getRank() == 4)
                    status = Status.INSIDE;
            } else {
                // terminate using the old simplex
                simplex.removeVertex();
                break;
            }
            
            iterations++;
            if (iterations > GJK_MAX_ITERATIONS)
                status = Status.FAILED;
        }
        
        distance = (status == Status.VALID ? ray.length() : 0f);
        this.status = status;
    }
}