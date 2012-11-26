/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.physics.dynamics;

import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Queue;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;
import com.ferox.math.bounds.Plane;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionBody;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;

public class ContactManifoldPool {
    private static final int MANIFOLD_POINT_SIZE = 4;
    private static final int RESTING_CONTACT_THRESHOLD = 2;
    private static final double ERP = .2;

    private final Map<CollisionPair, Integer> manifolds;
    private final CollisionPair query; // mutable, do not put in manifolds

    private double contactBreakingThreshold;
    private double contactProcessingThreshold;
    private EntitySystem entitySystem;

    /*
     * Packed data storing contact manifolds and manifold points
     */

    // we store the actual entity because this requires both RigidBodies and CollisionBodies
    private Entity[] objAs = new Entity[0]; // first entity in the pair
    private Entity[] objBs = new Entity[0]; // second entity in the pair

    private double[] combinedRestitutions = new double[0]; // total restitution between a pair of bodies
    private double[] combinedFrictions = new double[0]; // total friction between a pair of bodies

    private boolean[] alive = new boolean[0]; // true if this manifold contains valid data

    // there are MANIFOLD_POINT_SIZE entries per manifold
    private double[] mfLocalAs = new double[0]; // (Vector4) contact in A space
    private double[] mfLocalBs = new double[0]; // (Vector4) contact in B space
    private double[] mfWorldAs = new double[0]; // (Vector4) localA transformed to world space
    private double[] mfWorldBs = new double[0]; // (Vector4) localB transformed to world space

    private double[] mfWorldNormalInBs = new double[0]; // (Vector3) contact normal as seen by B, in world space
    private double[] mfFrictionDirs = new double[0]; // (Vector3) friction constraint direction
    private boolean[] mfFrictionComputed = new boolean[0]; // true if mfFrictionDirs has valid data

    private double[] mfDistances = new double[0]; // contact distance
    private int[] mfLifetimes = new int[0]; // number of frames this point has been around, negative implies invalid data

    private double[] mfAppliedContactImpulses = new double[0]; // last frame's impulse from contact constraints
    private double[] mfAppliedFrictionImpulses = new double[0]; // last frame's impulse from friction constraints
    private int[] mfContactConstraints = new int[0]; // index of linear constraint for contact constraint
    private int[] mfFrictionConstraints = new int[0]; // index of linear constraint for friction constraint

    // maximum index (+1) of an alive contact
    private int maxAliveContact;
    private final Queue<Integer> reuseQueue;

    // vectors for lookup/manipulation
    private final Vector4 worldA = new Vector4();
    private final Vector4 worldB = new Vector4();
    private final Vector4 localA = new Vector4();
    private final Vector4 localB = new Vector4();
    private final Vector3 normalInB = new Vector3();

    private final Matrix4 transform = new Matrix4();

    public ContactManifoldPool() {
        // increasing breaking threshold seems to create more stable box drops,
        // so i should investigate why .0343 was the original default
        this(.1, .0343);
    }

    public ContactManifoldPool(double contactProcessingThreshold,
                               double contactBreakingThreshold) {
        manifolds = new HashMap<CollisionPair, Integer>();
        query = new CollisionPair();
        maxAliveContact = 0;
        reuseQueue = new ArrayDeque<Integer>();

        setCapacity(10);
        setContactProcessingThreshold(contactProcessingThreshold);
        setContactBreakingThreshold(contactBreakingThreshold);
    }

    public void setEntitySystem(EntitySystem system) {
        entitySystem = system;
    }

    public EntitySystem getEntitySystem() {
        return entitySystem;
    }

    public void setContactProcessingThreshold(double threshold) {
        if (threshold <= 0.0) {
            throw new IllegalArgumentException("Threshold must be positive, not " + threshold);
        }
        contactProcessingThreshold = threshold;
    }

    public double getContactProcessingThreshold() {
        return contactProcessingThreshold;
    }

    public void setContactBreakingThreshold(double threshold) {
        if (threshold <= 0.0) {
            throw new IllegalArgumentException("Threshold must be positive, not " + threshold);
        }
        contactBreakingThreshold = threshold;
    }

    public double getContactBreakingThreshold() {
        return contactBreakingThreshold;
    }

