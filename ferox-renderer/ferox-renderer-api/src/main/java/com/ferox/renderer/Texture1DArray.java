package com.ferox.renderer;

/**
 * Texture1DArray is an array of one dimensional textures. Each 1D texture has the same width, data type, and
 * format. When supported by the hardware, Texture1DArrays allow many effective textures to be packed into a
 * single unit.  Unlike regular Texture1D's, the T texture coordinate is used as an index into the array. If
 * no T value is provided, the default is 0 and references the first image.
 * <p/>
 * In a related point, the method {@link #getRenderTarget()} returns the same render target as a call to
 * {@code getRenderTarget(0)}.
 * <p/>
 * Shaders can refer to a Texture1DArray in the GLSL code with the 'sampler1DArray' uniform type.
 *
 * @author Michael Ludwig
 */
public interface Texture1DArray extends Texture1D, TextureArray {
}
