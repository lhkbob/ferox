package com.ferox.renderer;

/**
 * DepthCubeMap is a DepthMap that has six separate two dimensional images storing depth
 * and optionally stencil data. Each face can be used as a depth/stencil render target for
 * render-to-texture with a {@link TextureSurface}. All six images have the same width and
 * height so they represent the six faces of a cube. Shaders can refer to a DepthCubeMap
 * in the GLSL code with the 'samplerCubeShadow' uniform type when the depth comparison is
 * not null. If the comparison is null, 'samplerCube' should be used instead.
 *
 * @author Michael Ludwig
 * @see TextureCubeMap
 */
public interface DepthCubeMap extends DepthMap {
    /**
     * @return A RenderTarget that renders into the positive-x face of the cube
     */
    public RenderTarget getPositiveXRenderTarget();

    /**
     * @return A RenderTarget that renders into the negative-x face of the cube
     */
    public RenderTarget getNegativeXRenderTarget();

    /**
     * @return A RenderTarget that renders into the positive-y face of the cube
     */
    public RenderTarget getPositiveYRenderTarget();

    /**
     * @return A RenderTarget that renders into the negative-x face of the cube
     */
    public RenderTarget getNegativeYRenderTarget();

    /**
     * @return A RenderTarget that renders into the positive-z face of the cube
     */
    public RenderTarget getPositiveZRenderTarget();

    /**
     * @return A RenderTarget that renders into the negative-z face of the cube
     */
    public RenderTarget getNegativeZRenderTarget();
}
