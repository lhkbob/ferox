package com.ferox.scene.fx.fixed;

import com.ferox.math.Frustum;
import com.ferox.math.Matrix4f;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.util.Bag;

public class ShadowMapPass implements RenderPass {
	private static Matrix4f convertHand = new Matrix4f(-1f, 0f, 0f, 0f,
														0f, 1f, 0f, 0f,
														0f, 0f,-1f, 0f,
														0f, 0f, 0f, 1f);
	private final Matrix4f view;
	private final Matrix4f modelView;
	
	private final Matrix4f projection;
	
	private final FixedFunctionRenderController controller;
	
	private Bag<RenderAtom> atoms;
	private Frustum lightFrustum;
	
	public ShadowMapPass(FixedFunctionRenderController controller) {
		modelView = new Matrix4f();
		view = new Matrix4f();
		
		projection = new Matrix4f();
		
		this.controller = controller;
	}
	
	public void setPass(Frustum light, Bag<RenderAtom> atoms) {
		this.atoms = atoms;
		lightFrustum = light;
	}
	
	@Override
	public void render(Renderer renderer, RenderSurface surface) {
		if (renderer instanceof FixedFunctionRenderer) {
			// compute the view and projection matrices
			convertHand.mul(lightFrustum.getViewMatrix(view), view);
			lightFrustum.getProjectionMatrix(projection);

			FixedFunctionRenderer ffp = (FixedFunctionRenderer) renderer;
			
			// set style to be just depth, while drawing only back faces
			ffp.setColorWriteMask(false, false, false, false);
			ffp.setDrawStyle(DrawStyle.NONE, DrawStyle.SOLID);
			
			// set projection matrix
			ffp.setProjectionMatrix(projection);
			
			ffp.setVertexBinding(controller.getVertexBinding());
			ffp.setNormalBinding(null);
			ffp.setTextureCoordinateBinding(0, null);
			
			RenderAtom r;
			int ct = atoms.size();
			for (int i = 0; i < ct; i++) {
				r = atoms.get(i);

				if (r.castsShadow) {
					// compute and set modelview matrix, and then render
					view.mul(r.worldTransform, modelView);
					ffp.setModelViewMatrix(modelView);
					ffp.render(r.geometry);
				}
			}
		}
	}
}
