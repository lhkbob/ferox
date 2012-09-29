package com.ferox.renderer.impl.drivers;

import java.nio.Buffer;

import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * VertexBufferObjectHandle is the handle type that represents the persisted
 * state of a VertexBufferObject, and is used by all
 * {@link AbstractVertexBufferObjectResourceDriver}.
 * 
 * @author Michael Ludwig
 */
public class VertexBufferObjectHandle {
    public DataType dataType;
    public int length;

    public StorageMode mode;
    public Buffer inmemoryBuffer;

    public int lastSyncedVersion;
    public Object lastSyncedKey;

    public final int vboID;

    public VertexBufferObjectHandle(VertexBufferObject res) {
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
