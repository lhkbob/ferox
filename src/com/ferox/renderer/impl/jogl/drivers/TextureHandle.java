package com.ferox.renderer.impl.jogl.drivers;

import javax.media.opengl.GL;

import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.EnumUtil;
import com.ferox.resource.texture.TextureImage.DepthMode;
import com.ferox.resource.texture.TextureImage.Filter;
import com.ferox.resource.texture.TextureImage.TextureWrap;
import com.ferox.state.State.PixelTest;

/** All ResourceDrivers for any of the TextureImage subclasses must use 
 * a subclass of TextureHandle to store their information or it will
 * not work correctly with offscreen rendering.
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
	
	public TextureHandle(int id, int glTarget, int glType, int glSrcFormat, int glDstFormat,
					  	 int width, int height, int depth, int numMipmaps) {
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
		this.filter = null;
		this.wrapS = null;
		this.wrapT = null;
		this.wrapR = null;
		this.depthTest = null;
		this.depthMode = null;
		this.enableDepthCompare = false;
		this.anisoLevel = -1f;
	}
	
	/** Set the min and mag filters for this texture handle object.  Assumes that
	 * the texture object is already bound on the gl context (otherwise 
	 * results are undefined). */
	public void setFilter(GL gl, Filter newFilter, boolean force) {
		if (force || newFilter != this.filter) {
			int min = EnumUtil.getGLMinFilter(newFilter);
			int mag = EnumUtil.getGLMagFilter(newFilter);
			
			gl.glTexParameteri(this.glTarget, GL.GL_TEXTURE_MIN_FILTER, min);
			gl.glTexParameteri(this.glTarget, GL.GL_TEXTURE_MAG_FILTER, mag);
			this.filter = newFilter;
		}
	}
	
	public void setWrapS(GL gl, TextureWrap wrap, boolean force) {
		if (force || wrap != this.wrapS) {
			int w = EnumUtil.getGLWrapMode(wrap);
			gl.glTexParameteri(this.glTarget, GL.GL_TEXTURE_WRAP_S, w);
			this.wrapS = wrap;
		}
	}
	
	public void setWrapT(GL gl, TextureWrap wrap, boolean force) {
		if (force || wrap != this.wrapT) {
			int w = EnumUtil.getGLWrapMode(wrap);
			gl.glTexParameteri(this.glTarget, GL.GL_TEXTURE_WRAP_T, w);
			this.wrapT = wrap;
		}
	}

	public void setWrapR(GL gl, TextureWrap wrap, boolean force) {
		if (force || wrap != this.wrapR) {
			int w = EnumUtil.getGLWrapMode(wrap);
			gl.glTexParameteri(this.glTarget, GL.GL_TEXTURE_WRAP_R, w);
			this.wrapR = wrap;
		}
	}
	
	public void setDepthTest(GL gl, PixelTest test, boolean force) {
		if (force || test != this.depthTest) {
			int t = EnumUtil.getGLPixelTest(test);
			gl.glTexParameteri(this.glTarget, GL.GL_TEXTURE_COMPARE_FUNC, t);
			this.depthTest = test;
		}
	}
	
	public void setDepthMode(GL gl, DepthMode mode, boolean force) {
		if (force || mode != this.depthMode) {
			int m = EnumUtil.getGLDepthMode(mode);
			gl.glTexParameteri(this.glTarget, GL.GL_DEPTH_TEXTURE_MODE, m);
			this.depthMode = mode;
		}
	}
	
	public void setDepthCompareEnabled(GL gl, boolean enable, boolean force) {
		if (force || enable != this.enableDepthCompare) {
			gl.glTexParameteri(this.glTarget, GL.GL_TEXTURE_COMPARE_MODE, (enable ? GL.GL_COMPARE_R_TO_TEXTURE : GL.GL_NONE));
			this.enableDepthCompare = enable;
		}
	}
	
	public void setAnisotropicLevel(GL gl, float level, float maxAniso, boolean force) {
		if (force || level != this.anisoLevel) {
			float amount = level * maxAniso + 1f;
			gl.glTexParameterf(this.glTarget, GL.GL_TEXTURE_MAX_ANISOTROPY_EXT, amount);
			this.anisoLevel = level;
		}
	}

	@Override
	public int getId() {
		return this.id;
	}
}
