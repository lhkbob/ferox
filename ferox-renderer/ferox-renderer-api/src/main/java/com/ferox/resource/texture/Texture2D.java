package com.ferox.resource.texture;

/**
 * Texture2D represents a two-dimensional texture image.  Its image can be mipmapped. The
 * texture data is mutable but its format and dimensions are fixed at creation time.
 * <p/>
 * 2D textures are perhaps the most common form of texture. Logically they are equivalent
 * to a 3D texture with a depth of 1. Color data is arranged left to right in rows from
 * bottom to top.
 *
 * @author Michael Ludwig
 */
public class Texture2D extends SingleImageTexture {
    /**
     * Create a new Textur2D that uses the given format and width and height. It will have
     * an effective depth of 1. All mipmap level's data will start null
     *
     * @param format The texture format
     * @param width  The width of the 0th mipmap level
     * @param height The height of the 0th mipmap level
     *
     * @throws NullPointerException     if format is null
     * @throws IllegalArgumentException if width or height is less than 1
     */
    public Texture2D(TextureFormat format, int width, int height) {
        super(format, width, height, 1);
    }

    @Override
    public int getRenderTargetCount() {
        return 1;
    }
}
