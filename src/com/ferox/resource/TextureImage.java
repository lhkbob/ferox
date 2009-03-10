package com.ferox.resource;

import com.ferox.resource.BufferData.DataType;
import com.ferox.state.State.PixelTest;

/** A TextureImage represents the actual data behind a texture
 * (since textures can be re-used and combined in various independent
 * ways, this provides a nice separation of responsibilities).
 * 
 * TextureImages use the BufferData class to store there data.  Subclasses
 * must hold to this convention and make sure that supplied BufferData's
 * have valid types for the TextureFormat used.  
 * 
 * To make textures simpler to use, the format and BufferData reference
 * used by the image are immutable.  The data within the image's BufferData's
 * may be changed but nothing else.  To maintain this rule, subclasses
 * must guarantee that the texture dimensions are also immutable.
 * 
 * Renderers using TextureImages that provide null references to BufferData
 * should treat that just as if a non-null BufferData were used with a null array.
 * The reason for allowing null buffer data's is to let texture surfaces hide
 * the data that is stored on the graphics card.  It simplifies the update process
 * for textures attached to surfaces, since the only thing that will be updated are
 * the mutable texture parameters.
 * 
 * When accessing or manipulating textures, it should be assumed that (0, 0, 0)
 * is the bottom left back corner of the image.  Horizontal pixels go left to right,
 * Vertical pixels are indexed bottom to top, and depth pixels are back to front.
 * If a TextureImage only has a subset of the 3 dimensions, then the extra dimensions
 * can be ignored.
 * 
 * Default values for null texture parameters:
 * TextureWrap = MIRROR
 * Filter = MIPMAP_LINEAR
 * DepthMode = LUMINANCE
 * DepthCompare = GREATER
 * 
 * @author Michael Ludwig
 *
 */
public abstract class TextureImage implements Resource {
	/** Base class that should be used for all subclasses of TextureImage
	 * for their dirty descriptors. */
	public static class TextureDirtyDescriptor {
		private boolean wrapDirty;
		private boolean filterDirty;
		private boolean dCompareDirty;
		private boolean anisoDirty;
		
		/** True if any of the S, T, and R wrap modes have changed. */
		public final boolean isTextureWrapDirty() { return this.wrapDirty; }
		/** True if the Filter enum has changed. */
		public final boolean isFilterDirty() { return this.filterDirty; }
		/** True if the DepthMode, depth compare, or depth compare enable
		 * boolean have changed. */
		public final boolean isDepthCompareDirty() { return this.dCompareDirty; }
		/** True if the amount anisotropic filtering is dirty. */
		public final boolean isAnisotropicFilteringDirty() { return this.anisoDirty; }
		
		/** Should be overridden by subclasses to clear their caches, too. 
		 * Must call super.clearDescriptor(). */
		protected void clearDescriptor() {
			this.wrapDirty = false;
			this.filterDirty = false;
			this.dCompareDirty = false;
			this.anisoDirty = false;
		}
	}
	
	/** Class that represents a region on a mipmap that is dirty. It 
	 * provides access to three dimensions.  For textures that don't
	 * have a given dimension, the extra dims. can be ignored. 
	 * 
	 * When requesting the offsets and lengths, it is guaranteed that
	 * the region will be within the constraints of the mipmap in question. 
	 * 
	 * (0, 0, 0) starts in the lower-left corner and extends up by the
	 * dirty dimensions in question. */
	public static class MipmapDirtyRegion {
		private int x, y, z, width, height, depth;
		
		/* Requires positive values for dimensions. The max dimensions must
		 * be the size of the mipmap in question. */
		protected MipmapDirtyRegion(int x, int y, int z, int width, int height, int depth,
									int maxWidth, int maxHeight, int maxDepth) {
			this.x = this.y = this.z = Integer.MAX_VALUE;
			this.width = this.height = this.depth = Integer.MIN_VALUE;
			this.merge(x, y, z, width, height, depth, maxWidth, maxHeight, maxDepth);
		}
		
