package com.ferox.physics.dynamics.constraint;

import com.ferox.math.MutableVector4f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.math.Transform;
import com.ferox.math.Vector4f;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.algorithm.ClosestPair;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.Bag;

public class ContactManifold extends NormalizableConstraint {
    private static final int MANIFOLD_POINT_SIZE = 4;
    
    private final Collidable objA;
    private final Collidable objB;
    
    private final float contactBreakingThreshold; // min pair distance when a contact is removed from the manifold
    private final float contactProcessingThreshold; // min pair distance where a contact is ignored by constraint solver
    
    private final ManifoldPoint[] manifold;
    private int size;
    
    public ContactManifold(Collidable objA, Collidable objB, 
                           float processThreshold, float breakThreshold) {
        this.objA = objA;
        this.objB = objB;
        
        contactProcessingThreshold = processThreshold;
        contactBreakingThreshold = breakThreshold;
        
        manifold = new ManifoldPoint[MANIFOLD_POINT_SIZE];
        size = 0;
    }
    
    public void addContact(ClosestPair pair, boolean swap) {
        ManifoldPoint newPoint = new ManifoldPoint(objA.getWorldTransform(), objB.getWorldTransform(),
                                                   pair, swap);
        
        if (size == manifold.length) {
            // must remove a point
            
        }
        
        // add point to the manifold
        manifold[size++] = newPoint;
    }
    
    public int getManifoldSize() {
        return size;
    }
    
    public void update() {
        ReadOnlyMatrix4f ta = objA.getWorldTransform();
        ReadOnlyMatrix4f tb = objB.getWorldTransform();
        for (int i = 0; i < size; i++)
            manifold[i].update(ta, tb);
    }
    
    public Collidable getCollidableA() {
        return objA;
    }
    
    public Collidable getCollidableB() {
        return objB;
    }
    
    @Override
    public RigidBody getBodyA() {
        return (objA instanceof RigidBody ? (RigidBody) objA : null);
    }

    @Override
    public RigidBody getBodyB() {
        return (objB instanceof RigidBody ? (RigidBody) objB : null);
    }

    @Override
    public void normalize(Bag<LinearConstraint> constraints, LinearConstraintPool pool) {
        ManifoldPoint p;
        for (int i = 0; i < size; i++) {
            p = manifold[i];
            if (p.distance < contactProcessingThreshold) {
                // compute linear constraint for manifold point
            }
        }
    }
    
    private static class ManifoldPoint {
        final MutableVector4f localA;
        final MutableVector4f localB;
        
        final MutableVector4f worldA;
        final MutableVector4f worldB;
        
        final ReadOnlyVector3f worldNormalInA; // points from A to B
        
        float distance;
        int lifetime;
        
        public ManifoldPoint(ReadOnlyMatrix4f ta, ReadOnlyMatrix4f tb, ClosestPair pair, boolean swap) {
            ReadOnlyVector3f wa, wb, na;
            
            if (swap) {
                wa = pair.getClosestPointOnB();
                wb = pair.getClosestPointOnA();
                na = pair.getContactNormal().scale(-1f, null);
            } else {
                wa = pair.getClosestPointOnA();
                wb = pair.getClosestPointOnB();
                na = pair.getContactNormal();
            }
            
            worldA = new Vector4f(wa.getX(), wa.getY(), wa.getZ(), 1f);
            worldB = new Vector4f(wb.getX(), wb.getY(), wb.getZ(), 1f);
            worldNormalInA = na;
            
            localA = ta.inverse(tempt.get()).mul(worldA, null);
            localB = tb.inverse(tempt.get()).mul(worldB, null);
            distance = pair.getDistance();
            lifetime = 0;
        }
        
        void update(ReadOnlyMatrix4f ta, ReadOnlyMatrix4f tb) {
            ta.mul(localA, worldA);
            tb.mul(localB, worldB);
            
            distance = dot(worldB, worldNormalInA) - dot(worldA, worldNormalInA);
            lifetime++;
        }
        
        static float dot(ReadOnlyVector4f v1, ReadOnlyVector3f v2) {
            return v1.getX() * v2.getX() + v1.getY() * v2.getY() + v1.getZ() * v2.getZ();
        }
    }
    
    private static final ThreadLocal<Transform> tempt = new ThreadLocal<Transform>() {
        @Override
        protected Transform initialValue() { return new Transform(); };
    };
}
