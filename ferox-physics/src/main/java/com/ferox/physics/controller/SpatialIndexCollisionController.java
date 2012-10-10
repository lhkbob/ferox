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
package com.ferox.physics.controller;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.bounds.IntersectionCallback;
import com.ferox.math.bounds.SpatialIndex;
import com.ferox.physics.collision.ClosestPair;
import com.ferox.physics.collision.CollisionAlgorithm;
import com.ferox.physics.collision.CollisionAlgorithmProvider;
import com.ferox.physics.collision.CollisionBody;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;

public class SpatialIndexCollisionController extends CollisionController {
    private SpatialIndex<Entity> index;
    private CollisionAlgorithmProvider algorithms;

    // FIXME: add a setter, too
    public SpatialIndexCollisionController(SpatialIndex<Entity> index,
                                           CollisionAlgorithmProvider algorithms) {
        this.index = index;
        this.algorithms = algorithms;
    }

    @Override
    public void preProcess(double dt) {
        super.preProcess(dt);
        index.clear(true);
    }

    @Override
    public void process(double dt) {
        CollisionBody body1 = getEntitySystem().createDataInstance(CollisionBody.ID);
        CollisionBody body2 = getEntitySystem().createDataInstance(CollisionBody.ID);

        // fill the index with all collision bodies
        ComponentIterator it = new ComponentIterator(getEntitySystem());
        it.addRequired(body1);

        while (it.next()) {
            index.add(body1.getEntity(), body1.getWorldBounds());
        }

        // query for all intersections
        index.query(new CollisionCallback(body1, body2));

        reportConstraints(dt);
    }

    @Override
    public void destroy() {
        index.clear();
        super.destroy();
    }

    private class CollisionCallback implements IntersectionCallback<Entity> {
        private final CollisionBody bodyA;
        private final CollisionBody bodyB;

        public CollisionCallback(CollisionBody bodyA, CollisionBody bodyB) {
            this.bodyA = bodyA;
            this.bodyB = bodyB;
        }

        @Override
        @SuppressWarnings({"rawtypes", "unchecked"})
        public void process(Entity a, AxisAlignedBox boundsA, Entity b,
                            AxisAlignedBox boundsB) {
            // at this point we know the world bounds of a and b intersect, but
            // we need to test for collision against their actual shapes
            a.get(bodyA);
            b.get(bodyB);

            CollisionAlgorithm algorithm = algorithms.getAlgorithm(bodyA.getShape()
                                                                        .getClass(),
                                                                   bodyB.getShape()
                                                                        .getClass());

            if (algorithm != null) {
                // have a valid algorithm to test
                ClosestPair pair = algorithm.getClosestPair(bodyA.getShape(),
                                                            bodyA.getTransform(),
                                                            bodyB.getShape(),
                                                            bodyB.getTransform());

                if (pair != null && pair.isIntersecting()) {
                    notifyContact(bodyA, bodyB, pair);
                }
            }
        }
    }
}
