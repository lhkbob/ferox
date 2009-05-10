package com.ferox.renderer.impl.jogl.drivers.geom;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.drivers.geom.JoglIndexedArrayGeometryDriver.IndexedGeometryHandle;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.IndexedArrayGeometry;

/* Implementation of IagDriverImpl that satisfies the requirements of CompileType.VERTEX_ARRAY */
class IagCompileVertexArrayDriver implements IagDriverImpl {

	@Override
	public void cleanUp(GL gl, JoglStateRecord record,
		IndexedArrayGeometry geom, IndexedGeometryHandle handle) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public int render(GL gl, JoglStateRecord record, IndexedArrayGeometry geom,
		IndexedGeometryHandle handle) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public String update(GL gl, JoglStateRecord record,
		IndexedArrayGeometry geom, IndexedGeometryHandle handle, boolean full) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean usesVbos() {
		return false;
	}

	@Override
	public boolean usesVertexArrays() {
		return true;
	}
}
