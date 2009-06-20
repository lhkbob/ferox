package com.ferox.renderer;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Matrix4f;
import org.openmali.vecmath.Vector3f;

import com.ferox.math.Plane;
import com.ferox.math.Transform;

/**
 * <p>
 * View represents the camera that a scene is rendered through. It holds onto
 * the type and specifics of the projection, and its location and orientation.
 * The visible region of a View is a 3D frustum. It provides functionality to
 * get the planes of the frustum in world space.
 * </p>
 * <p>
 * The View uses a right-handed coordinate system. With the default
 * configuration of the View, having an up vector of <0, 1, 0> and a direction
 * of <0, 0, -1>, the world coordinate space is as follows:
 * <ul>
 * <li>Positive X Axis: Horizontal, towards the right edge of the monitor.</li>
 * <li>Positive Y Axis: Vertical, towards the top of the monitor.</li>
 * <li>Positive Z Axis: Directly out of the monitor.</li>
 * </ul>
 * Keep in mind that a positive x change in view space causes the object to move
 * to the left, because the initial world direction is <0, 0, -1>.
 * </p>
 * 
 * @author Michael Ludwig
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

	private static final ThreadLocal<Vector3f> l = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> u = new ThreadLocal<Vector3f>() {
		@Override
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

	/**
	 * Construct a view located at the origin, looking down the negative z-axis.
	 * Sets the projection as setPerspective(60f, 1f, 1f, 100f).
	 */
	public View() {
		projection = new Matrix4f();

		worldPlanes = new Plane[6];
		viewTrans = new Transform();

		location = new Vector3f();
		up = new Vector3f(0f, 1f, 0f);
		direction = new Vector3f(0f, 0f, -1f);

		setViewPort(0f, 1f, 0f, 1f);
		setPerspective(60f, 1f, 1f, 100f);
	}

	/**
	 * Get the current plane state of this view. See setPlaneState(...) for
	 * more.
	 * 
	 * @return The current plane state of the view
	 */
	public int getPlaneState() {
		return planeState;
	}

	/**
	 * planeBits is a bitwise OR of the PLANE_BITS static variables. If a bit is
	 * set, signals this view that it's unnecessary to test that plane when
	 * testing bound intersection. This is used when traversing a scene to
	 * efficiently elimate plane tests.
	 * 
	 * @param planeBits The new plane state to use for subsequent
	 *            BoundVolume.testFrustum() calls
	 */
	public void setPlaneState(int planeBits) {
		planeState = planeBits;
	}

	/**
	 * <p>
	 * Return the transform that converts from "world" coordinates into view
	 * coordinates.
	 * </p>
	 * <p>
	 * This instance should not be modified. This will be stale if the location,
	 * direction, and up vectors or the projection matrix are changed without a
	 * subsequent call to updateView().
	 * </p>
	 * 
	 * @return The Transform instance going from world coordinates to view
	 *         coordinates
	 */
	public Transform getViewTransform() {
		return viewTrans;
	}

	/**
	 * <p>
	 * Return a plane representing the given plane of the view frustum, in world
	 * coordinates. This plane should not be modified.
	 * </p>
	 * <p>
	 * This will be stale if the location, direction, and up vectors or the
	 * projection matrix are changed without a subsequent call to updateView().
	 * It may return null if it is stale.
	 * </p>
	 * 
	 * @param plane The requested plane
	 * @return The Plane instance for the requested plane, in world coordinates
	 * @throws IndexOutOfBoundsException if plane isn't in [0, 5]
	 */
	public Plane getWorldPlane(int plane) {
		return worldPlanes[plane];
	}

	/**
	 * <p>
	 * Update the Transform returned by getViewTransform() and the Planes
	 * returned by getWorldPlane() to reflect any changes to the View's
	 * location, direction, up vector and projection matrix.
	 * </p>
	 * <p>
	 * This also resets the view's plane state to 0 (so all planes must be
	 * tested in the frustum).
	 * </p>
	 * <p>
	 * This must be called before the view is used by a render pass in the
	 * renderer.
	 * </p>
	 */
	public void updateView() {
		Vector3f left = View.l.get();
		Vector3f u = View.u.get();

		// compute the right-handed basis vectors of the view
		up.normalize();
		direction.normalize();

		left.cross(up, direction);
		left.normalize();
		u.cross(direction, left);
		u.normalize();

		// update viewTrans to the basis and possibly new location
		Matrix3f m = viewTrans.getRotation();
		m.setColumn(0, left.x, left.y, left.z);
		m.setColumn(1, u.x, u.y, u.z);
		m.setColumn(2, direction.x, direction.y, direction.z);

		viewTrans.setTranslation(location);
		viewTrans.setScale(1f);

		// invert the world transform to get the view transform
		viewTrans.inverse();

		if (useOrtho)
			computeOrthoWorldPlanes();
		else
			computePerspectiveWorldPlanes();

		planeState = 0;
	}

	/**
	 * Get the location vector of this view, in world space.
	 * 
	 * @return The location of the view
	 */
	public Vector3f getLocation() {
		return location;
	}

	/**
	 * Get the location vector of this view, in world space. If loc is null,
	 * this view's location is set to the origin.
	 * 
	 * @param loc New location for the view, null == origin
	 */
	public void setLocation(Vector3f loc) {
		if (loc == null)
			location.set(0f, 0f, 0f);
		else
			location.set(loc);
	}

	/**
	 * Get the up vector of this view, in world space. Together up and direction
	 * form a right-handed coordinate system.
	 * 
	 * @return The up vector of this view
	 */
	public Vector3f getUp() {
		return up;
	}

	/**
	 * Set the up vector of this view, in world space. If up is null, up is set
	 * to <0, 1, 0>.
	 * 
	 * @param up The new up vector for this view, null == <0, 1, 0>
	 */
	public void setUp(Vector3f up) {
		if (up == null)
			this.up.set(0f, 1f, 0f);
		else
			this.up.set(up);
	}

	/**
	 * <p>
	 * Get the direction vector of this view, in world space. Together up and
	 * direction form a right-handed coordinate system.
	 * </p>
	 * 
	 * @return The current direction that this camera is pointing
	 */
	public Vector3f getDirection() {
		return direction;
	}

	/**
	 * Set the direction vector of this view, in world space. If direction is
	 * null, direction is set to <0, 0, -1>.
	 * 
	 * @param dir The new direction for the view, null == <0, 0, -1>
	 */
	public void setDirection(Vector3f dir) {
		if (dir == null)
			direction.set(0f, 0f, -1f);
		else
			direction.set(dir);
	}

	/**
	 * Returns the projection matrix for this view. Modifying the values stored
	 * within will produce undefined results when rendering.
	 * 
	 * @return The projection matrix for this view
	 */
	public Matrix4f getProjectionMatrix() {
		return projection;
	}

	/**
	 * Set the frustum to be frustum with the given field of view (in degrees).
	 * Widths and heights are calculated using the assumed aspect ration and
	 * near and far values. Because perspective transforms only make sense for
	 * non-orthographic projections, it also sets this view to be
	 * non-orthographic.
	 * 
	 * @param fov The field of view
	 * @param aspect The aspect ratio of the view region (width / height)
	 * @param near The distance from the view's location to the near camera
	 *            plane
	 * @param far The distance from the view's location to the far camera plane
	 * @throws IllegalArgumentException if left > right, bottom > top, or near >
	 *             far, or if near <= 0
	 */
	public void setPerspective(float fov, float aspect, float near, float far) {
		float h = (float) Math.tan(Math.toRadians(fov)) * near * .5f;
		float w = h * aspect;
		useOrtho = false;
		setFrustum(-w, w, -h, h, near, far);
	}

	/**
	 * Get the left edge of the near frustum plane.
	 * 
	 * @see #setFrustum(float, float, float, float, float, float)
	 * @return The left edge of the near frustum plane
	 */
	public float getFrustumLeft() {
		return frustumLeft;
	}

	/**
	 * Get the right edge of the near frustum plane.
	 * 
	 * @see #setFrustum(float, float, float, float, float, float)
	 * @return The right edge of the near frustum plane
	 */
	public float getFrustumRight() {
		return frustumRight;
	}

	/**
	 * Get the top edge of the near frustum plane.
	 * 
	 * @see #setFrustum(float, float, float, float, float, float)
	 * @return The top edge of the near frustum plane
	 */
	public float getFrustumTop() {
		return frustumTop;
	}

	/**
	 * Get the bottom edge of the near frustum plane.
	 * 
	 * @see #setFrustum(float, float, float, float, float, float)
	 * @return The bottom edge of the near frustum plane
	 */
	public float getFrustumBottom() {
		return frustumBottom;
	}

	/**
	 * Get the distance to the near frustum plane from the origin, in camera
	 * coords.
	 * 
	 * @see #setFrustum(float, float, float, float, float, float)
	 * @return The distance to the near frustum plane
	 */
	public float getFrustumNear() {
		return frustumNear;
	}

	/**
	 * Get the distance to the far frustum plane from the origin, in camera
	 * coords.
	 * 
	 * @see #setFrustum(float, float, float, float, float, float)
	 * @return The distance to the far frustum plane
	 */
	public float getFrustumFar() {
		return frustumFar;
	}

	/**
	 * Sets the dimensions of the viewing frustum in camera coords. left, right,
	 * bottom, and top specify edges of the rectangular near plane. This plane
	 * is positioned perpendicular to the viewing direction, a distance near
	 * along the direction vector from the view's location. If this view is
	 * using orthogonal projection, the frustum is a rectangular prism extending
	 * from this near plane, out to an identically sized plane, that is distance
	 * far away. If not, the far plane is the far extent of a pyramid with it's
	 * point at the origin (camera coords), truncated at the near plane.
	 * 
	 * @param left The left edge of the near frustum plane
	 * @param right The right edge of the near frustum plane
	 * @param bottom The bottom edge of the near frustum plane
	 * @param top The top edge of the near frustum plane
	 * @param near The distance to the near frustum plane
	 * @param far The distance to the far frustum plane
	 * @throws IllegalArgumentException if left > right, bottom > top, near >
	 *             far, or near <= 0 when the view isn't orthographic
	 */
	public void setFrustum(float left, float right, float bottom, float top,
		float near, float far) {
		if (left > right || bottom > top || near > far)
			throw new IllegalArgumentException(
				"Frustum values would create an invalid frustum: " + left + " "
					+ right + " x " + bottom + " " + top + " x " + near + " "
					+ far);
		if (near <= 0 && !useOrtho)
			throw new IllegalArgumentException(
				"Illegal value for near frustum when using perspective projection: "
					+ near);

		frustumLeft = left;
		frustumRight = right;
		frustumBottom = bottom;
		frustumTop = top;
		frustumNear = near;
		frustumFar = far;

		computeProjectionMatrix();
	}

	/**
	 * Whether or not this view uses a perspective or orthogonal projection.
	 * 
	 * @return True if the projection matrix is orthographic
	 */
	public boolean isOrthogonalProjection() {
		return useOrtho;
	}

	/**
	 * Set whether or not to use orthogonal projection.
	 * 
	 * @param ortho Whether or not to use an orthographic projection
	 * @throws IllegalStateException if ortho is false and the near frustum
	 *             plane is <= 0
	 */
	public void setOrthogonalProjection(boolean ortho) {
		if (!ortho && frustumNear <= 0)
			throw new IllegalStateException(
				"Calling setOrthogonalProjection(false) when near frustum distance <= 0 is illegal");

		if (useOrtho != ortho) {
			useOrtho = ortho;
			computeProjectionMatrix();
		}
	}

	/**
	 * Get the left edge of the viewport, as a fraction of the width measured
	 * from the left side of the frame. Default value is 0.
	 * 
	 * @return Left edge of the viewport
	 */
	public float getViewLeft() {
		return viewLeft;
	}

	/**
	 * Get the right edge of the viewport, as a fraction of the width measured
	 * from the left side of the frame. Default value is 1.
	 * 
	 * @return Right edge of the viewport
	 */
	public float getViewRight() {
		return viewRight;
	}

	/**
	 * Get the top edge of the viewport, as a fraction of the height measured
	 * from the bottom side of the frame. Default value is 1.
	 * 
	 * @return Top edge of the viewport
	 */
	public float getViewTop() {
		return viewTop;
	}

	/**
	 * Get the bottom edge of the viewport, as a fraction of the height measured
	 * from the bottom side of the frame. Default value is 0.
	 * 
	 * @return Bottom edge of the viewport
	 */
	public float getViewBottom() {
		return viewBottom;
	}

	/**
	 * Set the dimensions of the viewport, as fractions of the width and height
	 * of a frame. Left and right are measured from the left edge, top and
	 * bottom are measured from the bottom edge.
	 * 
	 * @param left The left edge of the viewport
	 * @param right The right edge of the viewport
	 * @param bottom The bottom edge of the viewport
	 * @param top The bottom edge of the viewport
	 * @throws IllegalArgumentException if any value is outside of [0, 1], or if
	 *             top < bottom, or if left > right.
	 */
	public void setViewPort(float left, float right, float bottom, float top) {
		if (left < 0 || left > 1)
			throw new IllegalArgumentException(
				"Illegal value for left viewport edge: " + left);
		if (right < 0 || right > 1)
			throw new IllegalArgumentException(
				"Illegal value for right viewport edge: " + right);
		if (bottom < 0 || bottom > 1)
			throw new IllegalArgumentException(
				"Illegal value for bottom viewport edge: " + bottom);
		if (top < 0 || top > 1)
			throw new IllegalArgumentException(
				"Illegal value for top viewport edge: " + top);
		if (left > right || bottom > top)
			throw new IllegalArgumentException("Viewport edges are invalid: "
				+ left + " " + right + " x " + bottom + " " + top);
		viewBottom = bottom;
		viewLeft = left;
		viewRight = right;
		viewTop = top;
	}

	// compute the projection matrix
	private void computeProjectionMatrix() {
		projection.setZero();
		if (useOrtho)
			orthoMatrix(frustumRight, frustumLeft, frustumTop, frustumBottom,
				frustumNear, frustumFar, projection);
		else
			projMatrix(frustumRight, frustumLeft, frustumTop, frustumBottom,
				frustumNear, frustumFar, projection);
	}

	private void computeOrthoWorldPlanes() {
		Vector3f n = l.get();
		Vector3f p = u.get();

		// FAR
		p.scaleAdd(frustumFar, direction, location);
		n.scale(-1f, direction);
		setWorldPlane(FAR_PLANE, n, p);

		// NEAR
		p.scaleAdd(frustumNear, direction, location);
		n.set(direction);
		setWorldPlane(NEAR_PLANE, n, p);

		// LEFT
		n.cross(direction, up);
		p.scaleAdd(frustumLeft, n, location);
		setWorldPlane(LEFT_PLANE, n, p);

		// RIGHT
		n.scale(-1f);
		p.scaleAdd(-frustumRight, n, location);
		setWorldPlane(RIGHT_PLANE, n, p);

		// BOTTOM
		p.scaleAdd(frustumBottom, up, location);
		setWorldPlane(BOTTOM_PLANE, up, p);

		// TOP
		n.scale(-1f, up);
		p.scaleAdd(frustumTop, up, location);
		setWorldPlane(TOP_PLANE, n, p);
	}

	private void computePerspectiveWorldPlanes() {
		Vector3f n = l.get();
		Vector3f p = u.get();

		// FAR
		p.scaleAdd(frustumFar, direction, location);
		n.scale(-1f, direction);
		setWorldPlane(FAR_PLANE, n, p);

		// NEAR
		p.scaleAdd(frustumNear, direction, location);
		n.set(direction);
		setWorldPlane(NEAR_PLANE, n, p);

		// compute left vector for LEFT and RIGHT usage
		p.cross(up, direction);

		// LEFT
		float invHyp =
			1 / (float) Math.sqrt(frustumNear * frustumNear + frustumLeft
				* frustumLeft);
		n.scale(-frustumNear * invHyp, p);
		n.scaleAdd(Math.abs(frustumLeft) * invHyp, direction, n);
		setWorldPlane(LEFT_PLANE, n, location);

		// RIGHT
		invHyp =
			1 / (float) Math.sqrt(frustumNear * frustumNear + frustumRight
				* frustumRight);
		n.scale(frustumNear * invHyp, p);
		n.scaleAdd(Math.abs(frustumRight) * invHyp, direction, n);
		setWorldPlane(RIGHT_PLANE, n, location);

		// BOTTOM
		invHyp =
			1 / (float) Math.sqrt(frustumNear * frustumNear + frustumBottom
				* frustumBottom);
		n.scale(frustumNear * invHyp, up);
		n.scaleAdd(Math.abs(frustumBottom) * invHyp, direction, n);
		setWorldPlane(BOTTOM_PLANE, n, location);

		// TOP
		invHyp =
			1 / (float) Math.sqrt(frustumNear * frustumNear + frustumTop
				* frustumTop);
		n.scale(-frustumNear * invHyp, up);
		n.scaleAdd(Math.abs(frustumTop) * invHyp, direction, n);
		setWorldPlane(TOP_PLANE, n, location);
	}

	// set the given world plane so it's a plane with the given normal
	// that passes through pos, and then normalize it
	private void setWorldPlane(int plane, Vector3f normal, Vector3f pos) {
		setWorldPlane(plane, normal.x, normal.y, normal.z, -normal.dot(pos));
	}

	// set the given world plane, with the 4 values, and then normalize it
	private void setWorldPlane(int plane, float a, float b, float c, float d) {
		Plane cp = worldPlanes[plane];
		if (cp == null) {
			cp = new Plane(a, b, c, d);
			worldPlanes[plane] = cp;
		} else
			cp.setPlane(a, b, c, d);
		cp.normalize();
	}

	// computes an orthogonal projection matrix given the frustum values.
	private static void orthoMatrix(float fr, float fl, float ft, float fb,
		float fn, float ff, Matrix4f out) {
		out.m00 = 2f / (fr - fl);
		out.m11 = 2f / (ft - fb);
		out.m22 = 2f / (fn - ff);
		out.m33 = 1f;

		out.m03 = -(fr + fl) / (fr - fl);
		out.m13 = -(ft + fb) / (ft - fb);
		out.m23 = -(ff + fn) / (ff - fn);
	}

	// computes a perspective projection matrix given the frustum values.
	private static void projMatrix(float fr, float fl, float ft, float fb,
		float fn, float ff, Matrix4f out) {
		out.m00 = 2f * fn / (fr - fl);
		out.m11 = 2f * fn / (ft - fb);

		out.m02 = (fr + fl) / (fr - fl);
		out.m12 = (ft + fb) / (ft - fb);
		out.m22 = -(ff + fn) / (ff - fn);
		out.m32 = -1f;

		out.m23 = -2f * ff * fn / (ff - fn);
	}
}
