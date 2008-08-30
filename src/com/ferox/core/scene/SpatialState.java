package com.ferox.core.scene;

import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;

public abstract class SpatialState extends StateAtom {
	InfluenceLeaf leaf;

	public abstract void updateSpatial();
	public abstract StateUnit[] availableUnits();
	public abstract float getInfluence(SpatialLeaf leaf);
	
	public InfluenceLeaf getInfluenceLeaf() {
		return this.leaf;
	}
}
