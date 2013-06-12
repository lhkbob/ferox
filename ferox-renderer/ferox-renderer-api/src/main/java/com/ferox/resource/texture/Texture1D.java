package com.ferox.resource.texture;

/**
 * Texture1D represents a one-dimensional texture image.  Its image can be mipmapped. The
 * texture data is mutable but its format and dimension are fixed at creation time.
 * <p/>
 * A 1D texture is effectively the same as a color array, or a 2D texture with a height of
 * 1. The color data is arranged from left to right in the texture's OpenGLData
 * instances.
 *
 * @author Michael Ludwig
 */
public class Texture1D extends SingleImageTexture {
    /**
     * Create a new Texture1D with the given format and width. It will have an effective
     * height and depth of 1. All mipmap level's texel data will be null.
     *
     * @param format The texture format for the texture
     * @param width  The width of the texture
     *
     * @throws NullPointerException     if format is null
     * @throws IllegalArgumentException if width is less than 1
     */
    public Texture1D(TextureFormat format, int width) {
        super(format, width, 1, 1);
    }

    @Override
    public int getRenderTargetCount() {
        return 1;
    }
}
