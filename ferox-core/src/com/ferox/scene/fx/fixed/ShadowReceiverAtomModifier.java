package com.ferox.scene.fx.fixed;

import com.ferox.scene.fx.ShadowReceiver;
import com.ferox.scene.fx.fixed.EntityAtomBuilder.AtomModifier;
import com.ferox.util.entity.Component;

public class ShadowReceiverAtomModifier implements AtomModifier<ShadowReceiver> {
	private static final int SR_ID = Component.getTypeId(ShadowReceiver.class);
	
	@Override
	public int getComponentType() {
		return SR_ID;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom, ShadowReceiver component) {
		if (atom == null)
			atom = new RenderAtom();
		atom.receivesShadow = true;
		return atom;
	}

	@Override
	public RenderAtom modifyAtom(RenderAtom atom) {
		if (atom == null)
			atom = new RenderAtom();
		atom.receivesShadow = false;
		return atom;
	}
}
