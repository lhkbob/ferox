package com.ferox.renderer;

/**
 * Texture1D is a one-dimensional color texture. It is accessed using the S texture
 * coordinate.  Its height and depth can be considered to be 1. Shaders can refer to a
 * Texture1D in the GLSL code with the 'sampler1D' uniform type.
 *
 * @author Michael Ludwig
 */
public interface Texture1D extends Texture {
    /**
     * @return The single render target for this texture
     */
    public RenderTarget getRenderTarget();
}
