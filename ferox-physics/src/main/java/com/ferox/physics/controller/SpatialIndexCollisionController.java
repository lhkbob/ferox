package com.ferox.physics.controller;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.bounds.IntersectionCallback;
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.CollisionAlgorithmProvider;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.collision.algorithm.GjkEpaCollisionAlgorithm;
import com.ferox.physics.collision.algorithm.SphereSphereCollisionAlgorithm;
import com.ferox.physics.collision.shape.Box;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;


public class SpatialIndexCollisionController extends CollisionController {
    private SpatialIndex<Entity> index;
    private CollisionAlgorithmProvider algorithms;
    
    // FIXME: add a setter, too
    public SpatialIndexCollisionController(SpatialIndex<Entity> index, CollisionAlgorithmProvider algorithms) {
        this.index = index;
        this.algorithms = algorithms;
    }
    
    @Override
    public void preProcess(double dt) {
        super.preProcess(dt);
        index.clear(true);
    }
    
    public static long buildTime = 0;
    public static long queryTime = 0;
    public static long genTime = 0;
    
    @Override
    public void process(double dt) {
        CollisionBody body1 = getEntitySystem().createDataInstance(CollisionBody.ID);
        CollisionBody body2 = getEntitySystem().createDataInstance(CollisionBody.ID);
        
        // fill the index with all collision bodies
        ComponentIterator it = new ComponentIterator(getEntitySystem());
        it.addRequired(body1);
        
        buildTime -= System.nanoTime();
        while(it.next()) {
            index.add(body1.getEntity(), body1.getWorldBounds());
        }
        buildTime += System.nanoTime();
        
        queryTime -= System.nanoTime();
        // query for all intersections
        index.query(new CollisionCallback(body1, body2));
        queryTime += System.nanoTime();
        
        genTime -= System.nanoTime();
        reportConstraints(dt);
        genTime += System.nanoTime();
    }
    
    @Override
    public void destroy() {
        index.clear();
        super.destroy();
    }
    
    public static volatile boolean disableCallback = false;
    public static volatile boolean disableAlgorithm = false;
    public static volatile boolean algChoice = true;
    
    public static long callbackTime = 0;
    public static long addManifoldTime = 0;
    public static long collideTime = 0;
    
    private static CollisionAlgorithm boxes = new GjkEpaCollisionAlgorithm();
    private static CollisionAlgorithm spheres = new SphereSphereCollisionAlgorithm();
    
    private class CollisionCallback implements IntersectionCallback<Entity> {
        private final CollisionBody bodyA;
        private final CollisionBody bodyB;
        
        public CollisionCallback(CollisionBody bodyA, CollisionBody bodyB) {
            this.bodyA = bodyA;
            this.bodyB = bodyB;
        }
        
        @Override
        @SuppressWarnings({ "rawtypes", "unchecked" })
        public void process(Entity a, AxisAlignedBox boundsA, Entity b, AxisAlignedBox boundsB) {
            // at this point we know the world bounds of a and b intersect, but
            // we need to test for collision against their actual shapes
            callbackTime -= System.nanoTime();
            a.get(bodyA);
            b.get(bodyB);
            
            if (!disableCallback) {
                CollisionAlgorithm algorithm;
                if (algChoice) {
                    algorithm = algorithms.getAlgorithm(bodyA.getShape().getClass(), 
                                                        bodyB.getShape().getClass());
                } else {
                    algorithm = boxes;
                }

                if (algorithm != null) {
                    // have a valid algorithm to test
                    collideTime -= System.nanoTime();
                    ClosestPair pair = null;
                    if (!disableAlgorithm) {
                        pair = algorithm.getClosestPair(bodyA.getShape(), bodyA.getTransform(), 
                                                 bodyB.getShape(), bodyB.getTransform());
                    }
                    collideTime += System.nanoTime();

                    if (pair != null && pair.isIntersecting()) {
                        addManifoldTime -= System.nanoTime();
//                        System.out.println("Contact (" + bodyA.getEntity().getId() + ", " + bodyB.getEntity().getId() + ") depth = " + Math.abs(pair.getDistance()));
                        notifyContact(bodyA, bodyB, pair);
                        addManifoldTime += System.nanoTime();
                    }
                }
            }
            callbackTime += System.nanoTime();
        }
    }
}
