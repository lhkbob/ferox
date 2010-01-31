package com.ferox.scene.fx.fixed;

import com.ferox.scene.fx.TexturedMaterial;
import com.ferox.scene.fx.fixed.EntityAtomBuilder.AtomModifier;
import com.ferox.util.entity.Component;

public class TexturedMaterialAtomModifier implements AtomModifier<TexturedMaterial> {
	private static final int TM_ID = Component.getTypeId(TexturedMaterial.class);

	@Override
	public int getComponentType() {
		return TM_ID;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom, TexturedMaterial component) {
		if (atom == null)
			atom = new RenderAtom();
		atom.primaryTexture = component.getPrimaryTexture();
		atom.decalTexture = component.getDecalTexture();
		return atom;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom) {
		if (atom == null)
			atom = new RenderAtom();
		atom.primaryTexture = null;
		atom.decalTexture = null;
		return atom;
	}
}