    public void computeWarmstartImpulses(LinearConstraintPool contactPool,
                                         LinearConstraintPool frictionPool) {
        for (int manifold = 0; manifold < maxAliveContact; manifold++) {
            if (alive[manifold]) {
                for (int point = 0; point < MANIFOLD_POINT_SIZE; point++) {
                    int index = toIndex(manifold, point);

                    int contact = mfContactConstraints[index];
                    if (contact >= 0) {
                        mfAppliedContactImpulses[index] = contactPool.getAppliedImpulse(contact);
                    }

                    int friction = mfFrictionConstraints[index];
                    if (friction >= 0) {
                        mfAppliedFrictionImpulses[index] = frictionPool.getAppliedImpulse(friction);
                    }
                }
            }
        }
    }

    public void generateConstraints(double dt, LinearConstraintPool contactPool,
                                    LinearConstraintPool frictionPool) {
        CollisionBody bodyA = entitySystem.createDataInstance(CollisionBody.class);
        CollisionBody bodyB = entitySystem.createDataInstance(CollisionBody.class);
        RigidBody rbA = entitySystem.createDataInstance(RigidBody.class);
        RigidBody rbB = entitySystem.createDataInstance(RigidBody.class);

        // we need 4 temporary vectors to compute constraint info
        Vector3 relPosA = new Vector3();
        Vector3 relPosB = new Vector3();
        Vector3 t1 = new Vector3();
        Vector3 t2 = new Vector3();

        for (int manifold = 0; manifold < maxAliveContact; manifold++) {
            if (alive[manifold]) {
                // load in component data
                boolean valid = objAs[manifold].get(bodyA) && objBs[manifold].get(bodyB);
                objAs[manifold].get(rbA);
                objBs[manifold].get(rbB);

                if (!valid) {
                    // component was removed and entity should no longer be in the manifold pool
                    removeManifold(manifold);
                    continue;
                }

                Matrix4 ta = bodyA.getTransform();
                Matrix4 tb = bodyB.getTransform();

                // update all manifold points that are alive, and generate
                // constraints for those that are within range
                int updatedCount = 0;
                for (int point = 0; point < MANIFOLD_POINT_SIZE; point++) {
                    if (updateManifoldPoint(manifold, point, bodyA, bodyB)) {
                        updatedCount++;

                        int index = toIndex(manifold, point);
                        int vec4Index = toVector4Index(manifold, point);
                        int vec3Index = toVector3Index(manifold, point);

                        if (mfDistances[index] <= contactProcessingThreshold) {
                            worldA.set(mfWorldAs, vec4Index);
                            worldB.set(mfWorldBs, vec4Index);
                            normalInB.set(mfWorldNormalInBs, vec3Index);

                            relPosA.set(worldA.x - ta.m03, worldA.y - ta.m13,
                                        worldA.z - ta.m23);
                            relPosB.set(worldB.x - tb.m03, worldB.y - tb.m13,
                                        worldB.z - tb.m23);

                            // generate contact constraint
                            int contact = setupConstraint(rbA,
                                                          rbB,
                                                          normalInB,
                                                          relPosA,
                                                          relPosB,
                                                          t1,
                                                          t2,
                                                          -mfDistances[index] * ERP / dt,
                                                          mfAppliedContactImpulses[index],
                                                          mfLifetimes[index],
                                                          combinedRestitutions[manifold],
                                                          contactPool);
                            mfContactConstraints[index] = contact;

                            if (!mfFrictionComputed[index]) {
                                // compute lateral friction direction
                                Vector3 velA, velB;
                                if (rbA.isEnabled()) {
                                    velA = t1.cross(rbA.getAngularVelocity(), relPosA)
                                             .add(rbA.getVelocity());
                                } else {
                                    velA = t1.set(0, 0, 0);
                                }

                                if (rbB.isEnabled()) {
                                    velB = t2.cross(rbB.getAngularVelocity(), relPosB)
                                             .add(rbB.getVelocity());
                                } else {
                                    velB = t2.set(0, 0, 0);
                                }

                                Vector3 velocity = velA.sub(velB); // == t1 (t2 can be reused now)
                                double relVelocity = normalInB.dot(velocity);

                                Vector3 fricDir = t2.addScaled(velocity, -relVelocity,
                                                               normalInB); // == t2 (t1 can be reused now)
                                double lateralRelVelocity = fricDir.length();
                                if (lateralRelVelocity > 0.0001) {
                                    fricDir.scale(1.0 / lateralRelVelocity);
                                } else {
                                    Plane.getTangentSpace(normalInB, fricDir, t1);
                                    fricDir.normalize();
                                }

                                fricDir.get(mfFrictionDirs, vec3Index);
                                mfFrictionComputed[index] = true;
                            }

                            // generate friction constraint
                            normalInB.set(mfFrictionDirs, vec3Index);
                            int friction = setupConstraint(rbA,
                                                           rbB,
                                                           normalInB,
                                                           relPosA,
                                                           relPosB,
                                                           t1,
                                                           t2,
                                                           0.0,
                                                           mfAppliedFrictionImpulses[index],
                                                           -1,
                                                           combinedRestitutions[manifold],
                                                           frictionPool);
                            mfFrictionConstraints[index] = friction;
                            frictionPool.setDynamicLimits(friction, contact,
                                                          combinedFrictions[manifold]);
                        }
                    }
                }

                if (updatedCount == 0) {
                    // all manifold points are dead after the update, so we need
                    // to remove this manifold
                    removeManifold(manifold);
                }
            }
        }
    }

