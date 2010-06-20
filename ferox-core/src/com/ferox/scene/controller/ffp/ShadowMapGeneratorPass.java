package com.ferox.scene.controller.ffp;

import com.ferox.entity.Entity;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.Surface;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.util.Bag;

public class ShadowMapGeneratorPass extends AbstractFfpRenderPass {
	public ShadowMapGeneratorPass(RenderConnection connection, int maxMaterialTexUnits, String vertexBinding) {
		super(connection, maxMaterialTexUnits, vertexBinding, null, null);
	}

	@Override
	protected void render(FixedFunctionRenderer ffp) {
		// set style to be just depth, while drawing only back faces
//		ffp.setColorWriteMask(false, false, false, false);
		ffp.setDrawStyle(DrawStyle.NONE, DrawStyle.SOLID);
		
		// move everything backwards slightly to account for floating errors
		ffp.setDepthOffsets(0f, 5f);
		ffp.setDepthOffsetsEnabled(true);
		
		Bag<Entity> shadowAtoms = connection.getShadowCastingEntities();
		int ct = shadowAtoms.size();
		for (int i = 0; i < ct; i++) {
		    // we bypass the material setup normally used for render atoms
		    renderGeometry(shadowAtoms.get(i));
		}
	}

	@Override
	protected Frustum getFrustum() {
	    return connection.getShadowFrustum();
	}

    @Override
    protected void configureViewport(FixedFunctionRenderer renderer, Surface surface) {
        renderer.setViewport(0, 0, surface.getWidth(), surface.getHeight());
        renderer.clear(true, true, true, surface.getClearColor(), surface.getClearDepth(), surface.getClearStencil());
    }

    @Override
    protected void notifyPassBegin() {
        connection.notifyShadowMapBegin();
    }

    @Override
    protected void notifyPassEnd() {
        connection.notifyShadowMapEnd();
    }
}
