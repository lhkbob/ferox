package com.ferox.scene.fx.impl.fixed;

import com.ferox.effect.DepthTest;
import com.ferox.effect.Effect;
import com.ferox.effect.Texture;
import com.ferox.effect.Effect.PixelTest;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.RenderSurface;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.View;
import com.ferox.resource.Geometry;
import com.ferox.util.Bag;
import com.ferox.util.geom.Rectangle;

public class LightRenderPass implements RenderPass {
	// this pass will render all objects influenced by the shadow light
	//  - that is also set to receive shadows
	// it will have the shadow map depth map attached
	private RenderAtom renderAtom;
	private View ortho;
	
	public LightRenderPass(FixedFunctionAttachedSurface surface) {
		RenderSurface s = surface.getSurface();
		
		ortho = new View();
		ortho.getFrustum().setOrthogonalProjection(true);
		ortho.getFrustum().setFrustum(0, s.getWidth(), 0, s.getHeight(), -1, 1);
		
		Geometry square = new Rectangle(0, 128, 0, 128);
		Transform t = new Transform(new Vector3f(s.getWidth() - 128, 0f, 0f));
		
		Texture depth = new Texture(surface.getShadowMap().getDepthBuffer());
		DepthTest d = new DepthTest();
		d.setTest(PixelTest.ALWAYS);
		
		Bag<Effect> e = new Bag<Effect>();
		e.add(depth);
		e.add(d);
		
		renderAtom = new RenderAtom(t, square, e, null);
	}
	
	@Override
	public View preparePass() {
		ortho.updateView();
		return ortho;
	}

	@Override
	public void render(Renderer renderer, View view) {
		renderer.renderAtom(renderAtom);
	}
}
