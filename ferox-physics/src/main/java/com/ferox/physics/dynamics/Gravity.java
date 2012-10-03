package com.ferox.physics.dynamics;

import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.math.entreri.Vector3Property;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.Unmanaged;

public class Gravity extends ComponentData<Gravity> {
    public static final TypeId<Gravity> ID = TypeId.get(Gravity.class);

    private Vector3Property gravity;

    @Unmanaged
    private final Vector3 gravityCache = new Vector3();

    private Gravity() {}

    public @Const
    Vector3 getGravity() {
        return gravityCache;
    }

    public Gravity setGravity(@Const Vector3 gravity) {
        gravityCache.set(gravity);
        this.gravity.set(gravity, getIndex());
        return this;
    }

    @Override
    protected void onSet(int index) {
        gravity.get(index, gravityCache);
    }
}
