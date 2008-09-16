package com.ferox.impl.jsr231.peers;

import java.util.HashMap;

import javax.media.opengl.GL;

import com.ferox.core.renderer.RenderManager;
import com.ferox.core.renderer.RenderToTexturePass;
import com.ferox.core.states.NumericUnit;
import com.ferox.core.states.atoms.TextureData;
import com.ferox.core.states.atoms.TextureCubeMap.Face;
import com.ferox.core.util.FeroxException;
import com.ferox.impl.jsr231.JOGLRenderContext;
import com.ferox.impl.jsr231.peers.JOGLRenderToTexturePassPeer.RTTPeer;

public class FBOPeer implements RTTPeer {
	private static class TextureAttachment {
		int slice;
		Face face;
		int texID;
		int target;
		TextureRecord prev;
	}
	
	private static class RenderBuffer {
		int id;
		int width, height;
	}
	
	private static class FBOPassImpl {
		int fboId;
		int[] colorBuffers;
		RenderBuffer stencil;
		RenderBuffer depth;
		TextureAttachment depthT;
		TextureAttachment[] colors;
		int contextVersion;
	}
	
	private HashMap<RenderToTexturePass, FBOPassImpl> fboRecord;
	
	public FBOPeer() {
		this.fboRecord = new HashMap<RenderToTexturePass, FBOPassImpl>();
	}
	
	public void finish(RenderToTexturePass pass, JOGLRenderContext context) {
		FBOPassImpl fbo = this.fboRecord.get(pass);
		if (fbo != null)
			restoreFBO(fbo, context);
	}

	public void prepare(RenderToTexturePass pass, JOGLRenderContext context) throws FeroxException {
		FBOPassImpl fbo = this.fboRecord.get(pass);
		if (fbo == null || context.getContextVersion() > fbo.contextVersion) {
			fbo = createFBO(context.getGL(), context.getContextVersion());
			this.fboRecord.put(pass, fbo);
		}
		applyFBO(pass, fbo, context);
	}
	