		public int getDirtyXOffset() { return this.x; }
		public int getDirtyYOffset() { return this.y; }
		public int getDirtyZOffset() { return this.z; }
		public int getDirtyWidth() { return this.width; }
		public int getDirtyHeight() { return this.height; }
		public int getDirtyDepth() { return this.depth; }
		
		/* Requires positive values for dimensions. The max dimensions must
		 * be the size of the mipmap in question. */
		protected void merge(int x, int y, int z, int width, int height, int depth,
							 int maxWidth, int maxHeight, int maxDepth) {
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
				
			this.x = Math.min(this.x, minX); this.width = Math.max(oldMaxX, maxX) - this.x;
			this.y = Math.min(this.y, minY); this.height = Math.max(oldMaxY, maxY) - this.y;
			this.z = Math.min(this.z, minZ); this.depth = Math.max(oldMaxZ, maxZ) - this.z;
		}
	}
	
	/** An enum representing the supported texture subclasses. */
	public static enum TextureTarget {
		T_1D, 		/** Corresponds to Texture1D. */
		T_2D, 		/** Corresponds to Texture2D. */
		T_3D, 		/** Corresponds to Texture3D. */
		T_CUBEMAP, 	/** Corresponds to TextureCubeMap. */
		T_RECT	   	/** Corresponds to TextureRectangle. */
	}
	
	/** Describes the wrapping behavior of the texture image when pixels sample
	 * beyond the normal region. */
	public static enum TextureWrap {
		CLAMP, REPEAT, MIRROR
	}
	
	/** Get the filter applied to the texture.  NEAREST and LINEAR do not use any mipmap data, even if present. 
	 * MIPMAP_NEAREST and MIPMAP_LINEAR default to their non-mipmap versions if a texture doesn't have mipmaps.
	 * Filter describes both minification and magnification. */
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
	
	private TextureFormat format;
	private DataType type;
	
	private TextureWrap wrapS;
	private TextureWrap wrapT;
	private TextureWrap wrapR;
	
	private Filter filter;
	private float anisoLevel;
	
	private DepthMode depthMode;
	private boolean enableDepthCompare;
	private PixelTest depthCompareTest;
	
	private Object renderData;
	private final TextureDirtyDescriptor dirty;
	
	/** Creates a texture image with the given format and type, default other values.
	 * Fails if format is null, if type is null, or if the type isn't valid for format.  
	 * Subclasses must also ensure that the provided BufferData match the given type.. */
	public TextureImage(TextureFormat format, DataType type) throws IllegalArgumentException, NullPointerException {
		this(format, type, null);
	}
	
	/** Creates a texture image with the given type, format and filter, default other values.
	 * Fails if format is null, if type is null, or if the type isn't valid for format.
	 * Subclasses must also ensure that the provided BufferData match the given type. */
	public TextureImage(TextureFormat format, DataType type, Filter filter) throws IllegalArgumentException, NullPointerException {
		this(format, type, filter, null, null, null);
	}
	
	/** Create a texture image with the given format, type, filter, wrap mode for all coordinates,
	 * depth mode and test, and the depth comparison is disabled. Except for format, a value of
	 * null will use the default.
	 * 
	 * Fails if format is null, if type is null, or if the type isn't valid for format.
	 * Subclasses must also ensure that the provided BufferData match the given type. */
	public TextureImage(TextureFormat format, DataType type, Filter filter, TextureWrap wrapAll, DepthMode depthMode, PixelTest depthTest) throws IllegalArgumentException, NullPointerException {
		if (format == null || type == null)
			throw new NullPointerException("Can't specify a null TextureFormat or DataType: " + format + " " + type);
		if (!format.isTypeValid(type))
			throw new IllegalArgumentException("Type and format are not valid: " + format + " " + type);
		this.format = format;
		this.type = type;
		
		this.dirty = this.createTextureDirtyDescriptor();
		if (this.dirty == null)
			throw new NullPointerException("Can't return a null TextureDirtyDescriptor from createTextureDirtyDescriptor()");
		
		this.setFilter(filter);
		this.setWrapSTR(wrapAll);
		this.setDepthMode(depthMode);
		this.setDepthCompareTest(depthTest);
		this.setDepthCompareEnabled(false);
		this.setAnisotropicFiltering(0f);
	}
	
