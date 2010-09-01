package com.ferox.physics.collision;

import com.ferox.math.Matrix4f;
import com.ferox.physics.collision.shape.Box;
import com.ferox.physics.collision.shape.Sphere;

public class GjkTest {
    /**
     * @param args
     */
    public static void main(String[] args) {
        Matrix4f t1 = new Matrix4f(1f, 0f, 0f, 0f,
                                   0f, 1f, 0f, 1001.53f,
                                   0f, 0f, 1f, 0f,
                                   0f, 0f, 0f, 1f);
        
        Matrix4f t2 = new Matrix4f(1f, 0f, 0f, 0f,
                                   0f, 1f, 0f, 1000f,
                                   0f, 0f, 1f, 0f,
                                   0f, 0f, 0f, 1f);
        
        Collidable obj1 = new Collidable(t1, new Box(1f, 1f, 1f));
        Collidable obj2 = new Collidable(t2, new Box(2f, 2f, 2f));
        
        testPairDetector(new GjkEpaPairDetector(), obj1, obj2, 100000);
    }
    
    private static void testPairDetector(PairDetector detector, Collidable obj1, Collidable obj2, int numRuns) {
      for (int i = 0; i < numRuns; i++)
          detector.getClosestPair(obj1, obj2);
  
        long now = -System.currentTimeMillis();
        ClosestPair pair = null;
        for (int i = 0; i < numRuns; i++)
            pair = detector.getClosestPair(obj1, obj2);
        now += System.currentTimeMillis();
 
        System.out.println(detector.getClass().getSimpleName() + " took: " + now + " ms, answer = " + pair.getClosestPointOnA() + " " + pair.getClosestPointOnB() + " " + pair.getContactNormal() + " " + pair.getDistance());
    }
}
