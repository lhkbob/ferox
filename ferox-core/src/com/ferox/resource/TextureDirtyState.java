package com.ferox.resource;

import java.util.Arrays;


/**
 * The dirty descriptor class that is used by Texture1D, Texture2D and
 * Texture3D. Calls to getDirtyState() for those TextureImage subclasses
 * will return objects of this class.
 */
public class TextureDirtyState implements DirtyState<TextureDirtyState> {
	private final ImageRegion[] mipmaps;
	private boolean parameters;

	/**
	 * Create a new TextureDirtyState that can hold up to the given number of
	 * dirty mipmaps, and parameters as the dirty boolean for texture
	 * parameters. Initially no mipmaps are marked as dirty.
	 * 
	 * @param mipmaps The dirty mipmap regions
	 * @param parameters The dirtiness of texture parameters
	 * @throws IllegalArgumentException if numMipmaps < 1
	 */
	public TextureDirtyState(int numMipmaps, boolean parameters) {
		if (numMipmaps < 1)
			throw new IllegalArgumentException("numMipmaps must be at least 1: " + numMipmaps);
		this.mipmaps = new ImageRegion[numMipmaps];
		this.parameters = parameters;
	}
	
	private TextureDirtyState(ImageRegion[] regions, boolean params) {
		mipmaps = regions;
		parameters = params;
	}

	/**
	 * Get the ImageRegion for the TextureImage for the given mipmap level. The
	 * dimensions in use by the returned ImageRegion depends on the number of
	 * dimensions used by the TextureImage (e.g. 1 for Texture1D). If null is
	 * returned, then the data for that mipmap hasn't been flagged as dirty. The
	 * returned region will be constrained to be in the dimensions of the
	 * texture.
	 * 
	 * @param level The mipmap level to query
	 * @return ImageRegion for this dirty state's texture
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
	
	public TextureDirtyState setTextureParametersDirty() {
		if (parameters)
			return this;
		else
			return new TextureDirtyState(mipmaps, true);
	}
	
	public TextureDirtyState updateMipmap(int mipmap, ImageRegion region) {
		if (mipmap < 0 || mipmap >= mipmaps.length)
			throw new IllegalArgumentException("Invalid mipmap level: " + mipmap);
		
		ImageRegion[] mips = Arrays.copyOf(mipmaps, mipmaps.length);
		if (mips[mipmap] == null)
			mips[mipmap] = region;
		else
			mips[mipmap] = mips[mipmap].merge(region);
		
		return new TextureDirtyState(mips, parameters);
	}

	@Override
	public TextureDirtyState merge(TextureDirtyState state) {
		if (state == null)
			return this;
		
		int numMips = Math.max(mipmaps.length, state.mipmaps.length);
		ImageRegion[] mips = new ImageRegion[numMips];
		
		for (int i = 0; i < numMips; i++) {
			if (i < mipmaps.length && i < state.mipmaps.length) {
				if (mipmaps[i] != null)
					mips[i] = mipmaps[i].merge(state.mipmaps[i]);
				else
					mips[i] = state.mipmaps[i]; // will be null, or has no need to merge with this region
			} else if (i < mipmaps.length)
				mips[i] = mipmaps[i];
			else if (i < state.mipmaps.length)
				mips[i] = state.mipmaps[i];
		}
		
		return new TextureDirtyState(mips, parameters || state.parameters);
	}
}
