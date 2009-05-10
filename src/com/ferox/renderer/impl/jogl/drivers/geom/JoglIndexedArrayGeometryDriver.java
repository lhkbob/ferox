package com.ferox.renderer.impl.jogl.drivers.geom;

import java.util.List;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.GeometryDriver;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.VertexArrayRecord;
import com.ferox.resource.Geometry;
import com.ferox.resource.IndexedArrayGeometry;
import com.ferox.resource.Resource;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.resource.IndexedArrayGeometry.VectorBuffer;
import com.ferox.resource.Resource.Status;
import com.ferox.util.UnitList.Unit;

/**
 * JoglIndexedArrayGeometryDriver provides a GeometryDriver implementation for
 * IndexedArrayGeometrys and supports all compile types. If vertex buffers
 * aren't supported on the hardware, a type of VBO_STATIC or VBO_DYNAMIC will
 * default to VERTEX_ARRAY and the geometry's status is marked as DIRTY.
 * 
 * @author Michael Ludwig
 */
public class JoglIndexedArrayGeometryDriver implements GeometryDriver {
	/**
	 * The Handle implementation that is used by JoglIndexedArrayGeometryDriver
	 * and the IagDriverImpls it depends on.
	 */
	static class IndexedGeometryHandle implements Handle {
		Object driverData;
		private final IagDriverImpl internalDriver;

		// don't need booleans for verts or indices, since they're always
		// present
		private boolean hasNormals;
		private boolean[] hasTexCoords;
		private boolean[] hasVertexAttribs;

		public IndexedGeometryHandle(IagDriverImpl driver) {
			internalDriver = driver;
		}

		@Override
		public int getId() {
			return -1;
		}
	}

	private final JoglContextManager factory;

	// IagDriverImpls for all the CompileTypes that an IndexedArrayGeometry can have
	private final IagCompileNoneDriver none;
	private final IagCompileVertexArrayDriver va;
	private final IagCompileVboDriver vbo;
	private final IagCompileDisplayListDriver dl;

	private boolean vbosBound;
	private boolean arraysEnabled;

	public JoglIndexedArrayGeometryDriver(JoglContextManager factory) {
		this.factory = factory;

		none = new IagCompileNoneDriver();
		va = new IagCompileVertexArrayDriver();
		vbo = new IagCompileVboDriver();
		dl = new IagCompileDisplayListDriver();

		vbosBound = false;
		arraysEnabled = false;
	}

	@Override
	public int render(Geometry geom, ResourceData data) {
		GL gl = factory.getGL();
		JoglStateRecord record = factory.getRecord();
		IndexedGeometryHandle h = (IndexedGeometryHandle) data.getHandle();

		if (h.internalDriver.usesVertexArrays()) {
			// make sure client state is prepared for the next rendering
			enableVertexArrays(gl, record.vertexArrayRecord, h);
			arraysEnabled = true;

			if (vbosBound && !h.internalDriver.usesVbos()) {
				// make sure vbo bindings won't conflict
				unbindVbos(gl, record.vertexArrayRecord);
				vbosBound = false;
			}
		}

		// must flag this as true, in case next rendering can't use vbos
		if (h.internalDriver.usesVbos())
			vbosBound = true;

		return h.internalDriver.render(gl, record, (IndexedArrayGeometry) geom,
			h);
	}

	@Override
	public void reset() {
		GL gl = factory.getGL();
		VertexArrayRecord vr = factory.getRecord().vertexArrayRecord;

		// disable arrays and vbos as needed to clear the state record
		if (arraysEnabled) {
			disableVertexArrays(gl, vr);
			arraysEnabled = false;
		}
		if (vbosBound) {
			unbindVbos(gl, vr);
			vbosBound = false;
		}
	}

	@Override
	public void cleanUp(Resource resource, ResourceData data) {
		IndexedGeometryHandle h = (IndexedGeometryHandle) data.getHandle();
		h.internalDriver.cleanUp(factory.getGL(), factory.getRecord(),
			(IndexedArrayGeometry) resource, h);
	}

	@Override
	public void update(Resource resource, ResourceData data, boolean fullUpdate) {
		VertexArrayRecord var = factory.getRecord().vertexArrayRecord;
		IndexedArrayGeometry iag = (IndexedArrayGeometry) resource;
		IndexedGeometryHandle h = (IndexedGeometryHandle) data.getHandle();

		if (h == null) {
			// this is the first time seeing the geometry, so make a new handle
			h = new IndexedGeometryHandle(getDriver(iag.getCompileType()));

			if (h.internalDriver.usesVertexArrays()) {
				// allocate boolean arrays
				h.hasTexCoords = new boolean[var.enableTexCoordArrays.length];
				h.hasVertexAttribs =
					new boolean[var.enableVertexAttribArrays.length];
			}
			data.setHandle(h);
		}

		// make sure all element counts match up
		if (!validateElementCounts(iag)) {
			data.setStatus(Status.ERROR);
			data.setStatusMessage("Element counts are not all equal");
			return;
		}

		if (h.internalDriver.usesVertexArrays()) {
			// remember which arrays are used by this geometry
			h.hasNormals = iag.getNormals() != null;

			// set the tex coord boolean
			for (int i = 0; i < h.hasTexCoords.length; i++)
				h.hasTexCoords[i] = iag.getTextureCoordinates(i) != null;

			// set the vertex attrib boolean
			for (int i = 0; i < h.hasVertexAttribs.length; i++)
				h.hasVertexAttribs[i] = iag.getVertexAttributes(i) != null;
		}

		// invoke the internal driver to do an update
		String result =
			h.internalDriver.update(factory.getGL(), factory.getRecord(), iag,
				h, fullUpdate);

		if (result == null) {
			// mark it as dirty if we switched to va's, but vbo's were requested
			switch (iag.getCompileType()) {
			case VBO_DYNAMIC:
			case VBO_STATIC:
				if (!factory.getRenderer().getCapabilities()
					.getVertexBufferSupport()) {
					data.setStatus(Status.DIRTY);
					data
						.setStatusMessage("Vbos are unsupported, falling back on vertex arrays");
					break;
				}
				// falls through if we're okay
			case DISPLAY_LIST:
			case NONE:
			case VERTEX_ARRAY:
				data.setStatus(Status.OK);
				data.setStatusMessage("");
			}
		} else {
			// there was an error, so set the status
			data.setStatus(Status.ERROR);
			data.setStatusMessage(result);
		}
	}

