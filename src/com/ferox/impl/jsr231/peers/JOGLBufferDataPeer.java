package com.ferox.impl.jsr231.peers;

import java.nio.Buffer;

import javax.media.opengl.GL;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateUnit;
import com.ferox.core.states.StateUpdateException;
import com.ferox.core.states.StateAtom.StateRecord;
import com.ferox.core.states.atoms.BufferData;
import com.ferox.core.states.atoms.BufferData.BufferTarget;
import com.ferox.core.states.atoms.BufferData.UsageHint;
import com.ferox.core.util.FeroxException;
import com.ferox.impl.jsr231.JOGLRenderContext;

public class JOGLBufferDataPeer extends SimplePeer<BufferData, BufferRecord> {
	private static int getGLUsageHint(UsageHint hint) {
		switch(hint) {
		case STATIC: return GL.GL_STATIC_DRAW_ARB;
		case STREAM: return GL.GL_STREAM_DRAW_ARB;
		default:
			return GL.GL_STATIC_DRAW_ARB;
		}
	}
	
	public static int getGLTarget(BufferTarget target) {
		switch(target) {
		case ARRAY_BUFFER: return GL.GL_ARRAY_BUFFER_ARB;
		case ELEMENT_BUFFER: return GL.GL_ELEMENT_ARRAY_BUFFER_ARB;
		case PIXEL_READ_BUFFER: return GL.GL_PIXEL_PACK_BUFFER_EXT;
		case PIXEL_WRITE_BUFFER: return GL.GL_PIXEL_UNPACK_BUFFER_EXT;
		default:
			throw new FeroxException("Invalid buffer target");
		}
	}
	
	private int currentTarget;
	
	public JOGLBufferDataPeer(JOGLRenderContext context) {
		super(context);
	}
	
	/*public void setBufferSubData(GL gl, int target, Slice slice, Buffer source, int offset, int byteSize) {		
		int pos = source.position();
		int limit = source.limit();
		source.clear();
		source.position(offset);
			
		gl.glBufferSubData(JOGLBufferDataPeer.getGLTarget(target), slice.getOffset() * byteSize, slice.getLength() * byteSize, source);
			
		source.limit(limit);
		source.position(pos);
	}
	
	public void getBufferSubData(GL gl, int target, Slice slice, Buffer dest, int offset, int byteSize) {
		int pos = dest.position();
		int limit = dest.limit();
		dest.clear();
		dest.position(offset);
			
		gl.glGetBufferSubData(JOGLBufferDataPeer.getGLTarget(target), slice.getOffset() * byteSize, slice.getLength() * byteSize, dest);
			
		dest.limit(limit);
		dest.position(pos);
	}*/
	
	public void validateStateAtom(StateAtom atom) throws StateUpdateException {
		BufferData d = (BufferData)atom;
		if (!RenderManager.getSystemCapabilities().areVertexBuffersSupported() && d.isVBO())
			d.setVBO(false);
	}
	
	protected void applyState(BufferData prevA, BufferRecord prevR, BufferData nextA, BufferRecord nextR, GL gl) {
		if (prevR == null || prevR != nextR) {
			if (prevR != null && nextR.byteCapacity <= 0 && prevR.byteCapacity > 0)
				gl.glBindBufferARB(this.currentTarget, 0);
			else if (nextR.byteCapacity > 0)
				gl.glBindBufferARB(this.currentTarget, nextR.vboID);
		}
	}

	public void cleanupStateAtom(StateRecord record) {
		if (((BufferRecord)record).byteCapacity > 0)
			this.deleteBuffer(((BufferRecord)record).vboID, this.context.getGL());
	}

	public StateRecord initializeStateAtom(StateAtom a) {
		BufferData atom = (BufferData)a;
		
		BufferRecord r = new BufferRecord();
		r.byteCapacity = -1;
		r.vboID = 0;
		
		if (atom.isVBO()) {
			GL gl = this.context.getGL();
			int[] id = new int[1];
			gl.glGenBuffers(1, id, 0);
			r.vboID = id[0];
			this.fillBuffer(atom, r, gl);
		}
	
		return r;
	}

