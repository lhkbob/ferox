package com.ferox.scene.ffp;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.Frustum;
import com.ferox.resource.Geometry;
import com.ferox.scene.AmbientLight;
import com.ferox.scene.BlinnPhongLightingModel;
import com.ferox.scene.DirectionLight;
import com.ferox.scene.Renderable;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ShadowCaster;
import com.ferox.scene.ShadowReceiver;
import com.ferox.scene.Shape;
import com.ferox.scene.SolidLightingModel;
import com.ferox.scene.SpotLight;
import com.ferox.scene.TexturedMaterial;
import com.ferox.scene.ffp.LightAtom.Type;
import com.ferox.scene.ffp.RenderConnection.Stream;
import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Entity;
import com.ferox.util.geom.Box;

public class EntityAtomBuilder {
	// Components for RenderAtoms
	private static ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
	private static ComponentId<Renderable> R_ID = Component.getComponentId(Renderable.class);
	private static ComponentId<BlinnPhongLightingModel> BPL_ID = Component.getComponentId(BlinnPhongLightingModel.class);
	private static ComponentId<SolidLightingModel> SL_ID = Component.getComponentId(SolidLightingModel.class);
	private static ComponentId<Shape> S_ID = Component.getComponentId(Shape.class);
	private static ComponentId<TexturedMaterial> TM_ID = Component.getComponentId(TexturedMaterial.class);

	private static ComponentId<ShadowCaster> SC_ID = Component.getComponentId(ShadowCaster.class);
	private static ComponentId<ShadowReceiver> SR_ID = Component.getComponentId(ShadowReceiver.class);
	
	// Components for LightAtoms
	private static ComponentId<AmbientLight> LA_ID = Component.getComponentId(AmbientLight.class);
	private static ComponentId<DirectionLight> LD_ID = Component.getComponentId(DirectionLight.class);
	private static ComponentId<SpotLight> LS_ID = Component.getComponentId(SpotLight.class);
	
	
	private final Geometry defaultGeometry;
	private final Color4f defaultDiffuse;
	private final Color4f defaultBlack;
	
	public EntityAtomBuilder() {
		defaultGeometry = new Box(1f);
		defaultDiffuse = new Color4f(.5f, .5f, .5f, 1f);
		defaultBlack = new Color4f(0f, 0f, 0f, 1f);
	}
	
	public ShadowAtom buildShadowAtom(Entity e, Frustum f, Stream<ShadowAtom> stream) {
		Renderable r = e.get(R_ID);
		if (r != null && (e.get(SC_ID) != null || e.getMeta(r, SC_ID) != null)) {
			SceneElement se = e.get(SE_ID);
			if (!checkFrustum(se, f))
				return null; // not within view
			
			ShadowAtom atom = stream.newInstance();
			Shape shape = e.get(S_ID);
			atom.geometry = (shape == null ? defaultGeometry : shape.getGeometry());
			
			atom.worldTransform = (se == null ? new Matrix4f() : se.getTransform().get(atom.worldTransform));
			
			stream.push(atom);
			return atom;
		} else
			return null;
	}
	
	public RenderAtom buildRenderAtom(Entity e, Frustum f, Stream<RenderAtom> stream) {
		Renderable r = e.get(R_ID);
		if (r != null) {
			SceneElement se = e.get(SE_ID);
			if (!checkFrustum(se, f))
				return null; // not within view
			
			RenderAtom atom = stream.newInstance();
			atom.front = r.getDrawStyleFront();
			atom.back = r.getDrawStyleBack();
			
			if (se != null) {
				// scene element
				atom.worldBounds = se.getWorldBounds();
				atom.worldTransform = se.getTransform().get(atom.worldTransform);
			} else {
				atom.worldBounds = null;
				atom.worldTransform = new Matrix4f();
			}
			
			// shape
			Shape s = e.get(S_ID);
			atom.geometry = (s != null ? s.getGeometry() : defaultGeometry);
			
			// lighting
			SolidLightingModel sl = e.get(SL_ID);
			BlinnPhongLightingModel bl = e.get(BPL_ID);
			if (bl != null) {
				atom.ambient = bl.getAmbient();
				atom.diffuse = bl.getDiffuse();
				atom.specular = bl.getSpecular();
				atom.shininess = Math.min(128, bl.getShininess());
				
				atom.lit = true;
			} else if (sl != null) {
				atom.ambient = sl.getColor();
				atom.diffuse = sl.getColor();
				atom.specular = sl.getColor();
				
				atom.lit = false;
			} else {
				atom.ambient = defaultBlack;
				atom.diffuse = defaultDiffuse;
				atom.specular = defaultBlack;
				atom.shininess = 1;
				
				atom.lit = true;
			}
			
			atom.receivesShadow = e.get(SR_ID) != null;
			
			// texturing
			TexturedMaterial tm = e.get(TM_ID);
			if (tm != null) {
				atom.primaryTexture = tm.getPrimaryTexture();
				atom.decalTexture = tm.getDecalTexture();
			} else {
				atom.primaryTexture = null;
				atom.decalTexture = null;
			}
			
			stream.push(atom);
			return atom;
		} else
			return null;
	}
	
