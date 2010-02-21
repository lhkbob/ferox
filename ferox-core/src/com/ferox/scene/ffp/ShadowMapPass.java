package com.ferox.scene.ffp;

import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Renderer.DrawStyle;

public class ShadowMapPass extends AbstractFfpRenderPass {
	public ShadowMapPass(RenderMode renderMode, String vertexBinding) {
		super(renderMode, vertexBinding, null, null);
	}

	@Override
	protected void render(FixedFunctionRenderer ffp) {
		// set style to be just depth, while drawing only back faces
		ffp.setColorWriteMask(false, false, false, false);
		ffp.setDrawStyle(DrawStyle.NONE, DrawStyle.SOLID);
		// move everything backwards slightly to fix for floating errors
		ffp.setDepthOffsets(0f, 5f);
		ffp.setDepthOffsetsEnabled(true);
		
		RenderAtom r;
		int ct = renderAtoms.size();
		for (int i = 0; i < ct; i++) {
			r = renderAtoms.get(i);
			
			if (r.castsShadow) {
				// render the shape
				renderGeometry(r.geometry, r.worldTransform);
			}
		}
	}
}
