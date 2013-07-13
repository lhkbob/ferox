package com.ferox.renderer;

/**
 * TextureCubeMap is a color texture that has six separate two dimensional images. Each face can be used as a
 * render target for render-to-texture with a {@link TextureSurface}. All six images have the same width and
 * height so they represent the six faces of a cube. Shaders can refer to a TextureCubeMap in the GLSL code
 * with the 'samplerCube' uniform type.
 *
 * @author Michael Ludwig
 */
public interface TextureCubeMap extends Texture {
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
