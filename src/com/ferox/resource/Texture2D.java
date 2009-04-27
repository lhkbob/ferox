package com.ferox.resource;

import com.ferox.effect.Effect.PixelTest;
import com.ferox.resource.BufferData.DataType;

/**
 * <p>
 * Represents a square two-dimensional image. The texture data is accessed by
 * normalized texture coordinates. The dimensions are not required to be
 * power-of-two textures, but a Renderer will likely re-scale the image if they
 * aren't supported. Even if they are supported, power-of-two textures likely
 * have better performance.
 * </p>
 * <p>
 * While both Texture2D and TextureRectangle are two-dimensional, there are
 * important differences. Using Texture2D with arbitrary rectangles is only
 * available on the newest hardware (and if not the image will have to be
 * re-scaled). TextureRectangles are more supported, but can't have mipmaps, or
 * some clamp options, and don't use normalized coordinates.
 * </p>
 * <p>
 * Besides the arguments present in TextureImage's constructors, Texture2D adds
 * the additional parameters BufferData[] data and dimensions width/height data
 * is the array of all mipmaps for the image. If the data is null, all elements
 * are null, or if it has a length of one, then the texture is not mipmapped.
 * </p>
 * <p>
 * The constructors will additionally throw exceptions if side or the data array
 * are not valid. Here are the rules:
 * <ol>
 * <li>Dimensions must be positive.</li>
 * <li>If data isn't null, then all elements must be null, or all must not be
 * null.</li>
 * <li>A data array with non-null elements and a length > 1 is considered
 * mipmapped. All mipmaps must be present in this layer, based on the expected
 * number from side.</li>
 * <li>Every mipmap layer (including the 0th layer) must be sized correctly
 * based on the layer's dimension, format and type.</li>
 * <li>All non-null BufferData's must have the same data type as passed into the
 * constructor.</li>
 * </ol>
 * </p>
 * <p>
 * Texture2D provides methods to mark regions of the texture's mipmap levels as
 * dirty. These commands will always be clamped to the valid regions of the
 * texture. They will also update the dirty descriptor regardless of the null
 * status of any buffer data's, or the data's arrays. It is the Renderer's job
 * to make sure that null data's or arrays are treated correctly regardless of
 * what the dirty descriptor declares.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Texture2D extends TextureImage {
	/**
	 * The dirty descriptor class that is used by Texture2D. Calls to
	 * getDirtyDescriptor() for texture 2d's will return objects of this class.
	 */
	public static class Texture2DDirtyDescriptor extends TextureDirtyDescriptor {
		private MipmapDirtyRegion[] dirtyRegions;

		/**
		 * @param level Mipmap level to check
		 * @return True if there is a non-null MipmapDirtyRegion for the
		 *         associated mipmap level. Returns false if level is invalid.
		 */
		public boolean isDataDirty(int level) {
			if (dirtyRegions == null || level < 0
					|| level >= dirtyRegions.length)
				return false;
			return dirtyRegions[level] != null;
		}

		/**
		 * Get the MipmapDirtyRegion for the given mipmap level. If null is
		 * returned, then that mipmap isn't dirty or level is invalid. The
		 * returned region will be constrained to be in the dimensions of the
		 * mipmap level.
		 * 
		 * @param level Mipmap level to check
		 * @return The dirty region for the requested mipmap
		 */
		public MipmapDirtyRegion getDirtyRegion(int level) {
			if (dirtyRegions == null || level < 0
					|| level >= dirtyRegions.length)
				return null;
			return dirtyRegions[level];
		}

		/** @return True if at least one mipmap region is not null. */
		public boolean areMipmapsDirty() {
			return dirtyRegions != null;
		}

		@Override
		protected void clearDescriptor() {
			super.clearDescriptor();
			dirtyRegions = null;
		}
	}

	private BufferData[] data;
	private int width, height;
	private int numMipmaps;

	/**
	 * Creates a texture image with the given format and type, default other
	 * values.
	 * 
	 * @param data The array of mipmaps to use for this texture
	 * @param width The width dimension of the 0th mipmap
	 * @param height The height dimension of the 0th mipmap
	 * @param format
	 * @param type
	 * @throws NullPointerException if format or type are null
	 * @throws IllegalArgumentException if data, width, height, format and type
	 *             would create an invalid texture
	 */
	public Texture2D(BufferData[] data, int width, int height,
			TextureFormat format, DataType type) {
		super(format, type);
		setData(data, width, height);
	}

	/**
	 * Creates a texture image with the given type, format and filter, default
	 * other values.
	 * 
	 * @param data The array of mipmaps to use for this texture
	 * @param width The width dimension of the 0th mipmap
	 * @param height The height dimension of the 0th mipmap
	 * @param format
	 * @param type
	 * @param filter
	 * @throws NullPointerException if format or type are null
	 * @throws IllegalArgumentException if data, width, height, format and type
	 *             would create an invalid texture
	 */
	public Texture2D(BufferData[] data, int width, int height,
			TextureFormat format, DataType type, Filter filter) {
		super(format, type, filter);
		setData(data, width, height);
	}

	/**
	 * Create a texture image with the given format, type, filter, wrap mode for
	 * all coordinates, depth mode and test, and depth comparison is disabled.
	 * 
	 * @param data The array of mipmaps to use for this texture
	 * @param width The width dimension of the 0th mipmap
	 * @param height The height dimension of the 0th mipmap
	 * @param format
	 * @param type
	 * @param filter
	 * @param wrapAll
	 * @param depthMode
	 * @param depthTest
	 * @throws NullPointerException if format or type are null
	 * @throws IllegalArgumentException if data, width, height, format and type
	 *             would create an invalid texture
	 */
	public Texture2D(BufferData[] data, int width, int height,
			TextureFormat format, DataType type, Filter filter,
			TextureWrap wrapAll, DepthMode depthMode, PixelTest depthTest) {
		super(format, type, filter, wrapAll, depthMode, depthTest);
		setData(data, width, height);
	}

	/* Internal method used to validate the BufferData[] and dimensions. */
	private void setData(BufferData[] data, int width, int height)
			throws IllegalArgumentException {
		// expected mipmap count, if data.length > 1
		int numMipmaps = TextureImage.calculateMipmapCount(width, height, 1);
		TextureFormat format = getFormat();
		DataType type = getType();

		BufferData[] realData = null;
		if (data != null) {
			if (data.length != 1 && data.length != numMipmaps)
				throw new IllegalArgumentException(
						"If more than one BufferData is given, must provide all mipmap levels");
			numMipmaps = data.length;

			int nonNullCount = 0;
			for (int i = 0; i < data.length; i++)
				if (data[i] != null)
					nonNullCount++;
			if (nonNullCount == 0) {
				// do nothing, it should be a headless texture
			} else if (nonNullCount == data.length) {
				// make a new array to hold the buffers, so it can't be tampered
				// with later
				realData = new BufferData[data.length];
				System.arraycopy(data, 0, realData, 0, Math.min(data.length,
						realData.length));
			} else
				throw new IllegalArgumentException(
						"Cannot pass in an array with some values null.  Array length: "
								+ data.length + ", but has only "
								+ nonNullCount + " non-null buffers.");
		}

		if (realData != null) {
			int w = width;
			int h = height;
			for (int i = 0; i < realData.length; i++) {
				if (realData[i].getType() != type)
					throw new IllegalArgumentException(
							"BufferData doesn't have a matching type for the texture, expected: "
									+ type + ", but was: "
									+ realData[i].getType());
				if (realData[i].getCapacity() != format.getBufferSize(w, h, 1))
					throw new IllegalArgumentException(
							"Buffer at mipmap level: "
									+ i
									+ " is does not have the correct size, expected: "
									+ format.getBufferSize(w, h, 1)
									+ ", but was: " + realData[i].getCapacity());
				w = Math.max(1, w >> 1);
				h = Math.max(1, h >> 1);
			}

			numMipmaps = realData.length;
		} else {
			if (format.isCompressed())
				throw new IllegalArgumentException(
						"Headless Texture2D cannot have a client compressed texture: "
								+ format);
			numMipmaps = 1;
		}

		// everything is valid up to this point, so we can update our values
		this.width = width;
		this.height = height;
		this.numMipmaps = numMipmaps;
		this.data = realData;

		this.markDirty();
	}

	/**
	 * Mark the given mipmap level region dirty. If the level is outside of [0,
	 * numMipmaps - 1], this command does nothing. The x, y offsets and width
	 * and height will be clamped to be within the valid region of the given
	 * mipmap.
	 * 
	 * @param x X offset into the mipmap that starts the dirty region
	 * @param y Y offset into the mipmp that starts the dirty region
	 * @param width Width of dirty region
	 * @param height Height of the dirty region
	 * @param level The mipmap level to which x, y, width, height apply
	 */
	public void markDirty(int x, int y, int width, int height, int level) {
		if (level < 0 || level >= (numMipmaps - 1))
			return; // invalid level option

		Texture2DDirtyDescriptor dirty = (Texture2DDirtyDescriptor) getDirtyDescriptor();
		if (dirty.dirtyRegions == null || dirty.dirtyRegions.length <= level) {
			MipmapDirtyRegion[] temp = new MipmapDirtyRegion[level + 1];
			if (dirty.dirtyRegions != null)
				System.arraycopy(dirty.dirtyRegions, 0, temp, 0,
						dirty.dirtyRegions.length);
			dirty.dirtyRegions = temp;
		}

		int levelWidth = getWidth(level);
		int levelHeight = getHeight(level);
		MipmapDirtyRegion r = dirty.dirtyRegions[level];
		if (r == null) {
			r = new MipmapDirtyRegion(x, y, 0, width, height, 0, levelWidth,
					levelHeight, 0);
			dirty.dirtyRegions[level] = r;
		} else
			r.merge(x, y, 0, width, height, 0, levelWidth, levelHeight, 0);
	}

	/**
	 * Mark the entire mipmap level dirty. Does nothing if level isn't within
	 * [0, numMipmaps - 1].
	 * 
	 * @param level The mipmap level to mark dirty
	 */
	public void markDirty(int level) {
		this.markDirty(0, 0, getWidth(level), getHeight(level), level);
	}

	/** Mark the entire texture image as dirty. */
	public void markDirty() {
		Texture2DDirtyDescriptor dirty = (Texture2DDirtyDescriptor) getDirtyDescriptor();
		// create the whole array now for efficiency. It's okay to ignore old
		// array because
		// the new regions will take up the whole level.
		dirty.dirtyRegions = new MipmapDirtyRegion[numMipmaps];
		for (int i = 0; i < numMipmaps; i++)
			this.markDirty(i);
	}

	@Override
	public int getDepth(int level) {
		if (level < 0 || level >= numMipmaps)
			return -1;
		return 1;
	}

	@Override
	public int getHeight(int level) {
		if (level < 0 || level >= numMipmaps)
			return -1;
		return Math.max(1, height >> level);
	}

	@Override
	public int getNumMipmaps() {
		return numMipmaps;
	}

	@Override
	public int getWidth(int level) {
		if (level < 0 || level >= numMipmaps)
			return -1;
		return Math.max(1, width >> level);
	}

	@Override
	public final TextureTarget getTarget() {
		return TextureTarget.T_2D;
	}

	/**
	 * Get the buffer for the given level. Fails if the level is < 0 or >= the
	 * number of mipmaps. Returns null if the data isn't in client memory (most
	 * likely in the graphics card).
	 * 
	 * @param level The mipmap level whose data is requested
	 * @return The BufferData associated with level
	 * @throws IllegalArgumentException if level isn't a present mipmap
	 */
	public BufferData getData(int level) {
		if (level < 0 || level >= numMipmaps)
			throw new IllegalArgumentException(
					"Buffer data doesn't exist beyond mipmap levels, illegal level: "
							+ level);
		if (data == null)
			return null; // all we can return at this point
		return data[level];
	}

	@Override
	protected Texture2DDirtyDescriptor createTextureDirtyDescriptor() {
		return new Texture2DDirtyDescriptor();
	}

	@Override
	public Texture2DDirtyDescriptor getDirtyDescriptor() {
		return (Texture2DDirtyDescriptor) super.getDirtyDescriptor();
	}
}