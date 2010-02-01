package com.ferox.scene.fx.fixed;

import com.ferox.math.Frustum;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.renderer.Renderer.Comparison;
import com.ferox.resource.TextureImage;

public class ShadowLightPass extends AbstractFfpRenderPass {
	private static final Matrix4f bias = new Matrix4f(.5f, 0f, 0f, .5f,
													  0f, .5f, 0f, .5f,
													  0f, 0f, .5f, .5f,
													  0f, 0f, 0f, 1f);
	
	private final TextureImage shadowMap;
	private Frustum shadowFrustum;
	
	public ShadowLightPass(RenderMode renderMode, TextureImage shadowMap, String vertexBinding, 
						   String normalBinding, String texCoordBinding) {
		super(renderMode, vertexBinding, normalBinding, texCoordBinding);
		this.shadowMap = shadowMap;
	}
	
	public void setShadowFrustum(Frustum frustum) {
		shadowFrustum = frustum;
	}
	
	@Override
	protected void render(FixedFunctionRenderer ffp) {
		// setup single light
		setLight(0, shadowLight, false);
		ffp.setLightEnabled(0, true);

		// enable additive blending
		ffp.setBlendingEnabled(true);
		ffp.setBlendMode(BlendFunction.ADD, BlendFactor.SRC_ALPHA, BlendFactor.ONE);
		
		// setup shadow map texture
		int smUnit = renderMode.getMinimumTextures(); // min texture will be 1 or 2 if this pass is being used
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

		ffp.setDepthOffsetsEnabled(true);
		ffp.setDepthOffsets(0f, -5f);
		ffp.setDepthTest(Comparison.LEQUAL);
		
		RenderAtom atom;
		int count = renderAtoms.size();
		for (int i = 0; i < count; i++) {
			atom = renderAtoms.get(i);
			if (atom.receivesShadow && (shadowLight.worldBounds == null || 
										shadowLight.worldBounds.intersects(atom.worldBounds))) {
				renderAtom(atom);
			}
		}
		
		shadowFrustum = null;
	}
}
