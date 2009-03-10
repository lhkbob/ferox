package com.ferox.resource;

import com.ferox.resource.BufferData.DataType;
import com.ferox.state.StateException;
import com.ferox.state.State.PixelTest;

/** Represents a texture that is a three-dimensional block of texture
 * information.  Each mipmap layer of a texture is a block, and the block
 * can be thought of as depth-count 2d sub layers (width x height), ordered
 * from back to front.
 * 
 * Like Texture2D and Texture1D, only newer hardware supports 
 * non-power of two dimensions.  If necessary, a Renderer should
 * rescale an image so that it is still usable. Even if they are supported, 
 * power-of-two textures likely have better performance.
 * 
 * One noticable difference between Texture3D and Texture2D is that the
 * dimensions do not have to be equal.
 * 
 * Besides the arguments present in TextureImage's constructors,
 * Texture3D adds the additional parameters BufferData[] data and 
 * int width/height/depth.  width represents pixel width of a 2D layer,
 * height is the pixel height of that layer, and depth is the number of 2D
 * layers present in the block. data is the array of all mipmaps for the image. 
 * If the data is null, all elements are null, or if it has a length of one, then 
 * the texture is not mipmapped.
 * 
 * The constructors will additionally throw exceptions if width or the 
 * data array are not valid.  Here are the rules:
 * 1. width must be positive.
 * 2. If data isn't null, then all elements must be null, or all
 * 	  must not be null.
 * 3. A data array with non-null elements and a length > 1 is considered
 * 	  mipmapped.  All mipmaps must be present in this layer, based on
 *    the expected number from width.
 * 4. Every mipmap layer (including the 0th layer) must be sized correctly
 * 	  based on the layer's dimension, format and type.
 * 5. All non-null BufferData's must have the same data type as passed 
 *    into the constructor.
 * 
 * Texture3D does not allow formats of DEPTH, or XYZ_DXT?
 * 
 * Texture3D provides methods to mark regions of the texture's mipmap
 * levels as dirty.  These commands will always be clamped to the
 * valid regions of the texture.  They will also update the dirty descriptor
 * regardless of the null status of any buffer data's, or the data's
 * arrays.  It is the Renderer's job to make sure that null data's or arrays
 * are treated correctly regardless of what the dirty descriptor declares.
 * 
 * @author Michael Ludwig
 * 
 */
public class Texture3D extends TextureImage {
	/** The dirty descriptor class that is used by Texture3D.  Calls to
	 * getDirtyDescriptor() for texture 3d's will return objects of this class. */
	public static class Texture3DDirtyDescriptor extends TextureDirtyDescriptor {
		private MipmapDirtyRegion[] dirtyRegions;
		
		/** True if there is a non-null MipmapDirtyRegion
		 * for the associated mipmap level.
		 * 
		 *  Returns false if level is invalid. */
		public boolean isDataDirty(int level) { 
			if (this.dirtyRegions == null || level < 0 || level >= this.dirtyRegions.length)
				return false;
			return this.dirtyRegions[level] != null;
		}
		
		/** Get the MipmapDirtyRegion for the given mipmap level.
		 * If null is returned, then that mipmap isn't dirty or level was invalid.
		 * 
		 * The returned region will be constrained to be in the dimensions
		 * of the mipmap level. */
		public MipmapDirtyRegion getDirtyRegion(int level) {
			if (this.dirtyRegions == null || level < 0 || level >= this.dirtyRegions.length)
				return null;
			return this.dirtyRegions[level];
		}
		
		/** Return true if at least one mipmap region is not null. */
		public boolean areMipmapsDirty() {
			return this.dirtyRegions != null;
		}
		
		@Override
		protected void clearDescriptor() {
			super.clearDescriptor();
			this.dirtyRegions = null;
		}
	}
	
	private BufferData[] data;
	private int width, height, depth;
	private int numMipmaps;
	
	/** Creates a texture image with the given format and type, default other values.
	 * Fails like super(format, type), or if the dimensions and data array are invalid. */
	public Texture3D(BufferData[] data, int width, int height, int depth, TextureFormat format, DataType type) throws NullPointerException, IllegalArgumentException {
		super(format, type);
		this.setData(data, width, height, depth);
	}
	
	/** Creates a texture image with the given type, format and filter, default other values.
	 * Fails like super(format, type, filter), or if the dimensions and data array are invalid.*/
	public Texture3D(BufferData[] data, int width, int height, int depth, TextureFormat format,  DataType type, Filter filter) throws StateException {
		super(format, type, filter);
		this.setData(data, width, height, depth);
	}
	
	/** Create a texture image with the given format, type, filter, wrap mode for all coordinates,
	 * depth mode and test, and depth comparison is disabled. 
	 * Fails like super(format, type, filter, wrap, mode, test), or if the dimensions and data array are invalid. */
	public Texture3D(BufferData[] data, int width, int height, int depth, TextureFormat format, DataType type, Filter filter, TextureWrap wrapAll, DepthMode depthMode, PixelTest depthTest) throws StateException {
		super(format, type, filter, wrapAll, depthMode, depthTest);
		this.setData(data, width, height, depth);
	}
	
