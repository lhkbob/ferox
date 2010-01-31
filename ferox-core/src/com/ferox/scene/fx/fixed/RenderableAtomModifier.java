package com.ferox.scene.fx.fixed;

import com.ferox.scene.fx.Renderable;
import com.ferox.scene.fx.fixed.EntityAtomBuilder.AtomModifier;
import com.ferox.util.entity.Component;

public class RenderableAtomModifier implements AtomModifier<Renderable> {
	private static final int R_ID = Component.getTypeId(Renderable.class);
	
	@Override
	public int getComponentType() {
		return R_ID;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom, Renderable component) {
		if (atom == null)
			atom = new RenderAtom();
		atom.front = component.getDrawStyleFront();
		atom.back = component.getDrawStyleBack();
		return atom;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom) {
		// always fail, Renderable is required to render
		return null;
	}
}
