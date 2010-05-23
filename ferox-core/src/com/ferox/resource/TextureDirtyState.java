package com.ferox.resource;

import java.util.Arrays;

/**
 * The DirtyState subclass that is used by Texture.
 * 
 * @author Michael Ludwig
 */
public class TextureDirtyState implements DirtyState<TextureDirtyState> {
	private final ImageRegion[][] mipmaps;
	private boolean parameters;

    /**
     * Create a new TextureDirtyState that can hold up to the given number of
     * dirty mipmaps, and <tt>parameters</tt> as the dirty boolean for texture
     * parameters. Initially no mipmaps are marked as dirty.
     * 
     * @param numLayers The number of layers within the texture (e.g. 1 or 6
     *            usually)
     * @param numMipmaps The number of mipmaps available in the texture
     * @param parameters The dirtiness of texture parameters
     * @throws IllegalArgumentException if numMipmaps < 1 or numLayers < 1
     */
	public TextureDirtyState(int numLayers, int numMipmaps, boolean parameters) {
	    if (numLayers < 1)
	        throw new IllegalArgumentException("numLayers must be at least 1: " + numLayers);
		if (numMipmaps < 1)
			throw new IllegalArgumentException("numMipmaps must be at least 1: " + numMipmaps);
		this.mipmaps = new ImageRegion[numLayers][numMipmaps];
		this.parameters = parameters;
	}
	
	private TextureDirtyState(ImageRegion[][] regions, boolean params) {
		mipmaps = regions;
		parameters = params;
	}

    /**
     * Get the dirty ImageRegion for the Texture for the given mipmap level
     * within the specified layer. The dimensions in use by the returned
     * ImageRegion depend on the number of dimensions used by the Texture
     * (e.g. 1 for Texture1D). If null is returned, then the data for that
     * mipmap hasn't been flagged as dirty.
     * 
     * @param layer The layer of mipmaps to access
     * @param level The mipmap level to query
     * @return ImageRegion for this dirty state's texture
     * @throws IllegalArgumentException if level < 0 or level >= # mipmaps
     */
	public ImageRegion getDirtyMipmap(int layer, int level) {
	    if (layer < 0 || layer >= mipmaps.length)
	        throw new IllegalArgumentException("Invalid layer: " + layer);
		if (level < 0 || level >= mipmaps[layer].length)
			throw new IllegalArgumentException("Invalid mipmap level: " + level);
		return mipmaps[layer][level];
	}
	
	/**
	 * Return whether or not the texture parameters of a Texture have
	 * been modified.
	 * 
	 * @return True if parameters must be updated
	 */
	public boolean getTextureParametersDirty() {
		return parameters;
	}

    /**
     * If this TextureDirtyState's {@link #getTextureParametersDirty()} returns
     * true, then this instance is returned. Otherwise, a new TextureDirtyState
     * is created that has the same dirty image regions, but the texture
     * parameters are flagged as dirty.
     * 
     * @return A TextureDirtyState with equivalent dirty image data, but with
     *         {@link #getTextureParametersDirty()} returning true
     */
	public TextureDirtyState setTextureParametersDirty() {
		if (parameters)
			return this;
		else
			return new TextureDirtyState(mipmaps, true);
	}

	/**
	 * Create and return a new TextureDirtyState that has the given ImageRegion
	 * merged into any previous dirty ImageRegion at the given mipmap level and layer.
	 * This does not affect any other mipmap levels. It is assumed that the
	 * ImageRegion is constrained to the valid dimensions of the associated
	 * texture, for the given mipmap level.
	 * 
	 * @param mipmap The mipmap that will be updated
	 * @param region The new ImageRegion to merge in, using
	 *            {@link ImageRegion#merge(ImageRegion)}
	 * @return A new TextureDirtyState that's equivalent to this dirty state,
	 *         except that it includes the given region
	 * @throws IllegalArgumentException if mipmap < 0 or mipmap >= # mipmaps
	 */
	public TextureDirtyState updateMipmap(int layer, int mipmap, ImageRegion region) {
	    if (layer < 0 || layer >= mipmaps.length)
	        throw new IllegalArgumentException("Invalid layer: " + layer);
		if (mipmap < 0 || mipmap >= mipmaps[layer].length)
			throw new IllegalArgumentException("Invalid mipmap level: " + mipmap);
		
		ImageRegion[][] mips = new ImageRegion[mipmaps.length][mipmaps[0].length];
		for (int l = 0; l < mips.length; l++) {
		    for (int m = 0; m < mips[l].length; m++) {
		        if (l == layer && m == mipmap)
		            mips[l][m] = (mipmaps[l][m] == null ? region : mipmaps[l][m].merge(region));
		        else
		            mips[l][m] = mipmaps[l][m];
		    }
		}
		return new TextureDirtyState(mips, parameters);
	}

	@Override
	public TextureDirtyState merge(TextureDirtyState state) {
		if (state == null)
			return this;
		
		int numLayers = Math.max(mipmaps.length, state.mipmaps.length);
		int numMips = Math.max(mipmaps[0].length, state.mipmaps[0].length);
		ImageRegion[][] mips = new ImageRegion[numLayers][numMips];
		
		for (int l = 0; l < numLayers; l++) {
		    if (l < mipmaps.length && l < state.mipmaps.length) {
		        for (int m = 0; m < numMips; m++) {
		            if (m < mipmaps[l].length && m < state.mipmaps[l].length)
		                mips[l][m] = (mipmaps[l][m] != null ? mipmaps[l][m].merge(state.mipmaps[l][m]) : state.mipmaps[l][m]);
		            else if (m < mipmaps[l].length)
		                mips[l][m] = mipmaps[l][m];
		            else if (m < state.mipmaps[l].length)
		                mips[l][m] = state.mipmaps[l][m];
		        }
		    } else if (l < mipmaps.length)
		        mips[l] = Arrays.copyOf(mipmaps[l], numLayers);
		    else if (l < state.mipmaps.length)
		        mips[l] = Arrays.copyOf(state.mipmaps[l], numLayers);
		}
		
		return new TextureDirtyState(mips, parameters || state.parameters);
	}
}
