package com.ferox.resource.texture;

import com.ferox.resource.data.TexelData;

/**
 * TextureCubeMap is a special type of Texture often used for environment maps or
 * reflection effects. It is composed of six images, one corresponding to each face of a
 * cube. Each of these images is a square 2D image and every image has the same
 * dimensions. The images are mipmappable.
 * <p/>
 * The six faces of the cube map are also selectable layers for rendering. Each 2D face is
 * arranged in memory exactly like the data for {@link Texture2D}.
 *
 * @author Michael Ludwig
 */
public class TextureCubeMap extends Texture {
    /**
     * Image constant for the positive X face of the cube map.
     */
    public static final int PX = 0;

    /**
     * Image constant for the positive Y face of the cube map.
     */
    public static final int PY = 1;

    /**
     * Image constant for the positive Z face of the cube map.
     */
    public static final int PZ = 2;

    /**
     * Image constant for the negative X face of the cube map.
     */
    public static final int NX = 3;

    /**
     * Image constant for the negative Y face of the cube map.
     */
    public static final int NY = 4;

    /**
     * Image constant for the negative Z face of the cube map.
     */
    public static final int NZ = 5;

    private final TexelData<?>[][] data;

    /**
     * Create a new TextureCubeMap with the given format and dimension. Each cube face
     * image will have a width and height equal to <var>side</var>, and an effective depth
     * of 1. All mipmap data for all six images start null.
     *
     * @param format The texture format
     * @param side   The dimension of each cube face
     *
     * @throws NullPointerException     if format is null
     * @throws IllegalArgumentException if side is less than 1
     */
    public TextureCubeMap(TextureFormat format, int side) {
        super(format, side, side, 1);
        data = new TexelData<?>[6][getMipmapCount()];
    }

    /**
     * Get the texel data for the positive X cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to fetch
     *
     * @return Texel data for the positive x cube image
     *
     * @throws IndexOutOfBoundsException
     * @see #getMipmap(int, int)
     */
    public synchronized TexelData<?> getPositiveXMipmap(int level) {
        return getMipmap(PX, level);
    }

    /**
     * Set the texel data for the positive X cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to modify
     * @param data  The new texel data
     *
     * @return The new version from this texture's bulk change queue
     *
     * @throws IndexOutOfBoundsException
     * @throws IllegalArgumentException
     * @see #setMipmap(int, int, com.ferox.resource.data.TexelData)
     */
    public synchronized int setPositiveXMipmap(int level, TexelData<?> data) {
        return setMipmap(PX, level, data);
    }

    /**
     * Get the texel data for the positive Y cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to fetch
     *
     * @return Texel data for the positive y cube image
     *
     * @throws IndexOutOfBoundsException
     * @see #getMipmap(int, int)
     */
    public synchronized TexelData<?> getPositiveYMipmap(int level) {
        return getMipmap(PY, level);
    }

    /**
     * Set the texel data for the positive Y cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to modify
     * @param data  The new texel data
     *
     * @return The new version from this texture's bulk change queue
     *
     * @throws IndexOutOfBoundsException
     * @throws IllegalArgumentException
     * @see #setMipmap(int, int, com.ferox.resource.data.TexelData)
     */
    public synchronized int setPositiveYMipmap(int level, TexelData<?> data) {
        return setMipmap(PY, level, data);
    }

    /**
     * Get the texel data for the positive Z cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to fetch
     *
     * @return Texel data for the positive z cube image
     *
     * @throws IndexOutOfBoundsException
     * @see #getMipmap(int, int)
     */
    public synchronized TexelData<?> getPositiveZMipmap(int level) {
        return getMipmap(PZ, level);
    }

    /**
     * Set the texel data for the positive Z cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to modify
     * @param data  The new texel data
     *
     * @return The new version from this texture's bulk change queue
     *
     * @throws IndexOutOfBoundsException
     * @throws IllegalArgumentException
     * @see #setMipmap(int, int, com.ferox.resource.data.TexelData)
     */
    public synchronized int setPositiveZMipmap(int level, TexelData<?> data) {
        return setMipmap(PZ, level, data);
    }

