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
        public AppliedEffects applyGroupState(FixedFunctionRenderer r, AppliedEffects effects) {
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
        public AppliedEffects applyState(FixedFunctionRenderer r, AppliedEffects effects, int index) {
            r.setLightingEnabled(enable);
            return effects;
        }

        @Override
        public void unapplyState(FixedFunctionRenderer r, AppliedEffects effects, int index) {
            r.setLightingEnabled(false);
        }
    }
}
