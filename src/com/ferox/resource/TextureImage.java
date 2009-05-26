package com.ferox.resource;

import com.ferox.effect.Effect.PixelTest;
import com.ferox.renderer.Framework;
import com.ferox.resource.BufferData.DataType;

/**
 * <p>
 * A TextureImage represents the actual data behind a texture (since textures
 * can be re-used and combined in various independent ways, this provides a nice
 * separation of responsibilities).
 * </p>
 * <p>
 * TextureImages use the BufferData class to store there data. Subclasses must
 * hold to this convention and make sure that supplied BufferData's have valid
 * types for the TextureFormat used.
 * </p>
 * <p>
 * To make textures simpler to use, the format and BufferData reference used by
 * the image are immutable. The data within the image's BufferData's may be
 * changed but nothing else. To maintain this rule, subclasses must guarantee
 * that the texture dimensions are also immutable.
 * </p>
 * <p>
 * Renderers using TextureImages that provide null references to BufferData
 * should treat that just as if a non-null BufferData were used with a null
 * array. The reason for allowing null buffer data's is to let texture surfaces
 * hide the data that is stored on the graphics card. It simplifies the update
 * process for textures attached to surfaces, since the only thing that will be
 * updated are the mutable texture parameters.
 * </p>
 * <p>
 * When accessing or manipulating textures, it should be assumed that (0, 0, 0)
 * is the bottom left back corner of the image. Horizontal pixels go left to
 * right, Vertical pixels are indexed bottom to top, and depth pixels are back
 * to front. If a TextureImage only has a subset of the 3 dimensions, then the
 * extra dimensions can be ignored.
 * </p>
 * <p>
 * Default values for null texture parameters: <br>
 * TextureWrap = MIRROR <br>
 * Filter = MIPMAP_LINEAR <br>
 * DepthMode = LUMINANCE <br>
 * DepthCompare = GREATER
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class TextureImage implements Resource {
	/**
	 * Base class that should be used for all subclasses of TextureImage for
	 * their dirty descriptors.
	 */
	public static class TextureDirtyDescriptor {
		private boolean wrapDirty;
		private boolean filterDirty;
		private boolean dCompareDirty;
		private boolean anisoDirty;

		/**
		 * @return True if any of the S, T, and R wrap modes have changed.
		 */
		public final boolean isTextureWrapDirty() {
			return wrapDirty;
		}

		/**
		 * @return True if the Filter enum has changed.
		 */
		public final boolean isFilterDirty() {
			return filterDirty;
		}

		/**
		 * @return True if the DepthMode, depth compare, or depth compare enable
		 *         boolean have changed.
		 */
		public final boolean isDepthCompareDirty() {
			return dCompareDirty;
		}

		/**
		 * @return True if the amount anisotropic filtering is dirty.
		 */
		public final boolean isAnisotropicFilteringDirty() {
			return anisoDirty;
		}

		/**
		 * Should be overridden by subclasses to clear their descriptions, too.
		 * Must call super.clearDescriptor().
		 */
		protected void clearDescriptor() {
			wrapDirty = false;
			filterDirty = false;
			dCompareDirty = false;
			anisoDirty = false;
		}
	}

	/**
	 * <p>
	 * Class that represents a region on a mipmap that is dirty. It provides
	 * access to three dimensions. For textures that don't have a given
	 * dimension, the extra dims. can be ignored.
	 * </p>
	 * <p>
	 * When requesting the offsets and lengths, it is guaranteed that the region
	 * will be within the constraints of the mipmap in question.
	 * </p>
	 * <p>
	 * (0, 0, 0) starts in the lower-left corner and extends up by the dirty
	 * dimensions in question.
	 * </p>
	 */
	public static class MipmapDirtyRegion {
		private int x, y, z, width, height, depth;

		/*
		 * Requires positive values for dimensions. The max dimensions must be
		 * the size of the mipmap in question.
		 */
		protected MipmapDirtyRegion(int x, int y, int z, int width, int height,
			int depth, int maxWidth, int maxHeight, int maxDepth) {
			this.x = this.y = this.z = Integer.MAX_VALUE;
			this.width = this.height = this.depth = Integer.MIN_VALUE;
			merge(x, y, z, width, height, depth, maxWidth, maxHeight, maxDepth);
		}

		public int getDirtyXOffset() {
			return x;
		}

		public int getDirtyYOffset() {
			return y;
		}

		public int getDirtyZOffset() {
			return z;
		}

		public int getDirtyWidth() {
			return width;
		}

		public int getDirtyHeight() {
			return height;
		}

		public int getDirtyDepth() {
			return depth;
		}

		/*
		 * Requires positive values for dimensions. The max dimensions must be
		 * the size of the mipmap in question.
		 */
		protected void merge(int x, int y, int z, int width, int height,
			int depth, int maxWidth, int maxHeight, int maxDepth) {
			// extents of the dirty region, constrained to valid region
			int maxX = Math.min(x + width, maxWidth);
			int maxY = Math.min(y + height, maxHeight);
			int maxZ = Math.min(z + depth, maxDepth);
			int minX = Math.max(0, x);
			int minY = Math.min(0, y);
			int minZ = Math.min(0, z);

			int oldMaxX = this.x + this.width;
			int oldMaxY = this.y + this.height;
			int oldMaxZ = this.z + this.depth;

			this.x = Math.min(this.x, minX);
			this.width = Math.max(oldMaxX, maxX) - this.x;
			this.y = Math.min(this.y, minY);
			this.height = Math.max(oldMaxY, maxY) - this.y;
			this.z = Math.min(this.z, minZ);
			this.depth = Math.max(oldMaxZ, maxZ) - this.z;
		}
	}

	/** An enum representing the supported texture subclasses. */
	public static enum TextureTarget {
		/** Corresponds to Texture1D. */
		T_1D,
		/** Corresponds to Texture2D. */
		T_2D,
		/** Corresponds to Texture3D. */
		T_3D,
		/** Corresponds to TextureCubeMap. */
		T_CUBEMAP,
		/** Corresponds to TextureRectangle. */
		T_RECT

	}

	/**
	 * Describes the wrapping behavior of the texture image when pixels sample
	 * beyond the normal region.
	 */
	public static enum TextureWrap {
		CLAMP, REPEAT, MIRROR
	}

	/**
	 * Get the filter applied to the texture. NEAREST and LINEAR do not use any
	 * mipmap data, even if present. MIPMAP_NEAREST and MIPMAP_LINEAR default to
	 * their non-mipmap versions if a texture doesn't have mipmaps. Filter
	 * describes both minification and magnification.
	 */
	public static enum Filter {
		NEAREST, LINEAR, MIPMAP_NEAREST, MIPMAP_LINEAR
	}

	/** Depth mode of how to interpret depth texture values. */
	public static enum DepthMode {
		ALPHA, INTENSITY, LUMINANCE
	}

	private static final TextureWrap DEFAULT_TEX_WRAP = TextureWrap.MIRROR;
	private static final Filter DEFAULT_FILTER = Filter.MIPMAP_LINEAR;
	private static final DepthMode DEFAULT_DEPTHMODE = DepthMode.LUMINANCE;
	private static final PixelTest DEFAULT_DEPTHTEST = PixelTest.GREATER;

	private final TextureFormat format;
	private final DataType type;

	private TextureWrap wrapS;
	private TextureWrap wrapT;
	private TextureWrap wrapR;

	private Filter filter;
	private float anisoLevel;

	private DepthMode depthMode;
	private boolean enableDepthCompare;
	private PixelTest depthCompareTest;

	private final RenderDataCache renderData;
	private final TextureDirtyDescriptor dirty;

	/**
	 * Creates a texture image with the given format and type, default other
	 * values. Fails if format is null, if type is null, or if the type isn't
	 * valid for format. Subclasses must also ensure that the provided
	 * BufferData match the given type.
	 * 
	 * @param format The TextureFormat for this TextureImage
	 * @param type The DataType for this image
	 * @throws NullPointerException if format or type are null
	 * @throws IllegalArgumentException if format doesn't support type
	 */
	public TextureImage(TextureFormat format, DataType type) {
		this(format, type, null);
	}

	/**
	 * Creates a texture image with the given type, format and filter, default
	 * other values. Subclasses must also ensure that the provided BufferData
	 * match the given type.
	 * 
	 * @param format The TextureFormat for this TextureImage
	 * @param type The DataType for this image
	 * @param filter The Filter that is used when magnifying and minifying the
	 *            image during rendering
	 * @throws NullPointerException if format or type are null
	 * @throws IllegalArgumentException if format doesn't support type
	 */
	public TextureImage(TextureFormat format, DataType type, Filter filter)
		throws IllegalArgumentException, NullPointerException {
		this(format, type, filter, null, null, null);
	}

	/**
	 * Create a texture image with the given format, type, filter, wrap mode for
	 * all coordinates, depth mode and test, and the depth comparison is
	 * disabled. Except for format and type, a value of null will use the
	 * default. Subclasses must also ensure that the provided BufferData match
	 * the given type.
	 * 
	 * @param format The TextureFormat for this TextureImage
	 * @param type The DataType for this image
	 * @param filter The Filter that is used when magnifying and minifying the
	 *            image during rendering
	 * @param wrapAll The TextureWrap to use for the s/t/r coordinates
	 * @param depthMode The DepthMode to use if format is DEPTH
	 * @param depthTest The test to use if depth comparison is enabled
	 * @throws NullPointerException if format or type are null
	 * @throws IllegalArgumentException if format doesn't support type
	 */
	public TextureImage(TextureFormat format, DataType type, Filter filter,
		TextureWrap wrapAll, DepthMode depthMode, PixelTest depthTest)
		throws IllegalArgumentException, NullPointerException {
		if (format == null || type == null)
			throw new NullPointerException(
				"Can't specify a null TextureFormat or DataType: " + format
					+ " " + type);
		if (!format.isTypeValid(type))
			throw new IllegalArgumentException(
				"EffectType and format are not valid: " + format + " " + type);
		this.format = format;
		this.type = type;

		dirty = createTextureDirtyDescriptor();
		if (dirty == null)
			throw new NullPointerException(
				"Can't return a null TextureDirtyDescriptor from createTextureDirtyDescriptor()");

		renderData = new RenderDataCache();

		setFilter(filter);
		this.setWrapSTR(wrapAll);
		setDepthMode(depthMode);
		setDepthCompareTest(depthTest);
		setDepthCompareEnabled(false);
		setAnisotropicFiltering(0f);
	}

	/**
	 * Get the ratio of anisotropic filtering to apply on the image (from 0 to
	 * 1).
	 * 
	 * @return The amount anitostropic filtering, in [0, 1]
	 */
	public float getAnisotropicFiltering() {
		return anisoLevel;
	}

	/**
	 * Set the amount of anisotropic filtering to use on the texture image. The
	 * level is the ration from no filtering to max supported filtering. It is
	 * likely that hardware will clamp this to discrete, supported values.
	 * 
	 * @param level The fraction of anisotropic filtering to use (1 == maximum
	 *            hardware support), clamped to be in [0, 1]
	 */
	public void setAnisotropicFiltering(float level) {
		float old = anisoLevel;
		anisoLevel = Math.max(0f, Math.min(level, 1f));

		if (old != anisoLevel)
			dirty.anisoDirty = true;
	}

	/**
	 * Get the format of the texture image. Format is immutable and will not
	 * change over the lifetime of a TextureImage.
	 * 
	 * @return The TextureFormat of the image
	 */
	public TextureFormat getFormat() {
		return format;
	}

	/**
	 * Get the DataType that all BufferData's of the texture image must have.
	 * This will be immutable after construction.
	 * 
	 * @return The DataType for all image data of the texture
	 */
	public DataType getType() {
		return type;
	}

	/**
	 * Get the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the s texture coordinate.
	 * 
	 * @return The TextureWrap for the s coordinate
	 */
	public TextureWrap getWrapS() {
		return wrapS;
	}

	/**
	 * Set the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the s texture coordinate. If null, uses the
	 * default.
	 * 
	 * @param wrapS The TextureWrap to use, null = MIRROR
	 */
	public void setWrapS(TextureWrap wrapS) {
		if (wrapS == null)
			wrapS = DEFAULT_TEX_WRAP;
		this.wrapS = wrapS;
		dirty.wrapDirty = true;
	}

	/**
	 * Get the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the t texture coordinate.
	 * 
	 * @return The TextureWrap for the t coordinate
	 */
	public TextureWrap getWrapT() {
		return wrapT;
	}

	/**
	 * Set the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the t texture coordinate. If null, uses the
	 * default.
	 * 
	 * @param wrapT The TextureWrap to use, null = MIRROR
	 */
	public void setWrapT(TextureWrap wrapT) {
		if (wrapT == null)
			wrapT = DEFAULT_TEX_WRAP;
		this.wrapT = wrapT;
		dirty.wrapDirty = true;
	}

	/**
	 * Get the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the r texture coordinate.
	 * 
	 * @return The TextureWrap for the r coordinate
	 */
	public TextureWrap getWrapR() {
		return wrapR;
	}

	/**
	 * Set the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the r texture coordinate. If null, uses the
	 * default.
	 * 
	 * @param wrapR The TextureWrap to use, null = MIRROR
	 */
	public void setWrapR(TextureWrap wrapR) {
		if (wrapR == null)
			wrapR = DEFAULT_TEX_WRAP;
		this.wrapR = wrapR;
		dirty.wrapDirty = true;
	}

	/**
	 * Utility to set all texture coordinate wrap modes to the given value, if
	 * null uses the default.
	 * 
	 * @param all The TextureWrap to set for all 3 coordinates
	 */
	public void setWrapSTR(TextureWrap all) {
		this.setWrapSTR(all, all, all);
	}

	/**
	 * Convenience method to set the s, t, and r wrap modes to the three values.
	 * If any is null, that coordinate's mode is set to the default.
	 * 
	 * @param s The TextureWrap to set for the s coordinate
	 * @param t The TextureWrap to set for the t coordinate
	 * @param r The TextureWrap to set for the r coordinate
	 */
	public void setWrapSTR(TextureWrap s, TextureWrap t, TextureWrap r) {
		setWrapS(s);
		setWrapT(t);
		setWrapR(r);
	}

	/**
	 * <p>
	 * Get the filter applied to the texture when magnifying and minifying
	 * texels (when a texel is bigger/smaller than 1 pixel).
	 * </p>
	 * <p>
	 * If filter was set to one of the MIPMAP_X variety and this texture has a
	 * mipmap count of 0, then X is returned instead.
	 * </p>
	 * 
	 * @return The filter to use, will only be MIPMAP_X if getNumMipmaps > 1
	 */
	public Filter getFilter() {
		if (getNumMipmaps() == 1)
			// must make sure not to return an invalid filter
			if (filter == Filter.MIPMAP_LINEAR)
				return Filter.LINEAR;
			else if (filter == Filter.MIPMAP_NEAREST)
				return Filter.NEAREST;
		return filter;
	}

	/**
	 * Set the filter to be applied when magnifying and minifying texels.
	 * 
	 * @see #getFilter()
	 * @param filter The new filter to use, null = LINEAR
	 */
	public void setFilter(Filter filter) {
		if (filter == null)
			filter = DEFAULT_FILTER;

		this.filter = filter;
		dirty.filterDirty = true;
	}

	/**
	 * Get the depth mode for this texture.
	 * 
	 * @return The DepthMode used when the format is DEPTH
	 */
	public DepthMode getDepthMode() {
		return depthMode;
	}

	/**
	 * Set the depth mode for this texture. If null, uses the default. Depth
	 * mode represents how depth textures are treated in the color combination
	 * after the final depth value is computed.
	 * 
	 * @param depthMode The depth mode to use, null = LUMINANCE
	 */
	public void setDepthMode(DepthMode depthMode) {
		if (depthMode == null)
			depthMode = DEFAULT_DEPTHMODE;
		this.depthMode = depthMode;
		dirty.dCompareDirty = true;
	}

	/**
	 * Get the depth test to use when depth comparing is enabled.
	 * 
	 * @return The PixelTest used when depth comparing during texture
	 *         application
	 */
	public PixelTest getDepthCompareTest() {
		return depthCompareTest;
	}

	/**
	 * Set the depth test to use when depth comparing is enabled.
	 * 
	 * @param depthCompareTest The new PixelTest to use, null = GREATER
	 */
	public void setDepthCompareTest(PixelTest depthCompareTest) {
		if (depthCompareTest == null)
			depthCompareTest = DEFAULT_DEPTHTEST;
		this.depthCompareTest = depthCompareTest;
		dirty.dCompareDirty = true;
	}

	/**
	 * Whether or not depth comparing is enabled for depth textures. Like depth
	 * mode and depth compare test, this is meaningless for textures that don't
	 * have a depth format.
	 * 
	 * @return True if depth comparing is performed
	 */
	public boolean isDepthCompareEnabled() {
		return enableDepthCompare;
	}

	/**
	 * Set whether or not depth comparing is enabled. Meaningless for non-depth
	 * textures. If this is enabled, the final texture value is one or zero,
	 * depending on the passing of the depth comparison test.
	 * 
	 * @param enableDepthCompare Whether or not depth comparisons are enabled
	 *            for DEPTH textures
	 */
	public void setDepthCompareEnabled(boolean enableDepthCompare) {
		this.enableDepthCompare = enableDepthCompare;
		dirty.dCompareDirty = true;
	}

	/**
	 * Get the width of the texture for the given mipmap level. If level isn't
	 * present as a mipmap, less than 0, or above the number of mipmaps-1,
	 * return -1. Level 0 represents the largest mipmap.
	 * 
	 * @param level The mipmap level whose width is requested
	 * @return The mipmap width, or -1 if level was invalid
	 */
	public abstract int getWidth(int level);

	/**
	 * <p>
	 * Get the height of the texture for the given mipmap level. If level isn't
	 * present as a mipmap, less than 0, or above the number of mipmaps-1,
	 * return -1. Level 0 represents the largest mipmap.
	 * </p>
	 * <p>
	 * For textures without a 2nd dimension, this should return 1 if level was a
	 * valid level.
	 * </p>
	 * 
	 * @param level The mipmap level whose height is requested
	 * @return The mipmap height, or -1 if level was invalid
	 */
	public abstract int getHeight(int level);

	/**
	 * <p>
	 * Get the depth of the texture for the given mipmap level. If level isn't
	 * present as a mipmap, less than 0, or above the number of mipmaps-1,
	 * return -1. Level 0 represents the largest mipmap.
	 * </p>
	 * <p>
	 * For textures without a third dimension, this should return 1 if level was
	 * a valid level.
	 * </p>
	 * 
	 * @param level The mipmap level whose depth is requested
	 * @return The mipmap depth, or -1 if level was invalid
	 */
	public abstract int getDepth(int level);

	/**
	 * Get the number of mipmaps used for this texture. Even if the textures
	 * data is null, this should reflect the texture's mipmap policy.
	 * 
	 * @return The number of mipmaps held within this texture, at least 1
	 */
	public abstract int getNumMipmaps();

	/**
	 * Whether or not this texture is mipmapped. True if and only if
	 * getNumMipmaps() > 1.
	 * 
	 * @return If this texture image has mipmaps
	 */
	public final boolean isMipmapped() {
		return getNumMipmaps() > 1;
	}

	/**
	 * Return the texture target enum that corresponds to this images class.
	 * 
	 * @return The target for this texture image
	 */
	public abstract TextureTarget getTarget();

	/**
	 * Create the subclasses dirty descriptor instance to use. Must not return
	 * null. This will be the object returned by getDirtyDescriptor().
	 * 
	 * @return The dirty descriptor to use for this instance
	 */
	protected abstract TextureDirtyDescriptor createTextureDirtyDescriptor();

	/**
	 * <p>
	 * A utility method that will calculate the number of mipmaps for the given
	 * texture dimensions. This will still give the expected result for
	 * non-power-of-two and rectangular textures.
	 * </p>
	 * <p>
	 * Returns -1 if any of the dimensions are <= 0.
	 * </p>
	 * 
	 * @param width Width of the 0th mipmap level
	 * @param height Height of the 0th mipmap level
	 * @param depth Depth of the 0th mipmap level
	 * @return The number of mipmaps that are needed for a complete texture of
	 *         the given dimensions, or -1 if the dimensions are invalid
	 */
	public static int calculateMipmapCount(int width, int height, int depth) {
		if (width <= 0 || height <= 0 || depth <= 0)
			return -1;
		return (int) Math.floor(Math.log(Math.max(width, Math
			.max(height, depth)))
			/ Math.log(2)) + 1;
	}

	@Override
	public Object getRenderData(Framework renderer) {
		return renderData.getRenderData(renderer);
	}

	@Override
	public void setRenderData(Framework renderer, Object data) {
		renderData.setRenderData(renderer, data);
	}

	@Override
	public void clearDirtyDescriptor() {
		dirty.clearDescriptor();
	}

	/**
	 * Subclasses should override this with a more specific descriptor type if
	 * necessary. The returned instance if the object created by
	 * createTextureDirtyDescriptor().
	 * 
	 * @return The dirty descriptor for this texture
	 */
	@Override
	public TextureDirtyDescriptor getDirtyDescriptor() {
		return dirty;
	}
}
