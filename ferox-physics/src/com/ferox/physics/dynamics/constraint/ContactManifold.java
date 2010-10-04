package com.ferox.physics.dynamics.constraint;

import java.util.ArrayList;
import java.util.List;

import com.ferox.math.MutableVector4f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;
import com.ferox.physics.collision.Collidable;
import com.ferox.physics.collision.algorithm.ClosestPair;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.Bag;

public class ContactManifold extends NormalizableConstraint {
    private static final int MANIFOLD_POINT_SIZE = 4;
    private static final int RESTING_CONTACT_THRESHOLD = 2;
    private static final float ERP = .2f;
    
    private final Collidable objA;
    private final Collidable objB;
    
    private final float contactBreakingThreshold; // min pair distance when a contact is removed from the manifold
    private final float contactProcessingThreshold; // min pair distance where a contact is ignored by constraint solver
    
    private final List<ManifoldPoint> manifold;
    
    public ContactManifold(Collidable objA, Collidable objB, 
                           float processThreshold, float breakThreshold) {
        this.objA = objA;
        this.objB = objB;
        
        contactProcessingThreshold = processThreshold;
        contactBreakingThreshold = breakThreshold;
        
        manifold = new ArrayList<ContactManifold.ManifoldPoint>(MANIFOLD_POINT_SIZE);
    }
    
