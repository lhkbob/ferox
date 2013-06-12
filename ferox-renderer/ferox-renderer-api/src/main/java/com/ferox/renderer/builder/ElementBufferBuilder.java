package com.ferox.renderer.builder;

import com.ferox.renderer.ElementBuffer;

/**
 *
 */
public interface ElementBufferBuilder extends BufferBuilder<ElementBufferBuilder> {
    public ElementBuffer fromUnsigned(int[] data);

    public ElementBuffer fromUnsigned(short[] data);

    public ElementBuffer fromUnsigned(byte[] data);
}
