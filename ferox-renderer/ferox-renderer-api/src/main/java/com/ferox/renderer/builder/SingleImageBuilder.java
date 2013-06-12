package com.ferox.renderer.builder;

/**
 *
 */
public interface SingleImageBuilder<T, M> {
    public M mipmap(int level);

    public T build();
}
