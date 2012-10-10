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

import com.ferox.entity2.Component;
import com.ferox.entity2.ComponentId;
import com.ferox.entity2.Entity;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector4f;
import com.ferox.math.bounds.ReadOnlyAxisAlignedBox;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Surface;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.Texture;
import com.ferox.scene.Transform;
import com.ferox.scene.ShadowReceiver;
import com.ferox.util.Bag;

/**
 * ShadowedLightingPass is a RenderPass implementation that is the final stage
 * of shadow mapping. It using depth comparisons in the generated shadow map to
 * conditionally enable/disable the lighting, and uses additive blending to
 * contribute this lighting into the final rendering.
 * 
 * @author Michael Ludwig
 */
public class ShadowedLightingPass extends AbstractFixedFunctionRenderPass {
    private static final ComponentId<ShadowReceiver> SR_ID = Component.getComponentId(ShadowReceiver.class);
    private static final ComponentId<Transform> SE_ID = Component.getComponentId(Transform.class);
    
    private static final Matrix4f bias = new Matrix4f(.5f, 0f, 0f, .5f,
                                                      0f, .5f, 0f, .5f,
                                                      0f, 0f, .5f, .5f,
                                                      0f, 0f, 0f, 1f);
    
    private final Texture shadowMap;
    
    public ShadowedLightingPass(RenderConnection connection, Texture shadowMap, int maxMaterialTexUnits, 
                                String vertexBinding, String normalBinding, String texCoordBinding) {
        super(connection, maxMaterialTexUnits, vertexBinding, normalBinding, texCoordBinding);
        this.shadowMap = shadowMap;
    }
    
    @Override
    protected void render(FixedFunctionRenderer ffp) {
        Component shadowLight = connection.getShadowCastingLight();
        ReadOnlyAxisAlignedBox shadowBounds = connection.getShadowCastingLightBounds();
        
        Frustum shadowFrustum = connection.getShadowFrustum();
        Bag<Entity> renderAtoms = connection.getRenderedEntities();
        
        // setup single light and disable global ambient
        setLight(0, shadowLight);
        ffp.setGlobalAmbientLight(BLACK);

        // enable additive blending
        ffp.setBlendingEnabled(true);
        ffp.setBlendMode(BlendFunction.ADD, BlendFactor.SRC_ALPHA, BlendFactor.ONE);

        // setup shadow map texture
        int smUnit = maxMaterialTexUnits;
        ffp.setTextureEnabled(smUnit, true);
        ffp.setTexture(smUnit, shadowMap);
        ffp.setTextureCoordGeneration(smUnit, TexCoordSource.EYE);
        
        Vector4f plane = new Vector4f();
        Matrix4f texM = new Matrix4f();
        bias.mul(shadowFrustum.getProjectionMatrix(), texM).mul(shadowFrustum.getViewMatrix());
        
        ffp.setTextureEyePlane(smUnit, TexCoord.S, texM.getRow(0, plane));
        ffp.setTextureEyePlane(smUnit, TexCoord.T, texM.getRow(1, plane));
        ffp.setTextureEyePlane(smUnit, TexCoord.R, texM.getRow(2, plane));
        ffp.setTextureEyePlane(smUnit, TexCoord.Q, texM.getRow(3, plane));

        // offset depth values in the opposite direction from that in ShadowMapGeneratorPass
        ffp.setDepthOffsetsEnabled(true);
        ffp.setDepthOffsets(0f, -5f);
        ffp.setDepthTest(Comparison.LEQUAL);
        ffp.setDepthWriteMask(false); // depth buffer should be unchanged from last pass
        
        Entity atom;
        Transform se;
        int count = renderAtoms.size();
        for (int i = 0; i < count; i++) {
            atom = renderAtoms.get(i);
            se = atom.get(SE_ID);
            
            if (atom.get(SR_ID) != null) {
                // only update entities that are shadow receivers and
                // that are influenced by the shadow light
                if (se == null || se.getWorldBounds() == null || shadowBounds.intersects(se.getWorldBounds()))
                    render(atom);
            }
        }
    }

    @Override
    protected Frustum getFrustum() {
        return connection.getViewFrustum();
    }

    @Override
    protected void configureViewport(FixedFunctionRenderer renderer, Surface surface) {
        renderer.setViewport(connection.getViewportLeft(), connection.getViewportBottom(), 
                             connection.getViewportRight() - connection.getViewportLeft(), 
                             connection.getViewportTop() - connection.getViewportBottom());
        // don't clear anything
    }

    @Override
    protected void notifyPassBegin() {
        connection.notifyShadowedLightingPassBegin();
    }

    @Override
    protected void notifyPassEnd() {
        connection.notifyShadowedLightingPassEnd();
    }
}
