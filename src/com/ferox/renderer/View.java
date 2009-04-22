package com.ferox.renderer;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Matrix4f;
import org.openmali.vecmath.Vector3f;

import com.ferox.math.Plane;
import com.ferox.math.Transform;

/** 
 * View represents the camera that a scene is rendered through.  It holds onto the
 * type and specifics of the projection, and its location and orientation.  The
 * visible region of a View is a 3D frustum.  It provides functionality to get the
 * planes of the frustum in world space.
 * 
 * The View uses a right-handed coordinate system that looks down its negative,
 * local z-axis.
 * 
 * @author Michael Ludwig
 *
 */
public class View {
	/** Result of a frustum test against a BoundVolume. */
	public static enum FrustumIntersection {
		INSIDE, OUTSIDE, INTERSECT
	}
	
	public static final int NUM_PLANES = 6;
	
	public static final int NEAR_PLANE = 0;
	public static final int FAR_PLANE = 1;
	public static final int TOP_PLANE = 2;
	public static final int BOTTOM_PLANE = 3;
	public static final int LEFT_PLANE = 4;
	public static final int RIGHT_PLANE = 5;
	
	public static final int NEAR_PLANE_BIT = (1 << NEAR_PLANE);
	public static final int FAR_PLANE_BIT = (1 << FAR_PLANE);
	public static final int TOP_PLANE_BIT = (1 << TOP_PLANE);
	public static final int BOTTOM_PLANE_BIT = (1 << BOTTOM_PLANE);
	public static final int LEFT_PLANE_BIT = (1 << LEFT_PLANE);
	public static final int RIGHT_PLANE_BIT = (1 << RIGHT_PLANE);
	
	private static final ThreadLocal<Vector3f> r = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> u = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	
	private final Matrix4f projection;
	private boolean useOrtho;
	
	private float frustumLeft;
	private float frustumRight;
	private float frustumTop;
	private float frustumBottom;
	private float frustumNear;
	private float frustumFar;
	
	private float viewLeft;
	private float viewRight;
	private float viewTop;
	private float viewBottom;
	
	private final Vector3f location;
	private final Vector3f up;
	private final Vector3f direction;
	
	private int planeState;
	
	private final Plane[] worldPlanes;
	private final Transform viewTrans;
	
	/** Construct a view located at the origin, looking down the positive z-axis.
	 * Sets the projection as setPerspective(60f, 1f, 1f, 100f). */
	public View() {
		this.projection = new Matrix4f();
		
		this.worldPlanes = new Plane[6];
		this.viewTrans = new Transform();
		
		this.location = new Vector3f();
		this.up = new Vector3f(0f, 1f, 0f);
		this.direction = new Vector3f(0f, 0f, -1f);
		
		this.setViewPort(0f, 1f, 0f, 1f);
		this.setPerspective(60f, 1f, 1f, 100f);
	}
	
	/** Get the current plane state of this view.  See setPlaneState(...) for more. */
	public int getPlaneState() {
		return this.planeState;
	}
	
	/** planeBits is a bitwise OR of the PLANE_BITS static variables.  If a bit is
	 * set, signals this view that it's unnecessary to test that plane when testing bound intersection.
	 * This is used when traversing a scene to efficiently elimate plane tests. */
	public void setPlaneState(int planeBits) {
		this.planeState = planeBits;
	}
	
	/** Return the transform that converts from "world" coordinates
	 * into view coordinates.  This instance should not be modified.
	 * 
	 * This will be stale if the location, direction, and up vectors
	 * or the projection matrix are changed without a subsequent 
	 * call to updateView(). */
	public Transform getViewTransform() {
		return this.viewTrans;
	}
	
	/** Return a plane representing the given plane of the view frustum,
	 * in world coordinates.  This plane should not be modified.
	 * 
	 * Throws an exception if plane isn't one of the defined PLANE constants
	 * (which are 0 - 5).
	 * 
	 * This will be stale if the location, direction, and up vectors
	 * or the projection matrix are changed without a subsequent 
	 * call to updateView(). It may return null if it is stale. */
	public Plane getWorldPlane(int plane) throws ArrayIndexOutOfBoundsException {
		return this.worldPlanes[plane];
	}
	
