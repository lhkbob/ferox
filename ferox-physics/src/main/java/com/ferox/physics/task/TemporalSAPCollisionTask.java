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
package com.ferox.physics.task;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Functions;
import com.ferox.physics.collision.CollisionAlgorithmProvider;
import com.ferox.physics.collision.CollisionBody;
import com.ferox.physics.collision.CollisionPair;
import com.ferox.physics.collision.DefaultCollisionAlgorithmProvider;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.Task;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * TemporalSAPCollisionTask is a collision task that implements the broadphase using a temporal, 3-axis sweep
 * and prune technique. It maintains edge lists for the X, Y, and Z axis across frames of the simulation and
 * updates them as necessary. Collisions are reported as the sortedness of the lists are ensured.
 *
 * @author Michael Ludwig
 */
public class TemporalSAPCollisionTask extends CollisionTask {
    private final EdgeStore[] edges;
    private final Set<CollisionPair> overlappingPairCache;

    // cached local instances
    private CollisionBody body;
    private ComponentIterator bodyIterator; // iterates bodyA

    /**
     * Create a new TemporalSAPCollisionTask that uses a default collision algorithm provider.
     */
    public TemporalSAPCollisionTask() {
        this(new DefaultCollisionAlgorithmProvider());
    }

    /**
     * Create a new TemporalSAPCollisionTask that uses the given algorithm provider.
     *
     * @param algorithms The algorithm provider
     *
     * @throws NullPointerException if algorithms is null
     */
    public TemporalSAPCollisionTask(CollisionAlgorithmProvider algorithms) {
        super(algorithms);

        overlappingPairCache = new HashSet<>();

        edges = new EdgeStore[3];
        for (int i = 0; i < edges.length; i++) {
            edges[i] = new EdgeStore(overlappingPairCache);
        }

        // link edge axis
        edges[0].axis1 = edges[1];
        edges[0].axis2 = edges[2];
        edges[1].axis1 = edges[2];
        edges[1].axis2 = edges[0];
        edges[2].axis1 = edges[0];
        edges[2].axis2 = edges[1];
    }

    @Override
    public void reset(EntitySystem system) {
        super.reset(system);

        if (bodyIterator == null) {
            bodyIterator = system.fastIterator();
            body = bodyIterator.addRequired(CollisionBody.class);

            for (int i = 0; i < edges.length; i++) {
                edges[i].maxBodyEdges = system.decorate(CollisionBody.class, new IntProperty.Factory(-1));
                edges[i].minBodyEdges = system.decorate(CollisionBody.class, new IntProperty.Factory(-1));
            }
        }

        bodyIterator.reset();
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("detect-collisions");

        // update all edges, sorting as necessary, and keeping track of
        // overlapping pairs
        Profiler.push("update-overlaps");
        for (int i = 0; i < edges.length; i++) {
            edges[i].removeDeadEdges();
        }

        while (bodyIterator.next()) {
            AxisAlignedBox aabb = body.getWorldBounds();

            if (edges[0].minBodyEdges.get(body.getIndex()) < 0) {
                // add body to edge lists
                CollisionBody canonical = body.getEntity().get(CollisionBody.class);
                edges[0].addEdges(canonical, aabb.min.x, aabb.max.x, false);
                edges[1].addEdges(canonical, aabb.min.y, aabb.max.y, false);
                edges[2].addEdges(canonical, aabb.min.z, aabb.max.z, true);
            } else {
                // update doesn't need canonical reference
                edges[0].updateEdges(body, aabb.min.x, aabb.max.x);
                edges[1].updateEdges(body, aabb.min.y, aabb.max.y);
                edges[2].updateEdges(body, aabb.min.z, aabb.max.z);
            }
        }
        Profiler.pop();

        // iterate through pairs, removing those that have dead components,
        // and performing narrow-phase collisions on the remaining
        Profiler.push("process-overlaps");
        for (CollisionPair pair : overlappingPairCache) {
            if (pair.getBodyA().isAlive() && pair.getBodyB().isAlive()) {
                // both components are still valid check world bounds and then do further tests
                if (pair.getBodyA().getWorldBounds().intersects(pair.getBodyB().getWorldBounds())) {
                    notifyPotentialContact(pair.getBodyA(), pair.getBodyB());
                }
            }
        }
        Profiler.pop();

        // report constraints for accumulated collisions
        Profiler.push("generate-constraints");
        reportConstraints(job);
        Profiler.pop();

        Profiler.pop();

        return super.process(system, job);
    }