    /**
     * Get the texel data for the negative X cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to fetch
     *
     * @return Texel data for the negative x cube image
     *
     * @throws IndexOutOfBoundsException
     * @see #getMipmap(int, int)
     */
    public synchronized TexelData<?> getNegativeXMipmap(int level) {
        return getMipmap(NX, level);
    }

    /**
     * Set the texel data for the negative X cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to modify
     * @param data  The new texel data
     *
     * @return The new version from this texture's bulk change queue
     *
     * @throws IndexOutOfBoundsException
     * @throws IllegalArgumentException
     * @see #setMipmap(int, int, com.ferox.resource.data.TexelData)
     */
    public synchronized int setNegativeXMipmap(int level, TexelData<?> data) {
        return setMipmap(NX, level, data);
    }

    /**
     * Get the texel data for the negative Y cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to fetch
     *
     * @return Texel data for the negative y cube image
     *
     * @throws IndexOutOfBoundsException
     * @see #getMipmap(int, int)
     */
    public synchronized TexelData<?> getNegativeYMipmap(int level) {
        return getMipmap(NY, level);
    }

    /**
     * Set the texel data for the negative Y cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to modify
     * @param data  The new texel data
     *
     * @return The new version from this texture's bulk change queue
     *
     * @throws IndexOutOfBoundsException
     * @throws IllegalArgumentException
     * @see #setMipmap(int, int, com.ferox.resource.data.TexelData)
     */
    public synchronized int setNegativeYMipmap(int level, TexelData<?> data) {
        return setMipmap(NY, level, data);
    }

    /**
     * Get the texel data for the negative Z cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to fetch
     *
     * @return Texel data for the negative z cube image
     *
     * @throws IndexOutOfBoundsException
     * @see #getMipmap(int, int)
     */
    public synchronized TexelData<?> getNegativeZMipmap(int level) {
        return getMipmap(NZ, level);
    }

    /**
     * Set the texel data for the negative Z cube face at the given mipmap
     * <var>level</var>.
     *
     * @param level The mipmap level to modify
     * @param data  The new texel data
     *
     * @return The new version from this texture's bulk change queue
     *
     * @throws IndexOutOfBoundsException
     * @throws IllegalArgumentException
     * @see #setMipmap(int, int, com.ferox.resource.data.TexelData)
     */
    public synchronized int setNegativeZMipmap(int level, TexelData<?> data) {
        return setMipmap(NZ, level, data);
    }

    /**
     * Get the texel data for the cube face, <var>image</var>, at the given mipmap
     * <var>level</var>. If the texture is incomplete, this can return null if the level
     * does not have a data instance specified.
     * <p/>
     * Frameworks will mark incomplete textures as erroneous and will be unusable until
     * they are corrected.
     * <p/>
     * The image value must be one of PX, PY, PZ, NX, NY, or NZ (which is the range 0 -
     * 5).
     *
     * @param image The cube map image to select from
     * @param level The mipmap level to retrieve
     *
     * @return The texel data for the given cube face and level
     *
     * @throws IndexOutOfBoundsException if level is less than 0 or greater than or equal
     *                                   to the number of mipmaps in the texture, or if
     *                                   image is not in [0, 5]
     */
    public synchronized TexelData<?> getMipmap(int image, int level) {
        return data[image][level];
    }

    /**
     * Set the texel data for the cube face, <var>image</var>, at the given mipmap
     * <var>level</var>. It is permitted to set levels to null data instances although if
     * that level is within the base and maximum mipmap level configured for the texture,
     * it will be considered incomplete.
     * <p/>
     * Frameworks will mark incomplete textures as erroneous and will be unusable until
     * they are corrected.
     * <p/>
     * The image value must be one of PX, PY, PZ, NX, NY, or NZ (which is the range 0 -
     * 5).
     *
     * @param image The cube map image that is being modified
     * @param level The mipmap level to assign the image data to
     * @param data  The texel data for the given level
     *
     * @return The new version in the texture's bulk change queue
     *
     * @throws IndexOutOfBoundsException if level is less than 0 or greater than or equal
     *                                   to the number of mipmaps in the texture, or if
     *                                   image is not in [0, 5]
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
        return 6;
    }

    @Override
    public int getImageCount() {
        return 6;
    }
}
