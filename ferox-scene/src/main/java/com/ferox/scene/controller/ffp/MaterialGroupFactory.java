package com.ferox.scene.controller.ffp;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ferox.math.ColorRGB;
import com.ferox.math.Vector4;
import com.ferox.renderer.FixedFunctionRenderer;
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
        diffuseColor = system.createDataInstance(DiffuseColor.ID);
        specularColor = system.createDataInstance(SpecularColor.ID);
        emittedColor = system.createDataInstance(EmittedColor.ID);
        transparent = system.createDataInstance(Transparent.ID);

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
        public AppliedEffects applyGroupState(FixedFunctionRenderer r,
                                              AppliedEffects effects) {
            return effects;
        }

        @Override
        public void unapplyGroupState(FixedFunctionRenderer r, AppliedEffects effects) {
            r.setMaterial(DEFAULT_AMBIENT, DEFAULT_DIFFUSE, DEFAULT_SPECULAR,
                          DEFAULT_EMITTED);
        }
    }

    private static class MaterialState implements State {
        private final Vector4 diffuse;
        private final Vector4 specular;
        private final Vector4 emitted;
        private final Vector4 ambient;

        public MaterialState() {
            diffuse = new Vector4();
            specular = new Vector4();
            emitted = new Vector4();
            ambient = new Vector4();
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

            ambient.set(DEFAULT_AMBIENT);

            double alpha = (t.isEnabled() ? t.getOpacity() : 1.0);
            diffuse.w = alpha;
            specular.w = alpha;
            emitted.w = alpha;
            ambient.w = alpha;
        }

        @Override
        public void add(Entity e) {
            // do nothing
        }

        @Override
        public AppliedEffects applyState(FixedFunctionRenderer r, AppliedEffects effects,
                                         int index) {
            r.setMaterial(ambient, diffuse, specular, emitted);
            return effects;
        }

        @Override
        public void unapplyState(FixedFunctionRenderer r, AppliedEffects effects,
                                 int index) {
            // do nothing
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof MaterialState)) {
                return false;
            }
            MaterialState m = (MaterialState) o;
            return (m.diffuse.equals(diffuse) && m.specular.equals(specular) && m.emitted.equals(emitted) && m.ambient.equals(ambient));
        }

        @Override
        public int hashCode() {
            int result = 17;
            result += 31 * diffuse.hashCode();
            result += 31 * specular.hashCode();
            result += 31 * emitted.hashCode();
            result += 31 * ambient.hashCode();
            return result;
        }
    }
}
