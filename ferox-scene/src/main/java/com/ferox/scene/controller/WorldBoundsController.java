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
        ComponentIterator it = new ComponentIterator(getEntitySystem()).addRequired(renderable)
                                                                       .addRequired(transform);

        AxisAlignedBox sceneBounds = new AxisAlignedBox();
        boolean first = true;

        while (it.next()) {
            worldBounds.transform(renderable.getLocalBounds(), transform.getMatrix());
            renderable.setWorldBounds(worldBounds);

            if (first) {
                sceneBounds.set(worldBounds);
            } else {
                sceneBounds.union(worldBounds);
            }
        }

        getEntitySystem().getControllerManager()
                         .report(new SceneBoundsResult(sceneBounds));
    }
}
