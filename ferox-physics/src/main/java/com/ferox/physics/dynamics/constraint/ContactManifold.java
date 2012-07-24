package com.ferox.physics.dynamics.constraint;

import java.util.ArrayList;
import java.util.List;

import com.ferox.math.MutableVector3f;
import com.ferox.math.MutableVector4f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.math.AffineTransform;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.math.bounds.Plane;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.physics.dynamics.constraint.LinearConstraint.ImpulseListener;
import com.ferox.physics.dynamics.constraint.LinearConstraintAccumulator.ConstraintLevel;

public class ContactManifold extends NormalizableConstraint {
    private static final int MANIFOLD_POINT_SIZE = 4;
    private static final int RESTING_CONTACT_THRESHOLD = 2;
    private static final float ERP = .2f;
    
    private final CollisionBody objA;
    private final CollisionBody objB;
    
    private final float contactBreakingThreshold; // min pair distance when a contact is removed from the manifold
    private final float contactProcessingThreshold; // min pair distance where a contact is ignored by constraint solver
    
    private final List<ManifoldPoint> manifold;
    
    private final float combinedRestitution;
    private final float combinedFriction;
    
    public ContactManifold(CollisionBody objA, CollisionBody objB, 
                           float processThreshold, float breakThreshold) {
        this.objA = objA;
        this.objB = objB;
        
        contactProcessingThreshold = processThreshold;
        contactBreakingThreshold = breakThreshold;
        
        combinedRestitution = objA.getRestitution() * objB.getRestitution();
        combinedFriction = Math.min(10f, Math.max(objA.getFriction() * objB.getFriction(), -10f));
        
        manifold = new ArrayList<ContactManifold.ManifoldPoint>(MANIFOLD_POINT_SIZE);
    }
    
    public void addContact(ClosestPair pair, boolean swap) {
        ManifoldPoint newPoint = new ManifoldPoint(objA.getTransform(), objB.getTransform(),
                                                   pair, swap);
        
        int replaceIndex = findNearPoint(newPoint);
        if (replaceIndex >= 0) {
            ManifoldPoint oldPoint = manifold.set(replaceIndex, newPoint);
            newPoint.appliedImpulse = oldPoint.appliedImpulse;
            newPoint.appliedFrictionImpulse = oldPoint.appliedFrictionImpulse;
        } else if (manifold.size() == MANIFOLD_POINT_SIZE) {
            // must remove a point
            replaceWorstPoint(newPoint);
        } else {
            // just add point to the manifold
            manifold.add(newPoint);
        }
    }
    
    private int findNearPoint(ManifoldPoint newPoint) {
        float shortestDist = contactBreakingThreshold * contactBreakingThreshold;
        int nearestPoint = -1;
        Vector4f diffA = new Vector4f();
        ManifoldPoint mp;
        for (int i = manifold.size() - 1; i >= 0; i--) {
            mp = manifold.get(i);
            mp.localA.sub(newPoint.localA, diffA);
            float distToOld = diffA.lengthSquared();
            
            if (distToOld < shortestDist) {
                shortestDist = distToOld;
                nearestPoint = i;
            }
        }
        
        return nearestPoint;
    }
    
