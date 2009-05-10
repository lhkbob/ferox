package com.ferox.renderer.impl.jogl.drivers.geom;

import java.util.List;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.renderer.impl.jogl.drivers.geom.JoglIndexedArrayGeometryDriver.IndexedGeometryHandle;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.IndexedArrayGeometry;
import com.ferox.resource.IndexedArrayGeometry.VectorBuffer;
import com.ferox.util.UnitList.Unit;

/* Implementation of IagDriverImpl that satisfies the requirements of the CompileType.NONE */
class IagCompileNoneDriver implements IagDriverImpl {

	@Override
	public void cleanUp(GL gl, JoglStateRecord record,
		IndexedArrayGeometry geom, IndexedGeometryHandle handle) {
		// do nothing
	}

	@Override
	public int render(GL gl, JoglStateRecord record, IndexedArrayGeometry geom,
		IndexedGeometryHandle handle) {
		
		if (JoglIndexedArrayGeometryDriver.validateElementCounts(geom)) {
			float[] vertices = geom.getVertices();
			float[] normals = geom.getNormals();
			List<Unit<VectorBuffer>> tcs = geom.getTextureCoordinates();
			List<Unit<VectorBuffer>> vas = geom.getVertexAttributes();
			
			int[] indices = geom.getIndices();
			int tcCount = tcs.size();
			int vaCount = vas.size();
			
			int index, j;
			Unit<VectorBuffer> vb;
			float[] vbData;
			
			gl.glBegin(JoglUtil.getGLPolygonConnectivity(geom.getPolygonType()));
				for (int i = 0; i < indices.length; i++) {
					index = indices[i];
					
					// generic vertex attributes
					for (j = 0; j < vaCount; j++) {
						vb = vas.get(j);
						vbData = vb.getData().getBuffer();
						
						switch(vb.getData().getElementSize()) {
						case 1:
							gl.glVertexAttrib1f(vb.getUnit(), vbData[index]);
							break;
						case 2:
							gl.glVertexAttrib2f(vb.getUnit(), vbData[index * 2], vbData[index * 2 + 1]);
							break;
						case 3:
							gl.glVertexAttrib3f(vb.getUnit(), vbData[index * 3], vbData[index * 3 + 1], vbData[index * 3 + 2]);
							break;
						case 4:
							gl.glVertexAttrib4f(vb.getUnit(), vbData[index * 4], vbData[index * 4 + 1], vbData[index * 4 + 2], vbData[index * 4 + 3]);
							break;
						}
					}
					
					// texture coordinates
					for (j = 0; j < tcCount; j++) {
						vb = tcs.get(j);
						vbData = vb.getData().getBuffer();
						
						switch(vb.getData().getElementSize()) {
						case 1:
							gl.glMultiTexCoord1f(GL.GL_TEXTURE0 + vb.getUnit(), vbData[index]);
							break;
						case 2:
							gl.glMultiTexCoord2f(GL.GL_TEXTURE0 + vb.getUnit(), vbData[index * 2], vbData[index * 2 + 1]);
							break;
						case 3:
							gl.glMultiTexCoord3f(GL.GL_TEXTURE0 + vb.getUnit(), vbData[index * 3], vbData[index * 3 + 1], vbData[index * 3 + 2]);
							break;
						}
					}
					
					// normals
					if (normals != null)
						gl.glNormal3f(normals[index * 3], normals[index * 3 + 1], normals[index * 3 + 2]);
					
					// and finally a vertex
					gl.glVertex3f(vertices[index * 3], vertices[index * 3 + 1], vertices[index * 3 + 2]);
				}
			gl.glEnd();
			return geom.getPolygonCount();
		} else {
			// just don't render it
			return 0;
		}
	}

	@Override
	public String update(GL gl, JoglStateRecord record,
		IndexedArrayGeometry geom, IndexedGeometryHandle handle, boolean full) {
		// do nothing
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
