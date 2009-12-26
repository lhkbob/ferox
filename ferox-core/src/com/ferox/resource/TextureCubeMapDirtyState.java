package com.ferox.resource;

import java.util.Arrays;

/**
 * The DirtyState subclass that is used by TextureCubeMap. Calls to
 * getDirtyState() on instances of TextureCubeMap will return objects of this
 * class.
 * 
 * @author Michael Ludwig
 */
public class TextureCubeMapDirtyState implements DirtyState<TextureCubeMapDirtyState> {
	private final ImageRegion[] drPX, drPY, drPZ, drNX, drNY, drNZ;
	private boolean parameters;
	
	/**
	 * Create a new TextureCubeMapDirtyState that can hold up to the given number of
	 * dirty mipmaps, and <tt>parameters</tt> as the dirty boolean for texture
	 * parameters. Initially no mipmaps are marked as dirty.
	 * 
	 * @param numMipmaps The number of mipmaps available in the texture
	 * @param parameters The dirtiness of texture parameters
	 * @throws IllegalArgumentException if numMipmaps < 1
	 */
	public TextureCubeMapDirtyState(int numMipmaps, boolean parameters) {
		if (numMipmaps < 1)
			throw new IllegalArgumentException("The number of mipmaps must be at least 1: " + numMipmaps);
		
		drPX = new ImageRegion[numMipmaps];
		drPY = new ImageRegion[numMipmaps];
		drPZ = new ImageRegion[numMipmaps];
		drNX = new ImageRegion[numMipmaps];
		drNY = new ImageRegion[numMipmaps];
		drNZ = new ImageRegion[numMipmaps];
		
		this.parameters = parameters;
	}

	private TextureCubeMapDirtyState(ImageRegion[] px, ImageRegion[] py, ImageRegion[] pz, 
					 				 ImageRegion[] nx, ImageRegion[] ny, ImageRegion[] nz, boolean parameters) {
		drPX = px;
		drPY = py;
		drPZ = pz;
		drNX = nx;
		drNY = ny;
		drNZ = nz;
	
		this.parameters = parameters;
	}

	/**
	 * <p>
	 * Get the dirty ImageRegion for the TextureCubeMap for the given cube face
	 * and mipmap level. If null is returned, then the data for that mipmap and
	 * face hasn't been flagged as dirty.
	 * </p>
	 * <p>
	 * face must be one of PX, PY, PZ, NX, NY, NZ as defined in TextureCubeMap.
	 * </p>
	 * 
	 * @param face The cube face to fetch from
	 * @param level The mipmap level that's retrieved from <tt>face</tt>
	 * @return ImageRegion for this dirty state's texture
	 * @throws IllegalArgumentException if level < 0 or level >= # mipmaps, or
	 *             if face is invalid
	 */
	public ImageRegion getDirtyMipmap(int face, int level) {
		ImageRegion[] dirtyRegions = getMipmaps(face);
		if (level < 0 || level >= dirtyRegions.length)
			throw new IllegalArgumentException("Invalid mipmap level: " + level);
		return dirtyRegions[level];
	}
	
	/**
	 * Return whether or not the texture parameters of a TextureCubeMap have
	 * been modified.
	 * 
	 * @return True if parameters must be updated
	 */
	public boolean getTextureParametersDirty() {
		return parameters;
	}

	/**
	 * If this TextureCubeMapDirtyState's {@link #getTextureParametersDirty()}
	 * returns true, then this instance is returned. Otherwise, a new
	 * TextureCubeMapDirtyState is created that has the same dirty mipmaps, but
	 * the texture parameters are flagged as dirty.
	 * 
	 * @return A TextureCubeMapDirtyState with equivalent dirty image data, but
	 *         with {@link #getTextureParametersDirty()} returning true
	 */
	public TextureCubeMapDirtyState setTextureParametersDirty() {
		if (parameters)
			return this;
		else
			return new TextureCubeMapDirtyState(drPX, drPY, drPZ, drNX, drNY, drNZ, true);
	}

