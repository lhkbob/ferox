package com.ferox.resource.geom;

import com.ferox.resource.data.VertexData;

/**
 *
 */
public class VertexBufferObject extends BufferObject<VertexData<?>> {
    public VertexBufferObject(VertexData<?> data) {
        super(data);
    }

    public VertexBufferObject(VertexData<?> data, StorageMode storageMode) {
        super(data, storageMode);
    }
}
