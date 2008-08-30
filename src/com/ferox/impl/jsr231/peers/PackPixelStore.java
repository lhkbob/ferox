package com.ferox.impl.jsr231.peers;

import javax.media.opengl.GL;

class PackPixelStore {
	int rowLength;
	int skipRows;
	int skipPixels;
	int alignment;
	int imageHeight;
	int skipImages;
	boolean swapBytes;
	boolean lsbFirst;
		
	private PackPixelStore() {
		
	}
		
	public static PackPixelStore get(GL gl) {
		PackPixelStore p = new PackPixelStore();
		int[] s = new int[1];
			
		gl.glGetIntegerv(GL.GL_PACK_ROW_LENGTH, s, 0);
		p.rowLength = s[0];
		gl.glGetIntegerv(GL.GL_PACK_SKIP_ROWS, s, 0);
		p.skipRows = s[0];
		gl.glGetIntegerv(GL.GL_PACK_SKIP_PIXELS, s, 0);
		p.skipPixels = s[0];
		gl.glGetIntegerv(GL.GL_PACK_ALIGNMENT, s, 0);
		p.alignment = s[0];
		gl.glGetIntegerv(GL.GL_PACK_IMAGE_HEIGHT, s, 0);
		p.imageHeight = s[0];
		gl.glGetIntegerv(GL.GL_PACK_SKIP_IMAGES, s, 0);
		p.skipImages = s[0];
			
		byte[] b = new byte[1];
			
		gl.glGetBooleanv(GL.GL_PACK_SWAP_BYTES, b, 0);
		p.swapBytes = b[0] != 0;
		gl.glGetBooleanv(GL.GL_PACK_LSB_FIRST, b, 0);
		p.lsbFirst = b[0] != 0;
			
		return p;
	}
		
	public void set(GL gl) {
		gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES, (this.swapBytes ? GL.GL_TRUE : GL.GL_FALSE));
		gl.glPixelStorei(GL.GL_PACK_LSB_FIRST, (this.lsbFirst ? GL.GL_TRUE : GL.GL_FALSE));
		gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH, this.rowLength);
		gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS, this.skipRows);
		gl.glPixelStorei(GL.GL_PACK_SKIP_PIXELS, this.skipPixels);
		gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, this.alignment);
		gl.glPixelStorei(GL.GL_PACK_IMAGE_HEIGHT, this.imageHeight);
		gl.glPixelStorei(GL.GL_PACK_SKIP_IMAGES, this.skipImages);
	}
		
	public static void setUseful(GL gl) {
		gl.glPixelStorei(GL.GL_PACK_SWAP_BYTES, GL.GL_FALSE);
		gl.glPixelStorei(GL.GL_PACK_LSB_FIRST, GL.GL_FALSE);
		gl.glPixelStorei(GL.GL_PACK_ROW_LENGTH, 0);
		gl.glPixelStorei(GL.GL_PACK_SKIP_ROWS, 0);
		gl.glPixelStorei(GL.GL_PACK_SKIP_PIXELS, 0);
		gl.glPixelStorei(GL.GL_PACK_ALIGNMENT, 1);
		gl.glPixelStorei(GL.GL_PACK_IMAGE_HEIGHT, 0);
		gl.glPixelStorei(GL.GL_PACK_SKIP_IMAGES, 0);
	}
}