    private void replaceWorstPoint(ManifoldPoint pt) {
        // this should only be called when the manifold is full, so we have at least 1
        int deepestIndex = 0;
        float depth = manifold.get(0).distance;
        for (int i = 1; i < manifold.size(); i++) {
            if (manifold.get(i).distance < depth) {
                deepestIndex = i;
                depth = manifold.get(i).distance;
            }
        }
        
        // check for possible manifold areas (keeping pt and the deepest index)
        float res0, res1, res2, res3;
        if (deepestIndex != 0) {
            // try removing first point
            res0 = area(pt.localB, manifold.get(1).localB, manifold.get(3).localB, manifold.get(2).localB);
        } else
            res0 = 0f;
        if (deepestIndex != 1) {
            // try removing second point
            res1 = area(pt.localB, manifold.get(0).localB, manifold.get(3).localB, manifold.get(2).localB);
        } else
            res1 = 0f;
        if (deepestIndex != 2) {
            // try removing third point
            res2 = area(pt.localB, manifold.get(0).localB, manifold.get(3).localB, manifold.get(1).localB);
        } else
            res2 = 0f;
        if (deepestIndex != 3) {
            // try removing fourth point
            res3 = area(pt.localB, manifold.get(0).localB, manifold.get(2).localB, manifold.get(1).localB);
        } else
            res3 = 0f;
        
        // get index that can be removed to insert pt
        // with the largest area
        int maximizingIndex = 0;
        float maxArea = res0;
        if (res1 > maxArea) {
            maximizingIndex = 1;
            maxArea = res1;
        }
        if (res2 > maxArea) {
            maximizingIndex = 2;
            maxArea = res2;
        }
        if (res3 > maxArea) {
            maximizingIndex = 3;
            maxArea = res3;
        }
        
        manifold.set(maximizingIndex, pt);
    }
    
    private float area(ReadOnlyVector4f p1, ReadOnlyVector4f p2, ReadOnlyVector4f p3, ReadOnlyVector4f p4) {
        Vector3f a = temp1.get();
        Vector3f b = temp2.get();
        
        a.set(p1.getX() - p2.getX(), p1.getY() - p2.getY(), p1.getZ() - p2.getZ());
        b.set(p3.getX() - p4.getX(), p3.getY() - p4.getY(), p3.getZ() - p4.getZ());
        return a.cross(b).lengthSquared();
    }
    
    public int getManifoldSize() {
        return manifold.size();
    }
    
    public void update() {
        ReadOnlyMatrix4f ta = objA.getTransform();
        ReadOnlyMatrix4f tb = objB.getTransform();
        
        Vector3f projectedPoint = temp1.get();
        Vector3f projectedDifference = temp2.get();
        
        ManifoldPoint mp;
        for (int i = manifold.size() - 1; i >= 0; i--) {
            mp = manifold.get(i);
            mp.update(ta, tb);
            if (mp.distance > contactBreakingThreshold) {
                // separated too far along normal
                manifold.remove(i);
            } else {
                // check for orthogonal motion drift
                projectedPoint.set(mp.worldA.getX() - mp.worldNormalInB.getX() * mp.distance,
                                   mp.worldA.getY() - mp.worldNormalInB.getY() * mp.distance,
                                   mp.worldA.getZ() - mp.worldNormalInB.getZ() * mp.distance);
                projectedDifference.set(mp.worldB.getX() - projectedPoint.getX(),
                                        mp.worldB.getY() - projectedPoint.getY(),
                                        mp.worldB.getZ() - projectedPoint.getZ());
                if (projectedDifference.lengthSquared() > contactBreakingThreshold * contactBreakingThreshold) {
                    manifold.remove(i);
                }
            }
        }
    }
    
    public CollisionBody getCollidableA() {
        return objA;
    }
    
