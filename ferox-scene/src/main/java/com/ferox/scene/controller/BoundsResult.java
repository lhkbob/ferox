package com.ferox.scene.controller;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.lhkbob.entreri.task.Result;

public class BoundsResult extends Result {
    private final AxisAlignedBox bounds;

    public BoundsResult(@Const AxisAlignedBox bounds) {
        this.bounds = new AxisAlignedBox(bounds);
    }

    @Const
    public AxisAlignedBox getBounds() {
        return bounds;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
