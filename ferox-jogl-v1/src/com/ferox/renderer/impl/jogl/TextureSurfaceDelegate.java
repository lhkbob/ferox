package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLAutoDrawable;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * Provide a flexible implementation for rendering-to-texture. Implementations
 * of this will be instantiated by JoglTextureSurface.
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

	public abstract JoglStateRecord getStateRecord();

	public abstract GLAutoDrawable getGLAutoDrawable();

	public abstract void init();

	public abstract void preRenderAction(int layer);

	public abstract void postRenderAction(Action next);

	public abstract void destroySurface();

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
