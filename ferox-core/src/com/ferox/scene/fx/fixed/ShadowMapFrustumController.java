package com.ferox.scene.fx.fixed;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ferox.math.Color4f;
import com.ferox.math.Frustum;
import com.ferox.math.Matrix3f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.scene.DirectedLight;
import com.ferox.scene.Light;
import com.ferox.scene.SceneController;
import com.ferox.scene.SceneElement;
import com.ferox.scene.fx.ViewNode;
import com.ferox.util.Bag;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.Controller;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;

public class ShadowMapFrustumController extends Controller {
	private static final int LT_ID = Component.getTypeId(Light.class);
	private static final int DR_ID = Component.getTypeId(DirectedLight.class);
	private static final int SE_ID = Component.getTypeId(SceneElement.class);
	private static final int VN_ID = Component.getTypeId(ViewNode.class);
	
	private float maxDistance;
	private float focusDistance;
	
	private Map<ViewNode, LightAndFrustum> lights;
	private Map<Frustum, Bag<Entity>> visibleEntities;
	
	public ShadowMapFrustumController(EntitySystem system) {
		super(system);
		
		lights = new HashMap<ViewNode, LightAndFrustum>();
		visibleEntities = new HashMap<Frustum, Bag<Entity>>();
		
		setMaximumLightDistance(500f);
		setFocalDistance(10f);
	}
	
	public float getMaximumLightDistance() {
		return maxDistance;
	}
	
	public void setMaximumLightDistance(float maxDistance) {
		if (maxDistance <= 0f)
			throw new IllegalArgumentException("maxDistance must be greater than 0, not: " + maxDistance);
		this.maxDistance = maxDistance;
	}
	
	public float getFocalDistance() {
		return focusDistance;
	}
	
	public void setFocalDistance(float focusDistance) {
		if (focusDistance <= 0f)
			throw new IllegalArgumentException("focusDistance must be greater than 0, not: " + focusDistance);
		this.focusDistance = focusDistance;
	}
	
	public Frustum getShadowMapFrustum(ViewNode view) {
		LightAndFrustum lf = lights.get(view);
		return (lf == null ? null : lf.lightFrustum);
	}
	
	public Entity getShadowMapLight(ViewNode view) {
		LightAndFrustum lf = lights.get(view);
		return (lf == null ? null : lf.light);
	}
	
	public Bag<Entity> getShadowMapEntities(Frustum frustum) {
		return visibleEntities.get(frustum);
	}

	@Override
	public void process() {
		SceneController scene = system.getController(SceneController.class);
		
		Map<ViewNode, LightAndFrustum> newLights = new HashMap<ViewNode, LightAndFrustum>();
		Map<Frustum, Bag<Entity>> pvs = new HashMap<Frustum, Bag<Entity>>();
		
		ViewNode viewNode;
		LightAndFrustum lf;
		Iterator<Entity> vi = system.iterator(VN_ID);
		while(vi.hasNext()) {
			viewNode = (ViewNode) vi.next().get(VN_ID);
			lf = lights.get(viewNode);
			if (lf == null)
				lf = new LightAndFrustum();
			
			lf.light = chooseLight(viewNode.getFrustum(), lf.light);
			updateLightFrustum(viewNode.getFrustum(), lf);
			if (scene != null)
				computeVisibility(scene, lf.lightFrustum, pvs);

			newLights.put(viewNode, lf);
		}
		
		lights = newLights;
		visibleEntities = pvs;
	}
	
	private void computeVisibility(SceneController scene, Frustum f, Map<Frustum, Bag<Entity>> pvs) {
		Bag<Entity> result = visibleEntities.get(f);
		if (f == null)
			result = new Bag<Entity>();
		
		scene.query(f, result);
		pvs.put(f, result);
		
		// modify all scene elements to be potentially visible
		SceneElement se;
		int ct = result.size();
		for (int i = 0; i < ct; i++) {
			se = (SceneElement) result.get(i).get(SE_ID);
			if (se != null)
				se.setPotentiallyVisible(true);
		}
	}
	
