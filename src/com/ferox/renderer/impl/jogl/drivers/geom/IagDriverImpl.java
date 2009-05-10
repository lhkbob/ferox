package com.ferox.renderer.impl.jogl.drivers.geom;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.drivers.geom.JoglIndexedArrayGeometryDriver.IndexedGeometryHandle;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.IndexedArrayGeometry;

/* 
 * Internal class to perform the operations of a GeometryDriver
 * on an IndexedArrayGeometry, using one of the CompileTypes defined
 * in Geometry.
 */
interface IagDriverImpl {
	/*
	 * Render the given geometry. If this driver returns true from
	 * usesVertexArrays(), it can be assumed that client state has been enabled
	 * correctly for this geometry.
	 */
	public int render(GL gl, JoglStateRecord record, IndexedArrayGeometry geom,
		IndexedGeometryHandle handle);

	/*
	 * Perform the actual update for the given geometry. Return the error
	 * message, or null if the status should be OK.
	 */
	public String update(GL gl, JoglStateRecord record,
		IndexedArrayGeometry geom, IndexedGeometryHandle handle, boolean full);

	/* Cleanup the given geometry. */
	public void cleanUp(GL gl, JoglStateRecord record,
		IndexedArrayGeometry geom, IndexedGeometryHandle handle);

	/*
	 * Return true if this driver uses vertex arrays for rendering. This implies
	 * that render() will need to have some client state enabled, and that
	 * rendering is performed using glDrawElements() (or some function related
	 * to that).
	 */
	public boolean usesVertexArrays();

	/*
	 * Return true if the vertex arrays used by this driver are stored on the
	 * graphics card in vbos. This should only return true if usesVertexArrays()
	 * is also true.
	 */
	public boolean usesVbos();
}
