package com.ferox.renderer.impl.drivers;

import java.nio.Buffer;

import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * VertexBufferObjectHandle is a concrete subclass of ResourceHandle that
 * represents the persisted state of a VertexBufferObject, and is used by any
 * {@link AbstractVertexBufferObjectResourceDriver} when they manage
 * VertexBufferObjects.
 * 
 * @author Michael Ludwig
 */
public class VertexBufferObjectHandle extends ResourceHandle {
    public DataType dataType;
    public int length;
    
    public StorageMode mode;
    public Buffer inmemoryBuffer;
    
    public int lastSyncedVersion;
    public Object lastSyncedKey;
    
    public final int vboID;
    
    public VertexBufferObjectHandle(VertexBufferObject res) {
        super(res);
        
        vboID = res.getId();
        
        // blank parameters
        length = 0;
        inmemoryBuffer = null;
        dataType = null;
        lastSyncedVersion = -1;
        lastSyncedKey = null;
        mode = null;
    }
}
