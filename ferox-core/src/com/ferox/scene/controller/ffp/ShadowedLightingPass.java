package com.ferox.scene.controller.ffp;

import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Entity;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector4f;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Surface;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.Texture;
import com.ferox.scene.SceneElement;
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
    private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
    
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
        AxisAlignedBox shadowBounds = connection.getShadowCastingLightBounds();
        
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
        SceneElement se;
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
