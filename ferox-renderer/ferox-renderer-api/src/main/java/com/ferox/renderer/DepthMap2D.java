package com.ferox.renderer;

/**
 * DepthMap2D is a depth map for a single two dimensional image. It can be square or rectangular. It has a
 * single render target that can be used as a the depth/stencil target for a {@link TextureSurface}. Shaders
 * can refer to a DepthMap2D in the GLSL code with the 'sampler2DShadow' uniform type when the depth
 * comparison is not null. If the depth comparison is null, the 'sampler2D' type should be used instead.
 *
 * @author Michael Ludwig
 * @see Texture2D
 */
public interface DepthMap2D extends DepthMap {
    /**
     * @return The render target to render directly into the depth map
     */
    public RenderTarget getRenderTarget();
}
