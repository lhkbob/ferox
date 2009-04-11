package com.ferox.renderer.impl.jogl.drivers;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.ResourceData;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.ferox.renderer.impl.jogl.record.VertexArrayRecord;
import com.ferox.resource.BufferData;
import com.ferox.resource.Resource;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.geometry.VertexBufferObject;
import com.ferox.resource.geometry.VertexBufferObject.VboDirtyDescriptor;

/** JoglVertexBufferObjectResourceDriver implements the necessary opengl
 * operations to allocate, update and clean-up vbos on the graphics card.
 * It is based off of the VertexBufferObject resource class.
 * At the moment, it only assumes targets of ARRAY_BUFFER and ELEMENT_ARRAY_BUFFER.
 * The target is guessed on the signed-ness of the type: unsigned types are
 * assumed to be element values.
 * 
 * Also, all gl calls use the ARB extension to better support older hardware.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglVertexBufferObjectResourceDriver implements ResourceDriver {
	/* Class used for the vbo handles created by this resource driver. */
	private static class VboHandle implements Handle {
		private int id;
		private int glTarget;
		private int glUsage;
		private boolean valid; // false if original glBufferData() failed
		
		@Override
		public int getId() {
			return this.id;
		}
	}
	
	private JoglSurfaceFactory factory;
	private boolean vboSupport;
	
	public JoglVertexBufferObjectResourceDriver(JoglSurfaceFactory factory) {
		this.factory = factory;
		this.vboSupport = factory.getRenderer().getCapabilities().getVertexBufferSupport();
	}

	@Override
	public void update(Resource resource, ResourceData data, boolean fullUpdate) {
		GL gl = this.factory.getGL();
		VertexArrayRecord vr = this.factory.getRecord().vertexArrayRecord;
		
		VboHandle handle = (VboHandle) data.getHandle();
		VertexBufferObject vbo = (VertexBufferObject) resource;

		if (handle == null) {
			// we're creating a new vbo
			if (!this.vboSupport) {
				data.setStatus(Status.ERROR);
				data.setStatusMessage("VBOs aren't supported on this machine");
				return; // abort the update
			}

			// make a new handle
			handle = new VboHandle();
			data.setHandle(handle);

			handle.glTarget = getBufferTarget(vbo.getData().getType());
			handle.glUsage = EnumUtil.getGLUsageHint(vbo.getUsageHint());
			handle.id = generateVboId(gl);

			handle.valid = false; // will force glBufferData()
		}

		gl.glBindBufferARB(handle.glTarget, handle.id);

		if (!handle.valid) {
			gl.glBufferDataARB(handle.glTarget, vbo.getData().getCapacity() * vbo.getData().getType().getByteSize(), 
					wrap(vbo.getData()).clear(), handle.glUsage);
			int e = gl.glGetError();
			if (e == GL.GL_OUT_OF_MEMORY) {
				// we're still invalid
				data.setStatus(Status.ERROR);
				data.setStatusMessage("Out of memory, cannot allocate enough space for vbo");
			} else {
				// first successful alloc - after this we can use glBufferSubData()
				data.setStatus(Status.OK);
				data.setStatusMessage("");
				handle.valid = true;
			}
		} else {
			// perform an in-place update
			VboDirtyDescriptor dirty = (fullUpdate ? null : vbo.getDirtyDescriptor());
			int pos = (dirty == null ? 0 : dirty.getDirtyOffset());
			int len = (dirty == null ? vbo.getData().getCapacity() : dirty.getDirtyLength());
			
			Buffer nio = wrap(vbo.getData());
			if (nio != null) {
				// only do a sub-update if it's not null
				nio.position(pos);
				nio.limit(pos + len);
				int byteSize = vbo.getData().getType().getByteSize();
				
				gl.glBufferSubDataARB(handle.glTarget, pos * byteSize, len * byteSize, nio);
			}
		}

		// restore the previous buffer binding
		int prevBinding = (handle.glTarget == GL.GL_ARRAY_BUFFER_ARB ? vr.arrayBufferBinding : vr.elementBufferBinding);
		gl.glBindBufferARB(handle.glTarget, prevBinding);
		
		// we're done with the update
		vbo.clearDirtyDescriptor();
	}
	
	@Override
	public void cleanUp(Resource resource, ResourceData data) {
		if (data.getHandle() != null) {
			// if we have a handle, then it needs deleting
			deleteVbo(this.factory.getGL(), data.getHandle().getId());
		}
	}
	
	/* Return a nio buffer wrapping bd's array, or null
	 * if the array is null. */
	private static Buffer wrap(BufferData bd) {
		Object array = bd.getData();
		if (array == null)
			return null; // bail out
		
		switch(bd.getType()) {
		case BYTE: case UNSIGNED_BYTE: return ByteBuffer.wrap((byte[]) array);
		case INT: case UNSIGNED_INT: return IntBuffer.wrap((int[]) array);
		case SHORT: case UNSIGNED_SHORT: return ShortBuffer.wrap((short[]) array);
		case FLOAT: return FloatBuffer.wrap((float[]) array);
		}
		// shouldn't happen
		return null;
	}
	
	/* Return a new id for a VboHandle. */
	private static int generateVboId(GL gl) {
		int[] id = new int[1];
		gl.glGenBuffersARB(1, id, 0);
		return id[0];
	}
	
	/* Delete the given vbo. */
	private static void deleteVbo(GL gl, int id) {
		gl.glDeleteBuffersARB(1, new int[] {id}, 0);
	}
	
	/* Return the expected vbo target, based on the data type. */
	private static int getBufferTarget(DataType type) {
		if (type.isUnsigned())
			return GL.GL_ELEMENT_ARRAY_BUFFER_ARB;
		else
			return GL.GL_ARRAY_BUFFER_ARB;
	}
}
