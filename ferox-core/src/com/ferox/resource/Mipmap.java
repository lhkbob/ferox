package com.ferox.resource;

import java.nio.Buffer;
import java.util.Arrays;

import com.ferox.resource.BufferData.DataType;

/**
 * <p>
 * A Mipmap contains the image data used by {@link Texture}. A Mipmap a 1D, 2D
 * or 3D grid of pixel data stored within a {@link Buffer}. The Mipmap makes the
 * assumption that all 1D grids are 2D grids with a height of 1, and all 2D
 * grids are 3D with a depth of 1. This allows the Mipmap to support all
 * dimensions of texture types in a single class. In addition to this, a Mipmap
 * optionally contains a number of mipmap levels that form a hierarchy of image
 * data. Each level contains a grid of pixels with dimensions half of the level
 * before it. These levels can be used to define how the image appears when
 * greatly zoomed in or out with better control.
 * </p>
 * <p>
 * Each level of a Mipmap has its data stored within a BufferData, valid
 * DataTypes are determined by the supported DataTypes from
 * {@link TextureFormat}. The elements within each Buffer are first ordered by
 * increasing row within a 2D slice, then by increasing slice within the 3D
 * grid. A 1D mipmap simplifies to a simple array, and a 2D mipmap simplifies to
 * the common array layout used to represent two dimensional data. Individual
 * pixels are stored in adjacent array elements, dependent on the
 * TextureFormat's specific format.
 * </p>
 * <p>
 * A Mipmap can have either a single level, in which case it's not considered to
 * be "mipmapped" and the level represents the root level of texture data, or it
 * can provide an entire set of levels. A valid set of full levels is a number
 * of BufferData, ordered from 0 to
 * <code>Mipmap.getMipmapCount(width, height, depth) - 1</code>, with a capacity
 * equal to the required number of elements for the mipmap level. As stated
 * before, each level has half the length along each axis. Each BufferData in
 * this set must have the same data type.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Mipmap {
    private final BufferData[] levels;
    private final int width;
    private final int height;
    private final int depth;
    
    private final DataType dataType;
    private final TextureFormat format;

    /**
     * Create a Mipmap that initially has its levels set to null BufferDatas. It
     * will use <tt>dataType</tt> to validate any future BufferDatas that may be
     * assigned to the Mipmap. If <tt>mipmap</tt> is true, then this Mipmap will
     * allocate enough null levels to be considered "mipmapped", otherwise it
     * will have a single null level. The width, height and depth are the
     * dimensions of the root level. The provided TextureFormat must be
     * compatible with the data type.
     * 
     * @param dataType The data type of the texture data that will be stored
     *            within the Mipmap at a later point
     * @param mipmap True if the Mipmap should have a full set of levels, false
     *            for a single level
     * @param width The number of pixels of the root level along the x axis
     * @param height The number of pixels of the root level along the y axis
     * @param depth The number of pixels of the root level along the z axis
     * @param format The TextureFormat describing how elements of the Mipmap
     *            data buffers are converted to pixel colors
     * @throws NullPointerException if dataType or format is null
     * @throws IllegalArgumentException if dataType and format are not
     *             compatible, or if the dimensions are invalid
     */
    public Mipmap(DataType dataType, boolean mipmap, int width, int height, int depth, TextureFormat format) {
        if (format == null)
            throw new NullPointerException("Format cannot be null");
        if (!format.isTypeValid(dataType))
            throw new IllegalArgumentException("Data type is not supported by desired TextureFormat: " + dataType + " " + format);
        
        int mipCount = getMipmapCount(width, height, depth);
        this.dataType = dataType;
        levels = new BufferData[mipmap ? mipCount : 1];
        
        this.width = width;
        this.height = height;
        this.depth = depth;
        
        this.format = format;
    }

    /**
     * Create a Mipmap that has a single level of pixel data that is sourced
     * from <tt>data</tt>. The provided BufferData must be appropriately sized
     * to hold the image data for an image with the provided dimensions and
     * TextureFormat. The data type of the Mipmap is derived from the input
     * BufferData. This type must be compatible with the given format.
     * {@link #Mipmap(DataType, boolean, int, int, int, TextureFormat)}.
     * 
     * @param data The BufferData containing the pixel data for the single level
     *            of the Mipmap
     * @param width The width of the root level
     * @param height The height of the root level
     * @param depth The depth of the root level
     * @param format The TextureFormat of the Mipmap
     * @throws NullPointerException if data or format are null
     * @throws IllegalArgumentException if the dimensions are invalid, if format
     *             and data are incompatible, if a DataType is ambiguous, or if
     *             data does not have the correct capacity
     */
    public Mipmap(BufferData data, int width, int height, int depth, TextureFormat format) {
        this(new BufferData[] {data}, width, height, depth, format);
    }

    /**
     * Create a Mipmap that contains the provided set of mipmap levels, where
     * each level takes its pixel data from a subsequent element within
     * <tt>levels</tt>. The <tt>levels</tt> can have a length of 1, in which
     * case the created Mipmap is not "mipmapped", or it must have a length
     * equal to {@link #getMipmapCount(int, int, int)} when using the provided
     * dimensions. Each provided BufferData must be of the same DataType, which
     * determines the data type of this Mipmap. This data type must be
     * compatible with the requested TextureFormat.
     * 
     * @param levels An array of BufferDatas that provide pixel data for each
     *            mipmap level
     * @param width The width of the root level
     * @param height The height of the root level
     * @param depth The depth of the root level
     * @param format The requested TextureFormat used by each mipmap level
     * @throws NullPointerException if levels or format are null, or if any
     *             element in levels is null
     * @throws IllegalArgumentException if the length of levels is not 1 or the
     *             expected number of mipmaps, if the dimensions are invalid, or
     *             if any provided Buffer has a mismatched data type or invalid
     *             capacity for its level, or if a unique data type cannot be
     *             determined
     */
    public Mipmap(BufferData[] levels, int width, int height, int depth, TextureFormat format) {
        if (levels == null)
            throw new NullPointerException("BufferData levels cannot be a null array");
        if (format == null)
            throw new NullPointerException("TextureFormat cannot be null");
        
        int mipmapCount = getMipmapCount(width, height, depth);
        if (levels.length != 1 && levels.length != mipmapCount)
            throw new IllegalArgumentException("The number of levels must be 1, or the expected number of levels (" + mipmapCount + "), and not " + levels.length);
        
        dataType = validate(levels, width, height, depth, format);
        this.levels = Arrays.copyOf(levels, levels.length); // defensive copy
        
        this.width = width;
        this.height = height;
        this.depth = depth;
        
        this.format = format;
    }
    
    /**
     * @return True if this Mipmap has a complete set of mipmaps, or false if it
     *         only provides the root layer
     */
    public boolean isMipmapped() {
        return levels.length > 1;
    }

    /**
     * @return The number of provided mipmap levels within this Mipmap. This
     *         will either be 1 or {@link #getMipmapCount(int, int, int)} using
     *         the root dimensions of this Mipmap
     */
    public int getNumMipmaps() {
        return levels.length;
    }

    /**
     * Return the current BufferData at the requested level. This may return
     * null if no specific pixel data is provided for that level.
     * 
     * @param level The mipmap level
     * @return The BufferData at the given level
     * @throws IndexOutOfBoundsException if level < 0 or level >=
     *             {@link #getNumMipmaps()}
     */
    public BufferData getData(int level) {
        return levels[level];
    }

    /**
     * Assign a new BufferData to use at the given mipmap level. The BufferData may be
     * null; if it is not null then it must have an appropriate capacity to fit
     * the mipmap level, and it must have the same DataType.
     * 
     * @param level The mipmap level whose data is being reassigned
     * @param data The new BufferData of pixel data for the mipmap level
     * @throws IndexOutOfBoundsException if level is < 0 or level >=
     *             {@link #getNumMipmaps()}
     * @throws IllegalArgumentException if data has an invalid capacity, is of
     *             the incorrect type, or is not direct
     */
    public void setData(int level, BufferData data) {
        if (level < 0 || level >= levels.length)
            throw new IndexOutOfBoundsException("Mipmap level is invalid, must be in [0, " + (levels.length - 1) + "], not " + level);
        if (data == null)
            throw new NullPointerException("BufferData cannot be null");
        
        if (dataType != data.getDataType())
            throw new IllegalArgumentException("Buffer was expected to be a " + dataType + ", but was a " + data.getClass());
        int reqCap = format.getBufferSize(getWidth(level), getHeight(level), getDepth(level));
        if (data.getLength() != reqCap)
            throw new IllegalArgumentException("Buffer does not have the correct capacity, expected to be " + reqCap + ", but was " + data.getLength());
        
        levels[level] = data;
    }

    /**
     * @param level The mipmap level
     * @return The width of the mipmap at the given level. getWidth(0) is the
     *         root width
     * @throws IndexOutOfBoundsException if level < 0 or level >=
     *             {@link #getNumMipmaps()}
     */
    public int getWidth(int level) {
        if (level < 0 || level >= levels.length)
            throw new IndexOutOfBoundsException("Mipmap level is invalid, must be in [0, " + (levels.length - 1) + "], not " + level);
        return Math.max(width >> level, 1);
    }
    
    /**
     * @param level The mipmap level
     * @return The height of the mipmap at the given level. getHeight(0) is the
     *         root height
     * @throws IndexOutOfBoundsException if level < 0 or level >=
     *             {@link #getNumMipmaps()}
     */
    public int getHeight(int level) {
        if (level < 0 || level >= levels.length)
            throw new IndexOutOfBoundsException("Mipmap level is invalid, must be in [0, " + (levels.length - 1) + "], not " + level);
        return Math.max(height >> level, 1);
    }
    
    /**
     * @param level The mipmap level
     * @return The depth of the mipmap at the given level. getDepth(0) is the
     *         root depth
     * @throws IndexOutOfBoundsException if level < 0 or level >=
     *             {@link #getNumMipmaps()}
     */
    public int getDepth(int level) {
        if (level < 0 || level >= levels.length)
            throw new IndexOutOfBoundsException("Mipmap level is invalid, must be in [0, " + (levels.length - 1) + "], not " + level);
        return Math.max(depth >> level, 1);
    }

    /**
     * @return The DataType that every BufferData must have in this Mipmap
     */
    public DataType getDataType() {
        return dataType;
    }
    
    /**
     * @return The TextureFormat of all the pixel data within this Mipmap
     */
    public TextureFormat getFormat() {
        return format;
    }

    /**
     * Compute and return the required number of mipmap levels needed to provide
     * a complete set of mipmaps for the given dimension. A full set would have
     * levels from
     * <tt>(width, height, depth), (width / 2, height / 2, depth / 2),
     * ... , (1, 1, 1)</tt>. This is computed as
     * <code>floor(log2(max(width,height,depth))) + 1</code>.
     * 
     * @param width The width of the top mipmap level
     * @param height The height of the top mipmap level
     * @param depth The depth of the top mipmap level
     * @return The required number of mipmap levels to form a complete set of
     *         mipmaps
     * @throws IllegalArgumentException if any dimensions is < 1
     */
    public static int getMipmapCount(int width, int height, int depth) {
        if (width <= 0 || height <= 0 || depth <= 0)
            throw new IllegalArgumentException("Dimensions must all be at least 1: " + width + " x " + height + " x " + depth);
        int max = Math.max(width, Math.max(height, depth));
        return (int) Math.floor(Math.log(max) / Math.log(2)) + 1;
    }
    
    // internal method used to verify that every non-null BufferData is the correct size for its level
    // that it matches the required data type, and that all levels have the same type
    private DataType validate(BufferData[] levels, int width, int height, int depth, TextureFormat format) {
        DataType type = format.getSupportedType();
        
        for (int i = 0; i < levels.length; i++) {
            if (levels[i] != null) {
                if (type == null)
                    type = levels[i].getDataType();
                
                if (type != levels[i].getDataType())
                    throw new IllegalArgumentException("Buffer data at level " + i + " does not match expected type of " + type + ", but was " + levels[i].getDataType());
                int expectedSize = format.getBufferSize(width, height, depth);
                if (expectedSize != levels[i].getLength())
                    throw new IllegalArgumentException("Buffer data at level " + i + " does not have expected capacity of " + expectedSize + ", but was " + levels[i].getLength());
            } else
                throw new NullPointerException("BufferData cannot be null");
            width = Math.max(width >> 1, 1);
            height = Math.max(height >> 1, 1);
            depth = Math.max(depth >> 1, 1);
        }
        
        // this will only be null if every level was null and the provided format
        // can support any data type (this situation should be used with the other 
        // constructor anyway)
        if (type == null)
            throw new IllegalArgumentException("Required Buffer type is indeterminate with provided arguments");
        return type;
    }
}
