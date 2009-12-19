package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2GL3;

import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.resource.TextureCubeMap;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/**
 * FramebufferObject is a low-level wrapper around an OpenGL fbo and can be used
 * to perform render-to-texture effects. These should not be used directly and
 * are intended to be managed by a {@link FboSurfaceDelegate}.
 * 
 * @author Michael Ludwig
 */
public class FramebufferObject {
	private final int fboId;
	private int renderBufferId;

	private final TextureTarget colorTarget;

	private int boundLayer;
	private int[] colorImageIds;

	public FramebufferObject(JoglFramework framework, int width, int height, 
				   			 TextureTarget colorTarget, TextureTarget depthTarget, 
				   			 TextureImage[] colors, TextureImage depth, 
				   			 int layer, boolean useDepthRenderBuffer) {
		JoglContext context = JoglContext.getCurrent();
		if (context == null)
			throw new RenderException("FramebufferObject's can only be constructed when there's a current context");
		if (!framework.getCapabilities().getFboSupport())
			throw new RenderException("Current hardware doesn't support the creation of fbos");

		GL2GL3 gl = context.getGL();

		this.colorTarget = colorTarget;

		int[] id = new int[1];
		gl.glGenFramebuffers(1, id, 0);
		fboId = id[0];
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, fboId);

		if (depth != null) {
			// attach the depth texture
			int glDepthTarget = getGlTarget(depthTarget, layer);

			ResourceHandle h = framework.getResourceManager().getHandle(depth);
			attachImage(gl, glDepthTarget, h.getId(), layer, GL.GL_DEPTH_ATTACHMENT);

			renderBufferId = 0;
		} else if (useDepthRenderBuffer) {
			// make and attach the render buffer
			gl.glGenRenderbuffers(1, id, 0);
			renderBufferId = id[0];

			gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, renderBufferId);
			gl.glRenderbufferStorage(GL.GL_RENDERBUFFER, GL2GL3.GL_DEPTH_COMPONENT, width, height);

			if (gl.glGetError() == GL.GL_OUT_OF_MEMORY) {
				gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0);
				destroy();
				throw new RenderException("Error creating a new FBO, not enough memory for the depth RenderBuffer");
			} else
				gl.glBindRenderbuffer(GL.GL_RENDERBUFFER, 0);
			gl.glFramebufferRenderbuffer(GL.GL_FRAMEBUFFER, GL.GL_DEPTH_ATTACHMENT, 
										 GL.GL_RENDERBUFFER, renderBufferId);
		}

		if (colors != null && colors.length > 0) {
			// attach all of the images
			int glColorTarget = getGlTarget(colorTarget, layer);

			colorImageIds = new int[colors.length];
			ResourceHandle h;
			for (int i = 0; i < colors.length; i++) {
				h = framework.getResourceManager().getHandle(colors[i]);
				attachImage(gl, glColorTarget, h.getId(), layer, GL.GL_COLOR_ATTACHMENT0 + i);
				colorImageIds[i] = h.getId();
			}
		} else
			colorImageIds = null;

		boundLayer = layer;

		// Enable/disable the read/draw buffers to make the fbo "complete"
		gl.glReadBuffer(GL.GL_NONE);
		if (colorImageIds != null) {
			int[] drawBuffers = new int[colorImageIds.length];
			for (int i = 0; i < drawBuffers.length; i++)
				drawBuffers[i] = GL.GL_COLOR_ATTACHMENT0 + i;
			gl.glDrawBuffers(drawBuffers.length, drawBuffers, 0);
		} else
			gl.glDrawBuffer(GL.GL_NONE);

		int complete = gl.glCheckFramebufferStatus(GL.GL_FRAMEBUFFER);
		if (complete != GL.GL_FRAMEBUFFER_COMPLETE) {
			String msg = "FBO failed completion test, unable to render";
			switch (complete) {
			case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT:
				msg = "Fbo attachments aren't complete";
				break;
			case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT:
				msg = "Fbo needs at least one attachment";
				break;
			case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER:
				msg = "Fbo draw buffers improperly enabled";
				break;
			case GL2GL3.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER:
				msg = "Fbo read buffer improperly enabled";
				break;
			case GL.GL_FRAMEBUFFER_UNSUPPORTED:
				msg = "TextureEnvironment/Renderbuffer combinations aren't supported on the hardware";
				break;
			case 0:
				msg = "glCheckFramebufferStatusEXT() had an error while checking fbo status";
				break;
			}
			// clean-up and then throw an exception
			destroy();
			throw new RenderException(msg);
		}

		// restore the old binding
		gl.glBindFramebuffer(GL.GL_FRAMEBUFFER, context.getRecord().getFbo());
	}

	public void bind(int layer) {
		JoglContext context = JoglContext.getCurrent();
		GL2GL3 gl = context.getGL();
		
		// bind the fbo if needed
		context.getRecord().bindFbo(gl, fboId);

		// possibly re-attach the images (in the case of cubemaps or 3d textures)
		if (layer != boundLayer) {
			if (colorImageIds != null) {
				int target = getGlTarget(colorTarget, layer);
				for (int i = 0; i < colorImageIds.length; i++)
					attachImage(gl, target, colorImageIds[i], layer, 
								GL.GL_COLOR_ATTACHMENT0 + i);
			}
			// we don't have to re-attach depth images -> will always be 1d/2d/rect -> 1 layer only
			boundLayer = layer;
		}
	}

	public void release() {
		JoglContext context = JoglContext.getCurrent();
		context.getRecord().bindFbo(context.getGL(), 0);
	}

	public void destroy() {
		GL2GL3 gl = JoglContext.getCurrent().getGL();
		gl.glDeleteFramebuffers(1, new int[] { fboId }, 0);
		if (renderBufferId != 0)
			gl.glDeleteRenderbuffers(1, new int[] { renderBufferId }, 0);
	}

	// Get the appropriate texture target based on the layer and high-level target
	private static int getGlTarget(TextureTarget target, int layer) {
		switch (target) {
		case T_1D:
			return GL2GL3.GL_TEXTURE_1D;
		case T_2D:
			return GL2GL3.GL_TEXTURE_2D;
		case T_3D:
			return GL2GL3.GL_TEXTURE_3D;
		case T_RECT:
			return GL2GL3.GL_TEXTURE_RECTANGLE_ARB;
		case T_CUBEMAP:
			switch (layer) {
			case TextureCubeMap.PX:
				return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
			case TextureCubeMap.NX:
				return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
			case TextureCubeMap.PY:
				return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
			case TextureCubeMap.NY:
				return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
			case TextureCubeMap.PZ:
				return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
			case TextureCubeMap.NZ:
				return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
			}
		}

		return -1;
	}

	// Attach the given texture image to the currently bound fbo (on target FRAMEBUFFER)
	private static void attachImage(GL2GL3 gl, int target, int id, int layer, int attachment) {
		switch (target) {
		case GL2GL3.GL_TEXTURE_1D:
			gl.glFramebufferTexture1D(GL.GL_FRAMEBUFFER, attachment, target, id, 0);
			break;
		case GL2GL3.GL_TEXTURE_3D:
			gl.glFramebufferTexture3D(GL.GL_FRAMEBUFFER, attachment, target, id, 0, layer);
			break;
		default: // 2d, rect, or a cubemap face
			gl.glFramebufferTexture2D(GL.GL_FRAMEBUFFER, attachment, target, id, 0);
		}
	}
}
