package com.ferox.physics.collision.narrow;

import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.algorithm.GjkEpaCollisionAlgorithm;
import com.ferox.physics.collision.shape.Box;

public class GjkTest {
    public static void main(String[] args) {
        Box b1 = new Box(2f, 2f, 2f);
        Box b2 = new Box(2f, 2f, 2f);
        
        Collidable objA = new Collidable(new Transform(new Vector3f(7.6200185f, 3.3421328f, -9.141048f)), b1);
        Collidable objB = new Collidable(new Transform(new Vector3f(6.982416f, 5.32309f, -9.085202f)), b2);
        
        ClosestPair p = new GjkEpaCollisionAlgorithm().getClosestPair(objA, objB);
        System.out.println(p.getClosestPointOnA() + " " + p.getClosestPointOnB() + " " + p.getContactNormal() + " " + p.getDistance());
    }
}
