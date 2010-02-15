package com.ferox.scene;

import com.ferox.util.entity.AbstractComponent;
import com.ferox.util.entity.Description;

import com.ferox.util.entity.NonIndexable;

@NonIndexable
@Description("Designates an entity as a shadow caster, either as an occluder or light")
public class ShadowCaster extends AbstractComponent<ShadowCaster> {
	public ShadowCaster() {
		super(ShadowCaster.class);
	}
}
