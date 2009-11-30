package com.ferox.resource;


/**
 * The dirty descriptor class that is used by TextureRectangle. Calls to
 * getDirtyState() for texture rectangle's will return objects of this
 * class.
 */
public class TextureRectangleDirtyState implements DirtyState<TextureRectangleDirtyState> {
	private ImageRegion dirtyRegion;
	private boolean parameters;
	
	public TextureRectangleDirtyState(ImageRegion dirtyRegion, boolean parameters) {
		this.dirtyRegion = dirtyRegion;
		this.parameters = parameters;
	}
	
	/**
	 * Get the ImageRegion for the TextureRectangle. If null is
	 * returned, then the data hasn't been flagged as dirty. The returned
	 * region will be constrained to be in the dimensions of the texture.
	 * 
	 * @return ImageRegion for this dirty descriptor's texture
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
	
	public TextureRectangleDirtyState setTextureParametersDirty() {
		if (parameters)
			return this;
		else
			return new TextureRectangleDirtyState(dirtyRegion, true);
	}
	
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