    public void addContact(CollisionBody objA, CollisionBody objB, ClosestPair pair) {
        int manifold = getManifold(objA, objB);
        addManifoldPoint(manifold, objA, objB, pair);
    }

    public void removeAllContacts(Component<CollisionBody> c) {
        Entity e = c.getEntity();
        for (int i = 0; i < maxAliveContact; i++) {
            if (objAs[i] == e || objBs[i] == e) {
                // collision pair needs to be removed
                removeManifold(i);
            }
        }
    }

    private void removeManifold(int manifold) {
        // remove map entry
        query.set(objAs[manifold].get(CollisionBody.class),
                  objBs[manifold].get(CollisionBody.class));
        manifolds.remove(query);

        // clear packed data
        objAs[manifold] = null;
        objBs[manifold] = null;
        alive[manifold] = false;

        // update counts
        if (manifold == maxAliveContact - 1) {
            // last manifold
            maxAliveContact--;
        } else {
            // internal manifold
            reuseQueue.add(manifold);
        }
    }

    private int setupConstraint(RigidBody rbA, RigidBody rbB,
                                @Const Vector3 constraintAxis, @Const Vector3 relPosA,
                                @Const Vector3 relPosB,
                                Vector3 torqueA,
                                Vector3 torqueB, // torqueA,B are computed in here
                                double positionalError, double appliedImpulse,
                                int lifetime, double combinedRestitution,
                                LinearConstraintPool pool) {
        torqueA.cross(relPosA, constraintAxis);
        torqueB.cross(relPosB, constraintAxis);

        int constraint = pool.addConstraint((rbA.isEnabled() ? rbA : null),
                                            (rbB.isEnabled() ? rbB : null),
                                            constraintAxis, torqueA, torqueB);

        double relativeVelocity = 0;
        double denom = 0.0;

        if (rbA.isEnabled()) {
            relativeVelocity += (constraintAxis.dot(rbA.getVelocity()) + torqueA.dot(rbA.getAngularVelocity()));
            // we don't need torqueA anymore, so the multiply and cross can be in-place
            denom += (rbA.getInverseMass() + torqueA.mul(rbA.getInertiaTensorInverse(),
                                                         torqueA).cross(relPosA)
                                                    .dot(constraintAxis));
        }
        if (rbB.isEnabled()) {
            relativeVelocity -= (constraintAxis.dot(rbB.getVelocity()) + torqueB.dot(rbB.getAngularVelocity()));
            // we don't need torqueB anymore, so the multiply and cross can be in-place
            denom += (rbB.getInverseMass() + torqueB.mul(rbB.getInertiaTensorInverse(),
                                                         torqueB).cross(relPosB)
                                                    .dot(constraintAxis));
        }

        double jacobian = 1.0 / denom;

        double restitution = 0.0;
        if (lifetime <= RESTING_CONTACT_THRESHOLD && lifetime >= 0) {
            restitution = -relativeVelocity * combinedRestitution;
            if (restitution < 0.0) {
                restitution = 0.0;
            }
        }

        // warmstart constraint solution
        pool.setWarmstartImpulse(constraint, .85 * appliedImpulse);

        double velocityError = restitution - relativeVelocity;
        double penetrationImpulse = positionalError * jacobian;
        double velocityImpulse = velocityError * jacobian;

        pool.setSolution(constraint, penetrationImpulse + velocityImpulse, 0.0, jacobian);
        pool.setStaticLimits(constraint, 0.0, Double.MAX_VALUE);

        return constraint;
    }