	private static void restoreFBO(FBOPassImpl fbo, JOGLRenderContext context) {
		context.getGL().glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, 0);
	}
	
	private static void applyFBO(RenderToTexturePass pass, FBOPassImpl fbo, JOGLRenderContext context) throws FeroxException {
		int width = pass.getWidth();
		int height = pass.getHeight();
		GL gl = context.getGL();
		gl.glBindFramebufferEXT(GL.GL_FRAMEBUFFER_EXT, fbo.fboId);
		applyDepthAttachment(pass, fbo, width, height, context);
		applyStencilAttachment(pass, fbo, width, height, context);
		applyColorAttachments(pass, fbo, context);
		
		int error = gl.glCheckFramebufferStatusEXT(GL.GL_FRAMEBUFFER_EXT);
		if (error != GL.GL_FRAMEBUFFER_COMPLETE_EXT) {
			String msg = "FBO failed completion test, unable to render";
			switch(error) {
			case GL.GL_FRAMEBUFFER_INCOMPLETE_ATTACHMENT_EXT: msg = "FBO attachments aren't complete"; break;
			case GL.GL_FRAMEBUFFER_INCOMPLETE_MISSING_ATTACHMENT_EXT: msg = "FBO needs at least one attachment"; break;
			case GL.GL_FRAMEBUFFER_INCOMPLETE_DRAW_BUFFER_EXT: msg = "FBO draw buffers improperly enabled"; break;
			case GL.GL_FRAMEBUFFER_INCOMPLETE_READ_BUFFER_EXT: msg = "FBO read buffer improperly enabled"; break;
			case GL.GL_FRAMEBUFFER_UNSUPPORTED_EXT: msg = "Texture attachment formats aren't supported by this vendor"; break;
			}
			throw new FeroxException(msg);
		}
	}
	
	private static void applyColorAttachments(RenderToTexturePass pass, FBOPassImpl fbo, JOGLRenderContext context) {
		GL gl = context.getGL();
		TextureData color;
		Face face;
		int slice;
		TextureAttachment tex;
		int bCount = 0;
		for (int i = 0; i < fbo.colors.length; i++) {
			color = pass.getColorBinding(i);
			tex = fbo.colors[i];
			if (color == null) {
				if (tex != null) {
					attachTexture(0, tex.target, tex.face, tex.slice, GL.GL_COLOR_ATTACHMENT0_EXT + i, gl);
					fbo.colors[i] = null;
				}
			} else {
				face = pass.getColorBindingFace(i);
				slice = pass.getColorBindingSlice(i);
				TextureRecord r = getRecord(color, context);
				if (tex == null) {
					tex = new TextureAttachment();
					updateTextureAttachment(tex, r, face, slice, GL.GL_COLOR_ATTACHMENT0_EXT + i, context);
					fbo.colors[i] = tex;
				} else if (tex.prev != r || r.target != tex.target || tex.slice != slice || tex.face != face) {
					if (r.target != tex.target)
						attachTexture(0, tex.target, tex.face, tex.slice, GL.GL_COLOR_ATTACHMENT0_EXT + i, gl);
					updateTextureAttachment(tex, r, face, slice, GL.GL_COLOR_ATTACHMENT0_EXT + i, context);
				}
				
				fbo.colorBuffers[bCount] = GL.GL_COLOR_ATTACHMENT0_EXT + i;
				bCount++;
			}
		}
		if (bCount == 0) {
			gl.glDrawBuffer(GL.GL_NONE);
			gl.glReadBuffer(GL.GL_NONE);
		} else {
			gl.glDrawBuffers(bCount, fbo.colorBuffers, 0);
			gl.glReadBuffer(fbo.colorBuffers[0]);
		}
	}
	
	private static void updateTextureAttachment(TextureAttachment tex, TextureRecord data, Face face, int slice, int attachment, JOGLRenderContext context) {
		tex.face = face;
		tex.slice = slice;
		
		tex.prev = data;
		tex.texID = data.texID;
		tex.target = data.target;
		attachTexture(tex.texID, tex.target, face, slice, attachment, context.getGL());
	}
	
	private static void updateRenderBufferAttachment(RenderBuffer rb, int width, int height, int attachment, int type, GL gl) {
		boolean attach = false;
		if (rb.id <= 0) {
			attach = true;
			rb.id = createRenderBuffer(gl);
		}
		rb.width = width;
		rb.height = height;
		bindRenderBuffer(rb.id, gl);
		renderBufferStorage(width, height, type, gl);
		if (attach)
			attachRenderBuffer(rb.id, attachment, gl);
		bindRenderBuffer(0, gl);
	}
	
	private static void applyStencilAttachment(RenderToTexturePass pass, FBOPassImpl fbo, int width, int height, JOGLRenderContext context) {
		GL gl = context.getGL();
		if (fbo.stencil == null) {
			if (pass.isStencilBufferUsed()) {
				fbo.stencil = new RenderBuffer();
				updateRenderBufferAttachment(fbo.stencil, width, height, GL.GL_STENCIL_ATTACHMENT_EXT, GL.GL_STENCIL_INDEX, gl);
			}
		} else {
			if (pass.isStencilBufferUsed()) {
				if (fbo.stencil.width != width || fbo.stencil.height != height) {
					updateRenderBufferAttachment(fbo.stencil, width, height, GL.GL_STENCIL_ATTACHMENT_EXT, GL.GL_STENCIL_INDEX, gl);
				}
			} else {
				attachRenderBuffer(0, GL.GL_STENCIL_ATTACHMENT_EXT, gl);
				deleteRenderBuffer(fbo.stencil.id, gl);
				fbo.stencil = null;
			}
		}
	}
	
	private static void applyDepthAttachment(RenderToTexturePass pass, FBOPassImpl fbo, int width, int height, JOGLRenderContext context) {
		TextureData depth = pass.getDepthBinding();
		GL gl = context.getGL();
		if (depth == null) {
			if (fbo.depth == null) {
				if (fbo.depthT != null) {
					attachTexture(0, fbo.depthT.target, fbo.depthT.face, fbo.depthT.slice, GL.GL_DEPTH_ATTACHMENT_EXT, gl);
					fbo.depthT = null;
				}
				fbo.depth = new RenderBuffer();
				updateRenderBufferAttachment(fbo.depth, width, height, GL.GL_DEPTH_ATTACHMENT_EXT, GL.GL_DEPTH_COMPONENT, gl);
			} else if (fbo.depth.width != width || fbo.depth.height != height) {
				updateRenderBufferAttachment(fbo.depth, width, height, GL.GL_DEPTH_ATTACHMENT_EXT, GL.GL_DEPTH_COMPONENT, gl);
			}
		} else {
			TextureRecord r = getRecord(depth, context);
			if (fbo.depthT == null) {
				if (fbo.depth != null) {
					attachRenderBuffer(0, GL.GL_DEPTH_ATTACHMENT_EXT, gl);
					deleteRenderBuffer(fbo.depth.id, gl);
					fbo.depth = null;
				}
				fbo.depthT = new TextureAttachment();
				updateTextureAttachment(fbo.depthT, r, pass.getDepthBindingFace(), pass.getDepthBindingSlice(), GL.GL_DEPTH_ATTACHMENT_EXT, context);
			} else {
				Face face = pass.getDepthBindingFace();
				int slice = pass.getDepthBindingSlice();
				if (fbo.depthT.prev != r || r.target != fbo.depthT.target || fbo.depthT.slice != slice || fbo.depthT.face != face) {
					if (r.target != fbo.depthT.target)
						attachTexture(0, fbo.depthT.target, fbo.depthT.face, fbo.depthT.slice, GL.GL_DEPTH_ATTACHMENT_EXT, gl);
					updateTextureAttachment(fbo.depthT, r, face, slice, GL.GL_DEPTH_ATTACHMENT_EXT, context);
				}
			}
		}
	}
	
	private static TextureRecord getRecord(TextureData data, JOGLRenderContext context) {
		RenderManager rm = context.getRenderManager();
		NumericUnit u = NumericUnit.get(0);
		TextureData prev = (TextureData)context.getActiveStateAtom(TextureData.class, u);
		data.applyState(rm, u);
		TextureRecord r = (TextureRecord)data.getStateRecord(rm);
		if (prev != null)
			prev.applyState(rm, u);
		else
			data.restoreState(rm, u);
		return r;
	}
	
	private static void attachTexture(int id, int target, Face face, int slice, int attachment, GL gl) {
		switch(target) {
		case GL.GL_TEXTURE_2D: case GL.GL_TEXTURE_RECTANGLE_ARB:
			gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, attachment, target, id, 0);
			break;
		case GL.GL_TEXTURE_3D:
			gl.glFramebufferTexture3DEXT(GL.GL_FRAMEBUFFER_EXT, attachment, GL.GL_TEXTURE_3D, id, 0, slice);
			break;
		case GL.GL_TEXTURE_CUBE_MAP:
			int cubetarget = 0;
			switch(face) {
			case PX: cubetarget = GL.GL_TEXTURE_CUBE_MAP_POSITIVE_X; break;
			case NX: cubetarget = GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_X; break;
			case PY: cubetarget = GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Y; break;
			case NY: cubetarget = GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y; break;
			case PZ: cubetarget = GL.GL_TEXTURE_CUBE_MAP_POSITIVE_Z; break;
			case NZ: cubetarget = GL.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z; break;
			}
			gl.glFramebufferTexture2DEXT(GL.GL_FRAMEBUFFER_EXT, attachment, cubetarget, id, 0);
			break;
		}
	}

	private static int createRenderBuffer(GL gl) {
		int[] id = new int[1];
		gl.glGenRenderbuffersEXT(1, id, 0);
		return id[0];
	}
	
	private static void deleteRenderBuffer(int rid, GL gl) {
		gl.glDeleteRenderbuffersEXT(1, new int[] {rid}, 0);
	}
	
	private static void attachRenderBuffer(int rid, int attach, GL gl) {
		gl.glFramebufferRenderbufferEXT(GL.GL_FRAMEBUFFER_EXT, attach, GL.GL_RENDERBUFFER_EXT, rid);
	}
	
	private static void bindRenderBuffer(int rid, GL gl) {
		gl.glBindRenderbufferEXT(GL.GL_RENDERBUFFER_EXT, rid);
	}
	
	private static void renderBufferStorage(int width, int height, int type, GL gl) {
		gl.glRenderbufferStorageEXT(GL.GL_RENDERBUFFER_EXT, type, width, height);
	}
	
	private static FBOPassImpl createFBO(GL gl, int version) {
		int[] id = new int[1];
		gl.glGenFramebuffersEXT(1, id, 0);
		
		FBOPassImpl fbo = new FBOPassImpl();
		fbo.fboId = id[0];
		
		fbo.depth = null;
		fbo.depthT = null;
		fbo.stencil = null;
		fbo.colors = new TextureAttachment[RenderToTexturePass.getMaxColorAttachments()];
		fbo.colorBuffers = new int[fbo.colors.length];
		fbo.contextVersion = version;
		return fbo;
	}
}
