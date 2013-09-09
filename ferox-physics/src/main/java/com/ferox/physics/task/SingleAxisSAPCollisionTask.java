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
import com.ferox.physics.collision.DefaultCollisionAlgorithmProvider;
import com.ferox.physics.dynamics.RigidBody;
import com.ferox.util.Bag;
import com.ferox.util.profile.Profiler;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;
import com.lhkbob.entreri.task.Job;
import com.lhkbob.entreri.task.ParallelAware;
import com.lhkbob.entreri.task.Task;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * SingleAxisSAPCollisionTask is a collision task that implements the broadphase using the single axis sweep
 * and prune technique. The current implementation uses the x axis as the sweeping axis.
 *
 * @author Michael Ludwig
 */
public class SingleAxisSAPCollisionTask extends CollisionTask implements ParallelAware {
    private static final Set<Class<? extends Component>> COMPONENTS;

    static {
        Set<Class<? extends Component>> types = new HashSet<>();
        types.add(CollisionBody.class);
        types.add(RigidBody.class);
        COMPONENTS = Collections.unmodifiableSet(types);
    }

    private final Bag<Entity> bodies;

    // cached instances that are normally local to process()
    private CollisionBody body;
    private ComponentIterator iterator;

    /**
     * Create a new SingleAxisSAPCollisionTask that uses a default collision algorithm provider.
     */
    public SingleAxisSAPCollisionTask() {
        this(new DefaultCollisionAlgorithmProvider());
    }

    /**
     * Create a new SingleAxisSAPCollisionTask that uses the given algorithm provider.
     *
     * @param algorithms The algorithm provider
     *
     * @throws NullPointerException if algorithms is null
     */
    public SingleAxisSAPCollisionTask(CollisionAlgorithmProvider algorithms) {
        super(algorithms);
        bodies = new Bag<>();
    }

    @Override
    public void reset(EntitySystem system) {
        super.reset(system);

        if (iterator == null) {
            iterator = system.fastIterator();
            body = iterator.addRequired(CollisionBody.class);
        }

        iterator.reset();
        bodies.clear(true);
    }

    @Override
    public Task process(EntitySystem system, Job job) {
        Profiler.push("detect-collisions");

        // build up axis lists to sort
        Profiler.push("prepare-axis");
        while (iterator.next()) {
            bodies.add(body.getEntity());
        }

        int[] edges = new int[bodies.size() * 2];
        int[] edgeLabels = new int[bodies.size() * 2];

        iterator.reset();
        int i = 0;
        while (iterator.next()) {
            AxisAlignedBox aabb = body.getWorldBounds();
            edges[(i << 1)] = Functions.sortableFloatToIntBits((float) aabb.min.x);
            edges[(i << 1) + 1] = Functions.sortableFloatToIntBits((float) aabb.max.x);
            edgeLabels[(i << 1)] = i;
            edgeLabels[(i << 1) + 1] = (0x80000000) | i;

            i++;
        }
        Profiler.pop();

        // sort edges, keeping edgeLabels in sync with swaps
        Profiler.push("sort-axis");
        quickSort(edges, edgeLabels, 0, edges.length);
        Profiler.pop();

        // iterate edges, checking overlapping pairs
        Profiler.push("prune");
        int openIndex = -1;
        int nextIndex = -1;

        int currLabel;
        boolean isMax;
        for (i = 0; i < edges.length; i++) {
            // check the next label
            isMax = edgeLabels[i] < 0;
            currLabel = (~0x80000000) & edgeLabels[i];

            if (isMax) {
                if (openIndex >= 0 && currLabel == edgeLabels[openIndex]) {
                    // reached the end of the current box, update loop if
                    // we know of another min edge to visit
                    if (nextIndex >= 0) {
                        i = nextIndex;
                    }

                    openIndex = nextIndex;
                    nextIndex = -1;
                }
                // otherwise we're skipping past the max edges of already
                // processed boxes
            } else {
                if (openIndex < 0) {
                    // open another box
                    openIndex = i;
                } else {
                    // perform intersection test with open box and current box
                    CollisionBody bodyA = bodies.get(edgeLabels[openIndex]).get(CollisionBody.class);
                    CollisionBody bodyB = bodies.get(currLabel).get(CollisionBody.class);

                    if (bodyA.getWorldBounds().intersects(bodyB.getWorldBounds())) {
                        notifyPotentialContact(bodyA, bodyB);
                    }

                    if (nextIndex < 0) {
                        // earliest next min edge we've found
                        nextIndex = i;
                    }
                }
            }
        }
        Profiler.pop();

        // generate constraints
        Profiler.push("generate-constraints");
        reportConstraints(job);
        Profiler.pop();

        Profiler.pop();
        return super.process(system, job);
    }

    @Override
    public Set<Class<? extends Component>> getAccessedComponents() {
        return COMPONENTS;
    }

    @Override
    public boolean isEntitySetModified() {
        return false;
    }

    // use quick sort to sort elements in x, swapping y along with it
    private void quickSort(int[] x, int[] y, int off, int len) {
        // insertion sort on smallest arrays
        if (len < 7) {
            for (int i = off; i < len + off; i++) {
                for (int j = i; j > off && x[j - 1] > x[j]; j--) {
                    swap(x, y, j, j - 1);
                }
            }
            return;
        }

        // choose a partition element, v
        int m = off + (len >> 1); // small arrays, middle element
        if (len > 7) {
            int l = off;
            int n = off + len - 1;
            if (len > 40) { // big arrays, pseudomedian of 9
                int s = len / 8;
                l = med3(x, l, l + s, l + 2 * s);
                m = med3(x, m - s, m, m + s);
                n = med3(x, n - 2 * s, n - s, n);
            }
            m = med3(x, l, m, n); // mid-size, med of 3
        }
        int v = x[m];

        int a = off, b = a, c = off + len - 1, d = c;
        while (true) {
            while (b <= c && x[b] <= v) {
                if (v == x[b]) {
                    swap(x, y, a++, b);
                }
                b++;
            }
            while (c >= b && x[c] >= v) {
                if (v == x[c]) {
                    swap(x, y, c, d--);
                }
                c--;
            }
            if (b > c) {
                break;
            }
            swap(x, y, b++, c--);
        }

        // swap partition elements back to middle
        int s, n = off + len;
        s = Math.min(a - off, b - a);
        vecswap(x, y, off, b - s, s);
        s = Math.min(d - c, n - d - 1);
        vecswap(x, y, b, n - s, s);

        // recursively sort non-partition-elements
        if ((s = b - a) > 1) {
            quickSort(x, y, off, s);
        }
        if ((s = d - c) > 1) {
            quickSort(x, y, n - s, s);
        }
    }

    // swaps the elements at indices a and b, along with the hashes in x
    private void swap(int[] x, int[] y, int a, int b) {
        int k = x[a];
        x[a] = x[b];
        x[b] = k;

        int l = y[a];
        y[a] = y[b];
        y[b] = l;
    }

    // swaps n elements starting at a and b, such that (a,b), (a+1, b+1), etc. are swapped
    private void vecswap(int[] x, int[] y, int a, int b, int n) {
        for (int i = 0; i < n; i++, a++, b++) {
            swap(x, y, a, b);
        }
    }

    // returns the index of the median of the three indexed elements
    private static int med3(int[] x, int a, int b, int c) {
        return (x[a] < x[b] ? (x[b] < x[c] ? b : x[a] < x[c] ? c : a)
                            : (x[b] > x[c] ? b : x[a] > x[c] ? c : a));
    }
}
