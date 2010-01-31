package com.ferox.scene.fx.fixed;

import com.ferox.math.Color4f;
import com.ferox.scene.fx.BlinnPhongLightingModel;
import com.ferox.scene.fx.fixed.EntityAtomBuilder.AtomModifier;
import com.ferox.util.entity.Component;

public class BlinnPhongAtomModifier implements AtomModifier<BlinnPhongLightingModel> {
	private static final int BL_ID = Component.getTypeId(BlinnPhongLightingModel.class);
	private static final Color4f defaultColor = new Color4f(.8f, .8f, .8f, 1f);
	
	@Override
	public int getComponentType() {
		return BL_ID;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom, BlinnPhongLightingModel component) {
		if (atom == null)
			atom = new RenderAtom();
		atom.specular = component.getSpecular();
		atom.diffuse = component.getDiffuse();
		atom.ambient = component.getAmbient();
		
		atom.lit = true;
		return atom;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom) {
		if (atom == null)
			atom = new RenderAtom();
		if (atom.lit) {
			// we own color
			atom.ambient = defaultColor;
			atom.diffuse = defaultColor;
			atom.specular = defaultColor;
			
			atom.lit = false;
		}
		return null;
	}
}
