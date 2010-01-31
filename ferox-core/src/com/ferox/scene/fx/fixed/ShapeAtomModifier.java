package com.ferox.scene.fx.fixed;

import com.ferox.scene.fx.Shape;
import com.ferox.scene.fx.fixed.EntityAtomBuilder.AtomModifier;
import com.ferox.util.entity.Component;

public class ShapeAtomModifier implements AtomModifier<Shape> {
	private static final int S_ID = Component.getTypeId(Shape.class);
	
	@Override
	public int getComponentType() {
		return S_ID;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom, Shape component) {
		if (atom == null)
			atom = new RenderAtom();
		atom.geometry = component.getGeometry();
		return atom;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom) {
		if (atom == null)
			atom = new RenderAtom();
		atom.geometry = null;
		return atom;
	}
}
