package com.ferox.scene.fx.fixed;

import com.ferox.math.Frustum;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector4f;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.FixedFunctionRenderer.TexCoord;
import com.ferox.renderer.FixedFunctionRenderer.TexCoordSource;
import com.ferox.renderer.Renderer.BlendFactor;
import com.ferox.renderer.Renderer.BlendFunction;
import com.ferox.resource.TextureImage;

public class ShadowLightPass extends AbstractFfpRenderPass {
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
		ffp.setBlendMode(BlendFunction.ADD, BlendFactor.ONE, BlendFactor.ONE);
		
		// setup shadow map texture
		int smUnit = renderMode.getMinimumTextures(); // min texture will be 1 or 2 if this pass is being used
		ffp.setTexture(smUnit, shadowMap);
		ffp.setTextureEnabled(smUnit, true);
		
		ffp.setTextureCoordGeneration(smUnit, TexCoordSource.EYE);
		
		Vector4f plane = new Vector4f();
		Matrix4f texM = shadowFrustum.getProjectionMatrix(null).mul(shadowFrustum.getViewMatrix(null));
		ffp.setTextureEyePlane(smUnit, TexCoord.S, texM.getRow(0, plane));
		ffp.setTextureEyePlane(smUnit, TexCoord.T, texM.getRow(1, plane));
		ffp.setTextureEyePlane(smUnit, TexCoord.R, texM.getRow(2, plane));
		ffp.setTextureEyePlane(smUnit, TexCoord.Q, texM.getRow(3, plane));

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
