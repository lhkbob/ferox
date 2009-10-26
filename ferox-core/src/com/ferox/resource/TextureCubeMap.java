package com.ferox.resource;

import com.ferox.resource.BufferData.DataType;

/**
 * <p>
 * Represents a square two-dimensional image. The texture data is accessed by
 * normalized texture coordinates. The dimensions are not required to be
 * power-of-two textures, but a Framework will likely re-scale the image if they
 * aren't supported.
 * <p>
 * <p>
 * Even if they are supported, power-of-two textures likely have better
 * performance.
 * <p>
 * <p>
 * Besides the arguments present in TextureImage's constructors, TextureCubeMap
 * adds the additional parameters BufferData[] data and int side. side
 * represents the pixel dimension of one side of the square image. data is the
 * array of all mipmaps for the image. If the data is null, all elements are
 * null, or if it has a length of one, then the texture is not mipmapped.
 * <p>
 * <p>
 * The constructors will additionally throw exceptions if side or the data array
 * are not valid. Here are the rules:
 * <ol>
 * <li>Side must be positive.</li>
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
 * <p>
 * <p>
 * TextureCubeMap provides methods to mark regions of the texture's mipmap
 * levels as dirty. These commands will always be clamped to the valid regions
 * of the texture. They will also update the dirty descriptor regardless of the
 * null status of any buffer data's, or the data's arrays. It is the Framework's
 * job to make sure that null data's or arrays are treated correctly regardless
 * of what the dirty descriptor declares.
 * <p>
 * 
 * @author Michael Ludwig
 */
public class TextureCubeMap extends TextureImage {
	/** Constant specifying the positive x cube map face. */
	public static final int PX = 0;
	/** Constant specifying the positive y cube map face. */
	public static final int PY = 1;
	/** Constant specifying the positive z cube map face. */
	public static final int PZ = 2;
	/** Constant specifying the negative x cube map face. */
	public static final int NX = 3;
	/** Constant specifying the negative y cube map face. */
	public static final int NY = 4;
	/** Constant specifying the negative z cube map face. */
	public static final int NZ = 5;

	private BufferData[] px, py, pz, nx, ny, nz;
	private int side;
	private int numMipmaps;
	
	private TextureCubeMapDirtyDescriptor dirty;

	/**
	 * Creates a texture image with the given format and type, default other
	 * values.
	 * 
	 * @param px Mipmaps for the px face
	 * @param py Mipmaps for the py face
	 * @param pz Mipmaps for the pz face
	 * @param nx Mipmaps for the nx face
	 * @param ny Mipmaps for the ny face
	 * @param nz Mipmaps for the nz face
	 * @param side Side length of a cube face
	 * @param format
	 * @param type
	 * @throws NullPointerException if format or type are null
	 * @throws IllegalArgumentException if the cube faces, side length, format
	 *             and type would create an invalid cube map
	 */
	public TextureCubeMap(BufferData[] px, BufferData[] py, BufferData[] pz, 
						  BufferData[] nx, BufferData[] ny, BufferData[] nz, 
						  int side, TextureFormat format, DataType type) {
		super(format, type);
		setData(px, py, pz, nx, ny, nz, side);
	}

	/**
	 * Create a texture image with the given format, type, filter, wrap mode for
	 * all coordinates, depth mode and test, and depth comparison is disabled.
	 * 
	 * @param px Mipmaps for the px face
	 * @param py Mipmaps for the py face
	 * @param pz Mipmaps for the pz face
	 * @param nx Mipmaps for the nx face
	 * @param ny Mipmaps for the ny face
	 * @param nz Mipmaps for the nz face
	 * @param side Side length of a cube face
	 * @param format
	 * @param type
	 * @param filter
	 * @param wrapAll
	 * @throws NullPointerException if format or type are null
	 * @throws IllegalArgumentException if the cube faces, side length, format
	 *             and type would create an invalid cube map
	 */
	public TextureCubeMap(BufferData[] px, BufferData[] py, BufferData[] pz, 
						  BufferData[] nx, BufferData[] ny, BufferData[] nz, 
						  int side, TextureFormat format, DataType type, 
						  Filter filter, TextureWrap wrapAll) {
		super(format, type, filter, wrapAll, null, null);
		setData(px, py, pz, nx, ny, nz, side);
	}

