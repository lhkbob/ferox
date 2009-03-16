package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GLAutoDrawable;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/** Provide a flexible implementation for rendering-to-texture.
 * Implementations of this will be instantiated by JoglTextureSurface.
 * 
 * @author Michael Ludwig
 *
 */
public abstract class TextureSurfaceDelegate {
	private TextureImage[] colors;
	private TextureImage depth;
	
	private int width; 
	private int height;
	
	private DisplayOptions options;
	private TextureTarget colorTarget;
	private TextureTarget depthTarget;
	
	private int referenceCount;
	
	public TextureSurfaceDelegate(DisplayOptions options, TextureTarget colorTarget, TextureTarget depthTarget,
								  int width, int height,
								  TextureImage[] colors, TextureImage depth) {
		this.options = options;
		this.colorTarget = colorTarget;
		this.depthTarget = depthTarget;
		
		this.width = width;
		this.height = height;
		
		this.colors = colors;
		this.depth = depth;
		this.referenceCount = 0;
	}
	
	public abstract JoglStateRecord getStateRecord();
	
	public abstract GLAutoDrawable getGLAutoDrawable();
	
	public abstract void init();
	
	public abstract void preRenderAction(int layer);
	
	public abstract void postRenderAction(JoglRenderSurface next);
	
	public abstract void destroySurface();
	
	public TextureImage getColorBuffer(int target) {
		if (this.colors == null || target < 0 || target >= this.colors.length)
			return null;
		return this.colors[target];
	}
	
	protected TextureImage[] getColorBuffers() {
		return this.colors;
	}

	public TextureImage getDepthBuffer() {
		return this.depth;
	}

	public int getNumColorTargets() {
		return (this.colors == null ? 0 : this.colors.length);
	}
	
	public int getReferenceCount() {
		return this.referenceCount;
	}
	
	public void addReference() {
		this.referenceCount++;
	}
	
	public void removeReference() {
		this.referenceCount--;
	}

	public TextureTarget getColorTarget() {
		return this.colorTarget;
	}
	
	public TextureTarget getDepthTarget() {
		return this.depthTarget;
	}

	public DisplayOptions getDisplayOptions() {
		return this.options;
	}

	public int getHeight() {
		return this.height;
	}

	public int getWidth() {
		return this.width;
	}
}
