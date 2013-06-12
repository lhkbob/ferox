package com.ferox.resource.texture;

import com.ferox.resource.data.TexelData;

/**
 * Texture1DArray is a new texture type supported in OpenGL 3+ capable graphics cards
 * where it stores multiple, independent 1D images in a single texture. Each 1D image can
 * have its own set of mipmaps.
 * <p/>
 * Each 1D image within the texture object is equivalent to a {@link Texture1D}.
 *
 * @author Michael Ludwig
 */
public class Texture1DArray extends Texture {
    private final TexelData<?>[][] data;

    /**
     * Create a Texture1DArray that will store <var>numImages</var> one dimensional images
     * of the given <var>width</var>. Each image will have a separate OpenGLData
     * instance.
     *
     * @param format    The texture format for each image
     * @param numImages The number of 1D images in the texture
     * @param width     The width of each image
     *
     * @throws NullPointerException     if format is null
     * @throws IllegalArgumentException if numImages or width are less than 1
     */
    public Texture1DArray(TextureFormat format, int numImages, int width) {
        super(format, width, 1, 1);
        data = new TexelData[numImages][getMipmapCount()];
    }

    /**
     * Get the texel data for the 1D <var>image</var>, at the given mipmap
     * <var>level</var>. If the texture is incomplete, this can return null if the level
     * does not have a data instance specified.
     * <p/>
     * Frameworks will mark incomplete textures as erroneous and will be unusable until
     * they are corrected.
     * <p/>
     * The image value must be in the range [0, {@link #getImageCount()}.
     *
     * @param image The 1D image to select from
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
     * Set the texel data for the 1D <var>image</var>, at the given mipmap
     * <var>level</var>. It is permitted to set levels to null data instances although if
     * that level is within the base and maximum mipmap level configured for the texture,
     * it will be considered incomplete.
     * <p/>
     * Frameworks will mark incomplete textures as erroneous and will be unusable until
     * they are corrected.
     * <p/>
     * The image value must be in the range [0, {@link #getImageCount()}.
     *
     * @param image The 1D image that is being modified
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
                .getBufferSize(getMipmapDimension(getWidth(), level), 1, 1);
        if (data != null && data.getLength() != requiredLength) {
            throw new IllegalArgumentException(
                    "TexelData has incorrect length, requires " + requiredLength +
                    ", but was " + data.getLength());
        }

        this.data[image][level] = data;
        return markDirty(image, level);
    }

    @Override
    public int getRenderTargetCount() {
        return data.length;
    }

    @Override
    public int getImageCount() {
        return data.length;
    }
}
