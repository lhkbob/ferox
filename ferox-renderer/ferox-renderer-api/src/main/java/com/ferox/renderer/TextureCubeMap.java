package com.ferox.renderer;

/**
 *
 */
public interface TextureCubeMap extends Texture {
    public RenderTarget getPositiveXRenderTarget();

    public RenderTarget getNegativeXRenderTarget();

    public RenderTarget getPositiveYRenderTarget();

    public RenderTarget getNegativeYRenderTarget();

    public RenderTarget getPositiveZRenderTarget();

    public RenderTarget getNegativeZRenderTarget();
}
