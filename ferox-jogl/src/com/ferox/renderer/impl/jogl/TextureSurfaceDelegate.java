package com.ferox.renderer.impl.jogl;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.impl.Action;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * TextureSurfaceDelegate is an abstract class that exposes much of the same
 * functionality that a TextureSurface would. This is used so that multiple
 * JoglTextureSurface's can support sharing the same delegate instance.
 * 
 * @author Michael Ludwig
 */
public abstract class TextureSurfaceDelegate {
	private final TextureImage[] colors;
	private final TextureImage depth;

	private final int width;
	private final int height;

	private final DisplayOptions options;
	private final TextureTarget colorTarget;
	private final TextureTarget depthTarget;

	private int referenceCount;

	public TextureSurfaceDelegate(DisplayOptions options, TextureTarget colorTarget,
								  TextureTarget depthTarget, int width, int height, 
								  TextureImage[] colors, TextureImage depth) {
		this.options = options;
		this.colorTarget = colorTarget;
		this.depthTarget = depthTarget;

		this.width = width;
		this.height = height;

		this.colors = colors;
		this.depth = depth;
		referenceCount = 0;
	}


	public abstract JoglContext getContext();

	public abstract void init();

	public abstract void preRender(int layer);

	public abstract void postRender(Action next);

	public abstract void destroy();

	public TextureImage getColorBuffer(int target) {
		if (colors == null || target < 0 || target >= colors.length)
			return null;
		return colors[target];
	}

	protected TextureImage[] getColorBuffers() {
		return colors;
	}

	public TextureImage getDepthBuffer() {
		return depth;
	}

	public int getNumColorTargets() {
		return (colors == null ? 0 : colors.length);
	}

	public int getReferenceCount() {
		return referenceCount;
	}

	public void addReference() {
		referenceCount++;
	}

	public void removeReference() {
		referenceCount--;
	}

	public TextureTarget getColorTarget() {
		return colorTarget;
	}

	public TextureTarget getDepthTarget() {
		return depthTarget;
	}

	public DisplayOptions getDisplayOptions() {
		return options;
	}

	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}
}
