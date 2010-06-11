package com.ferox.renderer.impl.resource;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import com.ferox.renderer.RenderCapabilities;
import com.ferox.resource.DirtyState;
import com.ferox.resource.Geometry;
import com.ferox.resource.GeometryDirtyState;
import com.ferox.resource.Resource;
import com.ferox.resource.VectorBuffer;
import com.ferox.resource.Geometry.CompileType;
import com.ferox.resource.GeometryDirtyState.BufferRange;
import com.ferox.resource.Resource.Status;

/**
 * AbstractGeometryResourceDriver is an almost complete implementation of a
 * ResourceDriver for {@link Geometry} instances. Subclasses are only required
 * to implement the actual OpenGL calls needed by the driver to perform its
 * work. This ResourceDriver uses {@link GeometryHandle} as its resource handle
 * type.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractGeometryResourceDriver implements ResourceDriver {
    private static final float REALLOC_FACTOR = .75f; // fraction of new buffer size that triggers a realloc
    
    private final boolean hasVboSupport;
    
    public AbstractGeometryResourceDriver(RenderCapabilities caps) {
        if (caps == null)
            throw new NullPointerException("Must specify a non-null RenderCapabilities");
        hasVboSupport = caps.getVertexBufferSupport();
    }
    
    @Override
    public void dispose(ResourceHandle handle) {
        GeometryHandle h = (GeometryHandle) handle;
        if (hasVboSupport && h.arrayVbo > 0)
            glDeleteBuffers(h);
    }

    /**
     * Destroy the VBO objects stored within <code>handle.arrayVbo</code> and
     * <code>handle.elementVbo</code>. It can be assumed that both are currently
     * existing vbos.
     * 
     * @param handle
     */
    protected abstract void glDeleteBuffers(GeometryHandle handle);
    
    @Override
    public ResourceHandle init(Resource res) {
        Geometry g = (Geometry) res;
        CompileType compile = g.getCompileType();
        String statusMsg = "";
        
        if (compile != CompileType.NONE && !hasVboSupport) {
            compile = CompileType.NONE;
            statusMsg = "Cannot support " + compile + ", defaulting to CompileType.NONE";
        }
        
        GeometryHandle handle = new GeometryHandle(compile);
        if (compile != CompileType.NONE)
            glCreateBuffers(handle);
        
        handle.setStatus(Status.READY);
        handle.setStatusMessage(statusMsg);
        
        update(g, handle, null);
        return handle;
    }

    /**
     * Allocate one array VBO and one element VBO and store their ids in the
     * appropriate fields within the given handle.
     * 
     * @param handle
     */
    protected abstract void glCreateBuffers(GeometryHandle handle);

    @Override
    public Status update(Resource res, ResourceHandle handle, DirtyState<?> dirtyState) {
        update((Geometry) res, (GeometryHandle) handle, (GeometryDirtyState) dirtyState);
        return handle.getStatus();
    }

    private void update(Geometry g, GeometryHandle handle, GeometryDirtyState dirty) {
        // check for errors
        if (g.getIndices() == null) {
            handle.setStatus(Status.ERROR);
            handle.setStatusMessage("Geometry cannot have null indices");
            return;
        }
        if (g.getAttributes().isEmpty()) {
            handle.setStatus(Status.ERROR);
            handle.setStatusMessage("Geometry must have at least on vertex attribute");
            return;
        }
        if (!checkVertexArraySize(g)) {
            handle.setStatus(Status.ERROR);
            handle.setStatusMessage("Geometry vertex attributes have mismatched element counts");
            return;
        }
        
        handle.indexCount = g.getIndices().capacity();
        handle.polyType = g.getPolygonType();
        handle.polyCount = handle.polyType.getPolygonCount(handle.indexCount);

        if (handle.compile != CompileType.NONE)
            updateForVbo(g, handle, dirty);
        else
            updateForVertexArray(g, handle, dirty);
    }

    /**
     * Bind both the array and element vbos to the current context so that
     * subsequent calls to glArrayData and glElementData modify the vbos of the
     * given handle. It can be assumed that the handle has valid vbo ids.
     * 
     * @param handle
     */
    protected abstract void glBindBuffers(GeometryHandle handle);
    
    /**
     * Restore the array and element vbo bindings to the state before the
     * last call to {@link #glBindBuffers(GeometryHandle)} was made on
     * the current thread.
     */
    protected abstract void glRestoreBuffers();
    
    private void updateForVbo(Geometry g, GeometryHandle handle, GeometryDirtyState dirty) {
        glBindBuffers(handle);
        
        List<VertexArray> vas = handle.compiledPointers;
        if (dirty != null) {
            boolean fullAllocate = false;
            
            List<VertexArray> reuseVbos = new ArrayList<VertexArray>();
            List<String> newBuffersReq = new ArrayList<String>();
            
            // check for buffers that have grown beyond their orig size
            // or that have been removed
            for (int i = vas.size() - 1; i >= 0; i--) {
                VertexArray va = vas.get(i);
                VectorBuffer vb = g.getAttribute(va.name);
                
                if (vb == null) {
                    // buffer has been removed from geometry, so attempt to
                    // reuse the vbo range it previously took up
                    reuseVbos.add(vas.remove(i));
                } else if (vb.getData().capacity() * 4 > va.vboLen) {
                    // buffer has grown beyond previously allocated capacity
                    newBuffersReq.add(va.name);
                    reuseVbos.add(vas.remove(i));
                }
            }
            
            newBuffersReq.addAll(dirty.getAddedAttributes()); // we assume this is correct
            // attempt to find positions within the vbo for the 
            // buffers that require assignment (new and expanded buffers)
            for (int i = newBuffersReq.size() - 1; i >= 0; i--) {
                VectorBuffer vb = g.getAttribute(newBuffersReq.get(i));
                VertexArray best = null;
                int bestDiff = Integer.MAX_VALUE;
                
                // search through no-longer used ranges for a best fit
                for (int j = reuseVbos.size() - 1; j >= 0; j--) {
                    int diff = reuseVbos.get(j).vboLen - vb.getData().capacity() * 4;
                    if (diff == 0) {
                        // just use this, since it's the best possible
                        best = reuseVbos.get(j);
                        break;
                    } else if (diff > 0 && diff < bestDiff) {
                        best = reuseVbos.get(j);
                        bestDiff = diff;
                    }
                }
                
                if (best != null) {
                    // found a range for the new attribute
                    VertexArray va = new VertexArray(newBuffersReq.get(i));
                    va.offset = best.offset;
                    va.vboLen = best.vboLen;
                    vas.add(va);
                } else {
                    // no room left, do a full allocate
                    fullAllocate = true;
                    break;
                }
            }
            
            if (reuseVbos.size() > 0)
                fullAllocate = true; // must compact the vbo
            
            if (!fullAllocate) {
                for (Entry<String, VectorBuffer> attr: g.getAttributes().entrySet()) {
                    String name = attr.getKey();
                    VectorBuffer vb = attr.getValue();

                    // look up VertexArray with given attribute name
                    VertexArray array = null;
                    for (int i = vas.size(); i >= 0; i--) {
                        if (vas.get(i).name.equals(name)) {
                            array = vas.get(i);
                            break;
                        }
                    }
                    
                    array.elementSize = vb.getElementSize();
                    BufferRange br = dirty.getModifiedAttributes().get(name);
                    if (br != null) {
                        // update based on dirty range
                        glArrayData(array.offset, br.getOffset(), br.getLength(), vb.getData());
                    } else if (newBuffersReq.contains(name)) {
                        // new attribute, so send the entire range
                        glArrayData(array.offset, 0, vb.getData().capacity(), vb.getData());
                    } // else ... no change needed
                }
            } else {
                // full allocation is required
                updateVboAttributesFull(g, handle);
            }
        } else {
            // full allocation required because we have no dirty info
            updateVboAttributesFull(g, handle);
        }
        
        // now handle the indice's element vbo
        IntBuffer indices = g.getIndices();
        int indexLength = indices.capacity() * 4;
        BufferRange dirtyI = (dirty == null ? null : dirty.getModifiedIndices());
        boolean computeMinMax = false;
        
        if (indexLength > handle.elementVboSize || handle.elementVboSize * REALLOC_FACTOR > indexLength) {
            glAllocateElementData(handle.compile, indices);
            handle.elementVboSize = indexLength;
            computeMinMax = true;
        } else if (dirty == null || dirtyI != null) {
            if (dirty == null)
                glElementData(0, 0, indices.capacity(), indices);
            else
                glElementData(0, dirtyI.getOffset() * 4, dirtyI.getLength(), indices);
            computeMinMax = true;
        }
        
        if (computeMinMax)
            updateMinMaxIndices(handle, indices);
        glRestoreBuffers();
    }
    
    private void updateVboAttributesFull(Geometry g, GeometryHandle handle) {
        List<VertexArray> vas = handle.compiledPointers;
        vas.clear();
        
        // position all attributes packed into the array vbo
        int offset = 0;
        for (Entry<String, VectorBuffer> attr: g.getAttributes().entrySet()) {
            VertexArray va = new VertexArray(attr.getKey());
            VectorBuffer vb = attr.getValue();
            
            va.elementSize = vb.getElementSize();
            va.offset = offset;
            va.vboLen = vb.getData().capacity() * 4;
            
            vas.add(va);
            offset += va.vboLen;
        }
        
        // at this point, offset contains the correct vbo size
        if (handle.arrayVboSize < offset || handle.arrayVboSize * REALLOC_FACTOR > offset) {
            glAllocateArrayData(handle.compile, offset);
            handle.arrayVboSize = offset;
        }
        
        // transfer attribute data
        for (int i = 0; i < vas.size(); i++) {
            VertexArray va = vas.get(i);
            FloatBuffer data = g.getAttribute(va.name).getData();
            glArrayData(va.offset, 0, data.capacity(), data);
        }
    }
    
    /**
     * Use glBufferSubData to transfer the contents of data to the currently
     * bound array buffer. Only dataLen float elements should be transferred
     * starting at dataOffset from data. The data should be inserted at
     * vboOffset.
     */
    protected abstract void glArrayData(int vboOffset, int dataOffset, int dataLen, FloatBuffer data);

    /**
     * As {@link #glArrayData(CompileType, int, int, int, FloatBuffer)} but for
     * the currently bound element array buffer.
     */
    protected abstract void glElementData(int vboOffset, int dataOffset, int dataLen, IntBuffer data);

    /**
     * Re-allocate the current array vbo using glBufferData to have a backing
     * store of the given <tt>vboSize</tt>.
     */
    protected abstract void glAllocateArrayData(CompileType type, int vboSize);

    /**
     * Re-allocate the current element vbo using glBufferData to have its
     * contents set to those within <tt>data</tt>. The element vbo should be
     * re-sized to hold the given IntBuffer perfectly.
     */
    protected abstract void glAllocateElementData(CompileType type, IntBuffer data);
    
    private void updateForVertexArray(Geometry g, GeometryHandle handle, GeometryDirtyState dirty) {
        List<VertexArray> vas = handle.compiledPointers;
        vas.clear();
        
        // this is quick, just re-create all vertex arrays
        for (Entry<String, VectorBuffer> attr: g.getAttributes().entrySet()) {
            VertexArray va = new VertexArray(attr.getKey());
            VectorBuffer vb = attr.getValue();
            
            va.buffer = vb.getData();
            va.elementSize = vb.getElementSize();
        }
        
        // now update the indices as well
        IntBuffer indices = g.getIndices();
        if (indices != handle.indices) {
            handle.indices = indices;
            updateMinMaxIndices(handle, indices);
        }
    }
    
    private boolean checkVertexArraySize(Geometry g) {
        int elementCount = -1;
        for (VectorBuffer vb: g.getAttributes().values()) {
            int e = vb.getData().capacity() / vb.getElementSize();
            if (elementCount < 0)
                elementCount = e;
            else if (e != elementCount)
                return false;
        }
        
        // true if all counts are valid, and at least one vector buffer exists
        return elementCount > 0;
    }
    
    private void updateMinMaxIndices(GeometryHandle handle, IntBuffer indices) {
        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;
        
        for (int i = 0; i < indices.capacity(); i++) {
            int index = indices.get(i);
            if (index < min)
                min = index;
            if (index > max)
                max = index;
        }
        
        handle.minIndex = min;
        handle.maxIndex = max;
    }
}
