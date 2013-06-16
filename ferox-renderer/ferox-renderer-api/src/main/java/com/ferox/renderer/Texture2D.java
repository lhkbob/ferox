package com.ferox.renderer;

/**
 * Texture2D is a two-dimensional color texture. It is accessed using the S and T texture
 * coordinates.  Its depth can be considered to be 1. Shaders can refer to a Texture2D in
 * the GLSL code with the 'sampler2D' uniform type.
 *
 * @author Michael Ludwig
 */
public interface Texture2D extends Texture {
    /**
     * @return The single render target for this texture
     */
    public RenderTarget getRenderTarget();
}
