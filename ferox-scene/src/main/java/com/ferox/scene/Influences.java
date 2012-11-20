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
package com.ferox.scene;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.Factory;
import com.lhkbob.entreri.PropertyFactory;
import com.lhkbob.entreri.property.ObjectProperty;

public final class Influences extends ComponentData<Influences> {
    @Factory(SetFactory.class)
    private ObjectProperty<Set<Entity>> entities;

    private Influences() {}

    public Set<Entity> getInfluencedSet() {
        return Collections.unmodifiableSet(entities.get(getIndex()));
    }

    public Influences setInfluenced(Entity e, boolean canInfluence) {
        if (e == null) {
            throw new NullPointerException("Entity cannot be null");
        }

        // SetFactory ensures that this is not null
        Set<Entity> set = entities.get(getIndex());
        if (canInfluence) {
            set.add(e);
        } else {
            set.remove(e);
        }
        return this;
    }

    public boolean canInfluence(Entity e) {
        if (e == null) {
            throw new NullPointerException("Entity cannot be null");
        }

        // SetFactory ensures that this is not null
        Set<Entity> set = entities.get(getIndex());
        return set.contains(e);
    }

    private static class SetFactory implements PropertyFactory<ObjectProperty<Set<Entity>>> {
        @Override
        public ObjectProperty<Set<Entity>> create() {
            return new ObjectProperty<Set<Entity>>();
        }

        @Override
        public void setDefaultValue(ObjectProperty<Set<Entity>> property, int index) {
            property.set(new HashSet<Entity>(), index);
        }

        @Override
        public void clone(ObjectProperty<Set<Entity>> src, int srcIndex,
                          ObjectProperty<Set<Entity>> dst, int dstIndex) {
            Set<Entity> toClone = src.get(srcIndex);
            dst.set(new HashSet<Entity>(toClone), dstIndex);
        }
    }
}
