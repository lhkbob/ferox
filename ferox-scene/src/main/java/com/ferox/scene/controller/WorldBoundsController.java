package com.ferox.scene.controller;

import com.ferox.math.AxisAlignedBox;
import com.ferox.scene.Renderable;
import com.ferox.scene.Transform;
import com.lhkbob.entreri.ComponentIterator;
import com.lhkbob.entreri.SimpleController;

public class WorldBoundsController extends SimpleController {
    @Override
    public void process(double dt) {
        AxisAlignedBox worldBounds = new AxisAlignedBox();

        Renderable renderable = getEntitySystem().createDataInstance(Renderable.ID);
        Transform transform = getEntitySystem().createDataInstance(Transform.ID);
        ComponentIterator it = new ComponentIterator(getEntitySystem())
        .addRequired(renderable)
        .addRequired(transform);

        while(it.next()) {
            worldBounds.transform(renderable.getLocalBounds(), transform.getMatrix());
            renderable.setWorldBounds(worldBounds);
        }
    }
}