    private static class EdgeStore {
        private final Set<CollisionPair> overlappingPairCache;
        private final CollisionPair query; // mutable, don't put in set

        private int[] edges;
        private CollisionBody[] edgeLabels;
        private boolean[] edgeIsMax;

        private IntProperty minBodyEdges;
        private IntProperty maxBodyEdges;

        private int edgeCount;

        private EdgeStore axis1;
        private EdgeStore axis2;

        @SuppressWarnings("unchecked")
        public EdgeStore(Set<CollisionPair> overlappingPairCache) {
            this.overlappingPairCache = overlappingPairCache;
            query = new CollisionPair();

            edges = new int[2];
            edgeLabels = new CollisionBody[2];
            edgeIsMax = new boolean[2];

            edgeCount = 0;

            // sentinel values
            edges[0] = Integer.MIN_VALUE;
            edges[1] = Integer.MAX_VALUE;
        }

        public void removeDeadEdges() {
            // remove disabled component edges by shifting everything to the left
            for (int i = 1; i < 1 + edgeCount; i++) {
                if (!edgeLabels[i].isAlive()) {
                    System.arraycopy(edgeLabels, i + 1, edgeLabels, i, edgeCount - i);
                    System.arraycopy(edges, i + 1, edges, i, edgeCount - i);
                    System.arraycopy(edgeIsMax, i + 1, edgeIsMax, i, edgeCount - i);
                    edgeCount--;
                }
            }

            // sync component edge index after everything has been moved
            for (int i = 1; i < 1 + edgeCount; i++) {
                if (edgeIsMax[i]) {
                    maxBodyEdges.set(i, edgeLabels[i].getIndex());
                } else {
                    minBodyEdges.set(i, edgeLabels[i].getIndex());
                }
            }
        }

        public void addEdges(CollisionBody body, double min, double max, boolean updateOverlaps) {
            // offset by 1 for sentinel value stored in 0th index
            int minIndex = 1 + edgeCount++;
            int maxIndex = 1 + edgeCount++;
            if (maxIndex >= edges.length - 2) {
                int newLength = (edges.length) * 2 + 2;
                edges = Arrays.copyOf(edges, newLength);
                edgeLabels = Arrays.copyOf(edgeLabels, newLength);
                edgeIsMax = Arrays.copyOf(edgeIsMax, newLength);
            }

            edges[minIndex] = Functions.sortableFloatToIntBits((float) min);
            edgeIsMax[minIndex] = false;
            edgeLabels[minIndex] = body;

            edges[maxIndex] = Functions.sortableFloatToIntBits((float) max);
            edgeIsMax[maxIndex] = true;
            edgeLabels[maxIndex] = body;

            minBodyEdges.set(minIndex, body.getIndex());
            maxBodyEdges.set(maxIndex, body.getIndex());

            // preserve ending sentinel value
            edges[maxIndex + 1] = Integer.MAX_VALUE;
            edgeIsMax[maxIndex + 1] = true;

            // sort to proper position
            sortMinDown(minIndex, updateOverlaps);
            sortMaxDown(maxIndex, updateOverlaps);
        }

