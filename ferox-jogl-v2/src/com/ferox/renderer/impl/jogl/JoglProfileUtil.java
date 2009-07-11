package com.ferox.renderer.impl.jogl;

import java.nio.Buffer;

import javax.media.opengl.GL2ES2;

/**
 * <p>
 * There are a number of methods that are shared by GL2 and GL3 but
 * unfortunately have no common super-interface. Therefore, I cannot easily and
 * cleanly program to both interfaces in certain circumstances.
 * </p>
 * <p>
 * To solve this issue, this class statically provides shared methods that
 * switch between profiles automatically.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class JoglProfileUtil {
	public static void glReadBuffer(GL2ES2 gl, int buffer) {
		if (gl.isGL2()) {
			gl.getGL2().glReadBuffer(buffer);
		} else { // assume GL3
			gl.getGL3().glReadBuffer(buffer);
		}
	}
	
	public static void glDrawBuffer(GL2ES2 gl, int buffer) {
		if (gl.isGL2()) {
			gl.getGL2().glDrawBuffer(buffer);
		} else { // assume GL3
			gl.getGL3().glDrawBuffer(buffer);
		}
	}
	
	public static void glDrawBuffers(GL2ES2 gl, int length, int[] buffers, int offset) {
		if (gl.isGL2()) {
			gl.getGL2().glDrawBuffers(length, buffers, offset);
		} else { // assume GL3
			gl.getGL3().glDrawBuffers(length, buffers, offset);
		}
	}
	
	public static void glFramebufferTexture3D(GL2ES2 gl, int target, int attach, int textarget, int id, int level, int layer) {
		if (gl.isGL2()) {
			gl.getGL2().glFramebufferTexture3D(target, attach, textarget, id, level, layer);
		} else { // assume GL3
			gl.getGL3().glFramebufferTexture3D(target, attach, textarget, id, level, layer);
		}
	}
	
	public static void glFramebufferTexture1D(GL2ES2 gl, int target, int attach, int textarget, int id, int level) {
		if (gl.isGL2()) {
			gl.getGL2().glFramebufferTexture1D(target, attach, textarget, id, level);
		} else { // assume GL3
			gl.getGL3().glFramebufferTexture1D(target, attach, textarget, id, level);
		}
	}
	
	public static void glTexImage1D(GL2ES2 gl, int target, int layer, int dstFormat, int width,
								    int border, int srcFormat, int type, Buffer data) {
		if (gl.isGL2()) {
			gl.getGL2().glTexImage1D(target, layer, dstFormat, width, border, srcFormat, type, data);
		} else { // assume GL3
			gl.getGL3().glTexImage1D(target, layer, dstFormat, width, border, srcFormat, type, data);
		}
	}

	public static void glTexSubImage1D(GL2ES2 gl, int target, int layer, int xOffset, 
									   int width, int srcFormat, int type, Buffer data) {
		if (gl.isGL2()) {
			gl.getGL2().glTexSubImage1D(target, layer, xOffset, width, srcFormat, type, data);
		} else { // assumes GL3
			gl.getGL3().glTexSubImage1D(target, layer, xOffset, width, srcFormat, type, data);
		} 
	}

	public static void glTexImage3D(GL2ES2 gl, int target, int layer, int dstFormat, int width, int height, int depth, 
									int border, int srcFormat, int type, Buffer data) {
		if (gl.isGL2()) {
			gl.getGL2().glTexImage3D(target, layer, dstFormat, width, height, depth, border, srcFormat, type, data);
		} else { // assume GL3
			gl.getGL3().glTexImage3D(target, layer, dstFormat, width, height, depth, border, srcFormat, type, data);
		}
	}

	public static void glTexSubImage3D(GL2ES2 gl, int target, int layer, int xOffset, int yOffset, int zOffset,
									   int width, int height, int depth, int srcFormat, int type, Buffer data) {
		if (gl.isGL2()) {
			gl.getGL2().glTexSubImage3D(target, layer, xOffset, yOffset, zOffset, width, height, depth, srcFormat, type, data);
		} else { // assumes GL3
			gl.getGL3().glTexSubImage3D(target, layer, xOffset, yOffset, zOffset, width, height, depth, srcFormat, type, data);
		} 
	}
	
	public static void glCopyTexSubImage1D(GL2ES2 gl, int target, int level, int xOffset, int x, int y, int width) {
		if (gl.isGL2()) {
			gl.getGL2().glCopyTexSubImage1D(target, level, xOffset, x, y, width);
		} else { // assumes GL3
			gl.getGL3().glCopyTexSubImage1D(target, level, xOffset, x, y, width);
		}
	}
	
	public static void glCopyTexSubImage3D(GL2ES2 gl, int target, int level, int xOffset, int yOffset, int zOffset, int x, int y, int width, int height) {
		if (gl.isGL2()) {
			gl.getGL2().glCopyTexSubImage3D(target, level, xOffset, yOffset, zOffset, x, y, width, height);
		} else { // assumes GL3
			gl.getGL3().glCopyTexSubImage3D(target, level, xOffset, yOffset, zOffset, x, y, width, height);
		}
	}
}