    public void addContact(ClosestPair pair, boolean swap) {
        ManifoldPoint newPoint = new ManifoldPoint(objA.getWorldTransform(), objB.getWorldTransform(),
                                                   pair, swap);
        
        if (manifold.size() == MANIFOLD_POINT_SIZE) {
            // must remove a point
            replaceWorstPoint(newPoint);
        } else {
            // just add point to the manifold
            manifold.add(newPoint);
        }
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
        ReadOnlyMatrix4f ta = objA.getWorldTransform();
        ReadOnlyMatrix4f tb = objB.getWorldTransform();
        
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
                if (projectedDifference.lengthSquared() > contactBreakingThreshold * contactBreakingThreshold)
                    manifold.remove(i);
            }
        }
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
    public void normalize(float dt, Bag<LinearConstraint> constraints, LinearConstraintPool pool) {
        ManifoldPoint p;
        LinearConstraint c;
        int ct = manifold.size();
        for (int i = 0; i < ct; i++) {
            p = manifold.get(i);
            if (p.distance <= contactProcessingThreshold) {
                // compute linear constraint for manifold point
                c = pool.get(getBodyA(), getBodyB());
                setupContactConstraint(p, c, dt);
                constraints.add(c);
            }
        }
    }
    
    private void setupContactConstraint(ManifoldPoint pt, LinearConstraint constraint, float dt) {
        // FIXME: must add restitution and friction coefficients in ManifoldPoints
        ReadOnlyMatrix4f ta = objA.getWorldTransform();
        ReadOnlyMatrix4f tb = objB.getWorldTransform();
        RigidBody rbA = getBodyA();
        RigidBody rbB = getBodyB();
        
        
        Vector3f relPosA = temp1.get();
        Vector3f relPosB = temp2.get();
        
        relPosA.set(pt.worldA.getX() - ta.get(0, 3),
                    pt.worldA.getY() - ta.get(1, 3),
                    pt.worldA.getZ() - ta.get(2, 3));
        relPosB.set(pt.worldB.getX() - tb.get(0, 3),
                    pt.worldB.getY() - tb.get(1, 3),
                    pt.worldB.getZ() - tb.get(2, 3));
        
        // FIXME: need more thread-locals or maybe not? ThreadLocal seems slower than new
        // compute torque axis
        constraint.setTorqueAxis(relPosA.cross(pt.worldNormalInB, null), relPosB.cross(pt.worldNormalInB, null).scale(-1f));
        
        // compute jacobian inverse for this constraint row
        float denomA = 0f;
        float denomB = 0f;
        Vector3f temp = new Vector3f();
        if (rbA != null) {
            rbA.getInertiaTensorInverse().mul(constraint.getTorqueAxisA(), temp).cross(relPosA);
            denomA = rbA.getInverseMass() + pt.worldNormalInB.dot(temp); 
        }
        if (rbB != null) {
            rbB.getInertiaTensorInverse().mul(constraint.getTorqueAxisB(), temp).cross(relPosB);
            denomB = rbB.getInverseMass() - pt.worldNormalInB.dot(temp);
        }
        constraint.setJacobianInverse(1f / (denomA + denomB));
        constraint.setConstraintAxis(pt.worldNormalInB);
        
//        System.out.println("Setup constraint");
//        System.out.println("A: " + rbA + " B: " + rbB);
//        ReadOnlyVector3f torqueAxisA = constraint.getTorqueAxisA();
//        ReadOnlyVector3f torqueAxisB = constraint.getTorqueAxisB();
//        System.out.printf("%.4f %.4f %.4f | %.4f %.4f %.4f\n", torqueAxisA.getX(), torqueAxisA.getY(), torqueAxisA.getZ(),
//                          torqueAxisB.getX(), torqueAxisB.getY(), torqueAxisB.getZ());
        
        Vector3f vel1 = temp; // reuse as much as possible
        Vector3f vel2 = new Vector3f();
        if (rbA != null)
            rbA.getAngularVelocity().cross(relPosA, vel1).add(rbA.getVelocity());
        else
            vel1.set(0f, 0f, 0f);
        if (rbB != null)
            rbB.getAngularVelocity().cross(relPosB, vel2).add(rbB.getVelocity());
        else
            vel2.set(0f, 0f, 0f);
        float relVelocity = pt.worldNormalInB.dot(vel1.sub(vel2)); // FIXME: vel2 - vel1 gets returned for friction constraint setup
        // FIXME: relVelocity, here does too (later rel_vel is not since it's redeclared)
        
        float restitution = 0f;
        /*if (pt.lifetime <= RESTING_CONTACT_THRESHOLD) {
            restitution = -relVelocity * pt.combinedRestitution;
            if (restitution < 0f)
                restitution = 0f;
        }*/
        
        float velDotN = 0f;
        if (rbA != null)
            velDotN += (constraint.getConstraintAxis().dot(rbA.getVelocity()) + constraint.getTorqueAxisA().dot(rbA.getAngularVelocity()));
        if (rbB != null)
            velDotN += (-constraint.getConstraintAxis().dot(rbB.getVelocity()) + constraint.getTorqueAxisB().dot(rbB.getAngularVelocity()));
        
        float positionalError = -pt.distance * ERP / dt;
        float velocityError = restitution - velDotN;
        
        float penetrationImpulse = positionalError * constraint.getJacobianInverse();
        float velocityImpulse = velocityError * constraint.getJacobianInverse();
        constraint.setSolutionParameters(penetrationImpulse + velocityImpulse, 0f);
        constraint.setLimits(0f, Float.MAX_VALUE);
        
//        System.out.printf("constraint: %.4f %.4f %.4f %.4f\n", velDotN, positionalError, velocityError, constraint.getRightHandSide());
    }
    
    private static class ManifoldPoint {
        // FIXME: what about friction directions? etc.?
        final MutableVector4f localA;
        final MutableVector4f localB;
        
        final MutableVector4f worldA;
        final MutableVector4f worldB;
        
        final ReadOnlyVector3f worldNormalInB; // points from A to B
        
        float distance;
        int lifetime;
        
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
            distance = pair.getDistance();
            lifetime = 0;
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
    
    private static final ThreadLocal<Vector3f> temp1 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); };
    };
    private static final ThreadLocal<Vector3f> temp2 = new ThreadLocal<Vector3f>() {
        @Override
        protected Vector3f initialValue() { return new Vector3f(); };
    };
    private static final ThreadLocal<Transform> tempt = new ThreadLocal<Transform>() {
        @Override
        protected Transform initialValue() { return new Transform(); };
    };
}
