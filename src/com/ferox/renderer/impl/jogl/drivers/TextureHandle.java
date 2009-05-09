package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.effect.Effect.PixelTest;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.JoglUtil;
import com.ferox.resource.TextureImage.DepthMode;
import com.ferox.resource.TextureImage.Filter;
import com.ferox.resource.TextureImage.TextureWrap;

/**
 * All ResourceDrivers for any of the TextureImage subclasses must use a
 * subclass of TextureHandle to store their information or it will not work
 * correctly with offscreen rendering.
 * 
 * @author Michael Ludwig
 * 
 */
public class TextureHandle implements Handle {
	// id
	public final int id;

	// formatting
	public final int glTarget;
	public final int glType;
	public final int glSrcFormat;
	public final int glDstFormat;

	// image size
	public final int width;
	public final int height;
	public final int depth;
	public final int numMipmaps;

	// texture parameters
	private Filter filter;

	private TextureWrap wrapS;
	private TextureWrap wrapT;
	private TextureWrap wrapR;

	private PixelTest depthTest;
	private DepthMode depthMode;
	private boolean enableDepthCompare;

	private float anisoLevel;

	public TextureHandle(int id, int glTarget, int glType, int glSrcFormat,
			int glDstFormat, int width, int height, int depth, int numMipmaps) {
		// constant parameters for a texture
		this.id = id;

		this.glTarget = glTarget;
		this.glType = glType;
		this.glSrcFormat = glSrcFormat;
		this.glDstFormat = glDstFormat;

		this.width = width;
		this.height = height;
		this.depth = depth;
		this.numMipmaps = numMipmaps;

		// changing texture params
		filter = null;
		wrapS = null;
		wrapT = null;
		wrapR = null;
		depthTest = null;
		depthMode = null;
		enableDepthCompare = false;
		anisoLevel = -1f;
	}

	/**
	 * Set the min and mag filters for this texture handle object. Assumes that
	 * the texture object is already bound on the gl context (otherwise results
	 * are undefined).
	 */
	public void setFilter(GL gl, Filter newFilter, boolean force) {
		if (force || newFilter != filter) {
			int min = JoglUtil.getGLMinFilter(newFilter);
			int mag = JoglUtil.getGLMagFilter(newFilter);

			gl.glTexParameteri(glTarget, GL.GL_TEXTURE_MIN_FILTER, min);
			gl.glTexParameteri(glTarget, GL.GL_TEXTURE_MAG_FILTER, mag);
			filter = newFilter;
		}
	}

	public void setWrapS(GL gl, TextureWrap wrap, boolean force) {
		if (force || wrap != wrapS) {
			int w = JoglUtil.getGLWrapMode(wrap);
			gl.glTexParameteri(glTarget, GL.GL_TEXTURE_WRAP_S, w);
			wrapS = wrap;
		}
	}

	public void setWrapT(GL gl, TextureWrap wrap, boolean force) {
		if (force || wrap != wrapT) {
			int w = JoglUtil.getGLWrapMode(wrap);
			gl.glTexParameteri(glTarget, GL.GL_TEXTURE_WRAP_T, w);
			wrapT = wrap;
		}
	}

	public void setWrapR(GL gl, TextureWrap wrap, boolean force) {
		if (force || wrap != wrapR) {
			int w = JoglUtil.getGLWrapMode(wrap);
			gl.glTexParameteri(glTarget, GL.GL_TEXTURE_WRAP_R, w);
			wrapR = wrap;
		}
	}

	public void setDepthTest(GL gl, PixelTest test, boolean force) {
		if (force || test != depthTest) {
			int t = JoglUtil.getGLPixelTest(test);
			gl.glTexParameteri(glTarget, GL.GL_TEXTURE_COMPARE_FUNC, t);
			depthTest = test;
		}
	}

	public void setDepthMode(GL gl, DepthMode mode, boolean force) {
		if (force || mode != depthMode) {
			int m = JoglUtil.getGLDepthMode(mode);
			gl.glTexParameteri(glTarget, GL.GL_DEPTH_TEXTURE_MODE, m);
			depthMode = mode;
		}
	}

	public void setDepthCompareEnabled(GL gl, boolean enable, boolean force) {
		if (force || enable != enableDepthCompare) {
			gl.glTexParameteri(glTarget, GL.GL_TEXTURE_COMPARE_MODE,
					(enable ? GL.GL_COMPARE_R_TO_TEXTURE : GL.GL_NONE));
			enableDepthCompare = enable;
		}
	}

	public void setAnisotropicLevel(GL gl, float level, float maxAniso,
			boolean force) {
		if (force || level != anisoLevel) {
			float amount = level * maxAniso + 1f;
			gl.glTexParameterf(glTarget, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT,
					amount);
			anisoLevel = level;
		}
	}

	@Override
	public int getId() {
		return id;
	}
}
