package com.ferox.scene.fx.impl.fixed;

import com.ferox.math.bounds.BoundVolume;
import com.ferox.renderer.Framework;
import com.ferox.resource.Geometry;

class NullGeometry implements Geometry {
	@Override
	public CompileType getCompileType() { return CompileType.NONE; }

	@Override
	public void getBounds(BoundVolume result) {	}

	@Override
	public float getVertex(int index, int coord) { return 0f; }

	@Override
	public int getVertexCount() { return 0; }

	@Override
	public void clearDirtyDescriptor() { }

	@Override
	public Object getDirtyState() { return null; }

	@Override
	public Object getRenderData(Framework renderer) { return null; }

	@Override
	public void setRenderData(Framework renderer, Object data) { }
}
