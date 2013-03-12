package com.ferox.resource.texture;

import com.ferox.resource.data.TexelData;

/**
 * SingleImageTexture is a Texture resource that is composed of a single image. It may
 * have multiple layers for RTT purposes, but each mipmap level will only contain a single
 * OpenGLData source.
 *
 * @author Michael Ludwig
 * @see Texture1D
 * @see Texture2D
 * @see Texture3D
 */
public abstract class SingleImageTexture extends Texture {
    private final TexelData<?>[] mipmapLevels;

    public SingleImageTexture(TextureFormat format, int width, int height, int depth) {
        super(format, width, height, depth);
        mipmapLevels = new TexelData<?>[getMipmapCount()];
    }

    /**
     * Get the texel data for the single image at the given mipmap <var>level</var>. If
     * the texture is incomplete, this can return null if the level does not have a data
     * instance specified.
     * <p/>
     * Frameworks will mark incomplete textures as erroneous and will be unusable until
     * they are corrected.
     *
     * @param level The mipmap level to retrieve
     *
     * @return The texel data for the given level
     *
     * @throws IndexOutOfBoundsException if level is less than 0 or greater than or equal
     *                                   to the number of mipmaps in the texture
     */
    public synchronized TexelData<?> getMipmap(int level) {
        return mipmapLevels[level];
    }

    /**
     * Set the texel data for the single image at the given mipmap <var>level</var>. It is
     * permitted to set levels to null data instances although if that level is within the
     * base and maximum mipmap level configured for the texture, it will be considered
     * incomplete.
     * <p/>
     * Frameworks will mark incomplete textures as erroneous and will be unusable until
     * they are corrected.
     *
     * @param level The mipmap level to assign the image data to
     * @param data  The texel data for the given level
     *
     * @return The new version in the texture's bulk change queue
     *
     * @throws IndexOutOfBoundsException if level is less than 0 or greater than or equal
     *                                   to the number of mipmaps in the texture
     * @throws IllegalArgumentException  if data's length is not correct for the mipmap
     *                                   level, texture format, and image dimensions
     */
    public synchronized int setMipmap(int level, TexelData<?> data) {
        int requiredLength = getFormat()
                .getBufferSize(getMipmapDimension(getWidth(), level),
                               getMipmapDimension(getHeight(), level),
                               getMipmapDimension(getDepth(), level));
        if (data != null && data.getLength() != requiredLength) {
            throw new IllegalArgumentException(
                    "TexelData has incorrect length, requires " + requiredLength +
                    ", but was " + data.getLength());
        }
        // FIXME validate data's type. I think it is okay to allow multiple typed data
        // so long as TextureFormat is unique for dst type

        mipmapLevels[level] = data;
        return markDirty(0, level);
    }

    @Override
    public final int getImageCount() {
        return 1;
    }
}