	/** Update the Transform returned by getViewTransform() and the Planes returned
	 * by getWorldPlane() to reflect any changes to the View's location, direction,
	 * up vector and projection matrix.
	 * 
	 * This also resets the view's plane state to 0 (or all planes must be tested
	 * in the frustum).
	 * 
	 * This must be called before the view is used by a render pass in the renderer. */
	public void updateView() {
		Vector3f r = View.r.get();
		Vector3f u = View.u.get();
		
		// compute the right-handed basis vectors of the view
		this.direction.normalize();
		this.up.normalize();
		r.cross(this.direction, this.up);
		r.normalize();
		u.cross(r, this.direction);
		u.normalize();
		
		// update viewTrans to the basis and possibly new location
		Matrix3f m = this.viewTrans.getRotation();
		m.setColumn(0, r.x, r.y, r.z);
		m.setColumn(1, u.x, u.y, u.z);
		m.setColumn(2, -this.direction.x, -this.direction.y, -this.direction.z);
		this.viewTrans.setTranslation(this.location);
		this.viewTrans.setScale(1f);
		
		// convert the ATM camera planes into world space
		this.computeCameraPlanes();
		for (int i = 0; i < this.worldPlanes.length; i++) {
			// at this point, viewTrans is actual viewTrans^-1 (or the world transform)
			this.worldPlanes[i].transform(this.viewTrans);
		}
		
		// invert the world transform to get the view transform
		this.viewTrans.inverse();
		
		this.planeState = 0;
	}
	
	/** Get the location vector of this view, in world space. */
	public Vector3f getLocation() {
		return location;
	}

	/** Get the location vector of this view, in world space. If loc is null, this 
	 * view's location is set to the origin. */
	public void setLocation(Vector3f loc) throws NullPointerException {
		if (loc == null)
			this.location.set(0f, 0f, 0f);
		else
			this.location.set(loc);
	}	
	
	/** Get the up vector of this view, in world space.  Together up and direction
	 * form a right-handed coordinate system. */
	public Vector3f getUp() {
		return up;
	}

	/** Set the up vector of this view, in world space.  If up is null, up is set to <0, 1, 0>. */
	public void setUp(Vector3f up) {
		if (up == null)
			this.up.set(0f, 1f, 0f);
		else
			this.up.set(up);
	}
	
	/** Get the direction vector of this view, in world space.  Together up and direction
	 * form a right-handed coordinate system. */
	public Vector3f getDirection() {
		return direction;
	}

	/** Set the direction vector of this view, in world space.  If up is null, up is set to <0, 0, 1>. */
	public void setDirection(Vector3f dir) {
		if (dir == null)
			this.direction.set(0f, 0f, 1f);
		else
			this.direction.set(dir);
	}
	
	/** Returns the projection matrix for this view.  Modifying the values stored within will produce 
	 * undefined results when rendering. */
	public Matrix4f getProjectionMatrix() {
		return this.projection;
	}
	
	/** Set the frustum to be frustum with the given field of view (in degrees).  Widths and heights
	 * are calculated using the assumed aspect ration and near and far values.  
	 * Because perspective transforms only make sense for non-orthographic projections, it
	 * also sets this view to be non-orthographic.  Fails if near > far or if near <= 0. */
	public void setPerspective(float fov, float aspect, float near, float far) throws IllegalArgumentException, IllegalStateException {
		float h = (float)Math.tan(Math.toRadians(fov)) * near * .5f;
		float w = h * aspect;
		this.setFrustum(-w, w, -h, h, near, far);
		this.setOrthogonalProjection(false);
	}
	
	/** Get the left edge of the near frustum plane. See setFrustum(...) for more details. */
	public float getFrustumLeft() {
		return this.frustumLeft;
	}

	/** Get the right edge of the near frustum plane. See setFrustum(...) for more details. */
	public float getFrustumRight() {
		return this.frustumRight;
	}

