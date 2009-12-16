package com.ferox.renderer.impl.jogl;

import javax.media.opengl.GL2GL3;

public class BoundObjectState {
	private int activeTexture;
	
	private int[] textures;
	private int[] boundTargets;
	
	private int arrayVbo;
	private int elementVbo;
	
	private int fbo;
	
	public BoundObjectState(int numTextures) {
		if (numTextures < 0)
			throw new IllegalArgumentException("numTextures must be at least 0, not: " + numTextures);
		
		activeTexture = 0;
		textures = new int[numTextures];
		boundTargets = new int[numTextures];
		
		arrayVbo = 0;
		elementVbo = 0;
		
		fbo = 0;
	}
	
	public int getArrayVbo() {
		return arrayVbo;
	}
	
	public int getElementVbo() {
		return elementVbo;
	}
	
	public int getActiveTexture() {
		return activeTexture;
	}
	
	public int getFbo() {
		return fbo;
	}
	
	public int getTexture(int tex) {
		return textures[tex];
	}
	
	public int getTextureTarget(int tex) {
		return boundTargets[tex];
	}
	
	public void bindArrayVbo(GL2GL3 gl, int vbo) {
		if (vbo != arrayVbo) {
			arrayVbo = vbo;
			gl.glBindBuffer(GL2GL3.GL_ARRAY_BUFFER, vbo);
		}
	}
	
	public void bindElementVbo(GL2GL3 gl, int vbo) {
		if (vbo != elementVbo) {
			elementVbo = vbo;
			gl.glBindBuffer(GL2GL3.GL_ELEMENT_ARRAY_BUFFER, vbo);
		}
	}
	
	public void setActiveTexture(GL2GL3 gl, int tex) {
		if (activeTexture != tex) {
			activeTexture = tex;
			gl.glActiveTexture(GL2GL3.GL_TEXTURE0 + tex);
		}
	}
	
	public void bindTexture(GL2GL3 gl, int target, int texId) {
		int prevTarget = boundTargets[activeTexture];
		int prevTex = textures[activeTexture];
		
		if (prevTex != texId) {
			if (prevTex != 0 && prevTarget != target) {
				// unbind old texture
				gl.glBindTexture(prevTarget, 0);
			}
			gl.glBindTexture(target, texId);
			
			boundTargets[activeTexture] = target;
			textures[activeTexture] = texId;
		}
	}
	
	public void bindFbo(GL2GL3 gl, int fboId) {
		if (fbo != fboId) {
			fbo = fboId;
			gl.glBindFramebuffer(GL2GL3.GL_FRAMEBUFFER, fboId);
		}
	}
}
