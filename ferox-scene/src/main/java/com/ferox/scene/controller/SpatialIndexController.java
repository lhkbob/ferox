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

import java.util.Iterator;

import com.ferox.math.bounds.SpatialIndex;
import com.ferox.scene.Renderable;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.SimpleController;

public class SpatialIndexController extends SimpleController {
    private final SpatialIndex<Entity> index;

    // FIXME: add a setter, too
    public SpatialIndexController(SpatialIndex<Entity> index) {
        this.index = index;
    }

    @Override
    public void preProcess(double dt) {
        // FIXME add a way to adjust the bounds of a SpatialIndex, and have this
        // listen for SceneBoundsResults and update the index so we don't lose
        // objects inappropriately
        index.clear(true);
    }

    @Override
    public void process(double dt) {
        Renderable r;
        Iterator<Renderable> it = getEntitySystem().iterator(Renderable.ID);
        while (it.hasNext()) {
            r = it.next();
            index.add(r.getEntity(), r.getWorldBounds());
        }

        // send the built index to everyone listened
        getEntitySystem().getControllerManager().report(new SpatialIndexResult(index));
    }

    @Override
    public void destroy() {
        index.clear();
        super.destroy();
    }
}
