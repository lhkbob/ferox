package com.ferox.resource.builder;

/**
 *
 */
public interface ArrayImageBuilder<T, M> {
    public M mipmap(int index, int mipmap);

    public T build();
}
