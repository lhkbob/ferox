package com.ferox.scene.controller.ffp;

import java.util.Collections;
import java.util.List;

import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.lhkbob.entreri.Entity;

public class CameraGroupFactory implements StateGroupFactory {
    private final StateGroupFactory childFactory;

    private final Frustum camera;

    public CameraGroupFactory(Frustum camera, StateGroupFactory childFactory) {
        this.camera = camera;
        this.childFactory = childFactory;
    }

    @Override
    public StateGroup newGroup() {
        return new CameraGroup();
    }

    private class CameraGroup implements StateGroup {
        private final StateNode node;

        public CameraGroup() {
            node = new StateNode((childFactory == null ? null : childFactory.newGroup()),
                                 new CameraState());
        }

        @Override
        public StateNode getNode(Entity e) {
            return node;
        }

        @Override
        public List<StateNode> getNodes() {
            return Collections.singletonList(node);
        }

        @Override
        public AppliedEffects applyGroupState(FixedFunctionRenderer r,
                                              AppliedEffects effects) {
            return effects;
        }

        @Override
        public void unapplyGroupState(FixedFunctionRenderer r, AppliedEffects effects) {}

    }

    private class CameraState implements State {
        @Override
        public void add(Entity e) {}

        @Override
        public AppliedEffects applyState(FixedFunctionRenderer r, AppliedEffects effects,
                                         int index) {
            r.setProjectionMatrix(camera.getProjectionMatrix());
            r.setModelViewMatrix(camera.getViewMatrix());
            return effects.applyViewMatrix(camera.getViewMatrix());
        }

        @Override
        public void unapplyState(FixedFunctionRenderer r, AppliedEffects effects,
                                 int index) {}
    }
}
