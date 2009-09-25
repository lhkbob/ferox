package com.ferox.scene.fx.impl.fixed;

import com.ferox.effect.DirectionLight;
import com.ferox.effect.Effect;
import com.ferox.effect.PolygonStyle;
import com.ferox.effect.PolygonStyle.DrawStyle;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.View;
import com.ferox.resource.Geometry;
import com.ferox.scene.SceneElement;
import com.ferox.scene.Shape;
import com.ferox.util.Bag;

public class ShadowMapRenderPass implements RenderPass {
	private static final Object KEY = new Object();
	private static final Transform DUMMY_TRANS = new Transform();
	private static final Geometry DUMMY_GEOM = new NullGeometry();
	
	private final FixedFunctionAttachedSurface surface;
	private final RenderAtom atom;
	private final View lightView;
	
	private Bag<SceneElement> shadowCasters;
	
	public ShadowMapRenderPass(FixedFunctionAttachedSurface surface) {
		Bag<Effect> effects = new Bag<Effect>();
		PolygonStyle poly = new PolygonStyle(DrawStyle.NONE, DrawStyle.SOLID);
		effects.add(poly);
		
		atom = new RenderAtom(DUMMY_TRANS, DUMMY_GEOM, effects, KEY);
		lightView = new View();
		this.surface = surface;
	}
	
	@Override
	public View preparePass() {
		View view = surface.getView();
		DirectionLight light = surface.getShadowCastingLight();

		if (light != null) {
			detectFrustum(light, view);
			//shadowCasters = surface.getCompositor().query(lightView.getFrustum(), Shape.class);
			//return lightView;
			shadowCasters = surface.getCompositor().query(view.getFrustum(), Shape.class);
			return view;
		} else {
			// no shadow casting light - we'll just clear the buffer then
			return null;
		}
	}

	private void detectFrustum(DirectionLight light, View camera) {
		Vector3f z = light.getDirection().normalize(lightView.getDirection());
		Vector3f y = camera.getUp().ortho(z, lightView.getUp()).normalize();
		Vector3f x = y.cross(z, null); // new vector here
		
		Vector3f minF = new Vector3f(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE);
		Vector3f maxF = new Vector3f(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE);
		
		Vector3f f = new Vector3f();
		// now project the 8 corners of the frustum onto x,y,z and store the min/max values
		// into minF and maxF
		
		// use these for the frustum values and determine the lightView position
		
		
		lightView.getFrustum().setOrthogonalProjection(true);
		//lightView.getFrustum().setFrustum(minF.x, maxF.x, minF.y, maxF.y, minF.z, maxF.z);
		lightView.updateView();
	}
	
	@Override
	public void render(Renderer renderer, View view) {
		FixedFunctionAppearance a;
		Shape s;
		int size = shadowCasters.size();
		for (int i = 0; i < size; i++) {
			s = (Shape) shadowCasters.get(i);
			a = surface.getCompositor().get(s.getAppearance());
			
			if (a.isShadowCaster()) {
				atom.setGeometry(s.getGeometry(), KEY);
				atom.setTransform(s.getWorldTransform(), KEY);
				renderer.renderAtom(atom);
			}
		}
		
		// reset everything - okay to do it here, since each surface is
		// only added to 1 pass
		shadowCasters = null;
		atom.setGeometry(DUMMY_GEOM, KEY);
		atom.setTransform(DUMMY_TRANS, KEY);
	}
}
