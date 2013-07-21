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

import com.lhkbob.entreri.Component;

public class CollisionPair {
    private Component<CollisionBody> a;
    private Component<CollisionBody> b;

    public CollisionPair() {
    }

    public CollisionPair(Component<CollisionBody> a, Component<CollisionBody> b) {
        set(a, b);
    }

    public void set(Component<CollisionBody> a, Component<CollisionBody> b) {
        if (a == null || b == null) {
            throw new NullPointerException("Bodies cannot be null");
        }
        this.a = a;
        this.b = b;
    }

    public Component<CollisionBody> getBodyA() {
        return a;
    }

    public Component<CollisionBody> getBodyB() {
        return b;
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