	/** Get the top edge of the near frustum plane. See setFrustum(...) for more details. */
	public float getFrustumTop() {
		return this.frustumTop;
	}

	/** Get the bottom edge of the near frustum plane. See setFrustum(...) for more details. */
	public float getFrustumBottom() {
		return this.frustumBottom;
	}

	/** Get the distance to the near frustum plane from the origin, in camera coords. See setFrustum(...) for more details. */
	public float getFrustumNear() {
		return this.frustumNear;
	}

	/** Get the distance to the far frustum plane from the origin, in camera coords. See setFrustum(...) for more details. */
	public float getFrustumFar() {
		return this.frustumFar;
	}

	/** Sets the dimensions of the viewing frustum in camera coords.  left, right, bottom, and top specify edges of the
	 * rectangular near plane.  This plane is positioned perpendicular to the viewing direction, a distance near along the
	 * direction vector from the view's location.  If this view is using orthogonal projection, the frustum is a rectangular
	 * prism extending from this near plane, out to an identically sized plane, that is distance far away.  If not, the
	 * far plane is the far extent of a pyramid with it's point at the origin (camera coords), truncated at the near plane.
	 * 
	 * Fails if left > right, bottom > top, near > far, or if near < 0 (when using perspective projection). */
	public void setFrustum(float left, float right, float bottom, float top, float near, float far) throws IllegalArgumentException {
		if (left > right || bottom > top || near > far)
			throw new IllegalArgumentException("Frustum values would create an invalid frustum: " + left + " " + right + " x " + bottom + " " + top + " x " + near + " " + far);
		if (near <= 0 && !this.useOrtho)
			throw new IllegalArgumentException("Illegal value for near frustum when using perspective projection: " + near);
		
		this.frustumLeft = left;
		this.frustumRight = right;
		this.frustumBottom = bottom;
		this.frustumTop = top;
		this.frustumNear = near;
		this.frustumFar = far;
		
		this.computeProjectionMatrix();
	}
	
	/** Whether or not this view uses a perspective or orthogonal projection. */
	public boolean isOrthogonalProjection() {
		return this.useOrtho;
	}
	
	/** Set whether or not to use orthogonal projection.  If ortho is false and the near frustum plane is <= 0, then
	 * this will throw an exception. */
	public void setOrthogonalProjection(boolean ortho) throws IllegalStateException {
		if (!ortho && this.frustumNear <= 0)
			throw new IllegalStateException("Calling setOrthogonalProjection(false) when near frustum distance <= 0 is illegal");
		
		if (this.useOrtho != ortho) {
			this.useOrtho = ortho;
			this.computeProjectionMatrix();
		}
	}
	
	/** Get the left edge of the viewport, as a fraction of the width measured from the left side of the frame.
	 * Default value is 0. */
	public float getViewLeft() {
		return this.viewLeft;
	}

	/** Get the right edge of the viewport, as a fraction of the width measured from the left side of the frame.
	 * Default value is 1. */
	public float getViewRight() {
		return this.viewRight;
	}
	
	/** Get the top edge of the viewport, as a fraction of the height measured from the bottom side of the frame.
	 * Default value is 1. */
	public float getViewTop() {
		return this.viewTop;
	}

	/** Get the bottom edge of the viewport, as a fraction of the height measured from the bottom side of the frame.
	 * Default value is 0. */
	public float getViewBottom() {
		return this.viewBottom;
	}

	/** Set the dimensions of the viewport, as fractions of the width and height of a frame.  Left and right are measured
	 * from the left edge, top and bottom are measured from the bottom edge. Fails any value is outside of [0, 1], or if
	 * top < bottom, or if left > right. */
	public void setViewPort(float left, float right, float bottom, float top) throws IllegalArgumentException {
		if (left < 0 || left > 1)
			throw new IllegalArgumentException("Illegal value for left viewport edge: " + left);
		if (right < 0 || right > 1)
			throw new IllegalArgumentException("Illegal value for right viewport edge: " + right);
		if (bottom < 0 || bottom > 1)
			throw new IllegalArgumentException("Illegal value for bottom viewport edge: " + bottom);
		if (top < 0 || top > 1)
			throw new IllegalArgumentException("Illegal value for top viewport edge: " + top);
		if (left > right || bottom > top)
			throw new IllegalArgumentException("Viewport edges are invalid: " + left + " " + right + " x " + bottom + " " + top);
		this.viewBottom = bottom;
		this.viewLeft = left;
		this.viewRight = right;
		this.viewTop = top;
	}
	
