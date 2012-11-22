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
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.scene.DiffuseColor;
import com.ferox.scene.EmittedColor;
import com.ferox.scene.SpecularColor;
import com.ferox.scene.Transparent;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.EntitySystem;

public class MaterialGroupFactory implements StateGroupFactory {
    public static final Vector4 DEFAULT_DIFFUSE = new Vector4(0.8, 0.8, 0.8, 1.0);
    public static final Vector4 DEFAULT_SPECULAR = new Vector4(0.0, 0.0, 0.0, 1.0);
    public static final Vector4 DEFAULT_EMITTED = new Vector4(0.0, 0.0, 0.0, 1.0);
    public static final Vector4 DEFAULT_AMBIENT = new Vector4(0.2, 0.2, 0.2, 1.0);

    private final DiffuseColor diffuseColor;
    private final SpecularColor specularColor;
    private final EmittedColor emittedColor;
    private final Transparent transparent;

    private final MaterialState access;

    private final StateGroupFactory childFactory;

    public MaterialGroupFactory(EntitySystem system, StateGroupFactory factory) {
        childFactory = factory;
        diffuseColor = system.createDataInstance(DiffuseColor.class);
        specularColor = system.createDataInstance(SpecularColor.class);
        emittedColor = system.createDataInstance(EmittedColor.class);
        transparent = system.createDataInstance(Transparent.class);

        access = new MaterialState();
    }

    @Override
    public StateGroup newGroup() {
        return new MaterialGroup();
    }

    private class MaterialGroup implements StateGroup {
        private final List<StateNode> allNodes;
        private final Map<MaterialState, StateNode> nodeLookup;

        public MaterialGroup() {
            allNodes = new ArrayList<StateNode>();
            nodeLookup = new HashMap<MaterialState, StateNode>();
        }

        @Override
        public StateNode getNode(Entity e) {
            e.get(diffuseColor);
            e.get(specularColor);
            e.get(emittedColor);
            e.get(transparent);

            access.set(diffuseColor, specularColor, emittedColor, transparent);
            StateNode node = nodeLookup.get(access);
            if (node == null) {
                // new color combination
                MaterialState newState = new MaterialState();
                newState.set(diffuseColor, specularColor, emittedColor, transparent);
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
        public AppliedEffects applyGroupState(HardwareAccessLayer access,
                                              AppliedEffects effects) {
            return effects;
        }

        @Override
        public void unapplyGroupState(HardwareAccessLayer access, AppliedEffects effects) {
            FixedFunctionRenderer r = access.getCurrentContext()
                                            .getFixedFunctionRenderer();
            r.setMaterial(DEFAULT_AMBIENT, DEFAULT_DIFFUSE, DEFAULT_SPECULAR,
                          DEFAULT_EMITTED);
        }
    }

    private static class MaterialState implements State {
        private final Vector4 diffuse;
        private final Vector4 specular;
        private final Vector4 emitted;

        public MaterialState() {
            diffuse = new Vector4();
            specular = new Vector4();
            emitted = new Vector4();
        }

        public void set(DiffuseColor diff, SpecularColor spec, EmittedColor emit,
                        Transparent t) {
            ColorRGB rgb;
            if (diff.isEnabled()) {
                rgb = diff.getColor();
                diffuse.x = rgb.red();
                diffuse.y = rgb.green();
                diffuse.z = rgb.blue();
            } else {
                diffuse.set(DEFAULT_DIFFUSE);
            }

            if (spec.isEnabled()) {
                rgb = spec.getColor();
                specular.x = rgb.red();
                specular.y = rgb.green();
                specular.z = rgb.blue();
            } else {
                specular.set(DEFAULT_SPECULAR);
            }

            if (emit.isEnabled()) {
                rgb = emit.getColor();
                emitted.x = rgb.red();
                emitted.y = rgb.green();
                emitted.z = rgb.blue();
            } else {
                emitted.set(DEFAULT_EMITTED);
            }

            double alpha = (t.isEnabled() ? t.getOpacity() : 1.0);
            diffuse.w = alpha;
            specular.w = alpha;
            emitted.w = alpha;
        }

        @Override
        public void add(Entity e) {
            // do nothing
        }

        @Override
        public AppliedEffects applyState(HardwareAccessLayer access,
                                         AppliedEffects effects, int index) {
            FixedFunctionRenderer r = access.getCurrentContext()
                                            .getFixedFunctionRenderer();
            r.setMaterial(diffuse, diffuse, specular, emitted);
            return effects;
        }

        @Override
        public void unapplyState(HardwareAccessLayer access, AppliedEffects effects,
                                 int index) {
            // do nothing
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MaterialState)) {
                return false;
            }
            MaterialState m = (MaterialState) o;
            return (m.diffuse.equals(diffuse) && m.specular.equals(specular) && m.emitted.equals(emitted));
        }

        @Override
        public int hashCode() {
            int result = 17;
            result += 31 * diffuse.hashCode();
            result += 31 * specular.hashCode();
            result += 31 * emitted.hashCode();
            return result;
        }
    }
}
