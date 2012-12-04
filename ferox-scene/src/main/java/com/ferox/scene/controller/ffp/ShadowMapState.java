package com.ferox.scene.controller.ffp;

import com.ferox.math.Matrix4;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.HardwareAccessLayer;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.resource.Texture;
import com.ferox.scene.Light;
import com.lhkbob.entreri.Component;

public class ShadowMapState implements State {
    private static final Matrix4 bias = new Matrix4().set(.5, 0, 0, .5, 0, .5, 0, .5, 0,
                                                          0, .5, .5, 0, 0, 0, 1);

    private final int shadowMapUnit;
    private final ShadowMapCache shadowMap;

    public ShadowMapState(ShadowMapCache cache, int textureUnit) {
        shadowMap = cache;
        shadowMapUnit = textureUnit;
    }

    @Override
    public void visitNode(StateNode currentNode, AppliedEffects effects,
                          HardwareAccessLayer access) {
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        // render base pass with current configuration
        currentNode.visitChildren(effects, access);

        // configure global state for shadow mapping passes
        r.setDepthOffsetsEnabled(true);
        r.setDepthOffsets(0, -5); // offset depth in opposite direction from SM depth
        AppliedEffects shadowEffects = effects.applyBlending(BlendFactor.SRC_ALPHA,
                                                             BlendFactor.ONE);
        shadowEffects.pushBlending(r); // this also sets the depth-mask/test appropriately

        // now apply shadow passes
        for (Component<? extends Light<?>> light : shadowMap.getShadowCastingLights()) {
            renderShadowPass(light, currentNode, shadowEffects, access);
        }

        // restore state (since there won't be multiple shadow-map states
        // together we can do it this way instead of using a reset node like
        // textures and colors, etc)
        r = access.getCurrentContext().getFixedFunctionRenderer(); // must re-get renderer, though
        effects.pushBlending(r);
        r.setDepthOffsetsEnabled(false);
    }

    private void renderShadowPass(Component<? extends Light<?>> shadowCaster,
                                  StateNode node, AppliedEffects effects,
                                  HardwareAccessLayer access) {
        Frustum smFrustum = shadowMap.getShadowMapFrustum(shadowCaster);
        Texture shadowTexture = shadowMap.getShadowMap(shadowCaster, access);

        // must get renderer after the shadow map because that will change
        // and restore the active surface (invalidating any previous renderer)
        FixedFunctionRenderer r = access.getCurrentContext().getFixedFunctionRenderer();

        // configure shadow map texturing
        r.setTexture(shadowMapUnit, shadowTexture);
        r.setTextureCoordGeneration(shadowMapUnit, TexCoordSource.EYE);

        Matrix4 texM = new Matrix4();
        texM.mul(bias, smFrustum.getProjectionMatrix()).mul(smFrustum.getViewMatrix());

        r.setTextureEyePlane(shadowMapUnit, TexCoord.S, texM.getRow(0));
        r.setTextureEyePlane(shadowMapUnit, TexCoord.T, texM.getRow(1));
        r.setTextureEyePlane(shadowMapUnit, TexCoord.R, texM.getRow(2));
        r.setTextureEyePlane(shadowMapUnit, TexCoord.Q, texM.getRow(3));

        // depth bias and blending have already been configured, since
        // they won't change from pass to pass
        node.visitChildren(effects.applyShadowMapping(shadowCaster), access);
    }
}
