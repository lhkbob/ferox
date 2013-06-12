package com.ferox.renderer.builder;

import com.ferox.renderer.VertexBuffer;

/**
 *
 */
public interface VertexBufferBuilder extends BufferBuilder<VertexBufferBuilder> {
    public VertexBuffer from(float[] data);

    public VertexBuffer fromNormalized(int[] data);

    public VertexBuffer fromNormalized(short[] data);

    public VertexBuffer fromNormalized(byte[] data);

    public VertexBuffer from(int[] data);

    public VertexBuffer from(short[] data);

    public VertexBuffer from(byte[] data);
}