    private boolean updateManifoldPoint(int manifold, int point, CollisionBody bodyA,
                                        CollisionBody bodyB) {
        int index = toIndex(manifold, point);

        if (mfLifetimes[index] < 0) {
            // not alive, so escape early
            return false;
        }

        // compute new world positions
        int vec4Offset = toVector4Index(manifold, point);

        localA.set(mfLocalAs, vec4Offset);
        localB.set(mfLocalBs, vec4Offset);

        worldA.mul(bodyA.getTransform(), localA);
        worldB.mul(bodyB.getTransform(), localB);

        worldA.get(mfWorldAs, vec4Offset);
        worldB.get(mfWorldBs, vec4Offset);

        // compute new distance
        normalInB.set(mfWorldNormalInBs, toVector3Index(manifold, point));
        double dist = (worldA.x * normalInB.x + worldA.y * normalInB.y + worldA.z * normalInB.z) - (worldB.x * normalInB.x + worldB.y * normalInB.y + worldB.z * normalInB.z);
        mfDistances[index] = dist;

        // increase lifetime
        mfLifetimes[index]++;

        // evaluate distance and drift to see if the contact has broken
        if (dist > contactBreakingThreshold) {
            // separated too far along contact normal
            mfLifetimes[index] = -1;
            return false;
        } else {
            // check for orthogonal motion drift
            // - inline vector operations to prevent allocation
            double x = worldB.x - worldA.x + normalInB.x * dist;
            double y = worldB.y - worldA.y + normalInB.y * dist;
            double z = worldB.z - worldA.z + normalInB.z * dist;

            if ((x * x + y * y + z * z) > contactBreakingThreshold * contactBreakingThreshold) {
                // too much drift
                mfLifetimes[index] = -1;
                return false;
            }
        }

        // manifold point is still valid
        return true;
    }

    public void setCapacity(int newCount) {
        int newManifoldCount = newCount * MANIFOLD_POINT_SIZE;

        // per manifold values
        alive = Arrays.copyOf(alive, newCount);
        combinedFrictions = Arrays.copyOf(combinedFrictions, newCount);
        combinedRestitutions = Arrays.copyOf(combinedRestitutions, newCount);
        objAs = Arrays.copyOf(objAs, newCount);
        objBs = Arrays.copyOf(objBs, newCount);

        // per manifold point simple values
        mfAppliedContactImpulses = Arrays.copyOf(mfAppliedContactImpulses,
                                                 newManifoldCount);
        mfAppliedFrictionImpulses = Arrays.copyOf(mfAppliedFrictionImpulses,
                                                  newManifoldCount);
        mfContactConstraints = Arrays.copyOf(mfContactConstraints, newManifoldCount);
        mfFrictionConstraints = Arrays.copyOf(mfFrictionConstraints, newManifoldCount);
        mfDistances = Arrays.copyOf(mfDistances, newManifoldCount);
        mfLifetimes = Arrays.copyOf(mfLifetimes, newManifoldCount);
        mfFrictionComputed = Arrays.copyOf(mfFrictionComputed, newManifoldCount);

        // per manifold point vector values
        mfLocalAs = Arrays.copyOf(mfLocalAs, newManifoldCount * 4);
        mfLocalBs = Arrays.copyOf(mfLocalBs, newManifoldCount * 4);
        mfWorldAs = Arrays.copyOf(mfWorldAs, newManifoldCount * 4);
        mfWorldBs = Arrays.copyOf(mfWorldBs, newManifoldCount * 4);
        mfWorldNormalInBs = Arrays.copyOf(mfWorldNormalInBs, newManifoldCount * 3);
        mfFrictionDirs = Arrays.copyOf(mfFrictionDirs, newManifoldCount * 3);

        maxAliveContact = Math.min(maxAliveContact, newCount);
        // remove all indices that are outside the valid range
        Iterator<Integer> it = reuseQueue.iterator();
        while (it.hasNext()) {
            if (it.next() >= newCount) {
                it.remove();
            }
        }
    }