    public CollisionBody getCollidableB() {
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
    public void normalize(float dt, LinearConstraintAccumulator accum, LinearConstraintPool pool) {
        ManifoldPoint p;
        LinearConstraint contact;
        LinearConstraint fric1;
        
        RigidBody rbA = getBodyA();
        RigidBody rbB = getBodyB();
        
        ReadOnlyMatrix4f ta = objA.getTransform();
        ReadOnlyMatrix4f tb = objB.getTransform();
        
        int ct = manifold.size();
        for (int i = 0; i < ct; i++) {
            p = manifold.get(i);
            if (p.distance <= contactProcessingThreshold) {
                Vector3f relPosA = temp1.get();
                Vector3f relPosB = temp2.get();
                
                relPosA.set(p.worldA.getX() - ta.get(0, 3),
                            p.worldA.getY() - ta.get(1, 3),
                            p.worldA.getZ() - ta.get(2, 3));
                relPosB.set(p.worldB.getX() - tb.get(0, 3),
                            p.worldB.getY() - tb.get(1, 3),
                            p.worldB.getZ() - tb.get(2, 3));
                
                // compute contact constraint for manifold point
                contact = pool.get(rbA, rbB);
                setupConstraint(rbA, rbB, p.worldNormalInB, relPosA, relPosB, -p.distance * ERP / dt, p.appliedImpulse, p.lifetime, contact);
                contact.setImpulseListener(p.nImpulse);
                accum.add(contact, ConstraintLevel.CONTACT);
                
                if (!p.frictionDirInitialized) {
                    // compute lateral friction directions
                    MutableVector3f velA = (rbA == null ? new Vector3f() : rbA.getAngularVelocity().cross(relPosA, null).add(rbA.getVelocity()));
                    MutableVector3f velB = (rbB == null ? new Vector3f() : rbB.getAngularVelocity().cross(relPosB, null).add(rbB.getVelocity()));
                    MutableVector3f vel = velA.sub(velB);
                    float relVelocity = p.worldNormalInB.dot(vel);
                    
                    p.worldNormalInB.scale(-relVelocity, p.frictionDir).add(vel);
                    float lateralRelVelocity = p.frictionDir.length();
                    if (lateralRelVelocity > .0001f) {
                        p.frictionDir.scale(1f / lateralRelVelocity);
                    } else {
                        Plane.getTangentSpace(p.worldNormalInB, p.frictionDir, temp1.get());
                        p.frictionDir.normalize();
                    }
                    
                    p.frictionDirInitialized = true;
                }
                
                // setup both friction direction constraints
                fric1 = pool.get(rbA, rbB);
                setupConstraint(rbA, rbB, p.frictionDir, relPosA, relPosB, 0f, p.appliedFrictionImpulse, -1, fric1);
                fric1.setImpulseListener(p.fImpulse);
                fric1.setDynamicLimits(contact, combinedFriction);
                accum.add(fric1, ConstraintLevel.FRICTION);
            }
        }
    }
    
    private void setupConstraint(RigidBody rbA, RigidBody rbB, ReadOnlyVector3f constraintAxis, 
                                 ReadOnlyVector3f relPosA, ReadOnlyVector3f relPosB, 
                                 float positionalError, float appliedImpulse, int lifetime, LinearConstraint constraint) {
        Vector3f t1 = temp3.get();
        Vector3f t2 = temp4.get();
        
        // compute constraint basis
        constraint.setConstraintAxis(constraintAxis, relPosA.cross(constraintAxis, t1), 
                                     relPosB.cross(constraintAxis, t2));
        
        // compute jacobian inverse for this constraint row
        float denomA = 0f;
        float denomB = 0f;
        if (rbA != null)
            denomA = rbA.getInverseMass() + rbA.getInertiaTensorInverse().mul(constraint.getTorqueAxisA(t1)).cross(relPosA).dot(constraintAxis); 
        if (rbB != null)
            denomB = rbB.getInverseMass() + rbB.getInertiaTensorInverse().mul(constraint.getTorqueAxisB(t2)).cross(relPosB).dot(constraintAxis);
        constraint.setJacobianInverse(1f / (denomA + denomB));
        float relativeVelocity = 0f;
        if (rbA != null) 
            relativeVelocity += (constraint.getConstraintAxis(t1).dot(rbA.getVelocity()) + constraint.getTorqueAxisA(t2).dot(rbA.getAngularVelocity()));
        if (rbB != null)
            relativeVelocity -= (constraint.getConstraintAxis(t1).dot(rbB.getVelocity()) + constraint.getTorqueAxisB(t2).dot(rbB.getAngularVelocity()));
        
        float restitution = 0f;
        if (lifetime <= RESTING_CONTACT_THRESHOLD && lifetime >= 0) {
            restitution = -relativeVelocity * combinedRestitution;
            if (restitution < 0f)
                restitution = 0f;
        }
        
        { // warmstarting, add switch? FIXME
            constraint.addDeltaImpulse(appliedImpulse * .85f);
        }
        
        float velocityError = restitution - relativeVelocity;

        float penetrationImpulse = positionalError * constraint.getJacobianInverse();
        float velocityImpulse = velocityError * constraint.getJacobianInverse();
        
        constraint.setSolutionParameters(penetrationImpulse + velocityImpulse, 0f);
        constraint.setLimits(0f, Float.MAX_VALUE);
    }
    
    private static class ManifoldPoint {
        final MutableVector4f localA;
        final MutableVector4f localB;
        
        final MutableVector4f worldA;
        final MutableVector4f worldB;
        
        final ReadOnlyVector3f worldNormalInB;
        
        float distance;
        int lifetime;
        
        // constraint parameters
        float appliedImpulse;
        float appliedFrictionImpulse;
        
        final Vector3f frictionDir;
        boolean frictionDirInitialized;
        
        final FrictionImpulseListener fImpulse;
        final NormalImpulseListener nImpulse;
        
        public ManifoldPoint(ReadOnlyMatrix4f ta, ReadOnlyMatrix4f tb, ClosestPair pair, boolean swap) {
            ReadOnlyVector3f wa, wb, na;
            
            if (swap) {
                wa = pair.getClosestPointOnB();
                wb = pair.getClosestPointOnA();
                na = pair.getContactNormal();
            } else {
                wa = pair.getClosestPointOnA();
                wb = pair.getClosestPointOnB();
                na = pair.getContactNormal().scale(-1f, null);
            }
            
            worldA = new Vector4f(wa.getX(), wa.getY(), wa.getZ(), 1f);
            worldB = new Vector4f(wb.getX(), wb.getY(), wb.getZ(), 1f);
            worldNormalInB = na;
            
            localA = ta.inverse(tempt.get()).mul(worldA, null);
            localB = tb.inverse(tempt.get()).mul(worldB, null);
            
            frictionDir = new Vector3f();
            
            distance = pair.getDistance();
            lifetime = 0;
            appliedImpulse = 0f;
            appliedFrictionImpulse = 0f;
            
            fImpulse = new FrictionImpulseListener(this);
            nImpulse = new NormalImpulseListener(this);
        }
        
        void update(ReadOnlyMatrix4f ta, ReadOnlyMatrix4f tb) {
            ta.mul(localA, worldA);
            tb.mul(localB, worldB);
            
            distance = dot(worldA, worldNormalInB) - dot(worldB, worldNormalInB);
            lifetime++;
        }
        
        static float dot(ReadOnlyVector4f v1, ReadOnlyVector3f v2) {
            return v1.getX() * v2.getX() + v1.getY() * v2.getY() + v1.getZ() * v2.getZ();
        }
    }
    
    private static class NormalImpulseListener implements ImpulseListener {
        final ManifoldPoint p;
        
        public NormalImpulseListener(ManifoldPoint p) { this.p = p; }
        
        @Override
        public void onApplyImpulse(LinearConstraint lc) {
            p.appliedImpulse = lc.getAppliedImpulse();
        }
    }
    
    private static class FrictionImpulseListener implements ImpulseListener {
        final ManifoldPoint p;
        
        public FrictionImpulseListener(ManifoldPoint p) { this.p = p; }
        
        @Override
        public void onApplyImpulse(LinearConstraint lc) {
            p.appliedFrictionImpulse = lc.getAppliedImpulse();
        }
    }
    
    private static final ThreadLocal<Vector3f> temp1 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); };
    };
    private static final ThreadLocal<Vector3f> temp2 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); };
    };
    private static final ThreadLocal<Vector3f> temp3 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); };
    };
    private static final ThreadLocal<Vector3f> temp4 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); };
    };
    private static final ThreadLocal<AffineTransform> tempt = new ThreadLocal<AffineTransform>() {
        @Override
        protected AffineTransform initialValue() { return new AffineTransform(); };
    };
}