	protected void restoreState(BufferData cleanA, BufferRecord r, GL gl) {
		if (r.vboID != 0 && r.byteCapacity > 0)
			gl.glBindBufferARB(this.currentTarget, 0);
	}

	public void setUnit(StateUnit unit) {
		this.currentTarget = getGLTarget((BufferTarget)unit);
	}

	public void updateStateAtom(StateAtom atom, StateRecord record) {
		BufferRecord r = (BufferRecord)record;
		BufferData d = (BufferData)atom;
		
		if (d.isVBO() && r.byteCapacity > 0) {
			if (d.getCapacity() * d.getByteSize() != r.byteCapacity)
				r.byteCapacity = -1;
			this.fillBuffer(d, r, this.context.getGL());
		} else if (!d.isVBO() && r.byteCapacity > 0) {
			this.deleteBuffer(r.vboID, this.context.getGL());
		} else if (d.isVBO() && r.byteCapacity <= 0) {
			int[] id = new int[1];
			this.context.getGL().glGenBuffers(1, id, 0);
			r.vboID = id[0];
			this.fillBuffer(d, r, this.context.getGL());
		}
	}
	
	private void fillBuffer(BufferData data, BufferRecord r, GL gl) {
		Buffer bd = data.getData();
		int pos = 0;
		int limit = 0;
		if (bd != null) {
			pos = bd.position();
			limit = bd.limit();
			bd.clear();
		}
		
		int target, binding;
		switch(data.getPrimaryTarget()) {
		case ELEMENT_BUFFER:
			target = GL.GL_ELEMENT_ARRAY_BUFFER_ARB;
			binding = GL.GL_ELEMENT_ARRAY_BUFFER_BINDING_ARB;
			break;
		case PIXEL_READ_BUFFER:
			target = GL.GL_PIXEL_PACK_BUFFER_EXT;
			binding = GL.GL_PIXEL_PACK_BUFFER_BINDING_EXT;
			break;
		case PIXEL_WRITE_BUFFER:
			target = GL.GL_PIXEL_UNPACK_BUFFER_EXT;
			binding = GL.GL_PIXEL_UNPACK_BUFFER_BINDING_EXT;
			break;
		case ARRAY_BUFFER:
		default:
			target = GL.GL_ARRAY_BUFFER_ARB;
			binding = GL.GL_ARRAY_BUFFER_BINDING_ARB;
		}
		
		if (target == GL.GL_PIXEL_UNPACK_BUFFER_EXT || target == GL.GL_PIXEL_PACK_BUFFER_EXT) {
			if (!RenderManager.getSystemCapabilities().arePixelBuffersSupported()) {
				// make a normal array buffer instead, all the fetch operations in RenderContext will still work
				target = GL.GL_ARRAY_BUFFER_ARB;
				binding = GL.GL_ARRAY_BUFFER_BINDING_ARB;
			}
		}
		
		int[] prev = new int[1];
		gl.glGetIntegerv(binding, prev, 0);
		gl.glBindBufferARB(target, r.vboID);
	
		if (r.byteCapacity <= 0) {
			gl.glBufferDataARB(target, data.getCapacity() * data.getByteSize(), bd, getGLUsageHint(data.getUsageHint()));
			if (gl.glGetError() == GL.GL_OUT_OF_MEMORY) {
				this.deleteBuffer(r.vboID, gl);
				data.setVBO(false);
				r.vboID = 0;
				System.err.println("WARNING: out of memory, reverting to non-vbo BufferData");
			} else 
				r.byteCapacity = data.getCapacity() * data.getByteSize();
		} else 
			gl.glBufferSubDataARB(target, 0, data.getCapacity() * data.getByteSize(), bd);
	
		gl.glBindBufferARB(target, prev[0]);
		
		if (bd != null) {
			bd.position(pos);
			bd.limit(limit);
		}
	}
	
	private void deleteBuffer(int vboID, GL gl) {
		if (vboID != 0)
			gl.glDeleteBuffersARB(1, new int[] {vboID}, 0);
	}
}