	/* Internal method used to validate the BufferData[] and dimensions. */
	private void setData(BufferData[] px, BufferData[] py, BufferData[] pz, 
						 BufferData[] nx, BufferData[] ny, BufferData[] nz, int side) {
		TextureFormat format = getFormat();
		DataType type = getType();

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
	private static BufferData[] validateFace(BufferData[] data, int side, 
											 TextureFormat format, DataType type) {
		// expected mipmap count if data.length > 1
		int numMipmaps = TextureImage.calculateMipmapCount(side, side, 1);

		BufferData[] realData = null;
		if (data != null) {
			if (data.length != 1 && data.length != numMipmaps)
				throw new IllegalArgumentException("If more than one BufferData is given, must provide all mipmap levels");
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
				System.arraycopy(data, 0, realData, 0, Math.min(data.length, realData.length));
			} else
				throw new IllegalArgumentException("Cannot pass in an array with some values null.  Array length: " 
												   + data.length + ", but has only " + nonNullCount + " non-null buffers.");
		}

		if (realData != null) {
			int s = side;
			for (int i = 0; i < realData.length; i++) {
				if (realData[i].getType() != type)
					throw new IllegalArgumentException("BufferData doesn't have a matching type for the texture, expected: " 
													   + type + ", but was: " + realData[i].getType());
				if (realData[i].getCapacity() != format.getBufferSize(s, s, 1))
					throw new IllegalArgumentException("Buffer at mipmap level: " + i + " is does not have the correct size, expected: " 
													   + format.getBufferSize(s, s, 1) + ", but was: " + realData[i].getCapacity());
				s = Math.max(1, s >> 1);
			}

			numMipmaps = realData.length;
		} else {
			if (format.isCompressed())
				throw new IllegalArgumentException("Headless TextureCubeMap cannot have a client compressed texture: " + format);
			numMipmaps = 1;
		}

		return realData;
	}

	/**
	 * Mark the given mipmap level region dirty. If the level is outside of [0,
	 * numMipmaps - 1], this command does nothing. Does nothing if face isn't
	 * PX, PY, PZ, NX, NY, or NZ. The x, y offsets and width and height will be
	 * clamped to be within the valid region of the given mipmap.
	 * 
	 * @param x X offset of region for face and level
	 * @param y Y offset of region for face and level
	 * @param width Width of the dirty region
	 * @param height Height of the dirty region
	 * @param face Cube face affected, one of PX, PY, PZ, NX, NY, NZ
	 * @param level Mipmap level of the cube face
	 */
	public void markDirty(int x, int y, int width, int height, int face, int level) {
		if (level < 0 || level >= (numMipmaps - 1))
			return; // invalid level option
		if (face < 0 || face > 5)
			return; // invalid face

		if (dirty == null)
			dirty = new TextureCubeMapDirtyDescriptor(numMipmaps);

		int levelSide = getWidth(level);
		ImageRegion r = dirty.getDirtyMipmap(face, level);
		
		if (r == null)
			r = new ImageRegion(x, y, 0, width, height, 0, levelSide, levelSide, 0);
		else
			r = r.merge(x, y, 0, width, height, 0);
		dirty.setDirtyMipmap(face, level, r);
	}

	/**
	 * Mark the entire mipmap level dirty. Does nothing if level isn't within
	 * [0, numMipmaps - 1] or if face isn't PX, PY, PZ, NX, NY, or NZ.
	 * 
	 * @param face The cube face that's marked dirty
	 * @param level The mipmap level of face that becomes dirty
	 */
	public void markDirty(int face, int level) {
		this.markDirty(0, 0, getWidth(level), getHeight(level), face, level);
	}

	/**
	 * Mark all of the image data as dirty for a specific cube face. Does
	 * nothing if face isn't PX, PY, PZ, NX, NY, or NZ.
	 * 
	 * @param face The cube face that has all mipmaps marked dirty
	 */
	public void markDirty(int face) {
		if (face < 0 || face > 5)
			return; // don't want to create the array now

		for (int i = 0; i < numMipmaps; i++)
			this.markDirty(face, i);
	}

	/** Mark the entire TextureCubeMaps' image data as dirty. */
	public void markDirty() {
		for (int i = 0; i < 6; i++)
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
		return getWidth(level);
	}

	@Override
	public int getNumMipmaps() {
		return numMipmaps;
	}

	@Override
	public int getWidth(int level) {
		if (level < 0 || level >= numMipmaps)
			return -1;
		return Math.max(1, side >> level);
	}

	@Override
	public final TextureTarget getTarget() {
		return TextureTarget.T_CUBEMAP;
	}

	/**
	 * Get the buffer for the given level. Fails if the level is < 0 or >= the
	 * number of mipmaps. Also fails if face isn't one of PX, PY, PZ, NX, NY, or
	 * NZ (0 - 5). Returns null if the data isn't in client memory (most likely
	 * in the graphics card).
	 * 
	 * @param face Cube face whose buffer data is retreived
	 * @param level Mipmap level of face
	 * @return BufferData associated with face's mipmap level
	 * @throws IllegalArgumentException if face or level are invalid
	 */
	public BufferData getData(int face, int level) {
		if (level < 0 || level >= numMipmaps)
			throw new IllegalArgumentException("Buffer data doesn't exist beyond mipmap levels, illegal level: " + level);
		if (px == null)
			return null; // all we can return at this point

		switch (face) {
		case PX:
			return px[level];
		case PY:
			return py[level];
		case PZ:
			return pz[level];
		case NX:
			return nx[level];
		case NY:
			return ny[level];
		case NZ:
			return nz[level];
		default:
			throw new IllegalArgumentException("Invalid face option: " + face);
		}
	}

	@Override
	public void clearDirtyDescriptor() {
		dirty = null;
	}

	@Override
	public TextureCubeMapDirtyDescriptor getDirtyDescriptor() {
		return dirty;
	}
	
	@Override
	protected void setTextureParametersDirty() {
		if (dirty == null)
			dirty = new TextureCubeMapDirtyDescriptor(numMipmaps);
		dirty.setParametersDirty();
	}
}