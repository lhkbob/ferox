package com.ferox.renderer.impl.drivers;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;
import java.util.List;

import com.ferox.renderer.impl.BufferUtil;
import com.ferox.renderer.impl.OpenGLContext;
import com.ferox.renderer.impl.ResourceDriver;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.resource.BufferData;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.DataRange;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.Resource.Status;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * AbstractVertexBufferObjectResourceDriver is a ResourceDriver for
 * VertexBufferObjects. It implements all necessary logic except for the actual
 * calls to OpenGL, which it exposes as protected abstract methods. It uses a
 * {@link VertexBufferObjectHandle} for its handles.
 * 
 * @author Michael Ludwig
 */
public abstract class AbstractVertexBufferObjectResourceDriver implements ResourceDriver<VertexBufferObject> {

    @Override
    public ResourceHandle update(OpenGLContext context, VertexBufferObject res, ResourceHandle handle) {
        if (handle == null)
            handle = new VertexBufferObjectHandle(res);
        VertexBufferObjectHandle h = (VertexBufferObjectHandle) handle;
        
        StorageMode newMode = (context.getRenderCapabilities().getVertexBufferSupport() ? res.getStorageMode() : StorageMode.IN_MEMORY);
        
        boolean oldIsGPU = h.mode == StorageMode.GPU_DYNAMIC || h.mode == StorageMode.GPU_STATIC;
        boolean newIsGPU = newMode == StorageMode.GPU_DYNAMIC || newMode == StorageMode.GPU_STATIC;
        boolean storageModeChange = h.mode != newMode;
        
        boolean isElementBuffer = res.getData().getDataType() != DataType.FLOAT;
        
        if (storageModeChange || res.getChangeQueue().isVersionStale(h.lastSyncedVersion)) {
            if (storageModeChange) {
                // Do some clean-up depending on the storage mode change
                if (oldIsGPU && !newIsGPU)
                    glDeleteBuffer(context, h);
            }
            
            if (storageModeChange || h.lastSyncedKey != res.getData().getKey() 
                || res.getChangeQueue().hasLostChanges(h.lastSyncedVersion)) {
                // Must push the whole buffer
                if (newIsGPU) {
                    // Operate on an actual VBO
                    Buffer nioData = bufferAlloc(res.getData(), h.inmemoryBuffer);
                    
                    if (nioData != null || h.lastSyncedKey == null) {
                        if (isElementBuffer) {
                            glBindElementBuffer(context, h);
                            glElementBufferData(context, nioData, res.getData().getDataType(), res.getData().getLength(), newMode);
                            glRestoreElementBuffer(context);
                        } else {
                            glBindArrayBuffer(context, h);
                            glArrayBufferData(context, nioData, res.getData().getDataType(), res.getData().getLength(), newMode);
                            glRestoreArrayBuffer(context);
                        }
                    } // no data and we've allocated data before, so don't change anything
                    
                    h.inmemoryBuffer = (res.getStorageMode() == StorageMode.GPU_DYNAMIC ? nioData : null);
                } else {
                    // Storage mode is IN_MEMORY so we want a buffer
                    Buffer nioData = bufferAlloc(res.getData(), h.inmemoryBuffer);
                    if (nioData == null)
                        nioData = BufferUtil.newBuffer(res.getData().getDataType(), res.getData().getLength());
                    h.inmemoryBuffer = nioData;
                }
                
                // Update properties in handle that could have changed because of doing a full push
                h.dataType = res.getData().getDataType();
                h.mode = newMode;
                h.length = res.getData().getLength();
                h.lastSyncedKey = res.getData().getKey();
            } else {
                // Process all queued changes
                if (res.getData().getArray() != null) {
                    // Can only update if we actually have data to sync
                    List<DataRange> changes = res.getChangeQueue().getChangesSince(h.lastSyncedVersion);
                    int numChanges = changes.size();
                    
                    if (h.mode != StorageMode.IN_MEMORY) {
                        if (isElementBuffer)
                            glBindElementBuffer(context, h);
                        else
                            glBindArrayBuffer(context, h);
                    }

                    for (int i = 0; i < numChanges; i++) {
                        DataRange range = changes.get(i);
                        int offset = Math.min(range.getOffset(), h.length);
                        int length = Math.min(range.getLength(), h.length - offset);
                        
                        if (h.mode == StorageMode.IN_MEMORY) {
                            h.inmemoryBuffer.position(offset);
                            bulkPut(res.getData(), offset, length, h.inmemoryBuffer);
                            h.inmemoryBuffer.rewind();
                        } else {
                            Buffer nioData;
                            if (h.mode == StorageMode.GPU_DYNAMIC && h.inmemoryBuffer != null) {
                                nioData = h.inmemoryBuffer;
                                nioData.limit(length).position(0);
                            } else {
                                nioData = BufferUtil.newBuffer(res.getData().getDataType(), length);
                            }
                            nioData.rewind();
                            
                            if (isElementBuffer)
                                glElementBufferSubData(context, nioData, res.getData().getDataType(), offset, length);
                            else
                                glArrayBufferSubData(context, nioData, res.getData().getDataType(), offset, length);
                        }
                    }
                }
                
                h.lastSyncedVersion = res.getChangeQueue().getVersion();
                
                if (h.mode != res.getStorageMode())
                    h.setStatusMessage("GPU storage modes are not supported, using IN_MEMORY instead");
                h.setStatus(Status.READY);
            }
        }
       
        return handle;
    }

