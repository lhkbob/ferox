package com.ferox.resource.geom;

import com.ferox.resource.data.ElementData;

/**
 *
 */
public class ElementBufferObject extends BufferObject<ElementData<?>> {
    public ElementBufferObject(ElementData<?> data) {
        super(data);
    }

    public ElementBufferObject(ElementData<?> data, StorageMode storageMode) {
        super(data, storageMode);
    }
}
