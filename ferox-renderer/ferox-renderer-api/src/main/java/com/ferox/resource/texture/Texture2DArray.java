package com.ferox.resource.texture;

import com.ferox.resource.data.TexelData;

/**
 * Texture2DArray is a new texture type supported in OpenGL 3+ capable graphics cards
 * where it stores multiple, independent 2D images in a single texture. Each 2D image can
 * have its own set of mipmaps.
 * <p/>
 * Each 2D image within the texture object is equivalent to a {@link Texture2D}.
 *
 * @author Michael Ludwig
 */
public class Texture2DArray extends Texture {
    private final TexelData<?>[][] data;

    /**
     * Create a Texture2DArray that will store <var>numImages</var> two dimensional images
     * of the given <var>width</var> and <var>height</var>. Each image will have a
     * separate OpenGLData instance.
     *
     * @param format    The texture format for each image
     * @param numImages The number of 2D images in the texture
     * @param width     The width of each image
     * @param height    The height of each image
     *
     * @throws NullPointerException     if format is null
     * @throws IllegalArgumentException if numImages, width, or height are less than 1
     */
    public Texture2DArray(TextureFormat format, int numImages, int width, int height) {
        super(format, width, height, 1);
        data = new TexelData<?>[numImages][getMipmapCount()];
    }

    /**
     * Get the texel data for the 2D <var>image</var>, at the given mipmap
     * <var>level</var>. If the texture is incomplete, this can return null if the level
     * does not have a data instance specified.
     * <p/>
     * Frameworks will mark incomplete textures as erroneous and will be unusable until
     * they are corrected.
     * <p/>
     * The image value must be in the range [0, {@link #getImageCount()}.
     *
     * @param image The 2D image to select from
     * @param level The mipmap level to retrieve
     *
     * @return The texel data for the given image and level
     *
     * @throws IndexOutOfBoundsException if level is less than 0 or greater than or equal
     *                                   to the number of mipmaps in the texture, or if
     *                                   image is not in [0, getImageCount()]
     */
    public synchronized TexelData<?> getMipmap(int image, int level) {
        return data[image][level];
    }

    /**
     * Set the texel data for the 2D <var>image</var>, at the given mipmap
     * <var>level</var>. It is permitted to set levels to null data instances although if
     * that level is within the base and maximum mipmap level configured for the texture,
     * it will be considered incomplete.
     * <p/>
     * Frameworks will mark incomplete textures as erroneous and will be unusable until
     * they are corrected.
     * <p/>
     * The image value must be in the range [0, {@link #getImageCount()}.
     *
     * @param image The 2D image that is being modified
     * @param level The mipmap level to assign the image data to
     * @param data  The texel data for the given level
     *
     * @return The new version in the texture's bulk change queue
     *
     * @throws IndexOutOfBoundsException if level is less than 0 or greater than or equal
     *                                   to the number of mipmaps in the texture, or if
     *                                   image is not in [0, getImageCount()]
     * @throws IllegalArgumentException  if data's length is not correct for the mipmap
     *                                   level, texture format, and image dimensions
     */
    public synchronized int setMipmap(int image, int level, TexelData<?> data) {
        int requiredLength = getFormat()
                .getBufferSize(getMipmapDimension(getWidth(), level),
                               getMipmapDimension(getHeight(), level), 1);
        if (data != null && data.getLength() != requiredLength) {
            throw new IllegalArgumentException(
                    "TexelData has incorrect length, requires " + requiredLength +
                    ", but was " + data.getLength());
        }

        this.data[image][level] = data;
        return markDirty(image, level);
    }

    @Override
    public int getLayerCount() {
        return data.length;
    }

    @Override
    public int getImageCount() {
        return data.length;
    }
}
