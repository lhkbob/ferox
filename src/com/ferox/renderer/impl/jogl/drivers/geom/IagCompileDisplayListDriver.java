package com.ferox.renderer.impl.jogl.drivers.geom;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.drivers.geom.JoglIndexedArrayGeometryDriver.IndexedGeometryHandle;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.IndexedArrayGeometry;

/* Implementation of IagDriverImpl that satisfies the requiremens of the DISPLAY_LIST compile type. */
class IagCompileDisplayListDriver implements IagDriverImpl {
	private static class DisplayListData {
		int id;
		int polyCount;
	}

	private final IagCompileNoneDriver internalDriver;

	public IagCompileDisplayListDriver() {
		internalDriver = new IagCompileNoneDriver();
	}

	@Override
	public void cleanUp(GL gl, JoglStateRecord record,
		IndexedArrayGeometry geom, IndexedGeometryHandle handle) {
		gl.glDeleteLists(((DisplayListData) handle.driverData).id, 1);
	}

	@Override
	public int render(GL gl, JoglStateRecord record, IndexedArrayGeometry geom,
		IndexedGeometryHandle handle) {
		DisplayListData dl = (DisplayListData) handle.driverData;
		gl.glCallList(dl.id);
		return dl.polyCount;
	}

	@Override
	public String update(GL gl, JoglStateRecord record,
		IndexedArrayGeometry geom, IndexedGeometryHandle handle, boolean full) {

		DisplayListData dl = (DisplayListData) handle.driverData;
		if (dl == null) {
			// must create a new list
			dl = new DisplayListData();
			dl.id = gl.glGenLists(1);
			handle.driverData = dl;
		}

		gl.glNewList(dl.id, GL.GL_COMPILE);
		// because it's Compile.NONE driver, handle won't be used
		dl.polyCount = internalDriver.render(gl, record, geom, handle);
		gl.glEndList();

		return null;
	}

	@Override
	public boolean usesVbos() {
		return false;
	}

	@Override
	public boolean usesVertexArrays() {
		return false;
	}
}
