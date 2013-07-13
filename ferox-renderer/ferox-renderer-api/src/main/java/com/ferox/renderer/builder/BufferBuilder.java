package com.ferox.renderer.builder;

/**
 * BufferBuilder is the super-type builder interface for all {@link com.ferox.renderer.Buffer} resources.
 *
 * @author Michael Ludwig
 */
public interface BufferBuilder<B extends BufferBuilder<B>> {
    /**
     * Configure the builder to create a buffer that is intended for frequent refreshes. The framework can
     * optimize where the memory is stored on the GPU in native buffers to make refreshes faster if they
     * happen often. This is usually at a tradeoff with raw rendinger speed.
     *
     * @return The builder
     */
    public B dynamic();
}
