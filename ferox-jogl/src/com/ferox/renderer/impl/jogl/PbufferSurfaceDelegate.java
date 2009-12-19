package com.ferox.renderer.impl.jogl;

import javax.media.opengl.DefaultGLCapabilitiesChooser;
import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;
import javax.media.opengl.GLCapabilities;
import javax.media.opengl.GLDrawableFactory;
import javax.media.opengl.GLPbuffer;
import javax.media.opengl.GLProfile;

import com.ferox.renderer.DisplayOptions;
import com.ferox.renderer.DisplayOptions.PixelFormat;
import com.ferox.renderer.impl.Action;
import com.ferox.renderer.impl.jogl.resource.TextureHandle;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * <p>
 * PbufferSurfaceDelegate is a TextureSurfaceDelegate that uses pbuffers to
 * render to a texture offscreen. Each time the rendering is completed, the
 * pbuffer is copied into an actual texture using glCopyTexSubImage(). It does
 * not support pbuffer direct RTT support since that is not well supported or
 * standardized across systems.
 * </p>
 * <p>
 * Like {@link FboSurfaceDelegate}, this should not be created directly but
 * instead is created when necessary by a {@link JoglTextureSurface}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class PbufferSurfaceDelegate extends TextureSurfaceDelegate {
	private final JoglFramework framework;
	
	private final GLPbuffer pbuffer;
	private final JoglContext context;
	
	private int swapBuffersLayer;

	public PbufferSurfaceDelegate(JoglFramework framework, DisplayOptions options, 
								  JoglTextureSurface surface, TextureTarget colorTarget, 
								  TextureTarget depthTarget, int width, int height, 
								  TextureImage color, TextureImage depth, boolean useDepthRenderBuffer) {
		super(options, colorTarget, depthTarget, width, height, new TextureImage[] { color }, depth);
		this.framework = framework;
		
		GLCapabilities caps = chooseCapabilities(options, framework.getGLProfile(), useDepthRenderBuffer);
		pbuffer = GLDrawableFactory.getFactory(framework.getGLProfile()).createGLPbuffer(caps, new DefaultGLCapabilitiesChooser(),
																 					     width, height, null);
		context = new JoglContext(framework, pbuffer);
		pbuffer.setContext(context.getContext());
	}

	@Override
	public JoglContext getContext() {
		return context;
	}

	@Override
	public void destroy() {
		pbuffer.destroy();
	}

	@Override
	public void preRender(int layer) {
		swapBuffersLayer = layer; // save for when swapBuffers is called
	}

	@Override
	public void init() {
		// do nothing
	}

	@Override
	public void postRender(Action next) {
		TextureImage color = getColorBuffer(0); // will be 1 color target at max
		TextureImage depth = getDepthBuffer();

		GL2GL3 gl = context.getGL();

		int ct = -1;
		int dt = -1;

		if (color != null) {
			TextureHandle handle = (TextureHandle) framework.getResourceManager().getHandle(color);
			gl.glBindTexture(handle.glTarget, handle.getId());
			copySubImage(gl, handle);
			ct = handle.glTarget;
		}
		if (depth != null) {
			TextureHandle handle = (TextureHandle) framework.getResourceManager().getHandle(depth);
			gl.glBindTexture(handle.glTarget, handle.getId());
			copySubImage(gl, handle);
			dt = handle.glTarget;
		}
		
		restoreBindings(gl, context.getRecord(), ct, dt);
	}

	/*
	 * Copy the buffer into the given TextureHandle. It assumes the texture was
	 * already bound.
	 */
	private void copySubImage(GL2GL3 gl, TextureHandle handle) {
		switch (handle.glTarget) {
		case GL2GL3.GL_TEXTURE_1D:
			gl.glCopyTexSubImage1D(handle.glTarget, 0, 0, 0, 0, getWidth());
			break;
		case GL2GL3.GL_TEXTURE_2D:
		case GL2GL3.GL_TEXTURE_RECTANGLE_ARB:
			gl.glCopyTexSubImage2D(handle.glTarget, 0, 0, 0, 0, 0, getWidth(), getHeight());
			break;
		case GL2GL3.GL_TEXTURE_CUBE_MAP:
			int face = Utils.getGLCubeFace(swapBuffersLayer);
			gl.glCopyTexSubImage2D(face, 0, 0, 0, 0, 0, getWidth(), getHeight());
		case GL2GL3.GL_TEXTURE_3D:
			gl.glCopyTexSubImage3D(handle.glTarget, 0, 0, 0, swapBuffersLayer, 
								   0, 0, getWidth(), getHeight());
			break;
		}
	}

	private static void restoreBindings(GL gl, BoundObjectState state, int colorTarget, int depthTarget) {
		int target = state.getTextureTarget(state.getActiveTexture());
		int tex = state.getTexture(state.getActiveTexture());
		
		if (colorTarget > 0) {
			if (target == colorTarget)
				// restore enabled texture
				gl.glBindTexture(colorTarget, tex);
			else
				gl.glBindTexture(colorTarget, 0); // not really the active unit
		}
		if (depthTarget > 0 && colorTarget != depthTarget) {
			if (target == depthTarget)
				// restore enabled texture
				gl.glBindTexture(depthTarget, tex);
			else
				gl.glBindTexture(depthTarget, 0); // not really the active unit
		}
	}

	private static GLCapabilities chooseCapabilities(DisplayOptions request, GLProfile profile,
													 boolean useDepthRenderBuffer) {
		GLCapabilities caps = new GLCapabilities(profile);

		PixelFormat pf = request.getPixelFormat();
		caps.setPbufferFloatingPointBuffers(pf == PixelFormat.RGB_FLOAT || 
											pf == PixelFormat.RGBA_FLOAT);

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