	/* Internal method used to validate the BufferData[] and dimensions. */
	private void setData(BufferData[] data, int width, int height, int depth) throws IllegalArgumentException {
		int numMipmaps = TextureImage.calculateMipmapCount(width, height, depth); // expected mipmap count, if data.length > 1
		TextureFormat format = this.getFormat();
		DataType type = this.getType();
		
		if (format == TextureFormat.DEPTH || format.isCompressed())
			throw new IllegalArgumentException("The texture format cannot be compressed or be of type DEPTH for a Texture3D: " + format);
		
		BufferData[] realData = null;
		if (data != null) {
			if (data.length != 1 && data.length != numMipmaps) // check the mipmap count
				throw new IllegalArgumentException("If more than one BufferData is given, must provide all mipmap levels");
			numMipmaps = data.length;
			
			int nonNullCount = 0;
			for (int i = 0; i < data.length; i++) {
				if (data[i] != null)
					nonNullCount++;
			}
			if (nonNullCount == 0) {
				// do nothing, it should be a headless texture
			} else if (nonNullCount == data.length) {
				realData = new BufferData[data.length]; // make a new array to hold the buffers, so it can't be tampered with later.
				System.arraycopy(data, 0, realData, 0, Math.min(data.length, realData.length));
			} else {
				throw new IllegalArgumentException("Cannot pass in an array with some values null.  Array length: " + data.length + ", but has only " + nonNullCount + " non-null buffers.");
			}
		}
		
		if (realData != null) {
			int w = width;
			int h = height;
			int d = depth;
			for (int i = 0; i < realData.length; i++) {
				if (realData[i].getType() != type)
					throw new IllegalArgumentException("BufferData doesn't have a matching type for the texture, expected: " + type + ", but was: " + realData[i].getType());
				if (realData[i].getCapacity() != format.getBufferSize(w, h, d))
					throw new IllegalArgumentException("Buffer at mipmap level: " + i + " is does not have the correct size, expected: " + format.getBufferSize(w, h, d) + ", but was: " + realData[i].getCapacity());
				w = Math.max(1, w >> 1);
				h = Math.max(1, h >> 1);
				d = Math.max(1, d >> 1);
			}
			
			numMipmaps = realData.length;
		} else {
			numMipmaps = 1;
		}
		
		// everything is valid up to this point, so we can update our values
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.numMipmaps = numMipmaps;
		this.data = realData;
		
		this.markDirty();
	}
	
	/** Mark the given mipmap level region dirty.  If the level
	 * is outside of [0, numMipmaps - 1], this command does nothing.
	 * The x, y offsets and width and height will be clamped to be
	 * within the valid region of the given mipmap. */
	public void markDirty(int x, int y, int z, int width, int height, int depth, int level) {
		if (level < 0 || level >= (this.numMipmaps - 1))
			return; // invalid level option
		
		Texture3DDirtyDescriptor dirty = (Texture3DDirtyDescriptor) this.getDirtyDescriptor();
		if (dirty.dirtyRegions == null || dirty.dirtyRegions.length <= level) {
			MipmapDirtyRegion[] temp = new MipmapDirtyRegion[level + 1];
			if (dirty.dirtyRegions != null)
				System.arraycopy(dirty.dirtyRegions, 0, temp, 0, dirty.dirtyRegions.length);
			dirty.dirtyRegions = temp;
		}
		
		int levelWidth = this.getWidth(level);
		int levelHeight = this.getHeight(level);
		int levelDepth = this.getDepth(level);
		MipmapDirtyRegion r = dirty.dirtyRegions[level];
		if (r == null) {
			r = new MipmapDirtyRegion(x, y, z, width, height, depth, 
									  levelWidth, levelHeight, levelDepth);
			dirty.dirtyRegions[level] = r;
		} else
			r.merge(x, y, 0, width, height, depth,
					levelWidth, levelHeight, levelDepth);
	}
	
	/** Mark the entire mipmap level dirty.  Does nothing if
	 * level isn't within [0, numMipmaps - 1]. */
	public void markDirty(int level) {
		this.markDirty(0, 0, 0, this.getWidth(level), this.getHeight(level), this.getDepth(level), level);
	}
	
	/** Mark the entire texture image as dirty. */
	public void markDirty() {
		Texture3DDirtyDescriptor dirty = (Texture3DDirtyDescriptor) this.getDirtyDescriptor();
		// create the whole array now for efficiency.  It's okay to ignore old array because
		// the new regions will take up the whole level.
		dirty.dirtyRegions = new MipmapDirtyRegion[this.numMipmaps]; 
		for (int i = 0; i < this.numMipmaps; i++)
			this.markDirty(i);
	}
	
	@Override
	public int getDepth(int level) {
		if (level < 0 || level >= this.numMipmaps)
			return -1;
		return Math.max(1, this.depth >> level);
	}

	@Override
	public int getHeight(int level) {
		if (level < 0 || level >= this.numMipmaps)
			return -1;
		return Math.max(1, this.height >> level);
	}

	@Override
	public int getNumMipmaps() {
		return this.numMipmaps;
	}

	@Override
	public int getWidth(int level) {
		if (level < 0 || level >= this.numMipmaps)
			return -1;
		return Math.max(1, this.width >> level);
	}
	
	@Override
	public final TextureTarget getTarget() {
		return TextureTarget.T_3D;
	}

	/** Get the buffer for the given level.  Fails if the
	 * level is < 0 or >= the number of mipmaps.
	 * 
	 * Returns null if the data isn't in client memory (most likely
	 * in the graphics card). */
	public BufferData getData(int level) throws IllegalArgumentException {
		if (level < 0 || level >= this.numMipmaps)
			throw new IllegalArgumentException("Buffer data doesn't exist beyond mipmap levels, illegal level: " + level);
		if (this.data == null)
			return null; // all we can return at this point
		return this.data[level];
	}

	@Override
	protected TextureDirtyDescriptor createTextureDirtyDescriptor() {
		return new Texture3DDirtyDescriptor();
	}
}