	/**
	 * Create and return a new TextureCubeDirtyState that has the given
	 * ImageRegion merged into any previous dirty ImageRegion at the given
	 * mipmap level and cube-face. This does not affect any other mipmap levels
	 * or faces. It is assumed that the ImageRegion is constrained to the valid
	 * dimensions of the associated texture, for the given mipmap level.
	 * 
	 * @param face The cube face that selects which mipmaps can be modified
	 * @param level The mipmap that will be updated, based on face
	 * @param region The new ImageRegion to merge in, using
	 *            {@link ImageRegion#merge(ImageRegion)}
	 * @return A new TextureCubeMapDirtyState that's equivalent to this dirty
	 *         state, except that it includes the given region
	 * @throws IllegalArgumentException if mipmap < 0 or mipmap >= # mipmaps, or
	 *             if face is invalid
	 */
	public TextureCubeMapDirtyState updateMipmap(int face, int level, ImageRegion region) {
		ImageRegion[] faceMips = getMipmaps(face);
		if (level < 0 || level >= faceMips.length)
			throw new IllegalArgumentException("Invalid mipmap level: " + level);
		
		ImageRegion[] mips = Arrays.copyOf(faceMips, faceMips.length);
		if (mips[level] == null)
			mips[level] = region;
		else
			mips[level] = mips[level].merge(region);
		
		switch(face) {
		case TextureCubeMap.PX:
			return new TextureCubeMapDirtyState(mips, drPY, drPZ, drNX, drNY, drNZ, parameters);
		case TextureCubeMap.PY:
			return new TextureCubeMapDirtyState(drPX, mips, drPZ, drNX, drNY, drNZ, parameters);
		case TextureCubeMap.PZ:
			return new TextureCubeMapDirtyState(drPX, drPY, mips, drNX, drNY, drNZ, parameters);
		case TextureCubeMap.NX:
			return new TextureCubeMapDirtyState(drPX, drPY, drPZ, mips, drNY, drNZ, parameters);
		case TextureCubeMap.NY:
			return new TextureCubeMapDirtyState(drPX, drPY, drPZ, drNX, mips, drNZ, parameters);
		case TextureCubeMap.NZ:
			return new TextureCubeMapDirtyState(drPX, drPY, drPZ, drNX, drNY, mips, parameters);
		default:
			return null; // won't happen
		}
	}
	
	public TextureCubeMapDirtyState merge(TextureCubeMapDirtyState state) {
		if (state == null)
			return this;
		
		ImageRegion[] px = merge(drPX, state.drPX);
		ImageRegion[] py = merge(drPY, state.drPY);
		ImageRegion[] pz = merge(drPZ, state.drPZ);
		ImageRegion[] nx = merge(drNX, state.drNX);
		ImageRegion[] ny = merge(drNY, state.drNY);
		ImageRegion[] nz = merge(drNZ, state.drNZ);
		
		return new TextureCubeMapDirtyState(px, py, pz, nx, ny, nz, parameters || state.parameters);
	}
	
	private ImageRegion[] merge(ImageRegion[] r1, ImageRegion[] r2) {
		int numMips = Math.max(r1.length, r2.length);
		ImageRegion[] mips = new ImageRegion[numMips];
		
		for (int i = 0; i < numMips; i++) {
			if (i < r1.length && i < r2.length) {
				if (r1[i] != null)
					mips[i] = r1[i].merge(r2[i]);
				else
					mips[i] = r2[i]; // will be null, or has no need to merge with this region
			} else if (i < r1.length)
				mips[i] = r1[i];
			else if (i < r2.length)
				mips[i] = r2[i];
		}
		
		return mips;
	}
	
	private ImageRegion[] getMipmaps(int face) {
		switch (face) {
		case TextureCubeMap.PX:
			return drPX;
		case TextureCubeMap.PY:
			return drPY;
		case TextureCubeMap.PZ:
			return drPZ;
		case TextureCubeMap.NX:
			return drNX;
		case TextureCubeMap.NY:
			return drNY;
		case TextureCubeMap.NZ:
			return drNZ;
		default:
			throw new IllegalArgumentException("Face value is invalid: " + face);
		}
	}
}