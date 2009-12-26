package com.ferox.renderer.impl.jogl.resource;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.renderer.impl.jogl.BoundObjectState;
import com.ferox.renderer.impl.jogl.JoglContext;
import com.ferox.renderer.impl.jogl.Utils;
import com.ferox.resource.DirtyState;
import com.ferox.resource.Geometry;
import com.ferox.resource.GeometryDirtyState;
import com.ferox.resource.Resource;
import com.ferox.resource.VectorBuffer;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.resource.GeometryDirtyState.BufferRange;
import com.ferox.resource.Resource.Status;

/**
 * An implementation of ResourceDriver that supports the Geometry resource type.
 * 
 * @author Michael Ludwig
 */
public class JoglGeometryResourceDriver implements ResourceDriver {
	private static final float REALLOC_FACTOR = .75f; // fraction of new buffer size that triggers a realloc
	
	private final boolean hasVboSupport;
	
	public JoglGeometryResourceDriver(RenderCapabilities caps) {
		if (caps == null)
			throw new NullPointerException("Must specify a non-null RenderCapabilities");
		
		hasVboSupport = caps.getVertexBufferSupport();
	}
	
	@Override
	public void dispose(ResourceHandle handle) {
		GL2GL3 gl = JoglContext.getCurrent().getGL();
		GeometryHandle h = (GeometryHandle) handle;
		
		if (h.arrayVbo > 0)
			gl.glDeleteBuffers(2, new int[] {h.arrayVbo, h.elementVbo}, 0);
	}

	@Override
	public ResourceHandle init(Resource res) {
		GL2GL3 gl = JoglContext.getCurrent().getGL();
		
		Geometry g = (Geometry) res;
		CompileType compile = g.getCompileType();
		String statusMsg = "";
		
		if (compile != CompileType.NONE && !hasVboSupport) {
			compile = CompileType.NONE;
			statusMsg = "Cannot support " + compile + ", defaulting to CompileType.NONE";
		}
		
		GeometryHandle handle = new GeometryHandle(g.getCompileType());
		if (compile != CompileType.NONE)
			createBuffers(gl, handle); // initialize the vbo ids
		
		handle.setStatus(Status.READY);
		handle.setStatusMessage(statusMsg);
		
		update(gl, g, handle, null);
		return handle;
	}

	@Override
	public Status update(Resource res, ResourceHandle handle, DirtyState<?> dirtyState) {
		GL2GL3 gl = JoglContext.getCurrent().getGL();
		update(gl, (Geometry) res, (GeometryHandle) handle, (GeometryDirtyState) dirtyState);
		return handle.getStatus();
	}

	private void update(GL2GL3 gl, Geometry g, GeometryHandle handle, GeometryDirtyState dirty) {
		// check for errors
		if (g.getIndices() == null) {
			handle.setStatus(Status.ERROR);
			handle.setStatusMessage("Geometry cannot have null indices");
			return;
		}
		if (g.getAttributes().isEmpty()) {
			handle.setStatus(Status.ERROR);
			handle.setStatusMessage("Geometry must have at least one vertex attribute");
			return;
		}
		if (!checkVertexArraySize(g)) {
			handle.setStatus(Status.ERROR);
			handle.setStatusMessage("Geometry vertex attribute element count mismatch");
			return;
		}
		
		if (handle.compile != CompileType.NONE)
			updateForVbo(gl, g, handle, dirty);
		else
			updateForVertexArray(gl, g, handle, dirty);
		handle.glPolyType = Utils.getGLPolygonConnectivity(g.getPolygonType());
		handle.version = handle.version + 1;
	}
	