	/** Get the ratio of anisotropic filtering to apply
	 * on the image (from 0 to 1). */
	public float getAnisotropicFiltering() {
		return this.anisoLevel;
	}
	
	/** Set the amount of anisotropic filtering to use on the
	 * texture image.  The level is the ration from no filtering
	 * to max supported filtering.  It is likely that hardware
	 * will clamp this to discrete, supported values. */
	public void setAnisotropicFiltering(float level) {
		float old = this.anisoLevel;
		this.anisoLevel = Math.max(0f, Math.min(level, 1f));
		
		if (old != this.anisoLevel)
			this.dirty.anisoDirty = true;
	}

	/** Get the format of the texture image. Format is immutable and will
	 * not change over the lifetime of a TextureImage. */
	public TextureFormat getFormat() {
		return this.format;
	}

	/** Get the DataType that all BufferData's of the texture image must have. */
	public DataType getType() {
		return this.type;
	}
	
	/** Get the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the s texture coordinate. */
	public TextureWrap getWrapS() {
		return this.wrapS;
	}

	/** Set the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the s texture coordinate. 
	 * If null, uses the default. */
	public void setWrapS(TextureWrap wrapS) {
		if (wrapS == null)
			wrapS = DEFAULT_TEX_WRAP;
		this.wrapS = wrapS;
		this.dirty.wrapDirty = true;
	}

	/** Get the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the t texture coordinate. */
	public TextureWrap getWrapT() {
		return this.wrapT;
	}

	/** Set the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the t texture coordinate. 
	 * If null, uses the default. */
	public void setWrapT(TextureWrap wrapT) {
		if (wrapT == null)
			wrapT = DEFAULT_TEX_WRAP;
		this.wrapT = wrapT;
		this.dirty.wrapDirty = true;
	}

	/** Get the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the r texture coordinate. */
	public TextureWrap getWrapR() {
		return this.wrapR;
	}

	/** Set the type of texture coordinate wrapping when coordinates go beyond
	 * the edge of the image, along the r texture coordinate. 
	 * If null, uses the default. */
	public void setWrapR(TextureWrap wrapR) {
		if (wrapR == null)
			wrapR = DEFAULT_TEX_WRAP;
		this.wrapR = wrapR;
		this.dirty.wrapDirty = true;
	}

	/** Utility to set all texture coordinate wrap modes to the given value,
	 * if null uses the default. */
	public void setWrapSTR(TextureWrap all) {
		this.setWrapSTR(all, all, all);
	}
	
	/** Convenience method to set the s, t, and r wrap modes to the three values.
	 * If any is null, that coordinate's mode is set to the default. */
	public void setWrapSTR(TextureWrap s, TextureWrap t, TextureWrap r) {
		this.setWrapS(s);
		this.setWrapT(t);
		this.setWrapR(r);
	}
	
	/** Get the filter applied to the texture when magnifying and minifying
	 * texels (when a texel is bigger/smaller than 1 pixel). 
	 * 
	 * If filter was set to one of the MIPMAP_X variety and this texture 
	 * has a mipmap count of 0, then X is returned instead. */
	public Filter getFilter() {
		if (this.getNumMipmaps() == 1) {
			// must make sure not to return an invalid filter
			if (this.filter == Filter.MIPMAP_LINEAR)
				return Filter.LINEAR;
			else if (this.filter == Filter.MIPMAP_NEAREST)
				return Filter.NEAREST;
		}
		return this.filter;
	}

	/** Set the filter to be applied when magnifying and minifying texels.
	 * If filter is null, the default filter is null. */
	public void setFilter(Filter filter) {
		if (filter == null)
			filter = DEFAULT_FILTER;
		
		this.filter = filter;
		this.dirty.filterDirty = true;
	}

	/** Get the depth mode for this texture. */
	public DepthMode getDepthMode() {
		return this.depthMode;
	}