    private int getManifold(CollisionBody a, CollisionBody b) {
        query.set(a.getComponent(), b.getComponent());
        Integer existingManifold = manifolds.get(query);
        if (existingManifold != null) {
            // reuse this one
            return existingManifold.intValue();
        }

        // otherwise we need a new index
        int newIndex = (reuseQueue.isEmpty() ? maxAliveContact++ : reuseQueue.poll()
                                                                             .intValue());
        if (newIndex >= objAs.length) {
            // increase size
            setCapacity((newIndex + 1) * 2);
        }

        // initialize certain data structures
        alive[newIndex] = true;
        objAs[newIndex] = a.getEntity();
        objBs[newIndex] = b.getEntity();
        combinedRestitutions[newIndex] = a.getRestitution() * b.getRestitution();
        combinedFrictions[newIndex] = Math.min(10.0,
                                               Math.max(a.getFriction() * b.getFriction(),
                                                        -10.0));

        // invalidate all manifold points
        for (int i = 0; i < MANIFOLD_POINT_SIZE; i++) {
            mfLifetimes[toIndex(newIndex, i)] = -1;
        }

        // store index into map for fast lookup later
        manifolds.put(new CollisionPair(a.getComponent(), b.getComponent()), newIndex);

        return newIndex;
    }

    private void addManifoldPoint(int manifold, CollisionBody bodyA, CollisionBody bodyB,
                                  ClosestPair pair) {
        boolean swap = objAs[manifold] != bodyA.getEntity();

        Vector3 pa = pair.getClosestPointOnA();
        Vector3 pb = pair.getClosestPointOnB();

        if (swap) {
            worldA.set(pb.x, pb.y, pb.z, 1.0);
            worldB.set(pa.x, pa.y, pa.z, 1.0);
            normalInB.set(pair.getContactNormal());

            localA.mul(transform.inverse(bodyB.getTransform()), worldA);
            localB.mul(transform.inverse(bodyA.getTransform()), worldB);
        } else {
            worldA.set(pa.x, pa.y, pa.z, 1.0);
            worldB.set(pb.x, pb.y, pb.z, 1.0);
            normalInB.scale(pair.getContactNormal(), -1.0);

            localA.mul(transform.inverse(bodyA.getTransform()), worldA);
            localB.mul(transform.inverse(bodyB.getTransform()), worldB);
        }

        // find an index to store the point in
        boolean resetImpulse = false;
        int point = findNearestPoint(manifold, localA);
        if (point < 0) {
            resetImpulse = true;
            point = findOpenPoint(manifold);
            if (point < 0) {
                point = findWorstPoint(manifold, localB);
            }
        }

        int index = toIndex(manifold, point);
        int vec4Index = toVector4Index(manifold, point);

        // initialize data from the vectors we just computed
        worldA.get(mfWorldAs, vec4Index);
        worldB.get(mfWorldBs, vec4Index);
        localA.get(mfLocalAs, vec4Index);
        localB.get(mfLocalBs, vec4Index);
        normalInB.get(mfWorldNormalInBs, toVector3Index(manifold, point));

        // initialize the rest of the manifold point
        mfDistances[index] = pair.getDistance();
        mfLifetimes[index] = 0; // now flagged as valid
        mfFrictionComputed[index] = false;

        if (resetImpulse) {
            mfAppliedContactImpulses[index] = 0.0;
            mfAppliedFrictionImpulses[index] = 0.0;
            mfContactConstraints[index] = -1;
            mfFrictionConstraints[index] = -1;
        } // else we're copying this data into an existing point so keep the data
    }

    private int findOpenPoint(int manifold) {
        for (int i = 0; i < MANIFOLD_POINT_SIZE; i++) {
            if (mfLifetimes[toIndex(manifold, i)] < 0) {
                // point is open
                return i;
            }
        }
        return -1;
    }

    private int findNearestPoint(int manifold, @Const Vector4 localA) {
        double shortestDistanceSqr = contactBreakingThreshold * contactBreakingThreshold;
        int nearestPoint = -1;

        for (int i = 0; i < MANIFOLD_POINT_SIZE; i++) {
            // only consider points that are alive
            if (mfLifetimes[toIndex(manifold, i)] >= 0) {
                int vec4 = toVector4Index(manifold, i);

                // inlined so we don't need to allocate, and so we can easily
                // convert from a Vector4 to a Vector3
                double x = mfLocalAs[vec4] - localA.x;
                double y = mfLocalAs[vec4 + 1] - localA.y;
                double z = mfLocalAs[vec4 + 2] - localA.z;
                double distSqr = x * x + y * y + z * z;

                if (distSqr < shortestDistanceSqr) {
                    shortestDistanceSqr = distSqr;
                    nearestPoint = i;
                }
            }
        }

        return nearestPoint;
    }

