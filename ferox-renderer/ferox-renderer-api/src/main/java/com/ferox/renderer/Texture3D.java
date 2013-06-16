package com.ferox.renderer;

/**
 * Texture3D is a three-dimensional color texture. It is accessed using the S, T, and R
 * texture coordinates. Shaders can refer to a Texture3D in the GLSL code with the
 * 'sampler3D' uniform type.
 *
 * @author Michael Ludwig
 */
public interface Texture3D extends Texture {
    /**
     * Get the render target that will render into the specific 2D slice of the 3D
     * texture. The layer must be greater than 0 and less than {@code getDepth() - 1}.
     *
     * @param layer The depth layer to fetch
     *
     * @return The render target for the 2D slice of the texture
     *
     * @throws IndexOutOfBoundsException if layer is less than 0 or greater than the
     *                                   number of 2D images in the texture
     */
    public RenderTarget getRenderTarget(int layer);
}
