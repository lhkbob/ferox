package com.ferox.physics.collision.narrow;

import com.ferox.math.Matrix4f;
import com.ferox.physics.collision.algorithm.ClosestPair;
import com.ferox.physics.collision.algorithm.GjkEpaCollisionAlgorithm;
import com.ferox.physics.collision.shape.Box;

public class GjkTest {
    public static void main(String[] args) {
        float size1 = 2f;
        float size2 = 50f;
        
        int testsFailed = 0;
        int numTests = 1;
        
        GjkEpaCollisionAlgorithm algo = new GjkEpaCollisionAlgorithm(0.9f);
        GjkEpaCollisionAlgorithm.NUM_EPA_CHECKS = 0;
        GjkEpaCollisionAlgorithm.NUM_GJK_CHECKS = 0;

        long time = -System.nanoTime();
        for (int i = 0; i < numTests; i++) {
            if (runTest(algo, size1, size2)) {
                testsFailed++;
            }
        }
        time += System.nanoTime();
        System.out.println(testsFailed + " out of " + numTests + " failed");
        System.out.println(GjkEpaCollisionAlgorithm.NUM_GJK_CHECKS + " " + GjkEpaCollisionAlgorithm.NUM_EPA_CHECKS + " in " + (time / 1e6f));
    }
    
    private static boolean runTest(GjkEpaCollisionAlgorithm algo, float size1, float size2) {
        float sx = 0f;//(float) (Math.random() * 100 - 50);
        float sy = 0f;//(float) (Math.random() * 100 - 50);
        float sz = 0f;//(float) (Math.random() * 100 - 50);
        
        float minExtent = -(size2 + 3 * size1) / 2f;
        float maxExtent = (size2 + 3 * size1) / 2f;
        float lim = Math.abs((size2 - size1) / 2f);
        
        Box b1 = new Box(size1, size1, size1);
        Box b2 = new Box(size2, size2, size2);
        
        boolean failed = false;
        GjkEpaCollisionAlgorithm base = new GjkEpaCollisionAlgorithm(-1f); // no scale
        for (float x = minExtent; x <= maxExtent; x += .5) {
            for (float y = minExtent; y <= maxExtent; y += .5) {
                for (float z = minExtent; z <= maxExtent; z += .5) {
                    if ((x < -lim || x > lim) && (y < -lim || y > lim) && (z < -lim || z > lim)) {
                        Matrix4f t1 = new Matrix4f(1f, 0f, 0f, sx + x,
                                                   0f, 1f, 0f, sy + y,
                                                   0f, 0f, 1f, sz + z,
                                                   0f, 0f, 0f, 1f);
                        Matrix4f t2 = new Matrix4f(1f, 0f, 0f, sx,
                                                   0f, 1f, 0f, sy,
                                                   0f, 0f, 1f, sz,
                                                   0f, 0f, 0f, 1f);
                        
                        
                        ClosestPair expected = base.getClosestPair(b1, t1, b2, t2);
                        ClosestPair actual = algo.getClosestPair(b1, t1, b2, t2);
                        
                        String fail = null;
                        if (expected == null) {
                            if (actual == null)
                                fail = "Both algorithms report null";
                        } else {
                            if (actual == null)
                                fail = "Pair expected to be " + expected + " but was null";
                            else {
                                float eps = .001f;
                                if (!actual.getClosestPointOnA().epsilonEquals(expected.getClosestPointOnA(), eps)
                                    || !actual.getClosestPointOnB().epsilonEquals(expected.getClosestPointOnB(), eps))
                                    fail = "Pair expected to be " + expected + " but was " + actual;
                            }
                        }
                        if (fail != null) {
                            failed = true;
                            System.out.println(fail);
                        }
                    }
                }
            }
        }
        
        return failed;
    }
}