    @Override
    public void reset(VertexBufferObject res, ResourceHandle handle) {
        if (handle instanceof VertexBufferObjectHandle) {
            VertexBufferObjectHandle h = (VertexBufferObjectHandle) handle;
            
            h.lastSyncedKey = null;
            h.lastSyncedVersion = 0;
        }
    }

    @Override
    public void dispose(OpenGLContext context, ResourceHandle handle) {
        if (handle instanceof VertexBufferObjectHandle) {
            VertexBufferObjectHandle h = (VertexBufferObjectHandle) handle;
            if (h.mode != StorageMode.IN_MEMORY)
                glDeleteBuffer(context, (VertexBufferObjectHandle) handle);
        }
    }

    @Override
    public Class<VertexBufferObject> getResourceType() {
        return VertexBufferObject.class;
    }
    
    private Buffer bufferAlloc(BufferData data, Buffer result) {
        if (data.getArray() != null) {
            if (BufferUtil.getBufferType(data.getDataType()).isInstance(result)
                && result.capacity() >= data.getLength()) {
                result.limit(data.getLength()).position(0);

                switch(data.getDataType()) {
                case FLOAT: ((FloatBuffer) result).put(data.<float[]>getArray()); break;
                case UNSIGNED_BYTE: ((ByteBuffer) result).put(data.<byte[]>getArray()); break;
                case UNSIGNED_INT: ((IntBuffer) result).put(data.<int[]>getArray()); break;
                case UNSIGNED_SHORT:((ShortBuffer) result).put(data.<short[]>getArray()); break; 
                }

                return result.rewind();
            } else {
                // alloc new data
                return BufferUtil.newBuffer(data);
            }
        } else
            return null;
    }
    
    private void bulkPut(BufferData data, int offset, int length, Buffer result) {
        switch(data.getDataType()) {
        case FLOAT:
            float[] fd = data.getArray();
            ((FloatBuffer) result).put(fd, offset, length);
            break;
        case UNSIGNED_BYTE:
            byte[] bd = data.getArray();
            ((ByteBuffer) result).put(bd, offset, length);
            break;
        case UNSIGNED_INT:
            int[] id = data.getArray();
            ((IntBuffer) result).put(id, offset, length);
            break;
        case UNSIGNED_SHORT:
            short[] sd = data.getArray();
            ((ShortBuffer) result).put(sd, offset, length);
            break;
        }
    }

    /**
     * Delete the VBO stored in the handle's <tt>vboID</tt>
     * 
     * @param context
     * @param handle
     */
    protected abstract void glDeleteBuffer(OpenGLContext context, VertexBufferObjectHandle handle);

    /**
     * Bind the VBO represented by the handle's <tt>vboID</tt> to the array
     * buffer target.
     * 
     * @param context
     * @param handle
     */
    protected abstract void glBindArrayBuffer(OpenGLContext context, VertexBufferObjectHandle handle);

    /**
     * Bind the VBO represented by the handle's <tt>vboID</tt> to the element
     * buffer target.
     * 
     * @param context
     * @param handle
     */
    protected abstract void glBindElementBuffer(OpenGLContext context, VertexBufferObjectHandle handle);

    /**
     * Rebind whatever VBO binding was overridden by the last call to
     * glBindArrayBuffer.
     * 
     * @param context
     */
    protected abstract void glRestoreArrayBuffer(OpenGLContext context);
    
    /**
     * Rebind whatever VBO binding was overridden by the last call to
     * glBindElementBuffer.
     * 
     * @param context
     */
    protected abstract void glRestoreElementBuffer(OpenGLContext context);

    /**
     * Invoke glBufferData on the array buffer target. The provided storage mode
     * will be one of GPU_STATIC or GPU_DYNAMIC. The buffer may be null, which
     * is why the type and length are provided. If the buffer is not null, its
     * position and limit are configured correctly.
     * 
     * @param context
     * @param buffer
     * @param type
     * @param length
     * @param mode
     */
    protected abstract void glArrayBufferData(OpenGLContext context, Buffer data, DataType type, int length, StorageMode mode);

    /**
     * Invoke glBufferData on the element buffer target. The provided storage
     * mode will be one of GPU_STATIC or GPU_DYNAMIC. The buffer may be null,
     * which is why the type and length are provided. If the buffer is not null,
     * its position and limit are configured correctly.
     * 
     * @param context
     * @param buffer
     * @param type
     * @param length
     * @param mode
     */
    protected abstract void glElementBufferData(OpenGLContext context, Buffer data, DataType type, int length, StorageMode mode);

    /**
     * Invoke glBufferSubData on the array buffer target. The buffer will not be
     * null and its position and limit are configured already. The offset and
     * length are into the VBO, and are in units of <tt>type</tt>.
     * 
     * @param context
     * @param data
     * @param type
     * @param offset
     * @param length
     */
    protected abstract void glArrayBufferSubData(OpenGLContext context, Buffer data, DataType type, int offset, int length);
    
    /**
     * Invoke glBufferSubData on the element buffer target. The buffer will not be
     * null and its position and limit are configured already. The offset and
     * length are into the VBO, and are in units of <tt>type</tt>.
     * 
     * @param context
     * @param data
     * @param type
     * @param offset
     * @param length
     */
    protected abstract void glElementBufferSubData(OpenGLContext context, Buffer data, DataType type, int offset, int length);
}
