package com.ferox.renderer.builder;

/**
 *
 */
public interface CubeImageBuilder<T, M> {
    public M positiveX(int mipmap);

    public M positiveY(int mipmap);

    public M positiveZ(int mipmap);

    public M negativeX(int mipmap);

    public M negativeY(int mipmap);

    public M negativeZ(int mipmap);

    public T build();
}