	/** Set the depth mode for this texture.  If null, uses the default.
	 * Depth mode represents how depth textures are treated in the color combination
	 * RenderQueue after the final depth value is computed (see depth compare). */
	public void setDepthMode(DepthMode depthMode) {
		if (depthMode == null)
			depthMode = DEFAULT_DEPTHMODE;
		this.depthMode = depthMode;
		this.dirty.dCompareDirty = true;
	}

	/** Get the depth test to use when depth comparing is enabled. */
	public PixelTest getDepthCompareTest() {
		return this.depthCompareTest;
	}

	/** Set the depth test to use when depth comparing is enabled.
	 * If null, uses the default. */
	public void setDepthCompareTest(PixelTest depthCompareTest) {
		if (depthCompareTest == null)
			depthCompareTest = DEFAULT_DEPTHTEST;
		this.depthCompareTest = depthCompareTest;
		this.dirty.dCompareDirty = true;
	}

	/** Whether or not depth comparing is enabled for depth textures.
	 * Like depth mode and depth compare test, this is meaningless for
	 * textures that don't have a depth format. */
	public boolean isDepthCompareEnabled() {
		return this.enableDepthCompare;
	}

	/** Set whether or not depth comparing is enabled. Meaningless
	 * for non-depth textures.  If this is enabled, the final texture value
	 * is one or zero, depending on the passing of the depth comparison test. */
	public void setDepthCompareEnabled(boolean enableDepthCompare) {
		this.enableDepthCompare = enableDepthCompare;
		this.dirty.dCompareDirty = true;
	}
	
	/** Get the width of the texture for the given mipmap level.  If
	 * level isn't present as a mipmap, less than 0, or above the number of
	 * mipmaps-1, return -1.  Level 0 represents the largest mipmap. */
	public abstract int getWidth(int level);
	
	/** Get the height of the texture for the given mipmap level.  If
	 * level isn't present as a mipmap, less than 0, or above the number of
	 * mipmaps-1, return -1.  Level 0 represents the largest mipmap.
	 * 
	 * For textures without a 2nd dimension, this should return 1 if level was
	 * a valid level. */
	public abstract int getHeight(int level);
	
	/** Get the depth of the texture for the given mipmap level.  If
	 * level isn't present as a mipmap, less than 0, or above the number of
	 * mipmaps-1, return -1.  Level 0 represents the largest mipmap. 
	 * 
	 * For textures without a third dimension, this should return 1 if level was
	 * a valid level. */
	public abstract int getDepth(int level);
	
	/** Get the number of mipmaps used for this texture.  Even if 
	 * the textures data is null, this should reflect the texture's mipmap policy.
	 * 
	 * Must return at least 1. */
	public abstract int getNumMipmaps();
	
	/** Whether or not this texture is mipmapped.  True if and only if
	 * getNumMipmaps() > 1. */
	public final boolean isMipmapped() {
		return this.getNumMipmaps() > 1;
	}
	
	/** Return the texture target enum that corresponds to this images class. */
	public abstract TextureTarget getTarget();
	
	/** Create the subclasses dirty descriptor instance to use.  Must not 
	 * return null.  This will be the object returned by getDirtyDescriptor(). */
	protected abstract TextureDirtyDescriptor createTextureDirtyDescriptor();

	/** A utility method that will calculate the number of mipmaps for the
	 * given texture dimensions.  This will still give the expected result for
	 * non-power-of-two and rectangular textures.
	 * 
	 * Returns -1 if any of the dimensions are <= 0. */
	public static int calculateMipmapCount(int width, int height, int depth) {
		if (width <= 0 || height <= 0 || depth <= 0)
			return -1;
		return (int) Math.floor(Math.log(Math.max(width, Math.max(height, depth))) / Math.log(2)) + 1;
	}
	
	@Override
	public Object getResourceData() {
		return this.renderData;
	}

	@Override
	public void setResourceData(Object data) {
		this.renderData = data;
	}

	@Override
	public final void clearDirtyDescriptor() {
		this.dirty.clearDescriptor();
	}

	/** Subclasses will provide objects that are subclasses
	 * of TextureDirtyDescriptor. */
	@Override
	public final TextureDirtyDescriptor getDirtyDescriptor() {
		return this.dirty;
	}
}
