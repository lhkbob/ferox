package com.ferox.scene;

import com.ferox.util.entity.AbstractComponent;
import com.ferox.util.entity.Description;
import com.ferox.util.entity.NonIndexable;

@NonIndexable
@Description("Hints that a Renderable should receive shadows")
public class ShadowReceiver extends AbstractComponent<ShadowReceiver> {
	public ShadowReceiver() {
		super(ShadowReceiver.class);
	}
}
