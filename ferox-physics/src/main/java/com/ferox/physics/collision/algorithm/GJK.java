package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

public class GJK {
    private static final int GJK_MAX_ITERATIONS = 128;
    private static final double GJK_MIN_DISTANCE = .00001;
    private static final double GJK_DUPLICATE_EPS = .0001;
    private static final double GJK_ACCURACY = .00001;

    public static Simplex evaluate(MinkowskiShape shape, @Const Vector3 guess) {
        Simplex simplex = new Simplex(shape);
        Vector3 ray = new Vector3(guess);
        if (ray.lengthSquared() < GJK_MIN_DISTANCE * GJK_MIN_DISTANCE)
            ray.set(1, 0, 0); // arbitrary guess
        
        double alpha = 0.0;
        
        // add first vertex
        ray.set(simplex.addVertex(guess, true));
        simplex.setWeight(0, 1.0);
        
        Vector3[] oldSupports = new Vector3[] { new Vector3(ray), new Vector3(ray),
                                                new Vector3(ray), new Vector3(ray) };
        int lastSupportIndex = 0;
        for (int i = 0; i < GJK_MAX_ITERATIONS; i++) {
            double rayLength = ray.length();
            if (rayLength < GJK_MIN_DISTANCE) {
                simplex.setIntersection(true);
                return simplex;
            }
            
            // add another vertex
            Vector3 support = simplex.addVertex(ray, true);
            
            // check for duplicates
            for (int j = 0; j < oldSupports.length; j++) {
                if (support.epsilonEquals(oldSupports[j], GJK_DUPLICATE_EPS)) {
                    // found a duplicate so terminate after removing duplicate
                    simplex.discardLastVertex();
                    return simplex;
                }
            }
            
            lastSupportIndex = (lastSupportIndex + 1) & 3;
            oldSupports[lastSupportIndex].set(support);
            
            // check termination condition
            alpha = Math.max(ray.dot(support) / rayLength, alpha);
            if ((rayLength - alpha) - (GJK_ACCURACY * rayLength) <= 0.0) {
                // error threshold is small enough
                simplex.discardLastVertex();
                return simplex;
            }
            
            // reduce for next iteration
            if (simplex.reduce()) {
                if (simplex.getRank() == Simplex.MAX_RANK) {
                    // it's a valid simplex, but represents an intersection
                    simplex.setIntersection(true);
                    return simplex;
                }
                
                // compute next guess
                ray.set(0.0, 0.0, 0.0);
                for (int j = 0; j < simplex.getRank(); j++) {
                    ray.add(new Vector3().scale(simplex.getVertex(j), simplex.getWeight(j)));
                }
            } else {
                simplex.discardLastVertex();
                return simplex;
            }
        }
        
        // if we've reached here, it's invalid
        return null;
    }
}