	private void updateForVbo(GL2GL3 gl, Geometry g, GeometryHandle handle, GeometryDirtyState dirty) {
		BoundObjectState record = JoglContext.getCurrent().getRecord();
		// bind this geometry's vbos
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, handle.arrayVbo);
		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, handle.elementVbo);
		
		List<VertexArray> vas = handle.compiledPointers;
		if (vas == null) {
			vas = new ArrayList<VertexArray>();
			handle.compiledPointers = vas;
		}
		
		if (dirty != null) {
			boolean fullAllocate = false;
			
			List<VertexArray> reuseVbos = new ArrayList<VertexArray>();
			List<String> newBuffersReq = new ArrayList<String>();

			for (int i = vas.size() - 1; i >= 0; i--) {
				VertexArray va = vas.get(i);
				VectorBuffer vb = g.getAttribute(va.name);
				
				if (vb == null) {
					// pool the vbo range
					reuseVbos.add(vas.remove(i));
				} else if (vb.getData().length * 4 > va.vboLen) {
					// array grew so we need to allocate a new buffer
					newBuffersReq.add(va.name);
					reuseVbos.add(vas.remove(i));
				}
			}
			
			newBuffersReq.addAll(dirty.getAddedAttributes()); // we assume this is correct
			for (int i = newBuffersReq.size() - 1; i >= 0; i--) {
				VectorBuffer vb = g.getAttribute(newBuffersReq.get(i));
				VertexArray best = null;
				int bestDiff = Integer.MAX_VALUE;
				
				for (int j = reuseVbos.size() - 1; j >= 0; j--) {
					int diff = reuseVbos.get(j).vboLen - vb.getData().length;
					if (diff == 0) {
						// just use this, since it will be the best
						best = reuseVbos.get(j);
						break;
					} else if (diff > 0 && diff < bestDiff) {
						best = reuseVbos.get(j);
						bestDiff = diff;
					}
				}
				
				if (best != null) {
					VertexArray va = new VertexArray(newBuffersReq.get(i));
					va.offset = best.offset;
					va.vboLen = best.vboLen;
					vas.add(va);
				} else {
					// must do full allocate, since we can't reuse space
					fullAllocate = true;
					break;
				}
			}
			
			if (reuseVbos.size() > 0)
				fullAllocate = true; // must compact the vbo
			
			if (!fullAllocate) {
				int vboType = getVboType(handle.compile);
				for (Entry<String, VectorBuffer> attr: g.getAttributes().entrySet()) {
					String name = attr.getKey();
					VertexArray array = null;
					for (int i = vas.size(); i >= 0; i--) {
						if (vas.get(i).name.equals(name)) {
							array = vas.get(i);
							break;
						}
					}
					
					// found the vertex array
					VectorBuffer vb = attr.getValue();
					array.elementSize = vb.getElementSize();
					BufferRange br = dirty.getModifiedAttributes().get(name);
					if (br != null) {
						// update based on dirty range
						glArrayData(gl, vboType, array.offset, br.getOffset(), br.getLength(), vb.getData());
					} else if (newBuffersReq.contains(name)) {
						// update entire vector buffer
						glArrayData(gl, vboType, array.offset, 0, vb.getData().length, vb.getData());
					} // else no change needed
				}
			} else {
				// must do a full update
				updateVboAttributesFull(gl, g, handle);
			}
		} else {
			// must do a full update since we have no dirty info
			updateVboAttributesFull(gl, g, handle);
		}
		
		// now handle the indices vbo
		int[] indices = g.getIndices();
		int indexLength = indices.length * 4;
		BufferRange dirtyI = (dirty == null ? null : dirty.getModifiedIndices());
		boolean computeMinMax = false;
		
		if (indexLength > handle.elementVboSize || handle.elementVboSize * REALLOC_FACTOR > indexLength) {
			// not enough space, do a full buffer allcate
			gl.glBufferData(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, indexLength, IntBuffer.wrap(indices), getVboType(handle.compile));
			handle.elementVboSize = indexLength;
			computeMinMax = true;
		} else if (dirty == null || dirtyI != null) {
			// update based on dirty state, w/0 BufferSubData for speed
			if (dirty == null)
				gl.glBufferSubData(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, 0, indices.length, IntBuffer.wrap(indices));
			else
				gl.glBufferSubData(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, dirtyI.getOffset() * 4, dirtyI.getLength() * 4, 
								   IntBuffer.wrap(indices, dirtyI.getOffset(), dirtyI.getLength()));
			computeMinMax = true;
		}
		
		// restore vbo bindings
		gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, record.getArrayVbo());
		gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, record.getElementVbo());
		
		handle.indexCount = indices.length;
		handle.polyCount = g.getPolygonType().getPolygonCount(handle.indexCount);
		if (computeMinMax)
			updateMinMaxIndices(handle, indices);
	}
	
	private void updateVboAttributesFull(GL2GL3 gl, Geometry g, GeometryHandle handle) {
		List<VertexArray> vas = new ArrayList<VertexArray>();
		int offset = 0;
		for (Entry<String, VectorBuffer> attr: g.getAttributes().entrySet()) {
			VertexArray va = new VertexArray(attr.getKey());
			VectorBuffer vb = attr.getValue();
			
			va.elementSize = vb.getElementSize();
			va.offset = offset;
			va.vboLen = vb.getData().length * 4;
			
			vas.add(va);
			offset += va.vboLen;
		}
		
		int vboType = getVboType(handle.compile);
		// at this point, offset contains minimum vbo size
		if (handle.arrayVboSize < offset || handle.arrayVboSize * REALLOC_FACTOR > offset) {
			gl.glBufferData(GL2GL3.GL_ARRAY_BUFFER, offset, null, vboType);
			handle.arrayVboSize = offset;
		}
		
		for (int i = 0; i < vas.size(); i++) {
			VertexArray va = vas.get(i);
			float[] data = g.getAttribute(va.name).getData();
			glArrayData(gl, vboType, va.offset, 0, data.length, data);
		}
		
		handle.compiledPointers = vas;
	}

	private void updateForVertexArray(GL2GL3 gl, Geometry g, GeometryHandle handle, GeometryDirtyState dirty) {
		List<VertexArray> vas = handle.compiledPointers;
		if (vas == null) {
			vas = new ArrayList<VertexArray>();
			handle.compiledPointers = vas;
		}
		
		// remove all vertex arrays, but pool their FloatBuffers for reuse
		List<FloatBuffer> toReuse = new ArrayList<FloatBuffer>();
		for (int i = vas.size() - 1; i >= 0; i--) {
			if (g.getAttribute(vas.get(i).name) == null) {
				// attribute no longer exists, so discard it
				toReuse.add(vas.remove(i).buffer);
			}
		}
		
		// update all vertex arrays
		Map<String, VectorBuffer> attrs = g.getAttributes();
		for (Entry<String, VectorBuffer> a: attrs.entrySet()) {
			VertexArray va = null;
			for (int i = vas.size() - 1; i >= 0; i--) {
				if (vas.get(i).name.equals(a.getKey())) {
					// found a match
					va = vas.get(i);
					break;
				}
			}
			
			// in case we haven't found an existing one
			if (va == null) {
				va = new VertexArray(a.getKey());
				vas.add(va);
			}
			
			// update the vertex array's FloatBuffer
			va.elementSize = a.getValue().getElementSize();
			float[] data = a.getValue().getData();
			if (va.buffer == null || data.length > va.buffer.capacity()) {
				// need a new buffer, first check the pool, then allocate
				int bestDiff = Integer.MAX_VALUE;
				FloatBuffer buff = null;
				for (int i = toReuse.size() - 1; i >= 0; i--) {
					int diff = toReuse.get(i).capacity() - data.length;
					if (diff == 0) { // will be best, so break now
						buff = toReuse.get(i);
						break;
					} else if (diff > 0 && diff < bestDiff) {
						bestDiff = diff;
						buff = toReuse.get(i);
					}
				}
				
				if (buff == null)
					buff = ByteBuffer.allocateDirect(data.length * 4).order(ByteOrder.nativeOrder()).asFloatBuffer();
				else
					toReuse.remove(buff); // can't reuse this anymore
				
				buff.position(0).limit(data.length);
				buff.put(data).rewind();
				
				if (va.buffer != null) // reuse existing buffer
					toReuse.add(va.buffer);
				va.buffer = buff;
			} else {
				// reuse assigned FloatBuffer, if possible just update based on dirty state
				if (dirty == null) {
					va.buffer.limit(data.length).position(0);
					va.buffer.put(data).rewind();
				} else {
					BufferRange br = dirty.getModifiedAttributes().get(va.name);
					if (br != null) { 
						// only update if attribute is modified
						va.buffer.limit(data.length).position(br.getOffset());
						va.buffer.put(data, br.getOffset(), br.getLength()).rewind();
					}
				}
			}
		}
		
		// update indices data and counts
		boolean computeMinMax = false;
		int[] indices = g.getIndices();
		if (handle.indices == null || handle.indices.capacity() < indices.length) {
			// make new indices
			handle.indices = ByteBuffer.allocateDirect(indices.length * 4).order(ByteOrder.nativeOrder()).asIntBuffer();
			handle.indices.put(indices).rewind();
			computeMinMax = true;
		} else {
			// reuse index buffer, and possibly just update the indices
			if (dirty == null) {
				handle.indices.limit(indices.length).position(0);
				handle.indices.put(indices).rewind();
				computeMinMax = true;
			} else {
				BufferRange br = dirty.getModifiedIndices();
				if (br != null) {
					// only update if indices were modified
					handle.indices.limit(indices.length).position(br.getOffset());
					handle.indices.put(indices, br.getOffset(), br.getLength()).rewind();
					computeMinMax = true;
				}
			}
		}
		
		handle.indexCount = indices.length;
		handle.polyCount = g.getPolygonType().getPolygonCount(handle.indexCount);
		if (computeMinMax)
			updateMinMaxIndices(handle, indices);
	}
	
	private int getVboType(CompileType type) {
		return (type == CompileType.RESIDENT_STATIC ? GL2GL3.GL_STATIC_DRAW : GL2GL3.GL_STREAM_DRAW);
	}
	
	private void glArrayData(GL2GL3 gl, int vboType, int vboOffset, int dataOffset, int len, float[] data) {
		FloatBuffer fb = FloatBuffer.wrap(data, dataOffset, len);
		gl.glBufferSubData(GL2GL3.GL_ARRAY_BUFFER, vboOffset, len * 4, fb);
	}
	
	private boolean checkVertexArraySize(Geometry g) {
		Map<String, VectorBuffer> attrs = g.getAttributes();
		int elementCount = -1;
		for (VectorBuffer vb: attrs.values()) {
			int e = vb.getData().length / vb.getElementSize();
			if (elementCount < 0)
				elementCount = e;
			else if (e != elementCount)
				return false;
		}
		
		// true if all counts are valid, and at least one vector buffer exists
		return elementCount > 0;
	}
	
	private void createBuffers(GL2GL3 gl, GeometryHandle handle) {
		int[] id = new int[2];
		gl.glGenBuffers(2, id, 0);
		
		handle.arrayVbo = id[0];
		handle.elementVbo = id[1];
	}
	
	private void updateMinMaxIndices(GeometryHandle handle, int[] indices) {
		int min = Integer.MAX_VALUE;
		int max = Integer.MIN_VALUE;
		
		for (int i = 0; i < indices.length; i++) {
			if (indices[i] < min)
				min = indices[i];
			if (indices[i] > max)
				max = indices[i];
		}
		
		handle.minIndex = min;
		handle.maxIndex = max;
	}
}
