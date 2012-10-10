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
package com.ferox.scene.controller.ffp;

import java.util.Arrays;
import java.util.List;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.scene.BlinnPhongMaterial;
import com.lhkbob.entreri.Entity;

public class LightingGroupFactory implements StateGroupFactory {
    private final StateGroupFactory unlitFactory;
    private final StateGroupFactory litFactory;

    public LightingGroupFactory(StateGroupFactory unlit, StateGroupFactory lit) {
        unlitFactory = unlit;
        litFactory = lit;
    }

    @Override
    public StateGroup newGroup() {
        return new LightingGroup();
    }

    private class LightingGroup implements StateGroup {
        private final StateNode lit;
        private final StateNode unlit;

        private final List<StateNode> nodes;

        public LightingGroup() {
            lit = new StateNode((litFactory == null ? null : litFactory.newGroup()),
                                new LightingState(true));
            unlit = new StateNode((unlitFactory == null ? null : unlitFactory.newGroup()),
                                  new LightingState(false));
            nodes = Arrays.asList(lit, unlit);
        }

        @Override
        public StateNode getNode(Entity e) {
            // FIXME check more than just BlinnPhongMaterials?
            if (e.get(BlinnPhongMaterial.ID) != null) {
                return lit;
            } else {
                return unlit;
            }
        }

        @Override
        public List<StateNode> getNodes() {
            return nodes;
        }

        @Override
        public AppliedEffects applyGroupState(FixedFunctionRenderer r,
                                              AppliedEffects effects) {
            return effects;
        }

        @Override
        public void unapplyGroupState(FixedFunctionRenderer r, AppliedEffects effects) {
            // do nothing
        }

    }

    private class LightingState implements State {
        private final boolean enable;

        public LightingState(boolean enable) {
            this.enable = enable;
        }

        @Override
        public void add(Entity e) {
            // do nothing
        }

        @Override
        public AppliedEffects applyState(FixedFunctionRenderer r, AppliedEffects effects,
                                         int index) {
            r.setLightingEnabled(enable);
            return effects;
        }

        @Override
        public void unapplyState(FixedFunctionRenderer r, AppliedEffects effects,
                                 int index) {
            r.setLightingEnabled(false);
        }
    }
}
