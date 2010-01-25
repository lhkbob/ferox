package com.ferox.scene.fx.fixed;

import com.ferox.math.Matrix4f;
import com.ferox.renderer.FixedFunctionRenderer;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;

public abstract class AbstractFfpRenderPass implements RenderPass {
	protected static Matrix4f convertHand = new Matrix4f(-1f, 0f, 0f, 0f,
														  0f, 1f, 0f, 0f,
														  0f, 0f,-1f, 0f,
														  0f, 0f, 0f, 1f);
	private final Matrix4f view;
	private final Matrix4f modelView;

	private FixedFunctionRenderer renderer;
	
	public AbstractFfpRenderPass() {
		modelView = new Matrix4f();
		view = new Matrix4f();
	}

	@Override
	public void render(Renderer renderer, RenderSurface surface) {
		if (renderer instanceof FixedFunctionRenderer) {
			this.renderer = (FixedFunctionRenderer) renderer;
			render(this.renderer);
			this.renderer = null;
		}
	}
	
	protected void setTransforms(Matrix4f projection, Matrix4f view) {
		renderer.setProjectionMatrix(projection);
		
		// convert the view transform to so that the direction now goes
		// down the negative z-axis
		convertHand.mul(view, this.view);
		
		// must also set the view so that lighting works
		// as expected, even though we perform the modelview compute on the cpu
		renderer.setModelViewMatrix(this.view);
	}
	
	protected void render(Geometry geom, Matrix4f toWorld) {
		view.mul(toWorld, modelView);
		renderer.setModelViewMatrix(modelView);
		renderer.render(geom);
	}
	
	protected abstract void render(FixedFunctionRenderer renderer);
}
