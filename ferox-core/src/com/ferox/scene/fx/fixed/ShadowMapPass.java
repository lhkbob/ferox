package com.ferox.scene.fx.fixed;

import com.ferox.math.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.util.Bag;

public class ShadowMapPass extends AbstractFfpRenderPass {
	private final FixedFunctionRenderController controller;
	
	private Bag<RenderAtom> atoms;
	private Frustum lightFrustum;
	
	public ShadowMapPass(FixedFunctionRenderController controller) {
		this.controller = controller;
	}
	
	public void setPass(Frustum light, Bag<RenderAtom> atoms) {
		this.atoms = atoms;
		lightFrustum = light;
	}

	@Override
	protected void render(FixedFunctionRenderer ffp) {
		// set up transforms
		setTransforms(lightFrustum.getProjectionMatrix(null), lightFrustum.getViewMatrix(null));
		
		// set style to be just depth, while drawing only back faces
		ffp.setColorWriteMask(false, false, false, false);
		ffp.setDrawStyle(DrawStyle.NONE, DrawStyle.SOLID);
		// move everything backwards slightly to fix for floating errors
		ffp.setDepthOffsets(0f, -5f);
		ffp.setDepthOffsetsEnabled(true);
		
		// setup the geometry config to just send the vertices over
		ffp.setVertexBinding(controller.getVertexBinding());
		ffp.setNormalBinding(null);
		ffp.setTextureCoordinateBinding(0, null);
		
		RenderAtom r;
		int ct = atoms.size();
		for (int i = 0; i < ct; i++) {
			r = atoms.get(i);
			
			if (r.castsShadow) {
				// render the shape
				render(r.geometry, r.worldTransform);
			}
		}
	}
}
