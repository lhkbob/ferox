package com.ferox.renderer;

/**
 * Texture2DArray is an array of two dimensional textures. Each 2D texture has the same width, height, data
 * type, and format. When supported by the hardware, Texture2DArrays allow many effective textures to be
 * packed into a single unit.  Unlike regular Texture2D's, the R texture coordinate is used as an index into
 * the array. If no R value is provided, the default is 0 and references the first image.
 * <p/>
 * In a related point, the method {@link #getRenderTarget()} returns the same render target as a call to
 * {@code getRenderTarget(0)}.
 * <p/>
 * Shaders can refer to a Texture2DArray in the GLSL code with the 'sampler2DArray' uniform type.
 *
 * @author Michael Ludwig
 */
public interface Texture2DArray extends Texture2D, TextureArray {
}
