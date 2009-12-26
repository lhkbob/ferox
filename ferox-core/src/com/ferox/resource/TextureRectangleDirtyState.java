package com.ferox.resource;


/**
 * The DirtyState class that is used by TextureRectangle. Calls to
 * getDirtyState() for TextureRectangle's will return objects of this
 * class.
 */
public class TextureRectangleDirtyState implements DirtyState<TextureRectangleDirtyState> {
	private ImageRegion dirtyRegion;
	private boolean parameters;

	/**
	 * Create a new TextureRectangleDirtyState that marks the given ImageRegion
	 * as dirty, and the texture parameters as dirty based on
	 * <tt>parameters</tt>. It is assumed that the dirty region is constrained
	 * to the size of the TextureRectangle. If <tt>dirtyRegion</tt> is null,
	 * then the image data is not considered dirty.
	 * 
	 * @param dirtyRegion The dirty image region, or null
	 * @param parameters True if the texture parameters are dirty
	 */
	public TextureRectangleDirtyState(ImageRegion dirtyRegion, boolean parameters) {
		this.dirtyRegion = dirtyRegion;
		this.parameters = parameters;
	}
	
	/**
	 * Get the dirty ImageRegion of the TextureRectangle. If null is
	 * returned, then the data hasn't been flagged as dirty.
	 * 
	 * @return ImageRegion for this dirty state's texture
	 */
	public ImageRegion getDirtyRegion() {
		return dirtyRegion;
	}

	/**
	 * Return whether or not the texture parameters of a TextureRectangle have
	 * been modified.
	 * 
	 * @return True if parameters must be updated
	 */
	public boolean getTextureParametersDirty() {
		return parameters;
	}

	/**
	 * If {@link #getTextureParametersDirty()} returns true, returns this
	 * TextureRectangleDirtyState, otherwise returns a new dirty state that is a
	 * clone of this state, except that its texture parameters are flagged
	 * dirty.
	 * 
	 * @return A TextureRectangleDirtyState that has its parameters dirty
	 */
	public TextureRectangleDirtyState setTextureParametersDirty() {
		if (parameters)
			return this;
		else
			return new TextureRectangleDirtyState(dirtyRegion, true);
	}

	/**
	 * Create a new TextureRectangleDirtyState that has a dirty region that's
	 * the union of <tt>region</tt> and the current dirty region. The new
	 * state's parameter's boolean matches this state's parameter boolean.
	 * 
	 * @param region The ImageRegion to merge in using
	 *            {@link ImageRegion#merge(ImageRegion)}
	 * @return A new TextureRectangleDirtyState that includes the given region
	 */
	public TextureRectangleDirtyState update(ImageRegion region) {
		ImageRegion merge = (dirtyRegion == null ? region : dirtyRegion.merge(region));
		return new TextureRectangleDirtyState(merge, parameters);
	}

	@Override
	public TextureRectangleDirtyState merge(TextureRectangleDirtyState state) {
		if (state == null)
			return this;
		
		ImageRegion r1 = dirtyRegion;
		ImageRegion r2 = state.dirtyRegion;
		ImageRegion merge = (r1 != null ? r1.merge(r2) : r2);
		return new TextureRectangleDirtyState(merge, parameters || state.parameters);
	}
}