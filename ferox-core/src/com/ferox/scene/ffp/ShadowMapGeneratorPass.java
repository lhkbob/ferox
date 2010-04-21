package com.ferox.scene.ffp;

import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.util.Bag;

public class ShadowMapGeneratorPass extends AbstractFfpRenderPass {
	public ShadowMapGeneratorPass(int maxMaterialTexUnits, String vertexBinding) {
		super(maxMaterialTexUnits, vertexBinding, null, null);
	}

	@Override
	protected void render(FixedFunctionRenderer ffp) {
		// set style to be just depth, while drawing only back faces
		ffp.setColorWriteMask(false, false, false, false);
		ffp.setDrawStyle(DrawStyle.NONE, DrawStyle.SOLID);
		
		// move everything backwards slightly to account for floating errors
		ffp.setDepthOffsets(0f, 5f);
		ffp.setDepthOffsetsEnabled(true);
		
		ShadowAtom s;
		Bag<ShadowAtom> shadowAtoms = renderDescr.getShadowAtoms();
		int ct = shadowAtoms.size();
		for (int i = 0; i < ct; i++) {
			s = shadowAtoms.get(i);
			// we bypass the material setup normally used for render atoms
			renderGeometry(s.geometry, s.worldTransform);
		}
	}

	@Override
	protected Frustum getFrustum() {
		// if this returns null, AbstractFppRenderPass will automatically
		// skip rendering, so we don't need to check for null in render()
		return (renderDescr == null ? null : renderDescr.getShadowMapFrustum());
	}

	@Override
	protected boolean initViewport(FixedFunctionRenderer renderer) {
		// do nothing, this pass should be cleared all the time
		// and we want to use the whole texture surface's viewport
		return false;
	}
}
