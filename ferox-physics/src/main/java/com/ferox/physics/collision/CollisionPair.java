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
package com.ferox.physics.collision;

/**
 * CollisionPair is a pair of CollisionBodies involved in a potential collision.
 *
 * @author Michael Ludwig
 */
public class CollisionPair {
    private CollisionBody a;
    private CollisionBody b;

    /**
     * Create an empty pair that can have its contents updated later (useful for querying collections of
     * pairs).
     */
    public CollisionPair() {
    }

    /**
     * Create a new pair over the given two bodies.
     *
     * @param a The first body
     * @param b The second body
     *
     * @see #set(CollisionBody, CollisionBody)
     */
    public CollisionPair(CollisionBody a, CollisionBody b) {
        set(a, b);
    }

    /**
     * Set the two bodies on this pair. This should not be called on pairs that are stored inside collections
     * that depend on {@link #equals(Object)} and {@link #hashCode()}.
     *
     * @param a The first body
     * @param b The second body
     *
     * @throws NullPointerException     if a or b are null
     * @throws IllegalArgumentException if a or b are flyweight instances
     */
    public void set(CollisionBody a, CollisionBody b) {
        if (a.isFlyweight() || b.isFlyweight()) {
            throw new IllegalArgumentException("Bodies should not be flyweight instances");
        }
        this.a = a;
        this.b = b;
    }

    /**
     * @return The first body in the pair
     */
    public CollisionBody getBodyA() {
        return a;
    }

    /**
     * @return The second body in the pair
     */
    public CollisionBody getBodyB() {
        return b;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof CollisionPair)) {
            return false;
        }
        CollisionPair t = (CollisionPair) o;
        return (t.a.equals(a) && t.b.equals(b)) || (t.b.equals(a) && t.a.equals(b));
    }

    @Override
    public int hashCode() {
        // sum of hashes -> follow Set hashcode since a pair is just a 2 element set
        return a.hashCode() + b.hashCode();
    }
}
