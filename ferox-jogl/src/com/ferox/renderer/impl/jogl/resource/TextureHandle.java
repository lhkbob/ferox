package com.ferox.renderer.impl.jogl.resource;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;

import com.ferox.renderer.Renderer.Comparison;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.renderer.impl.jogl.Utils;
import com.ferox.resource.TextureImage.DepthMode;
import com.ferox.resource.TextureImage.Filter;
import com.ferox.resource.TextureImage.TextureWrap;

public class TextureHandle extends ResourceHandle {
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
	public Filter filter;

	public TextureWrap wrapS;
	public TextureWrap wrapT;
	public TextureWrap wrapR;

	public Comparison depthTest;
	public DepthMode depthMode;
	public boolean enableDepthCompare;

	public float anisoLevel;
	
	public TextureHandle(int id, int glTarget, int glType, int glSrcFormat, int glDstFormat,
						 int width, int height, int depth, int numMipmaps) {
		super(id);
		this.glTarget = glTarget;
		this.glType = glType;
		this.glSrcFormat = glSrcFormat;
		this.glDstFormat = glDstFormat;
		
		this.width = width;
		this.height = height;
		this.depth = depth;
		this.numMipmaps = numMipmaps;
		
		// blank parameters
		filter = null;
		wrapS = null;
		wrapT = null;
		wrapR = null;
		
		depthTest = null;
		depthMode = null;
		enableDepthCompare = false;
		
		anisoLevel = -1f;
	}

	public void setFilter(GL gl, Filter newFilter, boolean force) {
		if (force || newFilter != filter) {
			int min = Utils.getGLMinFilter(newFilter);
			int mag = Utils.getGLMagFilter(newFilter);

			gl.glTexParameteri(glTarget, GL.GL_TEXTURE_MIN_FILTER, min);
			gl.glTexParameteri(glTarget, GL.GL_TEXTURE_MAG_FILTER, mag);
			filter = newFilter;
		}
	}

	public void setWrapS(GL gl, TextureWrap wrap, boolean force) {
		if (force || wrap != wrapS) {
			int w = Utils.getGLWrapMode(wrap);
			gl.glTexParameteri(glTarget, GL.GL_TEXTURE_WRAP_S, w);
			wrapS = wrap;
		}
	}

	public void setWrapT(GL gl, TextureWrap wrap, boolean force) {
		if (force || wrap != wrapT) {
			int w = Utils.getGLWrapMode(wrap);
			gl.glTexParameteri(glTarget, GL2.GL_TEXTURE_WRAP_T, w);
			wrapT = wrap;
		}
	}

	public void setWrapR(GL gl, TextureWrap wrap, boolean force) {
		if (force || wrap != wrapR) {
			int w = Utils.getGLWrapMode(wrap);
			gl.glTexParameteri(glTarget, GL2.GL_TEXTURE_WRAP_R, w);
			wrapR = wrap;
		}
	}

	public void setDepthTest(GL gl, Comparison test, boolean force) {
		if (force || test != depthTest) {
			int t = Utils.getGLPixelTest(test);
			gl.glTexParameteri(glTarget, GL2.GL_TEXTURE_COMPARE_FUNC, t);
			depthTest = test;
		}
	}

	public void setDepthMode(GL gl, DepthMode mode, boolean force) {
		if (force || mode != depthMode) {
			int m = Utils.getGLDepthMode(mode);
			gl.glTexParameteri(glTarget, GL2.GL_DEPTH_TEXTURE_MODE, m);
			depthMode = mode;
		}
	}

	public void setDepthCompareEnabled(GL gl, boolean enable, boolean force) {
		if (force || enable != enableDepthCompare) {
			gl.glTexParameteri(glTarget, GL2.GL_TEXTURE_COMPARE_MODE, (enable ? GL2.GL_COMPARE_R_TO_TEXTURE : GL.GL_NONE));
			enableDepthCompare = enable;
		}
	}

	public void setAnisotropicLevel(GL gl, float level, float maxAniso, boolean force) {
		if (force || level != anisoLevel) {
			float amount = level * maxAniso + 1f;
			gl.glTexParameterf(glTarget, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, amount);
			anisoLevel = level;
		}
	}
}