    private int findWorstPoint(int manifold, @Const Vector4 localB) {
        // unfortunately we do need some temporary vectors for this search function
        Vector3 t1 = new Vector3();
        Vector3 t2 = new Vector3();

        // this should only be called when the manifold is full, so we can
        // assume that all points are alive
        int deepestIndex = 0;
        double depth = mfDistances[toIndex(manifold, 0)];
        for (int i = 1; i < MANIFOLD_POINT_SIZE; i++) {
            int offset = toIndex(manifold, i);
            if (mfDistances[offset] < depth) {
                deepestIndex = i;
                depth = mfDistances[offset];
            }
        }

        // compute hypothetical area of manifold if each of the 4 points were
        // removed (unless it is the deepest point)
        double area0Removed = (deepestIndex == 0 ? 0.0 : computeManifoldArea(manifold,
                                                                             localB, 1,
                                                                             3, 2, t1, t2));
        double area1Removed = (deepestIndex == 1 ? 0.0 : computeManifoldArea(manifold,
                                                                             localB, 0,
                                                                             3, 2, t1, t2));
        double area2Removed = (deepestIndex == 2 ? 0.0 : computeManifoldArea(manifold,
                                                                             localB, 0,
                                                                             3, 1, t1, t2));
        double area3Removed = (deepestIndex == 3 ? 0.0 : computeManifoldArea(manifold,
                                                                             localB, 0,
                                                                             2, 1, t1, t2));

        // find the index that maximizes the manifold area when replaced with localB
        int worstIndex = 0;
        double maxArea = area0Removed;
        if (area1Removed > maxArea) {
            worstIndex = 1;
            maxArea = area1Removed;
        }
        if (area2Removed > maxArea) {
            worstIndex = 2;
            maxArea = area2Removed;
        }
        if (area3Removed > maxArea) {
            worstIndex = 3;
            maxArea = area3Removed;
        }

        return worstIndex;
    }

    private double computeManifoldArea(int manifold, @Const Vector4 p1, int p2, int p3,
                                       int p4, Vector3 edge1, Vector3 edge2) {
        // p1 is a hypothetical point not in the manifold yet
        // p2, p3, and p4 are taken from manifold data
        int p2vec = toVector4Index(manifold, p2);
        int p3vec = toVector4Index(manifold, p3);
        int p4vec = toVector4Index(manifold, p4);
        edge1.set(p1.x - mfLocalBs[p2vec], p1.y - mfLocalBs[p2vec + 1],
                  p1.z - mfLocalBs[p2vec + 2]);
        edge2.set(mfLocalBs[p3vec] - mfLocalBs[p4vec],
                  mfLocalBs[p3vec + 1] - mfLocalBs[p4vec + 1],
                  mfLocalBs[p3vec + 2] - mfLocalBs[p4vec + 2]);
        return edge1.cross(edge2).lengthSquared();
    }

    private int toIndex(int manifold, int point) {
        return manifold * MANIFOLD_POINT_SIZE + point;
    }

    private int toVector4Index(int manifold, int point) {
        return toIndex(manifold, point) * 4;
    }

    private int toVector3Index(int manifold, int point) {
        return toIndex(manifold, point) * 3;
    }

    private static class CollisionPair {
        private Component<CollisionBody> a;
        private Component<CollisionBody> b;

        public CollisionPair() {}

        public CollisionPair(Component<CollisionBody> a, Component<CollisionBody> b) {
            set(a, b);
        }

        public void set(Component<CollisionBody> a, Component<CollisionBody> b) {
            this.a = a;
            this.b = b;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof CollisionPair)) {
                return false;
            }
            CollisionPair t = (CollisionPair) o;
            return (t.a == a && t.b == b) || (t.b == a && t.a == b);
        }

        @Override
        public int hashCode() {
            // sum of hashes -> follow Set hashcode since a pair is just a 2 element set
            return a.hashCode() + b.hashCode();
        }
    }
}
