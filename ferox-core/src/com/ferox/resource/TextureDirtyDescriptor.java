package com.ferox.resource;

/**
 * The dirty descriptor class that is used by Texture1D, Texture2D and
 * Texture3D. Calls to getDirtyDescriptor() for those TextureImage subclasses
 * will return objects of this class.
 */
public class TextureDirtyDescriptor {
	private final ImageRegion[] mipmaps;
	private boolean parameters;

	/**
	 * Create a new TextureDirtyDescriptor configured for the given number of
	 * mipmaps, with nothing marked as dirty.
	 * 
	 * @param numMipmaps The number of mipmaps
	 * @throws IllegalArgumentException if numMipmaps < 1
	 */
	public TextureDirtyDescriptor(int numMipmaps) {
		if (numMipmaps < 1)
			throw new IllegalArgumentException("Must have at least one mipmap: " + numMipmaps);
		mipmaps = new ImageRegion[numMipmaps];
		parameters = false;
	}

	/**
	 * Get the ImageRegion for the TextureImage. The dimensions in use by the
	 * returned ImageRegion depends on the number of dimensions used by the
	 * TextureImage (e.g. 1 for Texture1D). If null is returned, then the data
	 * for that mipmap hasn't been flagged as dirty. The returned region will be
	 * constrained to be in the dimensions of the texture.
	 * 
	 * @return ImageRegion for this dirty descriptor's texture
	 * @throws IllegalArgumentException if level < 0 or level >= # mipmaps
	 */
	public ImageRegion getDirtyMipmap(int level) {
		if (level < 0 || level >= mipmaps.length)
			throw new IllegalArgumentException("Invalid mipmap level: " + level);
		return mipmaps[level];
	}
	
	/**
	 * Return whether or not the texture parameters of a TextureImage have
	 * been modified.
	 * 
	 * @return True if parameters must be updated
	 */
	public boolean getTextureParametersDirty() {
		return parameters;
	}
	
	void setDirtyMipmap(int level, ImageRegion mipmap) {
		mipmaps[level] = mipmap;
	}
	
	void setParametersDirty() {
		parameters = true;
	}
}
