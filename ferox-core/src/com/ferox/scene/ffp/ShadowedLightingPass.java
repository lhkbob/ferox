package com.ferox.scene.ffp;

import com.ferox.math.Matrix4f;
import com.ferox.math.Vector4f;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.TextureImage;
import com.ferox.util.Bag;

public class ShadowedLightingPass extends AbstractFfpRenderPass {
	private static final Matrix4f bias = new Matrix4f(.5f, 0f, 0f, .5f,
													  0f, .5f, 0f, .5f,
													  0f, 0f, .5f, .5f,
													  0f, 0f, 0f, 1f);
	
	private final TextureImage shadowMap;
	
	public ShadowedLightingPass(TextureImage shadowMap, int maxMaterialTexUnits, 
								String vertexBinding, String normalBinding, String texCoordBinding) {
		super(maxMaterialTexUnits, vertexBinding, normalBinding, texCoordBinding);
		this.shadowMap = shadowMap;
	}
	
	@Override
	protected void render(FixedFunctionRenderer ffp) {
		LightAtom shadowLight = renderDescr.getShadowCaster();
		Frustum shadowFrustum = renderDescr.getShadowMapFrustum();
		Bag<RenderAtom> renderAtoms = renderDescr.getRenderAtoms();
		
		// setup single light and disable global ambient
		setLight(0, shadowLight, false);
		ffp.setLightEnabled(0, true);
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
		bias.mul(shadowFrustum.getProjectionMatrix(texM), texM).mul(shadowFrustum.getViewMatrix(null));
		
		ffp.setTextureEyePlane(smUnit, TexCoord.S, texM.getRow(0, plane));
		ffp.setTextureEyePlane(smUnit, TexCoord.T, texM.getRow(1, plane));
		ffp.setTextureEyePlane(smUnit, TexCoord.R, texM.getRow(2, plane));
		ffp.setTextureEyePlane(smUnit, TexCoord.Q, texM.getRow(3, plane));

		// offset depth values in the opposite direction from that in ShadowMapGeneratorPass
		ffp.setDepthOffsetsEnabled(true);
		ffp.setDepthOffsets(0f, -5f);
		ffp.setDepthTest(Comparison.LEQUAL);
		
		RenderAtom atom;
		int count = renderAtoms.size();
		for (int i = 0; i < count; i++) {
			atom = renderAtoms.get(i);
			if (atom.requiresShadowPass)
				renderAtom(atom);
		}
	}

	@Override
	protected Frustum getFrustum() {
		// note that this returns null when there's no shadow map, which
		// will cause AbstractFfpRenderPass to skip rendering the rest of the pass
		return (renderDescr == null || renderDescr.getShadowMapFrustum() == null ? null : renderDescr.getViewFrustum());
	}

	@Override
	protected boolean initViewport(FixedFunctionRenderer ffp) {
		// set the viewport but don't clear anything (return false)
		ffp.setViewport(renderDescr.getViewportX(), renderDescr.getViewportY(), 
						renderDescr.getViewportWidth(), renderDescr.getViewportHeight());
		return false;
	}
}
