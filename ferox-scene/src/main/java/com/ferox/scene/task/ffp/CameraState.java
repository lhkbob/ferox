package com.ferox.scene.task.ffp;

import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.HardwareAccessLayer;

public class CameraState implements State {
    private final Frustum camera;

    public CameraState(Frustum camera) {
        this.camera = camera;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();
        r.setProjectionMatrix(camera.getProjectionMatrix());
        r.setModelViewMatrix(camera.getViewMatrix());

        AppliedEffects childEffects = effects.applyViewMatrix(camera.getViewMatrix());
        currentNode.visitChildren(childEffects, access);
    }
}