        public void updateEdges(CollisionBody body, double newMin, double newMax) {
            int minIndex = minBodyEdges.get(body.getIndex());
            int maxIndex = maxBodyEdges.get(body.getIndex());

            int sNewMin = Functions.sortableFloatToIntBits((float) newMin);
            int sNewMax = Functions.sortableFloatToIntBits((float) newMax);

            // calculate change along axis
            int dmin = sNewMin - edges[minIndex];
            int dmax = sNewMax - edges[maxIndex];

            // assign edge values
            edges[minIndex] = sNewMin;
            edges[maxIndex] = sNewMax;

            // expand (only adds overlaps)
            if (dmin < 0) {
                sortMinDown(minIndex, true);
            }
            if (dmax > 0) {
                sortMaxUp(maxIndex, true);
            }

            // shrink (only removes overlaps)
            if (dmin > 0) {
                sortMinUp(minIndex, true);
            }
            if (dmax < 0) {
                sortMaxDown(maxIndex, true);
            }
        }

        public boolean test2DOverlap(int bodyA, int bodyB) {
            // optimization: check the array index instead of the actual
            // coordinate value, since we know that they're sorted
            boolean a1A = axis1.maxBodyEdges.get(bodyA) < axis1.minBodyEdges.get(bodyB);
            boolean a1B = axis1.maxBodyEdges.get(bodyB) < axis1.minBodyEdges.get(bodyA);
            boolean a2A = axis2.maxBodyEdges.get(bodyA) < axis2.minBodyEdges.get(bodyB);
            boolean a2B = axis2.maxBodyEdges.get(bodyB) < axis2.minBodyEdges.get(bodyA);

            return !(a1A || a1B || a2A || a2B);
        }

        public void sortMinDown(int edge, boolean updateOverlaps) {
            int prevEdge = edge - 1;
            while (edges[edge] < edges[prevEdge]) {
                if (edgeIsMax[prevEdge]) {
                    // previous edge is a maximum so check the bounds for overlap,
                    // and if they do, add an overlap
                    if (updateOverlaps &&
                        test2DOverlap(edgeLabels[edge].getIndex(), edgeLabels[prevEdge].getIndex())) {
                        query.set(edgeLabels[edge], edgeLabels[prevEdge]);
                        if (!overlappingPairCache.contains(query)) {
                            // allocate new pair
                            overlappingPairCache
                                    .add(new CollisionPair(edgeLabels[edge], edgeLabels[prevEdge]));
                        } // else already overlapping
                    }

                    // update edge reference
                    maxBodyEdges.set(edge, edgeLabels[prevEdge].getIndex());
                } else {
                    minBodyEdges.set(edge, edgeLabels[prevEdge].getIndex());
                }

                // swap
                CollisionBody sb = edgeLabels[edge];
                int se = edges[edge];
                boolean sm = edgeIsMax[edge];

                edgeLabels[edge] = edgeLabels[prevEdge];
                edges[edge] = edges[prevEdge];
                edgeIsMax[edge] = edgeIsMax[prevEdge];

                edgeLabels[prevEdge] = sb;
                edges[prevEdge] = se;
                edgeIsMax[prevEdge] = sm;

                // sync up the body to the final position of the edge
                minBodyEdges.set(prevEdge, sb.getIndex());

                // decrement
                edge = prevEdge;
                prevEdge--;
            }
        }

