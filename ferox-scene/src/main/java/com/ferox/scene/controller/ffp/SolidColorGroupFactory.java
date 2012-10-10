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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ferox.math.ColorRGB;
import com.ferox.math.Vector4;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.scene.DiffuseColor;
import com.ferox.scene.Transparent;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;

public class SolidColorGroupFactory implements StateGroupFactory {
    private final StateGroupFactory childFactory;

    private final DiffuseColor color;
    private final Transparent alpha;

    private final SolidColorState access;

    public SolidColorGroupFactory(EntitySystem system, StateGroupFactory childFactory) {
        this.childFactory = childFactory;
        color = system.createDataInstance(DiffuseColor.ID);
        alpha = system.createDataInstance(Transparent.ID);

        access = new SolidColorState();
    }

    @Override
    public StateGroup newGroup() {
        return new SolidColorGroup();
    }

    private class SolidColorGroup implements StateGroup {
        private final List<StateNode> allNodes;
        private final Map<SolidColorState, StateNode> nodeLookup;

        public SolidColorGroup() {
            allNodes = new ArrayList<StateNode>();
            nodeLookup = new HashMap<SolidColorState, StateNode>();
        }

        @Override
        public StateNode getNode(Entity e) {
            e.get(color);
            e.get(alpha);

            access.set(color, alpha);
            StateNode node = nodeLookup.get(access);
            if (node == null) {
                // new color combination
                SolidColorState newState = new SolidColorState();
                newState.set(color, alpha);
                node = new StateNode((childFactory == null ? null : childFactory.newGroup()),
                                     newState);
                nodeLookup.put(newState, node);
                allNodes.add(node);
            }

            return node;
        }

        @Override
        public List<StateNode> getNodes() {
            return allNodes;
        }

        @Override
        public AppliedEffects applyGroupState(FixedFunctionRenderer r,
                                              AppliedEffects effects) {
            return effects;
        }

        @Override
        public void unapplyGroupState(FixedFunctionRenderer r, AppliedEffects effects) {
            r.setMaterial(MaterialGroupFactory.DEFAULT_AMBIENT,
                          MaterialGroupFactory.DEFAULT_DIFFUSE,
                          MaterialGroupFactory.DEFAULT_SPECULAR,
                          MaterialGroupFactory.DEFAULT_EMITTED);
        }

    }

    private class SolidColorState implements State {
        private final Vector4 color;

        public SolidColorState() {
            color = new Vector4();
        }

        public void set(DiffuseColor color, Transparent alpha) {
            if (color.isEnabled()) {
                ColorRGB rgb = color.getColor();
                this.color.x = rgb.red();
                this.color.y = rgb.green();
                this.color.z = rgb.blue();
            } else {
                this.color.set(MaterialGroupFactory.DEFAULT_DIFFUSE);
            }

            this.color.w = (alpha.isEnabled() ? alpha.getOpacity() : 1.0);
        }

        @Override
        public void add(Entity e) {
            // do nothing
        }

        @Override
        public AppliedEffects applyState(FixedFunctionRenderer r, AppliedEffects effects,
                                         int index) {
            r.setMaterial(MaterialGroupFactory.DEFAULT_AMBIENT, color,
                          MaterialGroupFactory.DEFAULT_SPECULAR,
                          MaterialGroupFactory.DEFAULT_SPECULAR);
            return effects;
        }

        @Override
        public void unapplyState(FixedFunctionRenderer r, AppliedEffects effects,
                                 int index) {
            // do nothing
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SolidColorState)) {
                return false;
            }
            return ((SolidColorState) o).color.equals(color);
        }

        @Override
        public int hashCode() {
            return color.hashCode();
        }
    }
}
