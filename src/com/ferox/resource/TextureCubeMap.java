package com.ferox.resource;

import com.ferox.resource.BufferData.DataType;
import com.ferox.state.StateException;
import com.ferox.state.State.PixelTest;

/** Represents a square two-dimensional image.  The texture data
 * is accessed by normalized texture coordinates.  The dimensions
 * are not required to be power-of-two textures, but a Renderer
 * will likely re-scale the image if they aren't supported.
 * 
 * Even if they are supported, power-of-two textures likely have
 * better performance.
 * 
 * Besides the arguments present in TextureImage's constructors,
 * TextureCubeMap adds the additional parameters BufferData[] data and 
 * int side.  side represents the pixel dimension of one side of the
 * square image.  data is the array of all mipmaps for the image.  If
 * the data is null, all elements are null, or if it has a length of one,
 * then the texture is not mipmapped.
 * 
 * The constructors will additionally throw exceptions if side or the 
 * data array are not valid.  Here are the rules:
 * 1. side must be positive.
 * 2. If data isn't null, then all elements must be null, or all
 * 	  must not be null.
 * 3. A data array with non-null elements and a length > 1 is considered
 * 	  mipmapped.  All mipmaps must be present in this layer, based on
 *    the expected number from side.
 * 4. Every mipmap layer (including the 0th layer) must be sized correctly
 * 	  based on the layer's dimension, format and type.
 * 5. All non-null BufferData's must have the same data type as passed 
 *    into the constructor.
 * 
 * TextureCubeMap provides methods to mark regions of the texture's mipmap
 * levels as dirty.  These commands will always be clamped to the
 * valid regions of the texture.  They will also update the dirty descriptor
 * regardless of the null status of any buffer data's, or the data's
 * arrays.  It is the Renderer's job to make sure that null data's or arrays
 * are treated correctly regardless of what the dirty descriptor declares.
 * 
 * @author Michael Ludwig
 * 
 */
public class TextureCubeMap extends TextureImage {
	public static final int PX = 0;
	public static final int PY = 1;
	public static final int PZ = 2;
	public static final int NX = 3;
	public static final int NY = 4;
	public static final int NZ = 5;
	
	/** The dirty descriptor class that is used by TextureCubeMap.  Calls to
	 * getDirtyDescriptor() for texture cubemaps's will return objects of this class. */
	public static class TextureCubeMapDirtyDescriptor extends TextureDirtyDescriptor {
		private MipmapDirtyRegion[] drPX, drPY, drPZ, drNX, drNY, drNZ;
		
		private MipmapDirtyRegion[] getRegion(int face) {
			switch(face) {
			case PX: return this.drPX;
			case PY: return this.drPY;
			case PZ: return this.drPZ;
			case NX: return this.drNX;
			case NY: return this.drNY;
			case NZ: return this.drNZ;
			default: return null;
			}
		}
		
		private void setRegion(int face, MipmapDirtyRegion[] region) {
			switch(face) {
			case PX: this.drPX = region; break;
			case PY: this.drPY = region; break;
			case PZ: this.drPZ = region; break;
			case NX: this.drNX = region; break;
			case NY: this.drNY = region; break;
			case NZ: this.drNZ = region; break;
			}
		}
		
		/** True if there is a non-null MipmapDirtyRegion
		 * for the associated mipmap level. If face or level
		 * are invalid, then false is returned. */
		public boolean isDataDirty(int face, int level) { 
			MipmapDirtyRegion[] dirtyRegions = this.getRegion(face);
			if (dirtyRegions == null || level < 0 || level >= dirtyRegions.length)
				return false;
			return dirtyRegions[level] != null;
		}
		
		/** Get the MipmapDirtyRegion for the given mipmap level.
		 * If face or level are invalid, or if the face and level aren't dirty,
		 * then null is returned.
		 * 
		 * The returned region will be constrained to be in the dimensions
		 * of the mipmap level. */
		public MipmapDirtyRegion getDirtyRegion(int face, int level) {
			MipmapDirtyRegion[] dirtyRegions = this.getRegion(face);
			if (dirtyRegions == null || level < 0 || level >= dirtyRegions.length)
				return null;
			return dirtyRegions[level];
		}
		
		/** Return true if at least one mipmap region is not null. 
		 * If face isn't one of PX, PY, PZ, NX, NY, or NZ returns false. */
		public boolean areMipmapsDirty(int face) {
			return this.getRegion(face) != null;
		}
		
		/** Return true if any mipmap region of any face of the cubemap is dirty. */
		public boolean areMipmapsDirty() {
			return this.drPX != null || this.drPY != null || this.drPZ != null || this.drNX != null || this.drNY != null || this.drNZ != null;
		}
		