	public LightAtom buildAmbientLightAtom(Entity e, Frustum f, Stream<LightAtom> stream) {
		AmbientLight al = e.get(LA_ID);
		if (al != null) {
			SceneElement se = e.get(SE_ID);
			if (!checkFrustum(se, f))
				return null; // not within view
			
			LightAtom atom = stream.newInstance();
			atom.type = Type.AMBIENT;
			atom.worldBounds = (se == null ? null : se.getWorldBounds());
			
			atom.diffuse = al.getColor();
			atom.specular = atom.diffuse;
			
			atom.castsShadows = false;
			
			atom.direction = null;
			atom.position = null;
			atom.cutoffAngle = 0f;
			
			atom.constCutoff = 1f;
			atom.linCutoff = 0f;
			atom.quadCutoff = 0f;
			
			stream.push(atom);
			return atom;
		} else
			return null;
	}
	
	public LightAtom buildSpotLightAtom(Entity e, Frustum f, Stream<LightAtom> stream) {
		SpotLight sl = e.get(LS_ID);
		if (sl != null) {
			SceneElement se = e.get(SE_ID);
			if (!checkFrustum(se, f))
				return null; // not within view
			
			LightAtom atom = stream.newInstance();
			atom.type = Type.SPOTLIGHT;
			atom.worldBounds = (se == null ? null : se.getWorldBounds());
			
			atom.diffuse = sl.getColor();
			atom.specular = atom.diffuse;
			
			atom.castsShadows = e.get(SC_ID) != null || e.getMeta(sl, SC_ID) != null;
			atom.direction = sl.getDirection().normalize(atom.direction);
			if (atom.position == null)
				atom.position = new Vector3f(sl.getPosition());
			else
				atom.position.set(sl.getPosition());
			
			atom.cutoffAngle = sl.getCutoffAngle();
			atom.constCutoff = 1f;
			atom.linCutoff = 0f;
			atom.quadCutoff = 0f;
			
			stream.push(atom);
			return atom;
		} else
			return null;
	}
	
	public LightAtom buildDirectionLightAtom(Entity e, Frustum f, Stream<LightAtom> stream) {
		DirectionLight dl = e.get(LD_ID);
		if (dl != null) {
			SceneElement se = e.get(SE_ID);
			if (!checkFrustum(se, f))
				return null; // not within view
			
			LightAtom atom = stream.newInstance();
			atom.type = Type.DIRECTION;
			atom.worldBounds = (se == null ? null : se.getWorldBounds());
			
			atom.diffuse = dl.getColor();
			atom.specular = atom.diffuse;
			
			atom.castsShadows = e.get(SC_ID) != null || e.getMeta(dl, SC_ID) != null;
			atom.direction = dl.getDirection().normalize(atom.direction);
			
			atom.position = null;
			atom.cutoffAngle = 0f;
			
			atom.constCutoff = 1f;
			atom.linCutoff = 0f;
			atom.quadCutoff = 0f;
			
			stream.push(atom);
			return atom;
		} else
			return null;
	}
	
	private boolean checkFrustum(SceneElement se, Frustum f) {
		if (se == null || f == null)
			return true;
		return se.isVisible(f);
	}
}
