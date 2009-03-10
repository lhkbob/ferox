package com.ferox.renderer.impl.jogl.drivers;

import java.nio.FloatBuffer;

import javax.media.opengl.GL;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Matrix4f;
import org.openmali.vecmath.Vector3f;

import com.ferox.math.Transform;
import com.ferox.renderer.View;
import com.ferox.renderer.impl.TransformDriver;
import com.ferox.renderer.impl.jogl.JoglRenderSurface;
import com.ferox.renderer.impl.jogl.JoglSurfaceFactory;
import com.sun.opengl.util.BufferUtil;

/** Provides a simple implementation of TransformDriver for
 * use with the BasicJoglRenderer.
 * 
 * Drivers that change the matrix mode must ensure that it is
 * set back to MODELVIEW before this driver is used again,
 * or errors will occur.
 * 
 * To minimize the amount of gl operations (including
 * removing extraneous glPush and glPopMatrix() calls), the
 * modelview stack is effectively not used.  Instead, the current
 * view is multipled to the desired world transform, and then
 * a glLoadMatrixf() call should be used.  Compared to the overhead
 * of executing the jni gl call, the matrix multiplication is free.
 * 
 * @author Michael Ludwig
 *
 */
public class JoglTransformDriver implements TransformDriver {
	private final FloatBuffer matrix;
	
	private final Transform currentView;
	private final Transform modelView;
	
	private JoglSurfaceFactory factory;
	
	public JoglTransformDriver(JoglSurfaceFactory factory) {
		this.factory = factory;
		this.matrix = BufferUtil.newFloatBuffer(16);
		
		this.currentView = new Transform();
		this.modelView = new Transform();
	}
	
	/** Returns the current "view" portion of the modelview
	 * matrix.  This is useful state drivers that submit data
	 * that is effected by the current modelview matrix. */
	public Transform getCurrentViewTransform() {
		return this.currentView;
	}
	
	/** Load the given matrix onto the current matrix
	 * stack, as determined by the given gl's matrix mode. */
	public void loadMatrix(GL gl, Transform t) {
		this.matrix.rewind();
		getOpenGLMatrix(t, this.matrix);
		gl.glLoadMatrixf(this.matrix);
	}
	
	@Override
	public void setModelTransform(Transform transform) {
		GL gl = this.factory.getGL();
			
		this.modelView.mul(this.currentView, transform);
		this.loadMatrix(gl, this.modelView);
	}
	
	@Override
	public void resetModel() {
		// do nothing
	}

	@Override
	public void setView(View view) {
		GL gl = this.factory.getGL();
		JoglRenderSurface current = this.factory.getCurrentSurface();
		// setup the viewport
		setViewport(gl, view.getViewLeft(), view.getViewRight(), view.getViewTop(), view.getViewBottom(), current.getWidth(), current.getHeight());
		
		// set the projection matrix
		gl.glMatrixMode(GL.GL_PROJECTION);
		getOpenGLMatrix(view.getProjectionMatrix(), (FloatBuffer) this.matrix.rewind());
		gl.glLoadMatrixf(this.matrix);
		
		// set the view portion of the modelview matrix
		gl.glMatrixMode(GL.GL_MODELVIEW);
		this.currentView.set(view.getViewTransform());
	}

	@Override
	public void resetView() {
		this.currentView.setIdentity();
	}
	
	/** Store the given transform into the float buffer, suitable for use
	 * with OpenGL calls.  It loads the transform in at ogl's current position.
	 * It does not modify the position, and assumes that there are at least 16 
	 * elements left in the buffer. */
	public static void getOpenGLMatrix(Transform transform, FloatBuffer ogl) {
		int pos = ogl.position();
		Matrix3f rot = transform.getRotation();
		Vector3f trans = transform.getTranslation();
		float scale = transform.getScale();
		
		ogl.put(pos + 0, rot.m00 * scale);
		ogl.put(pos + 1, rot.m10 * scale);
		ogl.put(pos + 2, rot.m20 * scale);
		ogl.put(pos + 3, 0f);
		
		ogl.put(pos + 4, rot.m01 * scale);
		ogl.put(pos + 5, rot.m11 * scale);
		ogl.put(pos + 6, rot.m21 * scale);
		ogl.put(pos + 7, 0f);
		
		ogl.put(pos + 8, rot.m02 * scale);
		ogl.put(pos + 9, rot.m12 * scale);
		ogl.put(pos + 10, rot.m22 * scale);
		ogl.put(pos + 11, 0f);
		
		ogl.put(pos + 12, trans.x);
		ogl.put(pos + 13, trans.y);
		ogl.put(pos + 14, trans.z);
		ogl.put(pos + 15, 1f);
	}
	
	/** Store the given 4x4 matrix into the float buffer, suitable for use
	 * with OpenGL calls.  It loads the matrix in at ogl's current position.
	 * It does not modify the position, and assumes that there are at least 16 
	 * elements left in the buffer. */
	public static void getOpenGLMatrix(Matrix4f matrix, FloatBuffer ogl) {
		int pos = ogl.position();

		ogl.put(pos + 0, matrix.m00);
		ogl.put(pos + 1, matrix.m10);
		ogl.put(pos + 2, matrix.m20);
		ogl.put(pos + 3, matrix.m30);

		ogl.put(pos + 4, matrix.m01);
		ogl.put(pos + 5, matrix.m11);
		ogl.put(pos + 6, matrix.m21);
		ogl.put(pos + 7, matrix.m31);

		ogl.put(pos + 8, matrix.m02);
		ogl.put(pos + 9, matrix.m12);
		ogl.put(pos + 10, matrix.m22);
		ogl.put(pos + 11, matrix.m32);

		ogl.put(pos + 12, matrix.m03);
		ogl.put(pos + 13, matrix.m13);
		ogl.put(pos + 14, matrix.m23);
		ogl.put(pos + 15, matrix.m33);
	}
	
	private static void setViewport(GL gl, float left, float right, float top, float bottom, int width, int height) {
		gl.glViewport((int) (left * width), (int) (bottom * height), (int)((right - left) * width), (int)((top - bottom) * height));
		gl.glScissor((int) (left * width), (int) (bottom * height), (int)((right - left) * width), (int)((top - bottom) * height));
	}
}
