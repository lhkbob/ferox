package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL2;
import javax.media.opengl.GL2ES2;

import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.ResourceData.Handle;
import com.ferox.renderer.impl.jogl.record.FramebufferRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.TextureCubeMap;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * A wrapper around framebuffer object functionality to make it slightly easier
 * to use with FboDelegate.
 * 
 * @author Michael Ludwig
 */
// FIXME: it may be the case that we'll have to create a color render buffer
// when no color buffers are attached.
public class JoglFbo {
	private final int fboId;
	private int renderBufferId;

	private final TextureTarget colorTarget;

	private int boundLayer;
	private int[] colorImageIds;

	/**
	 * Construct a new JoglFbo with the given dimensions, images, and
	 * parameters. These values have the same meaning as they do for FboDelegate
	 * or JoglTextureSurface. JoglFbos can only be constructed when there's a
	 * non-null JoglContext current. Afterwards, it is only usable with that
	 * given context, and undefined results occur if used otherwise.
	 */
	public JoglFbo(JoglContextManager factory, int width, int height, 
				   TextureTarget colorTarget, TextureTarget depthTarget, 
				   TextureImage[] colors, TextureImage depth, 
				   int layer, boolean useDepthRenderBuffer) {
		GL2ES2 gl = factory.getGL();
		if (gl == null)
			throw new RenderException("JoglFbo's can only be constructed when there's a current context");
		if (!factory.getFramework().getCapabilities().getFboSupport())
			throw new RenderException("Current hardware doesn't support the creation of fbos");

		FramebufferRecord fbr = factory.getRecord().frameRecord;

		this.colorTarget = colorTarget;

		int[] id = new int[1];
		gl.glGenFramebuffers(1, id, 0);
		fboId = id[0];
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId);

		int glColorTarget = getGlTarget(colorTarget, layer);
		int glDepthTarget = getGlTarget(depthTarget, layer);

		if (depth != null) {
			// attach the depth texture
			Handle h = factory.getFramework().getHandle(depth, factory);
			attachImage(gl, glDepthTarget, h.getId(), layer, GL2.GL_DEPTH_ATTACHMENT);

			renderBufferId = 0;
		} else if (useDepthRenderBuffer) {
			// make and attach the render buffer
			gl.glGenRenderbuffers(1, id, 0);
			renderBufferId = id[0];

			gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, renderBufferId);
			gl.glRenderbufferStorage(GL2.GL_RENDERBUFFER, GL2.GL_DEPTH_COMPONENT, 
										width, height);

