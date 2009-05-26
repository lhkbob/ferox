package com.ferox.renderer.impl.jogl;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord;
import com.ferox.renderer.impl.jogl.record.TextureRecord.TextureUnit;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/** Provides GLPBuffer support for the JoglTextureSurface. */
public class PbufferDelegate extends TextureSurfaceDelegate {
	private final GLPbuffer pbuffer;
	private final JoglStateRecord record;
	private final JoglContextManager factory;

	private int swapBuffersLayer;

	public PbufferDelegate(JoglContextManager factory, DisplayOptions options,
		JoglTextureSurface surface, TextureTarget colorTarget,
		TextureTarget depthTarget, int width, int height, TextureImage color,
		TextureImage depth, boolean useDepthRenderBuffer) {
		super(options, colorTarget, depthTarget, width, height,
			new TextureImage[] { color }, depth);

		GLCapabilities caps = chooseCapabilities(options, useDepthRenderBuffer);
		pbuffer =
			GLDrawableFactory.getFactory().createGLPbuffer(caps,
				new DefaultGLCapabilitiesChooser(), width, height,
				factory.getShadowContext());
		pbuffer.addGLEventListener(surface);

		this.factory = factory;
		record = new JoglStateRecord(factory.getFramework().getCapabilities());
	}

	@Override
	public JoglStateRecord getStateRecord() {
		return record;
	}

	@Override
	public GLPbuffer getGLAutoDrawable() {
		return pbuffer;
	}

	@Override
	public void destroySurface() {
		pbuffer.destroy();
	}

	@Override
	public void preRenderAction(int layer) {
		swapBuffersLayer = layer; // save for when swapBuffers is called
	}

	@Override
	public void init() {
		// do nothing
	}

	@Override
	public void postRenderAction(JoglRenderSurface next) {
		TextureImage color = getColorBuffer(0); // will be 1 color target at max
		TextureImage depth = getDepthBuffer();

		GL gl = factory.getGL();
		TextureRecord tr = factory.getRecord().textureRecord;
		TextureUnit tu = tr.textureUnits[tr.activeTexture];

		int ct = -1;
		int dt = -1;

		if (color != null) {
			TextureHandle handle =
				(TextureHandle) factory.getFramework().getHandle(color, factory);
			gl.glBindTexture(handle.glTarget, handle.id);
			copySubImage(gl, handle);
			ct = handle.glTarget;
		}
		if (depth != null) {
			TextureHandle handle =
				(TextureHandle) factory.getFramework().getHandle(depth, factory);
			gl.glBindTexture(handle.glTarget, handle.id);
			copySubImage(gl, handle);
			dt = handle.glTarget;
		}
		restoreBindings(gl, tu, ct, dt);
	}

	/*
	 * Copy the buffer into the given TextureHandle. It assumes the texture was
	 * already bound.
	 */
	private void copySubImage(GL gl, TextureHandle handle) {
		switch (handle.glTarget) {
		case GL.GL_TEXTURE_1D:
			gl.glCopyTexSubImage1D(handle.glTarget, 0, 0, 0, 0, getWidth());
			break;
		case GL.GL_TEXTURE_2D:
		case GL.GL_TEXTURE_RECTANGLE_ARB:
			gl.glCopyTexSubImage2D(handle.glTarget, 0, 0, 0, 0, 0, getWidth(),
				getHeight());
			break;
		case GL.GL_TEXTURE_CUBE_MAP:
			int face = JoglUtil.getGLCubeFace(swapBuffersLayer);
			gl
				.glCopyTexSubImage2D(face, 0, 0, 0, 0, 0, getWidth(),
					getHeight());
		case GL.GL_TEXTURE_3D:
			gl.glCopyTexSubImage3D(handle.glTarget, 0, 0, 0, swapBuffersLayer,
				0, 0, getWidth(), getHeight());
			break;
		}
	}

	private static void restoreBindings(GL gl, TextureUnit activeUnit,
		int colorTarget, int depthTarget) {
		if (colorTarget > 0)
			if (activeUnit.enabledTarget == colorTarget)
				gl.glBindTexture(colorTarget, activeUnit.texBinding); // restore
			// enabled
			// texture
			else
				gl.glBindTexture(colorTarget, 0); // not really the active unit

		if (depthTarget > 0 && colorTarget != depthTarget)
			if (activeUnit.enabledTarget == depthTarget)
				gl.glBindTexture(depthTarget, activeUnit.texBinding); // restore
			// enabled
			// texture
			else
				gl.glBindTexture(depthTarget, 0); // not really the active unit
	}

	private static GLCapabilities chooseCapabilities(DisplayOptions request,
		boolean useDepthRenderBuffer) {
		GLCapabilities caps = new GLCapabilities();

		PixelFormat pf = request.getPixelFormat();
		caps.setPbufferFloatingPointBuffers(pf == PixelFormat.RGB_FLOAT
			|| pf == PixelFormat.RGBA_FLOAT);

		// try to update the caps fields
		switch (pf) {
		case NONE:
			caps.setRedBits(0);
			caps.setGreenBits(0);
			caps.setBlueBits(0);
			caps.setAlphaBits(0);
			break;
		case RGB_16BIT:
			caps.setRedBits(5);
			caps.setGreenBits(6);
			caps.setBlueBits(5);
			caps.setAlphaBits(0);
			break;
		case RGB_24BIT:
			caps.setRedBits(8);
			caps.setGreenBits(8);
			caps.setBlueBits(8);
			caps.setAlphaBits(0);
			break;
		case RGB_FLOAT:
		case RGBA_FLOAT:
			caps.setRedBits(32);
			caps.setGreenBits(32);
			caps.setBlueBits(32);
			caps.setAlphaBits(pf == PixelFormat.RGB_FLOAT ? 0 : 32);
			break;
		case RGBA_32BIT:
			caps.setRedBits(8);
			caps.setGreenBits(8);
			caps.setBlueBits(8);
			caps.setAlphaBits(8);
			break;
		}

		switch (request.getDepthFormat()) {
		case DEPTH_16BIT:
			caps.setDepthBits(16);
			break;
		case DEPTH_24BIT:
			caps.setDepthBits(24);
			break;
		case DEPTH_32BIT:
			caps.setDepthBits(32);
			break;
		case NONE:
			caps.setDepthBits(useDepthRenderBuffer ? 24 : 0);
			break;
		}

		switch (request.getStencilFormat()) {
		case STENCIL_16BIT:
			caps.setStencilBits(16);
			break;
		case STENCIL_8BIT:
			caps.setStencilBits(8);
			break;
		case STENCIL_4BIT:
			caps.setStencilBits(4);
			break;
		case STENCIL_1BIT:
			caps.setStencilBits(1);
			break;
		case NONE:
			caps.setStencilBits(0);
			break;
		}

		return caps;
	}
}
