package com.ferox.renderer;

/**
 *
 */
public interface DepthCubeMap extends DepthMap {
    public RenderTarget getPositiveXRenderTarget();

    public RenderTarget getNegativeXRenderTarget();

    public RenderTarget getPositiveYRenderTarget();

    public RenderTarget getNegativeYRenderTarget();

    public RenderTarget getPositiveZRenderTarget();

    public RenderTarget getNegativeZRenderTarget();
}