			if (gl.glGetError() == GL2.GL_OUT_OF_MEMORY) {
				gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, 0);
				destroy(gl);
				throw new RenderException("Error creating a new FBO, not enough memory for the depth RenderBuffer");
			} else
				gl.glBindRenderbuffer(GL2.GL_RENDERBUFFER, 0);
			gl.glFramebufferRenderbuffer(GL2.GL_FRAMEBUFFER, GL2.GL_DEPTH_ATTACHMENT, 
											GL2.GL_RENDERBUFFER, renderBufferId);
		}

		if (colors != null && colors.length > 0) {
			// attach all of the images
			colorImageIds = new int[colors.length];
			Handle h;
			for (int i = 0; i < colors.length; i++) {
				h = factory.getFramework().getHandle(colors[i], factory);
				attachImage(gl, glColorTarget, h.getId(), layer, GL2.GL_COLOR_ATTACHMENT0 + i);
				colorImageIds[i] = h.getId();
			}
		} else
			colorImageIds = null;

		boundLayer = layer;

		// Enable/disable the read/draw buffers to make the fbo "complete"
		JoglProfileUtil.glReadBuffer(gl, GL2.GL_NONE);
		if (colorImageIds != null) {
			int[] drawBuffers = new int[colorImageIds.length];
			for (int i = 0; i < drawBuffers.length; i++)
				drawBuffers[i] = GL2.GL_COLOR_ATTACHMENT0 + i;
			JoglProfileUtil.glDrawBuffers(gl, drawBuffers.length, drawBuffers, 0);
		} else
			JoglProfileUtil.glDrawBuffer(gl, GL2.GL_NONE);

		int complete = gl.glCheckFramebufferStatus(GL2.GL_FRAMEBUFFER);
		if (complete != GL2.GL_FRAMEBUFFER_COMPLETE) {
			String msg = "FBO failed completion test, unable to render";
			switch (complete) {
			case GL2.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
				msg = "Fbo attachments aren't complete";
				break;
			case GL2.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
				msg = "Fbo needs at least one attachment";
				break;
			case GL2.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
				msg = "Fbo draw buffers improperly enabled";
				break;
			case GL2.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
				msg = "Fbo read buffer improperly enabled";
				break;
			case GL2.GL_FRAMEBUFFER_UNSUPPORTED:
				msg = "Texture/Renderbuffer combinations aren't supported on the hardware";
				break;
			case 0:
				msg = "glCheckFramebufferStatus() had an error while checking fbo status";
				break;
			}
			// clean-up and then throw an exception
			destroy(gl);
			throw new RenderException(msg);
		}

		// restore the old binding
		gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fbr.drawFramebufferBinding);
	}

	/**
	 * Bind this fbo to the given context (GL_FRAMEBUFFER target), using the
	 * given layer. It is assumed that the context is current and that the layer
	 * is valid. A valid layer is: 0 for 1d, 2d, and rect textures, 0-5 for
	 * cubmaps and 0 - depth-1 for 3d textures. It assumes that the state record
	 * is properly maintained and that the context was the original context this
	 * fbo was created on.
	 */
	public void bind(GL2ES2 gl, JoglStateRecord record, int layer) {
		FramebufferRecord fbr = record.frameRecord;

		// bind the fbo if needed
		if (fbr.drawFramebufferBinding != fboId) {
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, fboId);
			fbr.drawFramebufferBinding = fboId;
		}

		// possibly re-attach the images (in the case of cubemaps or 3d
		// textures)
		if (layer != boundLayer) {
			if (colorImageIds != null) {
				int target = getGlTarget(colorTarget, layer);
				for (int i = 0; i < colorImageIds.length; i++)
					attachImage(gl, target, colorImageIds[i], layer, 
								GL2.GL_COLOR_ATTACHMENT0 + i);
			}
			// we don't have to re-attach depth images -> will always be
			// 1d/2d/rect -> 1 layer only
			boundLayer = layer;
		}
	}

	/**
	 * Release the current fbo bound to the FRAMEBUFFER target. It assumes that
	 * the given context is current and the state record is properly maintained.
	 * The context must also have been the context that this fbo was originally
	 * constructed on.
	 */
	public void release(GL2ES2 gl, JoglStateRecord record) {
		FramebufferRecord fbr = record.frameRecord;

		if (fbr.drawFramebufferBinding != 0) {
			gl.glBindFramebuffer(GL2.GL_FRAMEBUFFER, 0);
			fbr.drawFramebufferBinding = 0;
		}
	}

	/**
	 * Destroy the fbo. It assumes that the context is current and that it was
	 * the context that this fbo was constructed on.
	 */
	public void destroy(GL2ES2 gl) {
		gl.glDeleteFramebuffers(1, new int[] { fboId }, 0);
		if (renderBufferId != 0)
			gl.glDeleteRenderbuffers(1, new int[] { renderBufferId }, 0);
	}

	// Get the appropriate texture target based on the layer and high-level
	// target
	private static int getGlTarget(TextureTarget target, int layer) {
		switch (target) {
		case T_1D:
			return GL2.GL_TEXTURE_1D;
		case T_2D:
			return GL2.GL_TEXTURE_2D;
		case T_3D:
			return GL2.GL_TEXTURE_3D;
		case T_RECT:
			return GL2.GL_TEXTURE_RECTANGLE_ARB;
		case T_CUBEMAP:
			switch (layer) {
			case TextureCubeMap.PX:
				return GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
			case TextureCubeMap.NX:
				return GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
			case TextureCubeMap.PY:
				return GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
			case TextureCubeMap.NY:
				return GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
			case TextureCubeMap.PZ:
				return GL2.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
			case TextureCubeMap.NZ:
				return GL2.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
			}
		}

		return -1;
	}

	// Attach the given texture image to the currently bound fbo (on target
	// FRAMEBUFFER)
	private static void attachImage(GL2ES2 gl, int target, int id, int layer, int attachment) {
		switch (target) {
		case GL2.GL_TEXTURE_1D:
			JoglProfileUtil.glFramebufferTexture1D(gl, GL2.GL_FRAMEBUFFER, attachment, target, id, 0);
			break;
		case GL2.GL_TEXTURE_3D:
			JoglProfileUtil.glFramebufferTexture3D(gl, GL2.GL_FRAMEBUFFER, attachment, target, id, 0, layer);
			break;
		default: // 2d, rect, or a cubemap face
			gl.glFramebufferTexture2D(GL2.GL_FRAMEBUFFER, attachment, target, id, 0);
		}
	}
}
