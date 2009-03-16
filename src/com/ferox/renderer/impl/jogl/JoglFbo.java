package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL;

import com.ferox.renderer.RenderException;
import com.ferox.renderer.impl.jogl.drivers.TextureHandle;
import com.ferox.renderer.impl.jogl.record.FramebufferRecord;
import com.ferox.renderer.impl.jogl.record.JoglStateRecord;
import com.ferox.resource.TextureCubeMap;
import com.ferox.resource.TextureImage;
import com.ferox.resource.TextureImage.TextureTarget;

/** A wrapper around framebuffer object functionality to make it slightly easier
 * to use with FboDelegate.
 * 
 * @author Michael Ludwig
 *
 */
// FIXME: it may be the case that we'll have to create a color render buffer when no
// color buffers are attached.
public class JoglFbo {
	private int fboId;
	private int renderBufferId;
	
	private TextureTarget colorTarget;
	
	private int boundLayer;
	private int[] colorImageIds;
	
	/** Construct a new JoglFbo with the given dimensions, images, and parameters.  These
	 * values have the same meaning as they do for FboDelegate or JoglTextureSurface.
	 * 
	 * JoglFbos can only be constructed when there's a non-null JoglContext current.
	 * Afterwards, it is only usable with that given context, and undefined results occur
	 * if used otherwise. */
	public JoglFbo(JoglSurfaceFactory factory, int width, int height, TextureTarget colorTarget, TextureTarget depthTarget,
				   TextureImage[] colors, TextureImage depth, 
				   int layer, boolean useDepthRenderBuffer) throws RenderException {
		GL gl = factory.getGL();
		if (gl == null)
			throw new RenderException("JoglFbo's can only be constructed when there's a current context");
		if (!factory.getRenderer().getCapabilities().getFboSupport())
			throw new RenderException("Current hardware doesn't support the creation of fbos");
		
		FramebufferRecord fbr = factory.getRecord().frameRecord;
		
		this.colorTarget = colorTarget;
		
		int[] id = new int[1];
		gl.glGenFramebuffersEXT(1, id, 0);
		this.fboId = id[0];
		gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, this.fboId);
		
		int glColorTarget = getGlTarget(colorTarget, layer);
		int glDepthTarget = getGlTarget(depthTarget, layer);
		
		if (depth != null) {
			// attach the depth texture
			TextureHandle h = (TextureHandle) factory.getRenderer().getHandle(depth);
			attachImage(gl, glDepthTarget, h.id, layer, GL.GL_DEPTH_ATTACHMENT_EXT);
			
			this.renderBufferId = 0;
		} else if (useDepthRenderBuffer) {
			// make and attach the render buffer
			gl.glGenRenderbuffersEXT(1, id, 0);
			this.renderBufferId = id[0];
			
			gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, this.renderBufferId);
			gl.glRenderbufferStorageEXT(GL.GL_RENDERBUFFER_EXT, GL.GL_DEPTH_COMPONENT, width, height);
			
