package com.ferox.scene.fx.fixed;

import com.ferox.scene.fx.ShadowCaster;
import com.ferox.scene.fx.fixed.EntityAtomBuilder.AtomModifier;
import com.ferox.util.entity.Component;

public class ShadowCasterAtomModifier implements AtomModifier<ShadowCaster> {
	private static final int SC_ID = Component.getTypeId(ShadowCaster.class);
	
	@Override
	public int getComponentType() {
		return SC_ID;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom, ShadowCaster component) {
		if (atom == null)
			atom = new RenderAtom();
		atom.castsShadow = true;
		return atom;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom) {
		if (atom == null)
			atom = new RenderAtom();
		atom.castsShadow = false;
		return atom;
	}
}
