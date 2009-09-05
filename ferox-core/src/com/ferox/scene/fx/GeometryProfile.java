package com.ferox.scene.fx;

import java.util.BitSet;

public class GeometryProfile {
	private final BitSet usedTextureUnits;
	private final BitSet usedVertexAttributes;
	
	public GeometryProfile() {
		usedTextureUnits = new BitSet();
		usedVertexAttributes = new BitSet();
	}
	
	public boolean isTextureUnitBound(int unit) {
		if (unit < 0)
			throw new IllegalArgumentException("Unit must be >= 0");
		return usedTextureUnits.get(unit);
	}
	
	public void setTextureUnitBound(int unit, boolean bound) {
		if (unit < 0)
			throw new IllegalArgumentException("Unit must be >= 0");
		usedTextureUnits.set(unit, bound);
	}
	
	public boolean isVertexAttributeBound(int unit) {
		if (unit < 0)
			throw new IllegalArgumentException("Unit must be >= 0");
		return usedVertexAttributes.get(unit);
	}
	
	public void setVertexAttributeBound(int unit, boolean bound) {
		if (unit < 0)
			throw new IllegalArgumentException("Unit must be >= 0");
		usedVertexAttributes.set(unit, bound);
	}
}
