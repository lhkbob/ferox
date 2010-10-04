package com.ferox.physics.collision.narrow;

import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.DefaultCollisionHandler;
import com.ferox.physics.collision.algorithm.ClosestPair;
import com.ferox.physics.collision.shape.Box;

public class GjkTest {
    public static void main(String[] args) {
        Box b1 = new Box(2f, 2f, 2f);
        Box b2 = new Box(40f, 2f, 40f);
        
        Collidable objA = new Collidable(new Transform(new Vector3f(0f, -39f, 0f)), b1);
        Collidable objB = new Collidable(new Transform(new Vector3f(0f, -40f, 0f)), b2);
        
        ClosestPair p = new DefaultCollisionHandler().getClosestPair(objB, objA);
        System.out.println(p.getClosestPointOnA() + " " + p.getClosestPointOnB() + " " + p.getContactNormal() + " " + p.getDistance());
    }
}