	/**
	 * Go through the float arrays of iag and make sure they all have the same
	 * element counts, based on the vector size for each buffer type.
	 */
	public static boolean validateElementCounts(IndexedArrayGeometry iag) {
		int count = iag.getVertexCount();

		if (count != iag.getVertices().length / 3)
			return false;
		if (iag.getNormals() != null && count != iag.getNormals().length / 3)
			return false;

		List<Unit<VectorBuffer>> tcs = iag.getTextureCoordinates();
		VectorBuffer data;
		for (int i = 0; i < tcs.size(); i++) {
			data = tcs.get(i).getData();
			if (data.getBuffer().length / data.getElementSize() != count)
				return false;
		}

		List<Unit<VectorBuffer>> vas = iag.getVertexAttributes();
		for (int i = 0; i < vas.size(); i++) {
			data = vas.get(i).getData();
			if (data.getBuffer().length / data.getElementSize() != count)
				return false;
		}

		return true;
	}

	private IagDriverImpl getDriver(CompileType type) {
		switch (type) {
		case DISPLAY_LIST:
			return dl;
		case NONE:
			return none;
		case VBO_DYNAMIC:
		case VBO_STATIC:
			if (factory.getRenderer().getCapabilities()
				.getVertexBufferSupport())
				return vbo;
			// falls through to va if vbos are unsupported
		case VERTEX_ARRAY:
			return va;
		}

		// shouldn't happen
		return null;
	}

	/* Unbind the vbos to make sure normal vertex arrays are usable. */
	private void unbindVbos(GL gl, VertexArrayRecord vr) {
		if (vr.arrayBufferBinding != 0) {
			vr.arrayBufferBinding = 0;
			gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, 0);
		}

		if (vr.elementBufferBinding != 0) {
			vr.elementBufferBinding = 0;
			gl.glBindBufferARB(GL.GL_ARRAY_BUFFER_ARB, 0);
		}
	}

	/* Make sure the client state is prepared for rendering handle. */
	private void enableVertexArrays(GL gl, VertexArrayRecord vr,
		IndexedGeometryHandle handle) {
		setVerticesEnabled(gl, vr, true);
		setNormalsEnabled(gl, vr, handle.hasNormals);
		for (int i = 0; i < handle.hasTexCoords.length; i++)
			setTexCoordsEnabled(gl, vr, i, handle.hasTexCoords[i]);
		for (int i = 0; i < handle.hasVertexAttribs.length; i++)
			setVertexAttribEnabled(gl, vr, i, handle.hasVertexAttribs[i]);
	}

	/* Make sure all enabled vertex arrays are disabled. */
	private void disableVertexArrays(GL gl, VertexArrayRecord vr) {
		setVerticesEnabled(gl, vr, false);
		setNormalsEnabled(gl, vr, false);
		for (int i = 0; i < vr.enableTexCoordArrays.length; i++)
			setTexCoordsEnabled(gl, vr, i, false);
		for (int i = 0; i < vr.enableVertexAttribArrays.length; i++)
			setVertexAttribEnabled(gl, vr, i, false);
		arraysEnabled = false;
	}

	private void setVerticesEnabled(GL gl, VertexArrayRecord vr, boolean enable) {
		if (vr.enableVertexArray != enable) {
			vr.enableVertexArray = enable;
			if (enable)
				gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			else
				gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
		}
	}

	private void setNormalsEnabled(GL gl, VertexArrayRecord vr, boolean enable) {
		if (vr.enableNormalArray != enable) {
			vr.enableNormalArray = enable;
			if (enable)
				gl.glEnableClientState(GL.GL_NORMAL_ARRAY);
			else
				gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
		}
	}

	private void setTexCoordsEnabled(GL gl, VertexArrayRecord vr, int unit,
		boolean enable) {
		if (vr.enableTexCoordArrays[unit] != enable) {
			vr.enableTexCoordArrays[unit] = enable;

			if (vr.clientActiveTexture != unit) {
				vr.clientActiveTexture = unit;
				gl.glClientActiveTexture(GL.GL_TEXTURE0 + unit);
			}

			if (enable)
				gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
			else
				gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
		}
	}

	private void setVertexAttribEnabled(GL gl, VertexArrayRecord vr, int unit,
		boolean enable) {
		if (vr.enableVertexAttribArrays[unit] != enable) {
			vr.enableVertexAttribArrays[unit] = enable;

			if (enable)
				gl.glEnableVertexAttribArray(unit);
			else
				gl.glDisableVertexAttribArray(unit);
		}
	}
}