        public void sortMaxDown(int edge, boolean updateOverlaps) {
            int prevEdge = edge - 1;
            while (edges[edge] < edges[prevEdge]) {
                if (!edgeIsMax[prevEdge]) {
                    // previous edge is a minimum so remove all overlaps between
                    // current and previous bodies
                    if (updateOverlaps &&
                        test2DOverlap(edgeLabels[edge].getIndex(), edgeLabels[prevEdge].getIndex())) {
                        query.set(edgeLabels[edge], edgeLabels[prevEdge]);
                        overlappingPairCache.remove(query);
                    }

                    // update edge reference
                    minBodyEdges.set(edge, edgeLabels[prevEdge].getIndex());
                } else {
                    maxBodyEdges.set(edge, edgeLabels[prevEdge].getIndex());
                }

                // swap
                CollisionBody sb = edgeLabels[edge];
                int se = edges[edge];
                boolean sm = edgeIsMax[edge];

                edgeLabels[edge] = edgeLabels[prevEdge];
                edges[edge] = edges[prevEdge];
                edgeIsMax[edge] = edgeIsMax[prevEdge];

                edgeLabels[prevEdge] = sb;
                edges[prevEdge] = se;
                edgeIsMax[prevEdge] = sm;

                // sync up the body to the final position of the edge
                maxBodyEdges.set(prevEdge, sb.getIndex());

                // decrement
                edge = prevEdge;
                prevEdge--;
            }
        }

        public void sortMinUp(int edge, boolean updateOverlaps) {
            int nextEdge = edge + 1;
            while (edges[edge] > edges[nextEdge]) {
                if (edgeIsMax[nextEdge]) {
                    // next edge is a maximum so remove any overlaps between
                    // the two bodies
                    if (updateOverlaps &&
                        test2DOverlap(edgeLabels[edge].getIndex(), edgeLabels[nextEdge].getIndex())) {
                        query.set(edgeLabels[edge], edgeLabels[nextEdge]);
                        overlappingPairCache.remove(query);
                    }

                    // update edge reference
                    maxBodyEdges.set(edge, edgeLabels[nextEdge].getIndex());
                } else {
                    minBodyEdges.set(edge, edgeLabels[nextEdge].getIndex());
                }

                // swap
                CollisionBody sb = edgeLabels[edge];
                int se = edges[edge];
                boolean sm = edgeIsMax[edge];

                edgeLabels[edge] = edgeLabels[nextEdge];
                edges[edge] = edges[nextEdge];
                edgeIsMax[edge] = edgeIsMax[nextEdge];

                edgeLabels[nextEdge] = sb;
                edges[nextEdge] = se;
                edgeIsMax[nextEdge] = sm;

                // sync up the body to the final position of the edge
                minBodyEdges.set(nextEdge, sb.getIndex());

                // increment
                edge = nextEdge;
                nextEdge++;
            }
        }

        public void sortMaxUp(int edge, boolean updateOverlaps) {
            int nextEdge = edge + 1;
            while (edges[edge] > edges[nextEdge]) {
                if (!edgeIsMax[nextEdge]) {
                    // next edge is a minimum so test for overlap and possibly
                    // report the new pair
                    if (updateOverlaps &&
                        test2DOverlap(edgeLabels[edge].getIndex(), edgeLabels[nextEdge].getIndex())) {
                        query.set(edgeLabels[edge], edgeLabels[nextEdge]);
                        if (!overlappingPairCache.contains(query)) {
                            // allocate new pair
                            overlappingPairCache
                                    .add(new CollisionPair(edgeLabels[edge], edgeLabels[nextEdge]));
                        } // else already overlapping
                    }

                    // update edge reference
                    minBodyEdges.set(edge, edgeLabels[nextEdge].getIndex());
                } else {
                    maxBodyEdges.set(edge, edgeLabels[nextEdge].getIndex());
                }

                // swap
                CollisionBody sb = edgeLabels[edge];
                int se = edges[edge];
                boolean sm = edgeIsMax[edge];

                edgeLabels[edge] = edgeLabels[nextEdge];
                edges[edge] = edges[nextEdge];
                edgeIsMax[edge] = edgeIsMax[nextEdge];

                edgeLabels[nextEdge] = sb;
                edges[nextEdge] = se;
                edgeIsMax[nextEdge] = sm;

                // sync up the body to the final position of the edge
                maxBodyEdges.set(nextEdge, sb.getIndex());

                // increment
                edge = nextEdge;
                nextEdge++;
            }
        }
    }
}
