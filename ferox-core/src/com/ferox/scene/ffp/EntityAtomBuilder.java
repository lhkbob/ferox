package com.ferox.scene.ffp;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.resource.Geometry;
import com.ferox.scene.Renderable;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ShadowCaster;
import com.ferox.scene.ShadowReceiver;
import com.ferox.scene.Shape;
import com.ferox.scene.ffp.LightAtom.Type;
import com.ferox.scene.ffp.RenderConnection.Stream;
import com.ferox.scene.fx.BlinnPhongLightingModel;
import com.ferox.scene.fx.SolidLightingModel;
import com.ferox.scene.fx.TexturedMaterial;
import com.ferox.scene.light.AmbientLight;
import com.ferox.scene.light.DirectionLight;
import com.ferox.scene.light.SpotLight;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.ComponentId;
import com.ferox.util.entity.Entity;
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
	
	public void buildShadowAtoms(Entity e, Stream<ShadowAtom> stream) {
		Renderable r = e.get(R_ID);
		if (r != null) {
			ShadowAtom atom = stream.newInstance();
			Shape shape = e.get(S_ID);
			atom.geometry = (shape == null ? defaultGeometry : shape.getGeometry());
			
			SceneElement se = e.get(SE_ID);
			atom.worldTransform = (se == null ? new Matrix4f() : se.getTransform().get(atom.worldTransform));
			
			stream.push(atom);
		}
	}
	
	public void buildRenderAtoms(Entity e, Stream<RenderAtom> stream) {
		Renderable r = e.get(R_ID);
		if (r != null) {
			RenderAtom atom = stream.newInstance();
			atom.front = r.getDrawStyleFront();
			atom.back = r.getDrawStyleBack();
			
			SceneElement se = e.get(SE_ID);
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
				atom.shininess = Math.max(128, bl.getShininess());
				
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
		} // else this version doesn't support non-Renderable RenderAtoms
	}
	
	public void buildLightAtoms(Entity e, Stream<LightAtom> stream) {
		SceneElement se = e.get(SE_ID);
		ShadowCaster sc = e.get(SC_ID);
		
		AmbientLight al = e.get(LA_ID);
		if (al != null) {
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
		}
		
		DirectionLight dl = e.get(LD_ID);
		if (dl != null) {
			LightAtom atom = stream.newInstance();
			atom.type = Type.DIRECTION;
			atom.worldBounds = (se == null ? null : se.getWorldBounds());
			
			atom.diffuse = dl.getColor();
			atom.specular = atom.diffuse;
			
			atom.castsShadows = sc != null;
			atom.direction = dl.getDirection().normalize(atom.direction);
			
			atom.position = null;
			atom.cutoffAngle = 0f;
			
			atom.constCutoff = 1f;
			atom.linCutoff = 0f;
			atom.quadCutoff = 0f;
			
			stream.push(atom);
		}
		
		SpotLight sl = e.get(LS_ID);
		if (sl != null) {
			LightAtom atom = stream.newInstance();
			atom.type = Type.SPOTLIGHT;
			atom.worldBounds = (se == null ? null : se.getWorldBounds());
			
			atom.diffuse = sl.getColor();
			atom.specular = atom.diffuse;
			
			atom.castsShadows = sc != null;
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
		}
	}
}
