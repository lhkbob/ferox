package com.ferox.resource.texture;

/**
 * Texture3D represents a three-dimensional texture image.  Its image can be mipmapped.
 * The texture data is mutable but its format and dimensions are fixed at creation time.
 * <p/>
 * 3D textures represent a block of color data querable by three texture coordinates.
 * Color data is arranged left to right in rows from bottom to top, with each two image
 * slice formed from the rows arranged back to front.
 * <p/>
 * Each 2D layer in 3D texture is a selectable target for rendering.
 *
 * @author Michael Ludwig
 */
public class Texture3D extends SingleImageTexture {
    /**
     * Create a new Texture3D instance with the given format and dimensions. All mipmap's
     * data start null.
     *
     * @param format The texture format
     * @param width  The width of the 3D texture's 0th mipmap
     * @param height The height of the 3D texture's 0th mipmap
     * @param depth  The depth of the 3D texture's 0th mipmap
     *
     * @throws NullPointerException     if format is null
     * @throws IllegalArgumentException if width, height, or depth is less than 1
     */
    public Texture3D(TextureFormat format, int width, int height, int depth) {
        super(format, width, height, depth);
    }

    @Override
    public int getRenderTargetCount() {
        return getDepth();
    }
}
