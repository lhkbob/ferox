package com.ferox.scene.fx.fixed;

import com.ferox.scene.SceneElement;
import com.ferox.scene.fx.fixed.EntityAtomBuilder.AtomModifier;
import com.ferox.util.entity.Component;

public class SceneElementAtomModifier implements AtomModifier<SceneElement> {
	private static final int SE_ID = Component.getTypeId(SceneElement.class);
	
	@Override
	public int getComponentType() {
		return SE_ID;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom, SceneElement component) {
		if (atom == null)
			atom = new RenderAtom();
		atom.worldTransform = component.getTransform().get(atom.worldTransform);
		atom.worldBounds = component.getWorldBounds();
		return atom;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom) {
		// always fail, SceneElement is required to render
		return null;
	}
}
