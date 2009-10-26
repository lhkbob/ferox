package com.ferox.resource;

/**
 * The dirty descriptor class that is used by TextureRectangle. Calls to
 * getDirtyDescriptor() for texture rectangle's will return objects of this
 * class.
 */
public class TextureRectangleDirtyDescriptor {
	private ImageRegion dirtyRegion;
	private boolean parameters;

	/**
	 * Create a new TextureRectangleDirtyDescriptor that has nothing flagged as
	 * dirty.
	 */
	public TextureRectangleDirtyDescriptor() {
		dirtyRegion = null;
		parameters = false;
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
	
	void setParametersDirty() {
		parameters = true;
	}
	
	void setDirtyRegion(ImageRegion region) {
		dirtyRegion = region;
	}
}