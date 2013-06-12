package com.ferox.resource.builder;

import com.ferox.resource.ElementBuffer;

/**
 *
 */
public interface ElementBufferBuilder extends BufferBuilder<ElementBufferBuilder> {
    public ElementBuffer fromUnsigned(int[] data);

    public ElementBuffer fromUnsigned(short[] data);

    public ElementBuffer fromUnsigned(byte[] data);
}
