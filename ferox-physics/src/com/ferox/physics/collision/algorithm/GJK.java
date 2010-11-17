package com.ferox.physics.collision.algorithm;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;

/**
 * <p>
 * GJK is a low-level implementation of the Gilbert-Johnson-Keerth algorithm for
 * detecting the closest pairs between two non-intersecting convex hulls. The
 * algorithm was originally presented in a paper titled: "A Fast Procedure for
 * Computing the Distance Between Complex Objects in Three-Dimensional Space".
 * </p>
 * <p>
 * The code within this class is a Java port, refactoring and clean-up of the
 * GJK implementation contained in the Bullet physics engine. Specifically, the
 * code in "/BulletCollision/NarrowPhaseCollision/btGjkEpa2.cpp" by Nathanael
 * Presson.
 * </p>
 * 
 * @author Michael Ludwig
 * @author Nathanael Presson
 */
public class GJK {
    public static final int GJK_MAX_ITERATIONS = 128;
    public static final float GJK_MIN_DISTANCE = .0001f;
    public static final float GJK_DUPLICATE_EPS = .0001f;
    public static final float GJK_ACCURACY = .0001f;

    /**
     * Status represents the three states that a GJK evaluation can take. A
     * state of VALID means that the convex shapes involved are not
     * intersecting. A state of INSIDE means the shapes are intersecting, but
     * contact points are unknown. A state of FAILED implies inconsistent data
     * means the GJK evaluation failed.
     */
    public static enum Status {
        VALID, INSIDE, FAILED
    }
    
    private final MinkowskiDifference function;
    private Simplex simplex;

    /**
     * Create a new GJK instance that will evaluate the pair of convex shapes
     * represented by the given {@link MinkowskiDifference}.
     * 
     * @param shape The MinkowskiDifference to evaluate
     * @throws NullPointerException if shape is null
     */
    public GJK(MinkowskiDifference support) {
        if (support == null)
            throw new NullPointerException("MinkowskiDifference cannot be null");
        
        function = support;
        simplex = null;
    }

    /**
     * Return the MinkowskiDifference used to build this GJK's Simplex.
     * 
     * @return The MinkowskiDifference
     */
    public MinkowskiDifference getMinkowskiDifference() {
        return function;
    }

    /**
     * Return the Simplex containing the results of the last run to
     * {@link #evaluate(ReadOnlyVector3f)}. If the GJK evaluation was VALID, the
     * returned Simplex can be used to reconstruct the closest pair. A status of
     * INSIDE means the returned Simplex can be used with {@link EPA}. A status
     * of FAILURE means the Simplex may be suitable for use with an EPA, but
     * numerical errors may result.
     * 
     * @return The Simplex formed from the last evaluation
     */
    public Simplex getSimplex() {
        return simplex;
    }

    /**
     * Run the GJK algorithm on the {@link MinkowskiDifference} specified when
     * the instance was constructed.
     * 
     * @param guess An initial guess as to the contact normal between the two
     *            convex shapes
     * @return The status of the evaluation
     * @throws NullPointerException if guess is null
     */
    public Status evaluate(ReadOnlyVector3f guess) {
        if (guess == null)
            throw new NullPointerException("Guess cannot be null");
        
        Status status = Status.VALID;
        simplex = new Simplex();
        Vector3f ray = new Vector3f(guess);
        if (ray.lengthSquared() < GJK_MIN_DISTANCE * GJK_MIN_DISTANCE)
            ray.set(-1f, 0f, 0f); // for 0-vector guess, choose arbitrary vector to start
        
        int iterations = 0;
        float alpha = 0f;
        
        simplex.addVertex(function, ray, true);
        simplex.getVertex(0).setWeight(1f);
        ray.set(simplex.getVertex(0).getVertex());
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
            simplex.addVertex(function, ray, true);
            support = simplex.getVertex(simplex.getRank() - 1).getVertex();
            
            // check for duplicates
            boolean duplicate = false;
            for (int i = 0; i < 4; i++) {
                if (support.epsilonEquals(oldSupports[i], (float) GJK_DUPLICATE_EPS)) {
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
                    simplex.getVertex(i).getVertex().scaleAdd(simplex.getVertex(i).getWeight(), ray, ray);
                
                // terminate if the simplex is full
                if (simplex.getRank() == 4)
                    status = Status.INSIDE;
            } else {
                // terminate using the old simplex
                simplex.removeVertex();
                status = Status.FAILED;
                break;
            }
            
            iterations++;
            if (iterations > GJK_MAX_ITERATIONS)
                status = Status.FAILED;
        }
        
        return status;
    }
}