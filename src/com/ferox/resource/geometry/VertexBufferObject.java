package com.ferox.resource.geometry;

import com.ferox.resource.BufferData;
import com.ferox.resource.Resource;


/** A VertexBufferObject represents a block of data that can be used 
 * to help describe geometry.  It is stored on the graphics card if vbo's
 * are supported by the Renderer.
 * 
 * Depending on if vbo's are supported, you will need to use different
 * implementations of Geometry.  Two are provided that have the same interface,
 * but one works only if vbo's are supported, and the other doesn't not rely
 * on vbo support.
 * 
 * Renderers that update VertexBufferObjects should treat a BufferData with
 * a null array identically to how TextureImage's data's are handled.
 * The same BufferData instance will be constant throughout a VBO's lifetime.
 * 
 * @author Michael Ludwig
 *
 */
public class VertexBufferObject implements Resource {
	/** Enum describing how a VertexBufferObject wil be used. */
	public static enum UsageHint {
		STATIC,	/** Updated rarely.  Will have a faster render time, but slower update. */
		STREAM  /** Updated frequently (e.g. once every frame or two). */
	}
	
	/** Class used to describe the dirty region of a VertexBufferObject.
	 * It can be assumed that the region will be validly constrained to the vbo,
	 * if isDirty() returns true. */
	public static class VboDirtyDescriptor {
		private int offset;
		private int length;
		
		/** Return true if the associated VertexBufferObject has had it's
		 * markDirty() methods called.  If true, then getDirtOffset() and
		 * getDirtyLength() represent a valid region in the BufferData. */
		public boolean isDirty() {
			return this.offset >= 0;
		}
		
		/** Return the starting index into the vbo that's dirty.
		 * Will be undefined if isDirty() is false. */
		public int getDirtyOffset() {
			return this.offset;
		}
		
		/** Return the length of the dirty region, that starts
		 * at getDirtyOffset().
		 * Will be undefined if isDirty() is false. */
		public int getDirtyLength() {
			return this.length;
		}
		
		/** Called by clearDirtyDescriptor() of the VBO. */
		protected void clear() {
			this.offset = -1;
			this.length = -1;
		}
	}
	
	private UsageHint hint;
	private BufferData data;
	
	private VboDirtyDescriptor dirty;
	private Object renderData;
	
	/** Create a VBO with the given data and usage hint.
	 * Throws an exception if the data is null, or if hint is null.
	 * 
	 * It is acceptable to use a BufferData object with a null data array
	 * (usage semantics are identical to TextureImages). */
	public VertexBufferObject(BufferData data, UsageHint hint) throws NullPointerException {
		if (data == null || hint == null)
			throw new NullPointerException("Cannot specify a null buffer data or hint: " + data + " " + hint);
		
		this.hint = hint;
		this.data = data;
		
		this.dirty = this.createDirtyDescriptor();
	}
	
	/** Return the BufferData used by the VBO.  The vbo's
	 * size and type are determined by the data's type cand
	 * capacity.  
	 * 
	 * This object will not be null, but the data's array may be.
	 * In a case such as this, when being updated, the renderer should
	 * ignore the update request, unless it needs to allocate the space
	 * initially. */
	public BufferData getData() {
		return this.data;
	}
	
	/** Return the UsageHint that hints at how the VBO will
	 * be used and updated. */
	public UsageHint getUsageHint() {
		return this.hint;
	}
	
	/** Mark the given region as dirty.  If the vbo is already
	 * dirty, the new dirty region will be the extents of the given
	 * region and the old dirty area.
	 * 
	 * The given region will be clamped to have an offset >= 0, and
	 * a length such that the region doesn't extend beyong the capacity. */
	public void markDirty(int offset, int length) {
		this.dirty.offset = Math.max(0, Math.min(this.dirty.offset, offset));
		int rightOffset = Math.min(this.data.getCapacity(), Math.max(this.dirty.offset + length, 
																	 this.dirty.offset + this.dirty.length));
		this.dirty.length = rightOffset - this.dirty.offset;
	}
	
	/** Mark all of this VBO's data as being dirty. */
	public void markDirty() {
		this.markDirty(0, this.data.getCapacity());
	}
	
	/** Construct the dirty descriptor instance for use with this vbo.
	 * Should be overridden by subclasses if they need a more specific object.
	 * 
	 * In this case, it is likely that you'll also need to override the
	 * markDirty() methods. */
	protected VboDirtyDescriptor createDirtyDescriptor() {
		return new VboDirtyDescriptor();
	}
	
	@Override
	public final void clearDirtyDescriptor() {
		this.dirty.clear();
	}
	
	@Override
	public final VboDirtyDescriptor getDirtyDescriptor() {
		return this.dirty;
	}
	
	@Override
	public Object getResourceData() {
		return this.renderData;
	}
	
	@Override
	public void setResourceData(Object data) {
		this.renderData = data;
	}
}
