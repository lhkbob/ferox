package com.ferox.scene.fx.fixed;

import com.ferox.math.Color4f;
import com.ferox.scene.fx.SolidLightingModel;
import com.ferox.scene.fx.fixed.EntityAtomBuilder.AtomModifier;
import com.ferox.util.entity.Component;

public class SolidColorAtomModifier implements AtomModifier<SolidLightingModel> {
	private static final int SC_ID = Component.getTypeId(SolidLightingModel.class);
	private static final Color4f defaultColor = new Color4f(.8f, .8f, .8f, 1f);
	
	@Override
	public int getComponentType() {
		return SC_ID;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom, SolidLightingModel component) {
		if (atom == null)
			atom = new RenderAtom();
		Color4f color = component.getColor();
		atom.specular = color;
		atom.diffuse = color;
		atom.ambient = color;
		
		atom.lit = false;
		return atom;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom) {
		if (atom == null)
			atom = new RenderAtom();
		if (!atom.lit) {
			// we have control over color
			atom.specular = defaultColor;
			atom.diffuse = defaultColor;
			atom.ambient = defaultColor;
		}
		return atom;
	}
}
