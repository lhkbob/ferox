package com.ferox.resource;

import com.ferox.effect.Effect.PixelTest;
import com.ferox.resource.BufferData.DataType;

/**
 * <p>
 * Represents a rectangular image with different access semantics. The texture
 * data is accessed by unnormalized texture coordinates, from 0 to width and 0
 * to height, which is very different for most other texture coordinate access
 * that uses a 0 to 1 scale.
 * </p>
 * <p>
 * TextureRectangles do not support mipmaps, and can only have the wrap mode of
 * CLAMP used for s and t coordinates. Given this, if a filter other than LINEAR
 * or NEAREST is used, it defaults to the appropriate non-mipped version.
 * setWrapX() is overridden to set it to CLAMP.
 * </p>
 * <p>
 * TextureRectangles are not supported on all hardware, but because texture
 * coordinate access is different, there is no automatic fallback that a
 * Renderer can use. Support should be checked before texture rectangles are
 * used, otherwise updates may result in a status of ERROR.
 * </p>
 * <p>
 * Besides the arguments present in TextureImage's constructors,
 * TextureRectangle adds the additional parameters BufferData data and int
 * width/height. width and height represents the pixel dimension of the sides of
 * the image. data is the primitive data for one image. TextureRectangles do not
 * support mipmaps.
 * </p>
 * <p>
 * The constructors will additionally throw exceptions if side or the data array
 * are not valid. Here are the rules:
 * <ol>
 * <li>Width and height must be positive.</li>
 * <li>Data must be sized correctly based on the layer's dimension, format and
 * type.</li>
 * <li>Data must have the same data type as passed into the constructor.</li>
 * </ol>
 * </p>
 * <p>
 * TextureRectangle provides methods to mark regions of the texture's image as
 * dirty. These commands will always be clamped to the valid regions of the
 * texture. They will also update the dirty descriptor regardless of the null
 * status of any buffer data's, or the data's arrays. It is the Renderer's job
 * to make sure that null data's or arrays are treated correctly regardless of
 * what the dirty descriptor declares.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class TextureRectangle extends TextureImage {
	/**
	 * The dirty descriptor class that is used by TextureRectangle. Calls to
	 * getDirtyDescriptor() for texture rectangle's will return objects of this
	 * class.
	 */
	public static class TextureRectangleDirtyDescriptor extends
			TextureDirtyDescriptor {
		private MipmapDirtyRegion dirtyRegions;

		/** True if the only mipmap for the texture rectangle is dirty. */
		public boolean isDataDirty() {
			return dirtyRegions != null;
		}

		/**
		 * Get the MipmapDirtyRegion for the TextureRectangle. If null is
		 * returned, then the data hasn't been flagged as dirty. The returned
		 * region will be constrained to be in the dimensions of the texture.
		 */
		public MipmapDirtyRegion getDirtyRegion() {
			return dirtyRegions;
		}

		@Override
		protected void clearDescriptor() {
			super.clearDescriptor();
			dirtyRegions = null;
		}
	}

	private BufferData data;
	private int width, height;

	/**
	 * Creates a texture image with the given format and type, default other
	 * values. Fails like super(format, type), or if the dimensions and data
	 * array are invalid.
	 */
	public TextureRectangle(BufferData data, int width, int height,
			TextureFormat format, DataType type) throws NullPointerException,
			IllegalArgumentException {
		super(format, type);
		setData(data, width, height);
	}

	/**
	 * Creates a texture image with the given type, format and filter, default
	 * other values. Fails like super(format, type, filter), or if the
	 * dimensions and data array are invalid.
	 */
	public TextureRectangle(BufferData data, int width, int height,
			TextureFormat format, DataType type, Filter filter)
			throws NullPointerException, IllegalArgumentException {
		super(format, type, filter);
		setData(data, width, height);
	}

	/**
	 * Create a texture image with the given format, type, filter, wrap mode for
	 * all coordinates, depth mode and test, and depth comparison is disabled.
	 * Fails like super(format, type, filter, wrap, mode, test), or if the
	 * dimensions and data array are invalid.
	 */
	public TextureRectangle(BufferData data, int width, int height,
			TextureFormat format, DataType type, Filter filter,
			TextureWrap wrapAll, DepthMode depthMode, PixelTest depthTest)
			throws NullPointerException, IllegalArgumentException {
		super(format, type, filter, wrapAll, depthMode, depthTest);
		setData(data, width, height);
	}

	/* Internal method used to validate the BufferData[] and dimensions. */
	private void setData(BufferData data, int width, int height)
			throws IllegalArgumentException {
		TextureFormat format = getFormat();
		DataType type = getType();

		if (data != null) {
			if (data.getType() != type)
				throw new IllegalArgumentException(
						"BufferData doesn't have a matching type for the texture, expected: "
								+ type + ", but was: " + data.getType());
			if (data.getCapacity() != format.getBufferSize(width, height, 1))
				throw new IllegalArgumentException(
						"Buffer does not have the correct size, expected: "
								+ format.getBufferSize(width, height, 1)
								+ ", but was: " + data.getCapacity());
		} else if (format.isCompressed())
			throw new IllegalArgumentException(
					"Headless TextureRectangle cannot have a client compressed texture: "
							+ format);

		// everything is valid up to this point, so we can update our values
		this.width = width;
		this.height = height;
		this.data = data;

		this.markDirty();
	}

	/**
	 * Mark a region of the texture image as dirty. The x, y offsets and width
	 * and height will be clamped to be within the valid region of the given
	 * layer.
	 */
	public void markDirty(int x, int y, int width, int height) {
		TextureRectangleDirtyDescriptor dirty = (TextureRectangleDirtyDescriptor) getDirtyDescriptor();

		if (dirty.dirtyRegions == null)
			dirty.dirtyRegions = new MipmapDirtyRegion(x, y, 0, width, height,
					0, this.width, this.height, 0);
		else
			dirty.dirtyRegions.merge(x, y, 0, width, height, 0, this.width,
					this.height, 0);
	}

	/** Mark the entire texture image as dirty. */
	public void markDirty() {
		this.markDirty(0, 0, width, height);
	}

	@Override
	public void setWrapS(TextureWrap wrap) {
		super.setWrapS(TextureWrap.CLAMP);
	}

	@Override
	public void setWrapT(TextureWrap wrap) {
		super.setWrapT(TextureWrap.CLAMP);
	}

	@Override
	public void setWrapR(TextureWrap wrap) {
		super.setWrapR(TextureWrap.CLAMP);
	}

	@Override
	public int getDepth(int level) {
		if (level != 0)
			return -1;
		return 1;
	}

	@Override
	public int getHeight(int level) {
		if (level != 0)
			return -1;
		return height;
	}

	@Override
	public int getNumMipmaps() {
		return 1;
	}

	@Override
	public int getWidth(int level) {
		if (level != 0)
			return -1;
		return width;
	}

	@Override
	public final TextureTarget getTarget() {
		return TextureTarget.T_RECT;
	}

	/**
	 * Get the buffer data for the texture rectangle (there are no other
	 * mipmaps). Returns null if the data isn't in client memory (most likely in
	 * the graphics card).
	 */
	public BufferData getData() throws IllegalArgumentException {
		return data;
	}

	@Override
	protected TextureDirtyDescriptor createTextureDirtyDescriptor() {
		return new TextureRectangleDirtyDescriptor();
	}
}