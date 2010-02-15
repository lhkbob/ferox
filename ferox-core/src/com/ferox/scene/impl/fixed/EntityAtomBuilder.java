package com.ferox.scene.impl.fixed;

import com.ferox.math.Color4f;
import com.ferox.math.Matrix4f;
import com.ferox.resource.Geometry;
import com.ferox.scene.BlinnPhongLightingModel;
import com.ferox.scene.Renderable;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ShadowCaster;
import com.ferox.scene.ShadowReceiver;
import com.ferox.scene.Shape;
import com.ferox.scene.SolidLightingModel;
import com.ferox.scene.TexturedMaterial;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.ComponentId;
import com.ferox.util.entity.Entity;
import com.ferox.util.geom.Box;

@SuppressWarnings("unchecked")
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
	
	
	
	private final Geometry defaultGeometry;
	private final Color4f defaultDiffuse;
	private final Color4f defaultBlack;
	
	public EntityAtomBuilder() {
		defaultGeometry = new Box(1f);
		defaultDiffuse = new Color4f(.5f, .5f, .5f, 1f);
		defaultBlack = new Color4f(0f, 0f, 0f, 1f);
	}
	
	public RenderAtom build(Entity e, RenderAtom atom) {
		SceneElement se = e.get(SE_ID);
		Renderable r = e.get(R_ID);
		
		if (r == null)
			return null; // can't make a valid render atom
		if (atom == null)
			atom = new RenderAtom();
		
		// renderable
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
		
		atom.castsShadow = e.get(SC_ID) != null;
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
		
		return atom;
	}
	
	public LightAtom build(Entity e, LightAtom atom) {
		
	}
}
