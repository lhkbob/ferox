package com.ferox.renderer;

/**
 *
 */
public interface TextureArray {
    public int getImageCount();

    public Sampler.RenderTarget getRenderTarget(int image);
}