	// compute the projection matrix
	private void computeProjectionMatrix() {		
		this.projection.setZero();
		if (this.useOrtho) {
			orthoMatrix(this.frustumRight, this.frustumLeft, this.frustumTop, this.frustumBottom, this.frustumNear, this.frustumFar, this.projection);
		} else {
			projMatrix(this.frustumRight, this.frustumLeft, this.frustumTop, this.frustumBottom, this.frustumNear, this.frustumFar, this.projection);
		}
	}
	
	// store the camera-space frustum planes into worldPlanes
	private void computeCameraPlanes() {
		this.setWorldPlane(RIGHT_PLANE, this.projection.m30 - this.projection.m00,
				this.projection.m31 - this.projection.m01,
				this.projection.m32 - this.projection.m02,
				this.projection.m33 - this.projection.m03);

		this.setWorldPlane(LEFT_PLANE, this.projection.m30 + this.projection.m00,
				this.projection.m31 + this.projection.m01,
				this.projection.m32 + this.projection.m02,
				this.projection.m33 + this.projection.m03);

		this.setWorldPlane(TOP_PLANE, this.projection.m30 - this.projection.m10,
				this.projection.m31 - this.projection.m11,
				this.projection.m32 - this.projection.m12,
				this.projection.m33 - this.projection.m13);

		this.setWorldPlane(BOTTOM_PLANE, this.projection.m30 + this.projection.m10,
				this.projection.m31 + this.projection.m11,
				this.projection.m32 + this.projection.m12,
				this.projection.m33 + this.projection.m13);

		this.setWorldPlane(FAR_PLANE, this.projection.m30 - this.projection.m20,
				this.projection.m31 - this.projection.m21,
				this.projection.m32 - this.projection.m22,
				this.projection.m33 - this.projection.m23);

		this.setWorldPlane(NEAR_PLANE, this.projection.m30 + this.projection.m20,
				this.projection.m31 + this.projection.m21,
				this.projection.m32 + this.projection.m22,
				this.projection.m33 + this.projection.m23);
	}
	
	// set the world plane at index plane, with the given values and normalize it
	private void setWorldPlane(int plane, float a, float b, float c, float d) {
		Plane cp = this.worldPlanes[plane];
		if (cp == null) {
			cp = new Plane(a, b, c, d);
			this.worldPlanes[plane] = cp;
		} else
			cp.setPlane(a, b, c, d);
		cp.normalize();
	}
	
	// computes an orthogonal projection matrix given the frustum values.
	private static void orthoMatrix(float fr, float fl, float ft, float fb, float fn, float ff, Matrix4f out) {
		out.m00 = 2f / (fr - fl);
		out.m11 = 2f / (ft - fb);
		out.m22 = 2f / (fn - ff);
		out.m33 = 1f;
		
		out.m03 = -(fr + fl) / (fr - fl);
		out.m13 = -(ft + fb) / (ft - fb);
		out.m23 = -(ff + fn) / (ff - fn);
	}
	
	// computes a perspective projection matrix given the frustum values.
	private static void projMatrix(float fr, float fl, float ft, float fb, float fn, float ff, Matrix4f out) {
		out.m00 = 2f * fn / (fr - fl);
		out.m11 = 2f * fn / (ft - fb);
		
		out.m02 = (fr + fl) / (fr - fl);
		out.m12 = (ft + fb) / (ft - fb);
		out.m22 = -(ff + fn) / (ff - fn);
		out.m32 = -1f;
		
		out.m23 = -2f * ff * fn / (ff - fn);
	}
}