			if (gl.glGetError() == GL.GL_OUT_OF_MEMORY) {
				gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, 0);
				this.destroy(gl);
				throw new RenderException("Error creating a new FBO, not enough memory for the depth RenderBuffer");
			} else
				gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, 0);
			gl.glFramebufferRenderbufferEXT(GL.GL_FRAMEBUFFER_EXT, GL.GL_DEPTH_ATTACHMENT_EXT, GL.GL_RENDERBUFFER_EXT, this.renderBufferId);
		}
		
		if (colors != null && colors.length > 0) {
			// attach all of the images
			this.colorImageIds = new int[colors.length];
			TextureHandle h;
			for (int i = 0; i < colors.length; i++) {
				h = (TextureHandle) factory.getRenderer().getHandle(colors[i]);
				attachImage(gl, glColorTarget, h.id, layer, GL.GL_COLOR_ATTACHMENT0_EXT + i);
				this.colorImageIds[i] = h.id;
			}
		} else
			this.colorImageIds = null;
		
		this.boundLayer = layer;
		
		// Enable/disable the read/draw buffers to make the fbo "complete"
		gl.glReadBuffer(GL.GL_NONE);
		if (this.colorImageIds != null) {
			int[] drawBuffers = new int[this.colorImageIds.length];
			for (int i = 0; i < drawBuffers.length; i++)
				drawBuffers[i] = GL.GL_COLOR_ATTACHMENT0_EXT + i;
			gl.glDrawBuffers(drawBuffers.length, drawBuffers, 0);
		} else
			gl.glDrawBuffer(GL.GL_NONE);
		
		int complete = gl.glCheckFramebufferStatusEXT(GL.GL_FRAMEBUFFER_EXT);
		if (complete != GL.GL_FRAMEBUFFER_COMPLETE_EXT) {
			String msg = "FBO failed completion test, unable to render";
			switch(complete) {
			case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT: msg = "Fbo attachments aren't complete"; break;
			case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT: msg = "Fbo needs at least one attachment"; break;
			case GL.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT: msg = "Fbo draw buffers improperly enabled"; break;
			case GL.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT: msg = "Fbo read buffer improperly enabled"; break;
			case GL.GL_FRAMEBUFFER_UNSUPPORTED_EXT: msg = "Texture/Renderbuffer combinations aren't supported on the hardware"; break;
			case 0: msg = "glCheckFramebufferStatusEXT() had an error while checking fbo status"; break;
			}
			// clean-up and then throw an exception
			this.destroy(gl);
			throw new RenderException(msg);
		}
		
		// restore the old binding
		gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, fbr.drawFramebufferBinding);
	}
	
	/** Bind this fbo to the given context (GL_FRAMEBUFFER target), using the given layer.
	 * It is assumed that the context is current and that the layer is valid.
	 * A valid layer is: 0 for 1d, 2d, and rect textures, 0-5 for cubmaps and
	 * 0 - depth-1 for 3d textures. 
	 * 
	 * It assumes that the state record is properly maintained and that the
	 * context was the original context this fbo was created on. */
	public void bind(GL gl, JoglStateRecord record, int layer) {
		FramebufferRecord fbr = record.frameRecord;
		
		// bind the fbo if needed
		if (fbr.drawFramebufferBinding != this.fboId) {
			gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, this.fboId);
			fbr.drawFramebufferBinding = this.fboId;
		}
		
		// possibly re-attach the images (in the case of cubemaps or 3d textures)
		if (layer != this.boundLayer) {
			if (this.colorImageIds != null) {
				int target = getGlTarget(this.colorTarget, layer);
				for (int i = 0; i < this.colorImageIds.length; i++)
					attachImage(gl, target, this.colorImageIds[i], layer, GL.GL_COLOR_ATTACHMENT0_EXT + i);
			}
			// we don't have to re-attach depth images -> will always be 1d/2d/rect -> 1 layer only
			this.boundLayer = layer;
		}
	}
	
	/** Release the current fbo bound to the FRAMEBUFFER target. 
	 * It assumes that the given context is current and the state record
	 * is properly maintained.  The context must also have been the 
	 * context that this fbo was originally constructed on. */
	public void release(GL gl, JoglStateRecord record) {
		FramebufferRecord fbr = record.frameRecord;
		
		if (fbr.drawFramebufferBinding != 0) {
			gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
			fbr.drawFramebufferBinding = 0;
		}
	}
	
	/** Destroy the fbo.  It assumes that the context is current
	 * and that it was the context that this fbo was constructed on. */
	public void destroy(GL gl) {
		gl.glDeleteFramebuffersEXT(1, new int[] {this.fboId}, 0);
		if (this.renderBufferId != 0)
			gl.glDeleteRenderbuffersEXT(1, new int[] {this.renderBufferId}, 0);
	}
	
	// Get the appropriate texture target based on the layer and high-level target
	private static int getGlTarget(TextureTarget target, int layer) {
		switch(target) {
		case T_1D: return GL.GL_TEXTURE_1D;
		case T_2D: return GL.GL_TEXTURE_2D;
		case T_3D: return GL.GL_TEXTURE_3D;
		case T_RECT: return GL.GL_TEXTURE_RECTANGLE_ARB;
		case T_CUBEMAP:
			switch(layer) {
			case TextureCubeMap.PX: return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X;
			case TextureCubeMap.NX: return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X;
			case TextureCubeMap.PY: return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y;
			case TextureCubeMap.NY: return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y;
			case TextureCubeMap.PZ: return GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z;
			case TextureCubeMap.NZ: return GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z;
			}
		}
		
		return -1;
	}
	
	// Attach the given texture image to the currently bound fbo (on target FRAMEBUFFER)
	private static void attachImage(GL gl, int target, int id, int layer, int attachment) {
		switch(target) {
		case GL.GL_TEXTURE_1D:
			gl.glFramebufferTexture1DEXT(GL.GL_FRAMEBUFFER_EXT, attachment, target, id, 0);
			break;
		case GL.GL_TEXTURE_3D:
			gl.glFramebufferTexture3DEXT(GL.GL_FRAMEBUFFER_EXT, attachment, target, id, 0, layer);
			break;
		default: // 2d, rect, or a cubemap face
			gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, attachment, target, id, 0);
		}
	}
}
