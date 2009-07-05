package com.ferox.renderer.impl.jogl.drivers;

import java.nio.FloatBuffer;

import javax.media.opengl.GL;
import javax.media.opengl.GL2;
import javax.media.opengl.fixedfunc.GLMatrixFunc;

import com.ferox.math.Matrix3f;
import com.ferox.math.Matrix4f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.renderer.View;
import com.ferox.renderer.impl.TransformDriver;
import com.ferox.renderer.impl.jogl.JoglContextManager;
import com.sun.opengl.util.BufferUtil;

/**
 * <p>
 * Provides a simple implementation of TransformDriver for use with the
 * JoglFramework.
 * </p>
 * <p>
 * Drivers that change the matrix mode must ensure that it is set back to
 * MODELVIEW before this driver is used again, or errors will occur.
 * </p>
 * <p>
 * When applying the transform state, this uses the push-pop mechanism. The view
 * portion of the modelview matrix is set, and then each model transform is
 * pushed on top of that. Thus when states are applied, the view transform will
 * be the current modelview matrix.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class JoglTransformDriver implements TransformDriver {
	// transform used to convert ferox view into jogl view
	private static final Transform convert = new Transform(new Vector3f(), 
														   new Matrix3f(-1, 0, 0, 0, 1, 0, 0, 0, -1));

	private final FloatBuffer matrix;

	private final Transform currentView;
	private boolean modelPushed;
	private final JoglContextManager factory;

	public JoglTransformDriver(JoglContextManager factory) {
		this.factory = factory;
		matrix = BufferUtil.newFloatBuffer(16);

		currentView = new Transform();
		modelPushed = false;
	}

	/**
	 * Returns the current "view" portion of the modelview matrix. This is
	 * useful state drivers that submit data that is effected by the current
	 * modelview matrix.
	 */
	public Transform getCurrentViewTransform() {
		return currentView;
	}

	/**
	 * Load the given matrix onto the current matrix stack, as determined by the
	 * given gl's matrix mode.
	 */
	public void loadMatrix(GLMatrixFunc gl, Transform t) {
		matrix.rewind();
		getOpenGLMatrix(t, matrix);
		gl.glLoadMatrixf(matrix);
	}

	/**
	 * Push and then multiply the current stack by t. Should be paired with
	 * gl.glPopMatrix().
	 */
	public void pushMatrix(GLMatrixFunc gl, Transform t) {
		gl.glPushMatrix();
		getOpenGLMatrix(t, matrix);
		gl.glMultMatrixf(matrix);
	}

	@Override
	public void setModelTransform(Transform transform) {
		if (!modelPushed) {
			pushMatrix(factory.getGL().getGL2(), transform);
			modelPushed = true;
		} else
			loadMatrix(factory.getGL().getGL2(), transform);
	}

	@Override
	public void resetModel() {
		if (modelPushed) {
			factory.getGL().getGL2().glPopMatrix();
			modelPushed = false;
		}
	}

	@Override
	public void setView(View view, int width, int height) {
		GL2 gl = factory.getGL().getGL2();

		resetModel();
		if (view != null) {
			// setup the viewport
			setViewport(gl, view.getViewLeft(), view.getViewRight(), 
						view.getViewTop(), view.getViewBottom(), width, height);

			// set the projection matrix
			gl.glMatrixMode(GL2.GL_PROJECTION);
			getOpenGLMatrix(view.getProjectionMatrix(), (FloatBuffer) matrix.rewind());
			gl.glLoadMatrixf(matrix);

			// set the view portion of the modelview matrix
			gl.glMatrixMode(GL2.GL_MODELVIEW);

			convert.mul(view.getViewTransform(), currentView);
			loadMatrix(gl, currentView);
		} else
			resetView();
	}

	@Override
	public void resetView() {
		factory.getGL().getGL2().glLoadIdentity();
		currentView.setIdentity();
	}

	/**
	 * Store the given transform into the float buffer, suitable for use with
	 * OpenGL calls. It loads the transform in at ogl's current position. It
	 * does not modify the position, and assumes that there are at least 16
	 * elements left in the buffer.
	 */
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

	/**
	 * Store the given 4x4 matrix into the float buffer, suitable for use with
	 * OpenGL calls. It loads the matrix in at ogl's current position. It does
	 * not modify the position, and assumes that there are at least 16 elements
	 * left in the buffer.
	 */
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

	private static void setViewport(GL gl, float left, float right, 
									float top, float bottom, int width, int height) {
		gl.glViewport((int) (left * width), (int) (bottom * height), 
					  (int) ((right - left) * width), (int) ((top - bottom) * height));
		gl.glScissor((int) (left * width), (int) (bottom * height), 
					 (int) ((right - left) * width), (int) ((top - bottom) * height));
	}
}