	private Matrix3f getRotation(Matrix4f m) {
		Matrix3f r = new Matrix3f(m.m00, m.m01, m.m02, 
								  m.m10, m.m11, m.m12, 
								  m.m20, m.m21, m.m22);
		return r;
	}
	
	private void updateLightFrustum(Frustum cf, LightAndFrustum lf) {
		Frustum lightView = lf.lightFrustum;
		Vector3f lightDir = ((DirectedLight) lf.light.get(DR_ID)).getDirection();
		
		float fr = cf.getFrustumRight();
		float fl = cf.getFrustumLeft();
		float fb = cf.getFrustumBottom();
		float ft = cf.getFrustumTop();
		float fn = cf.getFrustumNear();
		float ff = cf.getFrustumFar();
		
		// compute basis for the shadow map projection
		Vector3f z = lightDir.normalize(lightView.getDirection());
		Vector3f y = cf.getUp().ortho(z, lightView.getUp()).normalize();
		Vector3f x = y.cross(z, null);
		
		
		Matrix3f v = getRotation(cf.getViewMatrix(null));
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
		Vector3f camCenter = nbr.add(nbl, null).add(ntr).add(ntl)
						        .add(fbr).add(fbl).add(ftr).add(ftl).scale(1 / 8f);
		
		// transform frustum corners to light space by computing: L^-1 * V^-1 * (corner - camCenter)
		// since L and V are rotation matrices, the corners are being rotated about camCenter
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
		
		// configure the frustum finally
		Vector3f p = lightView.getLocation().set((minF.x + maxF.x) / 2f, (minF.y + maxF.y) / 2f, minF.z);
		lightView.setOrthogonalProjection(true);
		lightView.setFrustum(minF.x - p.x, maxF.x - p.x, minF.y - p.y, maxF.y - p.y, 0, maxF.z - p.z);
		
		// transform lightView's position back into world space
		l.mul(p).add(camCenter).add(cf.getLocation());
		
		// update the frustum
		lightView.updateFrustumPlanes();
	}
	
	private Entity chooseLight(Frustum view, Entity oldLight) {
		float weight = 0f;
		Entity node = null;
		
		Entity l;
		float w;
		Iterator<Entity> it = system.iterator(DR_ID);
		while(it.hasNext()) {
			l = it.next();
			if (l.get(LT_ID) != null) {
				w = calculateLightWeight(l, l == oldLight, view);
				if (w > weight) {
					node = l;
					weight = w;
				}
			}
		}
		
		return node;
	}
	
	private float calculateLightWeight(Entity e, boolean old, Frustum view) {
		Light l = (Light) e.get(LT_ID);
		Vector3f lightDir = ((DirectedLight) e.get(DR_ID)).getDirection();
		SceneElement se = (SceneElement) e.get(SE_ID);
		if (se != null)
			lightDir = se.getTransform().transform(lightDir, null);
		
		// [0, 1] - bonues lights that are brighter
		Color4f c = l.getColor();
		float brightness = (c.getRed() + c.getGreen() + c.getBlue()) / 3f;
		
		// [0, 1] - bonuses lights that are in line with the up direction
		float direction = (1 + view.getUp().dot(lightDir)) / 2f;
		
		// [0, 1] - bonuses lights that are closer to the focus point
		float position = .5f; // .5 is default for infinite direction lights w/o location
		if (se != null) {
			Vector3f focus = view.getDirection().scaleAdd(focusDistance, view.getLocation(), lightDir);
			position = focus.sub(se.getTransform().getTranslation()).length();
			position = Math.max(maxDistance - position, 0f) / maxDistance;
		}
		
		// [0, 1] - bonuses the previous shadow light to prevent flickering
		float bonus = (old ? 1f : 0f);
		
		// [0, 1] - average out everything
		return (brightness + direction + position + bonus) / 4f;
	}
	
	private static class LightAndFrustum {
		Entity light;
		final Frustum lightFrustum = new Frustum(60f, 1f, .1f, 100f);
	}
}
