package com.ferox.renderer.impl.jogl.drivers;

import java.nio.FloatBuffer;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.GeometryDriver;
import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.VertexArrayRecord;
import com.ferox.resource.Geometry;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.text.Text;
import com.sun.opengl.util.BufferUtil;

/** JoglTextDriver handles the rendering of instances of Text
 * by using glDrawArrays.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglTextDriver implements GeometryDriver {
	private static class TextHandle implements Handle {
		private FloatBuffer bufferedCoords;
		private int numVerts;
		
		@Override
		public int getId() {
			return -1;
		}
	}
	
	private final JoglSurfaceFactory factory;
	private TextHandle lastRendered;
	
	public JoglTextDriver(JoglSurfaceFactory factory) {
		this.factory = factory;
		this.lastRendered = null;
	}

	@Override
	public int render(Geometry geom, ResourceData data) {
		GL gl = this.factory.getGL();
		VertexArrayRecord vr = this.factory.getRecord().vertexArrayRecord;
		
		TextHandle toRender = (TextHandle) data.getHandle();
		if (this.lastRendered != toRender) {
			// must enable vertex arrays and the 1st tex coord array unit
			if (!vr.enableVertexArray) {
				vr.enableVertexArray = true;
				gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
			}
			
			if (vr.clientActiveTexture != 0) {
				vr.clientActiveTexture = 0;
				gl.glClientActiveTexture(GL.GL_TEXTURE0);
			}
			if (!vr.enableTexCoordArrays[0]) {
				vr.enableTexCoordArrays[0] = true;
				gl.glEnableClientState(GL.GL_TEXTURE_COORD_ARRAY);
			}
			
			// set the interleaved arrays
			FloatBuffer buff = toRender.bufferedCoords;
			buff.clear().limit(toRender.numVerts * 5);
			gl.glInterleavedArrays(GL.GL_T2F_V3F, 0, buff);
			
			this.lastRendered = toRender; // store this
		}
		
		// render it
		gl.glNormal3f(0f, 0f, 1f);
		gl.glDrawArrays(GL.GL_QUADS, 0, toRender.numVerts);
		return toRender.numVerts >> 2; // verts / 4 to get quad count
	}

	@Override
	public void reset() {
		if (this.lastRendered != null) {
			GL gl = this.factory.getGL();
			VertexArrayRecord vr = this.factory.getRecord().vertexArrayRecord;
			
			// we only have to disable vertex and texCoord 0, since that's all Text uses
			if (vr.enableVertexArray) {
				vr.enableVertexArray = false;
				gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
			}
			
			if (vr.clientActiveTexture != 0) {
				vr.clientActiveTexture = 0;
				gl.glClientActiveTexture(GL.GL_TEXTURE0);
			}
			if (vr.enableTexCoordArrays[0]) {
				vr.enableTexCoordArrays[0] = false;
				gl.glDisableClientState(GL.GL_TEXTURE_COORD_ARRAY);
			}
			
			this.lastRendered = null;
		}
	}

	@Override
	public void cleanUp(Resource resource, ResourceData data) {
		// do nothing -> no cleanUp necessary
	}

	@Override
	public void update(Resource resource, ResourceData data, boolean fullUpdate) {
		TextHandle handle = (TextHandle) data.getHandle();
		if (handle == null) {
			// need a new handle
			handle = new TextHandle();
			data.setHandle(handle);
		}
		
		Text text = (Text) resource;
		// perform layout if needed
		if (text.isLayoutDirty())
			text.layoutText();
		
		float[] coords = text.getInterleavedCoordinates();
		if (handle.bufferedCoords == null || handle.bufferedCoords.capacity() < coords.length) {
			// must expand the buffer
			handle.bufferedCoords = BufferUtil.newFloatBuffer(coords.length);
		}
		
		// fill the buffer
		handle.bufferedCoords.clear();
		handle.bufferedCoords.put(coords);
		
		handle.numVerts = text.getVertexCount();
		
		if (handle.numVerts > 0) {
			data.setStatus(Status.OK);
			data.setStatusMessage("");
		} else {
			data.setStatus(Status.ERROR);
			data.setStatusMessage("ERROR is used to not render empty strings");
		}
	}
}