		@Override
		protected void clearDescriptor() {
			super.clearDescriptor();
			this.drPX = null; this.drPY = null; this.drPZ = null;
			this.drNX = null; this.drNY = null; this.drNZ = null;
		}
	}
	
	private BufferData[] px, py, pz, nx, ny, nz;
	private int side;
	private int numMipmaps;
	
	/** Creates a texture image with the given format and type, default other values.
	 * Fails like super(format, type), or if the dimensions and data array are invalid. */
	public TextureCubeMap(BufferData[] px, BufferData[] py, BufferData[] pz, BufferData[] nx, BufferData[] ny, BufferData[] nz, int side, TextureFormat format, DataType type) throws NullPointerException, IllegalArgumentException {
		super(format, type);
		this.setData(px, py, pz, nx, ny, nz, side);
	}
	
	/** Creates a texture image with the given type, format and filter, default other values.
	 * Fails like super(format, type, filter), or if the dimensions and data array are invalid.*/
	public TextureCubeMap(BufferData[] px, BufferData[] py, BufferData[] pz, BufferData[] nx, BufferData[] ny, BufferData[] nz, int side, TextureFormat format,  DataType type, Filter filter) throws StateException {
		super(format, type, filter);
		this.setData(px, py, pz, nx, ny, nz, side);
	}
	
	/** Create a texture image with the given format, type, filter, wrap mode for all coordinates,
	 * depth mode and test, and depth comparison is disabled. 
	 * Fails like super(format, type, filter, wrap, mode, test), or if the dimensions and data array are invalid. */
	public TextureCubeMap(BufferData[] px, BufferData[] py, BufferData[] pz, BufferData[] nx, BufferData[] ny, BufferData[] nz, int side, TextureFormat format, DataType type, Filter filter, TextureWrap wrapAll, DepthMode depthMode, PixelTest depthTest) throws StateException {
		super(format, type, filter, wrapAll, depthMode, depthTest);
		this.setData(px, py, pz, nx, ny, nz, side);
	}
	
	/* Internal method used to validate the BufferData[] and dimensions. */
	private void setData(BufferData[] px, BufferData[] py, BufferData[] pz, BufferData[] nx, BufferData[] ny, BufferData[] nz, int side) throws IllegalArgumentException {
		TextureFormat format = this.getFormat();
		DataType type = this.getType();
		
		if (format == TextureFormat.DEPTH) 
			throw new IllegalArgumentException("CubeMaps do not support the DEPTH format");
		
		px = validateFace(px, side, format, type);
		py = validateFace(py, side, format, type);
		pz = validateFace(pz, side, format, type);
		nx = validateFace(nx, side, format, type);
		ny = validateFace(ny, side, format, type);
		nz = validateFace(nz, side, format, type);
		
		int numMipmaps;
		if (px == null) {
			if (py != null || pz != null || nx != null || ny != null || nz != null)
				throw new IllegalArgumentException("All faces of cube must be considered headless, all must be complete");
			numMipmaps = 1;
		} else {
			if (py == null || pz == null || nx == null || ny == null || nz == null)
				throw new IllegalArgumentException("All faces of cube must be considered headless, all must be complete");
			numMipmaps = px.length;
		}
		
		// everything is valid up to this point, so we can update our values
		this.side = side;
		this.numMipmaps = numMipmaps;
		this.px = px;
		this.py = py;
		this.pz = pz;
		this.nx = nx;
		this.ny = ny;
		this.nz = nz;
		
		this.markDirty();
	}
	
	/* Do validation of a single face. */
	private static BufferData[] validateFace(BufferData[] data, int side, TextureFormat format, DataType type) {
		int numMipmaps = TextureImage.calculateMipmapCount(side, side, 1); // expected mipmap count, if data.length > 1
		
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
			int s = side;
			for (int i = 0; i < realData.length; i++) {
				if (realData[i].getType() != type)
					throw new IllegalArgumentException("BufferData doesn't have a matching type for the texture, expected: " + type + ", but was: " + realData[i].getType());
				if (realData[i].getCapacity() != format.getBufferSize(s, s, 1))
					throw new IllegalArgumentException("Buffer at mipmap level: " + i + " is does not have the correct size, expected: " + format.getBufferSize(s, s, 1) + ", but was: " + realData[i].getCapacity());
				s = Math.max(1, s >> 1);
			}
			
			numMipmaps = realData.length;
		} else {
			if (format.isCompressed())
				throw new StateException("Headless TextureCubeMap cannot have a client compressed texture: " + format);
			numMipmaps = 1;
		}
		
