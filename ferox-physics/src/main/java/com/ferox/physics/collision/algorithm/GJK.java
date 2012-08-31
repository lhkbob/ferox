package com.ferox.physics.collision.algorithm;

import com.ferox.math.Const;
import com.ferox.math.Vector3;

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
    public static final double GJK_MIN_DISTANCE = .00001;
    public static final double GJK_DUPLICATE_EPS = .0001;
    public static final double GJK_ACCURACY = .00001;

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
    private Simplex2 simplex;

    /**
     * Create a new GJK instance that will evaluate the pair of convex shapes
     * represented by the given {@link MinkowskiDifference}.
     * 
     * @param support The MinkowskiDifference to evaluate
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
        return new Simplex(simplex);
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
    public Status evaluate(@Const Vector3 guess) {
        if (guess == null)
            throw new NullPointerException("Guess cannot be null");
        
        Status status = Status.VALID;
        simplex = new Simplex2(function.asShape());
        Vector3 ray = new Vector3(guess);
        if (ray.lengthSquared() < GJK_MIN_DISTANCE * GJK_MIN_DISTANCE)
            ray.set(-1, 0, 0); // for 0-vector guess, choose arbitrary vector to start
        
        int iterations = 0;
        double alpha = 0.0;
        
//        simplex.addVertex(function, ray, true);
        ray.set(simplex.addVertex(ray, true));
//        simplex.getVertex(0).setWeight(1);
        simplex.setWeight(0, 1.0);
//        ray.set(simplex.getVertex(0).getVertex());
        // old support values are tracked so we can terminate when a duplicate is 
        // returned in subsequent iterations
        Vector3[] oldSupports = new Vector3[] { new Vector3(ray), new Vector3(ray), 
                                                new Vector3(ray), new Vector3(ray) };
        
        int lastSupportIndex = 0;
//        Vector3 scaledVertex = new Vector3();

        double rayLength;
        Vector3 support;
        while(status == Status.VALID) {
//            System.out.println("gjk iter " + iterations + " " + simplex);
            rayLength = ray.length();
            if (rayLength < GJK_MIN_DISTANCE) {
                // touching or inside
                status = Status.INSIDE;
                break;
            }

            // add the new vertex
            support = simplex.addVertex(ray, true);
//            simplex.addVertex(function, ray, true);
//            support = simplex.getVertex(simplex.getRank() - 1).getVertex();
            
            // check for duplicates
            boolean duplicate = false;
            for (int i = 0; i < 4; i++) {
                if (support.epsilonEquals(oldSupports[i], GJK_DUPLICATE_EPS)) {
                    duplicate = true;
                    break;
                }
            }
            
            if (duplicate) {
                // return the old simplex since we didn't add any vertices
//                simplex.removeVertex();
                simplex.discardLastVertex();
                break;
            } else {
                // update the old support storage
                lastSupportIndex = (lastSupportIndex + 1) & 3;
                oldSupports[lastSupportIndex].set(support);
            }
            
            // check for termination
            alpha = Math.max(ray.dot(support) / rayLength, alpha);
            if ((rayLength - alpha) - (GJK_ACCURACY * rayLength) <= 0.0) {
                // error threshold is small enough so we can terminate
//                simplex.removeVertex();
                simplex.discardLastVertex();
                break;
            }
            
            // reduce the simplex
            if (simplex.reduce()) {
                // the simplex is valid, compute next guess
                ray.set(0.0, 0.0, 0.0);
                for (int i = 0; i < simplex.getRank(); i++)
                    ray.add(new Vector3().scale(simplex.getVertex(i), simplex.getWeight(i)));
//                    ray.add(new Vector3().scale(simplex.getVertex(i).getVertex(), simplex.getVertex(i).getWeight()));
                
                // terminate if the simplex is full
                if (simplex.getRank() == 4)
                    status = Status.INSIDE;
            } else {
                // terminate using the old simplex
//                simplex.removeVertex();
                simplex.discardLastVertex();
//                status = Status.FAILED;
                break;
            }
            
            iterations++;
            if (iterations > GJK_MAX_ITERATIONS)
                status = Status.FAILED;
        }
        
        return status;
    }
}