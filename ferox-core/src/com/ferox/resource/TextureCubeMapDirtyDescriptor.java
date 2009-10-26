package com.ferox.resource;


/**
 * The dirty descriptor class that is used by TextureCubeMap. Calls to
 * getDirtyDescriptor() for texture cubemaps's will return objects of this
 * class.
 */
public class TextureCubeMapDirtyDescriptor {
	private final ImageRegion[] drPX, drPY, drPZ, drNX, drNY, drNZ;
	private boolean parameters;
	
	public TextureCubeMapDirtyDescriptor(int numMipmaps) {
		drPX = new ImageRegion[numMipmaps];
		drPY = new ImageRegion[numMipmaps];
		drPZ = new ImageRegion[numMipmaps];
		drNX = new ImageRegion[numMipmaps];
		drNY = new ImageRegion[numMipmaps];
		drNZ = new ImageRegion[numMipmaps];
		
		parameters = false;
	}

	/**
	 * <p>
	 * Get the ImageRegion for the TextureCubeMap. If null is returned, then the
	 * data for that mipmap hasn't been flagged as dirty. The returned region
	 * will be constrained to be in the dimensions of the texture.
	 * </p>
	 * <p>
	 * face must be one of PX, PY, PZ, NX, NY, NZ as defined in TextureCubeMap.
	 * </p>
	 * 
	 * @return ImageRegion for this dirty descriptor's texture
	 * @throws IllegalArgumentException if level < 0 or level >= # mipmaps, or
	 *             iff face is invalid
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
	
	void setParametersDirty() {
		parameters = true;
	}
	
	void setDirtyMipmap(int face, int level, ImageRegion mipmap) {
		ImageRegion[] mipmaps = getMipmaps(face);
		mipmaps[level] = mipmap;
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