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