		return realData;
	}
	
	/** Mark the given mipmap level region dirty.  If the level
	 * is outside of [0, numMipmaps - 1], this command does nothing.
	 * Does nothing if face isn't PX, PY, PZ, NX, NY, or NZ. 
	 * 
	 * The x, y offsets and width and height will be clamped to be
	 * within the valid region of the given mipmap. */
	public void markDirty(int x, int y, int width, int height, int face, int level) {
		if (level < 0 || level >= (this.numMipmaps - 1))
			return; // invalid level option
		if (face < 0 || face > 5)
			return; // invalid face
		
		TextureCubeMapDirtyDescriptor dirty = (TextureCubeMapDirtyDescriptor) this.getDirtyDescriptor();
		MipmapDirtyRegion[] dirtyRegions = dirty.getRegion(face);
		if (dirtyRegions == null || dirtyRegions.length <= level) {
			MipmapDirtyRegion[] temp = new MipmapDirtyRegion[level + 1];
			if (dirtyRegions != null)
				System.arraycopy(dirtyRegions, 0, temp, 0, dirtyRegions.length);
			dirtyRegions = temp;
			dirty.setRegion(face, dirtyRegions);
		}
		
		int levelSide = this.getWidth(level);
		MipmapDirtyRegion r = dirtyRegions[level];
		if (r == null) {
			r = new MipmapDirtyRegion(x, y, 0, width, height, 0, 
									  levelSide, levelSide, 0);
			dirtyRegions[level] = r;
		} else
			r.merge(x, y, 0, width, height, 0,
					levelSide, levelSide, 0);
	}
	
	/** Mark the entire mipmap level dirty.  Does nothing if
	 * level isn't within [0, numMipmaps - 1] or if
	 * face isn't PX, PY, PZ, NX, NY, or NZ. */
	public void markDirty(int face, int level) {
		this.markDirty(0, 0, this.getWidth(level), this.getHeight(level), face, level);
	}
	
	/** Mark a whole texture face as dirty. Does nothing if
	 * face isn't PX, PY, PZ, NX, NY, or NZ. */
	public void markDirty(int face) {
		if (face < 0 || face > 5)
			return; // don't want to create the array now
		
		TextureCubeMapDirtyDescriptor dirty = (TextureCubeMapDirtyDescriptor) this.getDirtyDescriptor();
		// create the whole array now for efficiency.  It's okay to ignore old array because
		// the new regions will take up the whole level.
		dirty.setRegion(face, new MipmapDirtyRegion[this.numMipmaps]);
		for (int i = 0; i < this.numMipmaps; i++)
			this.markDirty(face, i);
	}
	
	/** Mark the entire cubemap as dirty. */
	public void markDirty() {
		for (int i = 0; i < 6; i++)
			this.markDirty(i);
	}
	
	@Override
	public int getDepth(int level) {
		if (level < 0 || level >= this.numMipmaps)
			return -1;
		return 1;
	}

	@Override
	public int getHeight(int level) {
		return this.getWidth(level);
	}

	@Override
	public int getNumMipmaps() {
		return this.numMipmaps;
	}

	@Override
	public int getWidth(int level) {
		if (level < 0 || level >= this.numMipmaps)
			return -1;
		return Math.max(1, this.side >> level);
	}
	
	@Override
	public final TextureTarget getTarget() {
		return TextureTarget.T_CUBEMAP;
	}

	/** Get the buffer for the given level.  Fails if the
	 * level is < 0 or >= the number of mipmaps. Also fails
	 * if face isn't one of PX, PY, PZ, NX, NY, or NZ (0 - 5).
	 * 
	 * Returns null if the data isn't in client memory (most likely
	 * in the graphics card). */
	public BufferData getData(int face, int level) throws IllegalArgumentException {
		if (level < 0 || level >= this.numMipmaps)
			throw new IllegalArgumentException("Buffer data doesn't exist beyond mipmap levels, illegal level: " + level);
		if (this.px == null)
			return null; // all we can return at this point
		
		switch(face) {
		case PX: return this.px[level];
		case PY: return this.py[level];
		case PZ: return this.pz[level];
		case NX: return this.nx[level];
		case NY: return this.ny[level];
		case NZ: return this.nz[level];
		default:
			throw new IllegalArgumentException("Invalid face option: " + face);
		}
	}

	@Override
	protected TextureDirtyDescriptor createTextureDirtyDescriptor() {
		return new TextureCubeMapDirtyDescriptor();
	}
}