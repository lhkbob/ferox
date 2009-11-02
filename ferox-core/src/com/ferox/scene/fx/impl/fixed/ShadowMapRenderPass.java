package com.ferox.scene.fx.impl.fixed;

import com.ferox.math.Frustum;
import com.ferox.math.Matrix3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.RenderPass;
import com.ferox.renderer.Renderer;
import com.ferox.resource.Geometry;
import com.ferox.scene.SceneElement;
import com.ferox.scene.Shape;
import com.ferox.shader.DirectionLight;
import com.ferox.shader.Effect;
import com.ferox.shader.PolygonStyle;
import com.ferox.shader.View;
import com.ferox.shader.PolygonStyle.DrawStyle;
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
			shadowCasters = surface.getCompositor().query(lightView.getFrustum(), Shape.class);
			return lightView;
		} else {
			// no shadow casting light - we'll just clear the buffer then
			return null;
		}
	}

	private void detectFrustum(DirectionLight light, View camera) {
		Frustum cf = camera.getFrustum();
		float fr = cf.getFrustumRight();
		float fl = cf.getFrustumLeft();
		float fb = cf.getFrustumBottom();
		float ft = cf.getFrustumTop();
		float fn = cf.getFrustumNear();
		float ff = cf.getFrustumFar();
		
		Vector3f z = light.getDirection().normalize(lightView.getDirection());
		Vector3f y = camera.getUp().ortho(z, lightView.getUp()).normalize();
		Vector3f x = y.cross(z, null); // new vector here
		
		Matrix3f v = camera.getViewTransform().getRotation();
		Matrix3f l = new Matrix3f();
		l.setCol(0, x);
		l.setCol(1, y);
		l.setCol(2, z);
		
		
		// compute the frustum corners in camera space
		Vector3f nbr = new Vector3f(fr, fb, fn);
		Vector3f nbl = new Vector3f(fl, fb, fn);
		Vector3f ntr = new Vector3f(fr, ft, fn);
		Vector3f ntl = new Vector3f(fl, ft, fn);
		Vector3f fbr = new Vector3f();
		Vector3f fbl = new Vector3f();
		Vector3f ftr = new Vector3f();
		Vector3f ftl = new Vector3f();
		
		if (cf.isOrthogonalProjection()) {
			fbr.set(fr, fb, ff);
			fbl.set(fl, fb, ff);
			ftr.set(fr, ft, ff);
			ftl.set(fl, ft, ff);
		} else {
			fbr.set(ff / fn * fr, ff / fn * fb, ff);
			fbl.set(ff / fn * fl, ff / fn * fb, ff);
			ftr.set(ff / fn * fr, ff / fn * ft, ff);
			ftl.set(ff / fn * fl, ff / fn * ft, ff);
		}
		Vector3f camCenter = nbr.add(nbl, null).add(ntr).add(ntl).
						     add(fbr).add(fbl).add(ftr).add(ftl).scale(1 / 8f);
		
		// transform frustum corners to light space
		l.mulPre(v.mulPre(nbr.sub(camCenter)));
		l.mulPre(v.mulPre(nbl.sub(camCenter)));
		l.mulPre(v.mulPre(ntr.sub(camCenter)));
		l.mulPre(v.mulPre(ntl.sub(camCenter)));
		l.mulPre(v.mulPre(fbr.sub(camCenter)));
		l.mulPre(v.mulPre(fbl.sub(camCenter)));
		l.mulPre(v.mulPre(ftr.sub(camCenter)));
		l.mulPre(v.mulPre(ftl.sub(camCenter)));
		
		// determine min and max frustum values in light space
		Vector3f minF = new Vector3f();
		Vector3f maxF = new Vector3f();
		minF.set(Math.min(nbr.x, Math.min(nbl.x, Math.min(ntr.x, Math.min(ntl.x, Math.min(fbr.x, Math.min(fbl.x, Math.min(ftr.x, ftl.x))))))), 
			     Math.min(nbr.y, Math.min(nbl.y, Math.min(ntr.y, Math.min(ntl.y, Math.min(fbr.y, Math.min(fbl.y, Math.min(ftr.y, ftl.y))))))),
			     Math.min(nbr.z, Math.min(nbl.z, Math.min(ntr.z, Math.min(ntl.z, Math.min(fbr.z, Math.min(fbl.z, Math.min(ftr.z, ftl.z))))))));
		maxF.set(Math.max(nbr.x, Math.max(nbl.x, Math.max(ntr.x, Math.max(ntl.x, Math.max(fbr.x, Math.max(fbl.x, Math.max(ftr.x, ftl.x))))))), 
				 Math.max(nbr.y, Math.max(nbl.y, Math.max(ntr.y, Math.max(ntl.y, Math.max(fbr.y, Math.max(fbl.y, Math.max(ftr.y, ftl.y))))))),
				 Math.max(nbr.z, Math.max(nbl.z, Math.max(ntr.z, Math.max(ntl.z, Math.max(fbr.z, Math.max(fbl.z, Math.max(ftr.z, ftl.z))))))));
		
		Vector3f p = lightView.getLocation().set((minF.x + maxF.x) / 2f, (minF.y + maxF.y) / 2f, minF.z);
		lightView.getFrustum().setOrthogonalProjection(true);
		lightView.getFrustum().setFrustum(minF.x - p.x, maxF.x - p.x, minF.y - p.y, maxF.y - p.y, 0, maxF.z - p.z);
		
		l.mul(p).add(camCenter).add(camera.getLocation());
		
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
