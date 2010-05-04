package com.ferox.math.bounds;

import com.ferox.math.Matrix4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;

/**
 * <p>
 * Frustum represents the mathematical construction of a frustum.
 * It is described as a 6 sided convex hull, where at least two
 * planes are parallel to each other.  It supports generating frustums
 * that represent perspective projections (a truncated pyramid), or
 * orthographic projections (a rectangular prism).
 * </p>
 * <p>
 * Each frustum has a direction vector and an up vector.  These
 * vectors define an orthonormal basis for the frustum.  The two
 * parallel planes of the frustum are specified as distances along
 * the direction vector (near and far).  The additional planes are
 * computed based on the locations of the four corners of the near
 * plane intersection.
 * </p>
 *
 * @author Michael Ludwig
 */
public class Frustum {
	/** Result of a frustum test against a BoundVolume. */
	public static enum FrustumIntersection {
	    /**
         * Returned when a candidate object is fully enclosed by the Frustum.
         */
		INSIDE, 
		/**
         * Returned when a candidate object is completely outside of the
         * Frustum.
         */
		OUTSIDE,
        /**
         * Returned when a candidate object intersects the Frustum but is not
         * completely contained.
         */
		INTERSECT
	}
	
	public static final int NUM_PLANES = 6;

	public static final int NEAR_PLANE = 0;
	public static final int FAR_PLANE = 1;
	public static final int TOP_PLANE = 2;
	public static final int BOTTOM_PLANE = 3;
	public static final int LEFT_PLANE = 4;
	public static final int RIGHT_PLANE = 5;
	
	private boolean useOrtho;

	// local values
	private float frustumLeft;
	private float frustumRight;
	private float frustumTop;
	private float frustumBottom;
	private float frustumNear;
	private float frustumFar;
	
	// frustum orientation
	private final Vector3f up;
	private final Vector3f direction;
	private final Vector3f location;
	
	// planes representing frustum, adjusted for
	// position, direction and up
	private final Vector4f[] worldPlanes;
	
	/**
	 * Instantiate a new Frustum that's positioned at the origin, looking down
	 * the negative z-axis. The given values are equivalent to those described
	 * in setPerspective() and are used for the initial frustum parameters.
	 * 
	 * @param fov
	 * @param aspect
	 * @param znear
	 * @param zfar
	 */
	public Frustum(float fov, float aspect, float znear, float zfar) {
		this();
		setPerspective(fov, aspect, znear, zfar);
	}

	/**
	 * Instantiate a new Frustum that's positioned at the origin, looking down
	 * the negative z-axis. The six values are equivalent to those specified in
	 * setFrustum() and are taken as the initial frustum parameters.
	 * 
	 * @param ortho True if the frustum values are for an orthographic
	 *            projection, otherwise it's a perspective projection
	 * @param fl
	 * @param fr
	 * @param fb
	 * @param ft
	 * @param fn
	 * @param ff
	 */
	public Frustum(boolean ortho, float fl, float fr, float fb, float ft, float fn, float ff) {
		this();
		setFrustum(ortho, fl, fr, fb, ft, fn, ff);
	}
	
	// initialize everything
	private Frustum() {
		worldPlanes = new Vector4f[6];

		location = new Vector3f();
		up = new Vector3f(0f, 1f, 0f);
		direction = new Vector3f(0f, 0f, -1f);
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
	 * <p>
	 * Sets the dimensions of the viewing frustum in camera coords. left, right,
	 * bottom, and top specify edges of the rectangular near plane. This plane
	 * is positioned perpendicular to the viewing direction, a distance near
	 * along the direction vector from the view's location.
	 * </p>
	 * <p>
	 * If this Frustum is using orthogonal projection, the frustum is a
	 * rectangular prism extending from this near plane, out to an identically
	 * sized plane, that is distance far away. If not, the far plane is the far
	 * extent of a pyramid with it's point at the location, truncated at the
	 * near plane.
	 * </p>
	 * 
	 * @param ortho True if the Frustum should use an orthographic projection
	 * @param left The left edge of the near frustum plane
	 * @param right The right edge of the near frustum plane
	 * @param bottom The bottom edge of the near frustum plane
	 * @param top The top edge of the near frustum plane
	 * @param near The distance to the near frustum plane
	 * @param far The distance to the far frustum plane
	 * @throws IllegalArgumentException if left > right, bottom > top, near >
	 *             far, or near <= 0 when the view isn't orthographic
	 */
	public void setFrustum(boolean ortho, float left, float right, float bottom, float top, float near, float far) {
		if (left > right || bottom > top || near > far)
			throw new IllegalArgumentException("Frustum values would create an invalid frustum: " + 
											   left + " " + right + " x " + bottom + " " + top + " x " + near + " " + far);
		if (near <= 0 && !ortho)
			throw new IllegalArgumentException("Illegal value for near frustum when using perspective projection: " + near);

		frustumLeft = left;
		frustumRight = right;
		frustumBottom = bottom;
		frustumTop = top;
		frustumNear = near;
		frustumFar = far;
		
		useOrtho = ortho;
		
		updateFrustumPlanes();
	}

	/**
	 * Set the frustum to be perspective projection with the given field of view
	 * (in degrees). Widths and heights are calculated using the assumed aspect
	 * ration and near and far values. Because perspective transforms only make
	 * sense for non-orthographic projections, it also sets this view to be
	 * non-orthographic.
	 * 
	 * @param fov The field of view
	 * @param aspect The aspect ratio of the view region (width / height)
	 * @param near The distance from the view's location to the near camera
	 *            plane
	 * @param far The distance from the view's location to the far camera plane
	 * @throws IllegalArgumentException if fov is outside of (0, 180], or aspect
	 *             is <= 0, or near > far, or if near <= 0
	 */
	public void setPerspective(float fov, float aspect, float near, float far) {
		if (fov <= 0f || fov > 180f)
			throw new IllegalArgumentException("Field of view must be in (0, 180], not: " + fov);
		if (aspect <= 0)
			throw new IllegalArgumentException("Aspect ration must be >= 0, not: " + aspect);
		
		float h = (float) Math.tan(Math.toRadians(fov * .5f)) * near;
		float w = h * aspect;
		setFrustum(false, -w, w, -h, h, near, far);
	}

	/**
	 * Set the frustum to be an orthographic projection that uses the given
	 * boundary edges for the near frustum plane. The near value is set to -1,
	 * and the far value is set to 1.
	 * 
	 * @param left
	 * @param right
	 * @param bottom
	 * @param top
	 * @see #setFrustum(boolean, float, float, float, float, float, float)
	 * @throws IllegalArgumentException if left > right or bottom > top
	 */
	public void setOrtho(float left, float right, float bottom, float top) {
		setFrustum(true, left, right, bottom, top, -1f, 1f);
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
	 * <p>
	 * Get the location vector of this view, in world space.
	 * </p>
	 * <p>
	 * This does not return a defensive copy, so modifications to the vector
	 * require a subsequent call to updateFrustumPlanes() to see the effects
	 * reflected in the frustum planes.
	 * 
	 * @return The location of the view
	 */
	public Vector3f getLocation() {
		return location;
	}

	/**
	 * <p>
	 * Get the up vector of this view, in world space. Together up and direction
	 * form a right-handed coordinate system.
	 * </p>
	 * <p>
	 * This does not return a defensive copy, so modifications to the vector
	 * require a subsequent call to updateFrustumPlanes() to see the effects
	 * reflected in the frustum planes.
	 * </p>
	 * 
	 * @return The up vector of this view
	 */
	public Vector3f getUp() {
		return up;
	}

	/**
	 * <p>
	 * Get the direction vector of this frustum, in world space. Together up and
	 * direction form a right-handed coordinate system.
	 * </p>
	 * <p>
	 * This does not return a defensive copy, so modifications to the vector
	 * require a subsequent call to updateFrustumPlanes() to see the effects
	 * reflected in the frustum planes.
	 * </p>
	 * 
	 * @return The current direction that this frustum is pointing
	 */
	public Vector3f getDirection() {
		return direction;
	}

	/**
	 * Compute and return the field of view along the vertical axis that this
	 * Frustum uses. This is meaningless for an orthographic projection, and
	 * returns -1 in that case. Otherwise, an angle in degrees is returned in
	 * the range 0 to 180. This works correctly even when the bottom and top
	 * edges of the Frustum are not centered about the location.
	 * 
	 * @return The field of view of this Frustum
	 */
	public float getFieldOfView() {
		if (useOrtho)
			return -1f;
		
		double fovTop = Math.atan(frustumTop / frustumNear);
		double fovBottom = Math.atan(frustumBottom / frustumNear);
		return (float) Math.toDegrees(fovTop - fovBottom);
	}

	/**
	 * Compute and return the 4x4 projection matrix that represents the
	 * mathematical projection from the frustum to homogenous device coordinates
	 * (essentially the unit cube).
	 * 
	 * @param projection The matrix is stored in <tt>projection</tt> if not null
	 * @return projection, or a new Matrix4f if it was null
	 */
	public Matrix4f getProjectionMatrix(Matrix4f projection) {
		if (projection == null)
			projection = new Matrix4f();
		projection.set(0, 0, 0, 0, 
					   0, 0, 0, 0, 
					   0, 0, 0, 0, 
					   0, 0, 0, 0);
		if (useOrtho)
			orthoMatrix(frustumRight, frustumLeft, 
						frustumTop, frustumBottom, 
						frustumNear, frustumFar, projection);
		else
			projMatrix(frustumRight, frustumLeft, 
					   frustumTop, frustumBottom,
					   frustumNear, frustumFar, projection);
		
		return projection;
	}

	/**
	 * <p>
	 * Compute and return the 'view' transform this Frustum. The view transform
	 * represents the the coordinate space transformation from world space to
	 * camera/frustum space. The local basis of the Frustum is formed by the
	 * left, up and direction vectors of the Frustum. The left vector is up X
	 * direction, and up and direction are user defined vectors.
	 * </p>
	 * <p>
	 * The result matrix is stored in <tt>view</tt> if it's not null. If it is
	 * null, a new matrix is created and returned.
	 * </p>
	 * 
	 * @param view The result matrix to hold the computation
	 * @return view, or a new Matrix4f if view was null
	 */
	public Matrix4f getViewMatrix(Matrix4f view) {
		if (view == null)
			view = new Matrix4f();
		
		Vector3f n = direction.scale(-1f, null).normalize();
		Vector3f u = up.normalize().cross(n, null);
		Vector3f v = n.cross(u, null);
		
		view.m00 = u.x; view.m01 = u.y; view.m02 = u.z; view.m03 = -location.dot(u);
		view.m10 = v.x; view.m11 = v.y; view.m12 = v.z; view.m13 = -location.dot(v);
		view.m20 = n.x; view.m21 = n.y; view.m22 = n.z; view.m23 = -location.dot(n);
		view.m30 = 0f; view.m31 = 0f; view.m32 = 0f; view.m33 = 1f;

		return view;
	}

	/**
	 * <p>
	 * Copy the given vectors into this Frustum for its location, direction and
	 * up vectors. The orientation is then normalized and orthogonalized, but
	 * this leaves the given vectors unmodified.
	 * </p>
	 * <p>
	 * Any later changes to the vectors' x, y, and z values will not be
	 * reflected in the frustum planes until updateFrustumPlanes() is called.
	 * </p>
	 * 
	 * @param location The new location vector
	 * @param direction The new direction vector
	 * @param up The new up vector
	 * @throws NullPointerException if location, direction or up is null
	 */
	public void setOrientation(Vector3f location, Vector3f direction, Vector3f up) {
		if (location == null || direction == null || up == null)
			throw new NullPointerException("Orientation vectors cannot be null: " + 
										   location + " " + direction + " " + up);
		
		this.location.set(location);
		this.direction.set(direction);
		this.up.set(up);
		
		updateFrustumPlanes();
	}
	
	/**
	 * Update the plane instances returned by getFrustumPlane() to reflect any
	 * changes to the frustum's local parameters or orientation.
	 */
	public void updateFrustumPlanes() {
		// compute the right-handed basis vectors of the frustum
		Vector3f left = up.normalize().cross(direction, Frustum.p.get()).normalize();
		direction.normalize().cross(left, up);
		
		if (useOrtho)
			computeOrthoWorldPlanes();
		else
			computePerspectiveWorldPlanes();
	}

	/**
	 * <p>
	 * Return a plane representing the given plane of the view frustum, in world
	 * coordinates. This plane should not be modified. The returned plane's
	 * normal is configured so that it points into the center of the Frustum.
	 * The returned {@link Vector4f} is encoded as a plane as defined in
	 * {@link Plane}; it is also normalized.
	 * </p>
	 * <p>
	 * This will be stale if the location, direction, and up vectors are changed
	 * without a subsequent call to updateFrustumPlanes(). A call to
	 * setOrientation(), setFrustum(), setPerspective, or
	 * setOrthogonalProjection() will automatically update the frustum planes.
	 * </p>
	 * 
	 * @param plane The requested plane
	 * @return The Vector4f instance for the requested plane, in world
	 *         coordinates
	 * @throws IndexOutOfBoundsException if plane isn't in [0, 5]
	 */
	public Vector4f getFrustumPlane(int i) {
		return worldPlanes[i];
	}

	private void computeOrthoWorldPlanes() {
		Vector3f n = Frustum.n.get();
		Vector3f p = Frustum.p.get();

		// FAR
		direction.scaleAdd(frustumFar, location, p);
		direction.scale(-1f, n);
		setWorldPlane(FAR_PLANE, n, p);

		// NEAR
		direction.scaleAdd(frustumNear, location, p);
		n.set(direction);
		setWorldPlane(NEAR_PLANE, n, p);

		// LEFT
		direction.cross(up, n);
		n.scaleAdd(frustumLeft, location, p);
		setWorldPlane(LEFT_PLANE, n, p);

		// RIGHT
		n.scaleAdd(frustumRight, location, p);
		n.scale(-1f, n);
		setWorldPlane(RIGHT_PLANE, n, p);

		// BOTTOM
		up.scaleAdd(frustumBottom, location, p);
		setWorldPlane(BOTTOM_PLANE, up, p);

		// TOP
		up.scale(-1f, n);
		up.scaleAdd(frustumTop, location, p);
		setWorldPlane(TOP_PLANE, n, p);
	}

	private void computePerspectiveWorldPlanes() {
		Vector3f n = Frustum.n.get();
		Vector3f p = Frustum.p.get();

		// FAR
		direction.scaleAdd(frustumFar, location, p);
		direction.scale(-1f, n);
		setWorldPlane(FAR_PLANE, n, p);

		// NEAR
		direction.scaleAdd(frustumNear, location, p);
		n.set(direction);
		setWorldPlane(NEAR_PLANE, n, p);

		// compute left vector for LEFT and RIGHT usage
		up.cross(direction, p);

		// LEFT
		float invHyp = 1 / (float) Math.sqrt(frustumNear * frustumNear + frustumLeft * frustumLeft);
		p.scale(-frustumNear * invHyp, n);
		direction.scaleAdd(Math.abs(frustumLeft) * invHyp, n, n);
		setWorldPlane(LEFT_PLANE, n, location);

		// RIGHT
		invHyp = 1 / (float) Math.sqrt(frustumNear * frustumNear + frustumRight * frustumRight);
		p.scale(frustumNear * invHyp, n);
		direction.scaleAdd(Math.abs(frustumRight) * invHyp, n, n);
		setWorldPlane(RIGHT_PLANE, n, location);

		// BOTTOM
		invHyp = 1 / (float) Math.sqrt(frustumNear * frustumNear + frustumBottom * frustumBottom);
		up.scale(frustumNear * invHyp, n);
		direction.scaleAdd(Math.abs(frustumBottom) * invHyp, n, n);
		setWorldPlane(BOTTOM_PLANE, n, location);

		// TOP
		invHyp = 1 / (float) Math.sqrt(frustumNear * frustumNear + frustumTop * frustumTop);
		up.scale(-frustumNear * invHyp, n);
		direction.scaleAdd(Math.abs(frustumTop) * invHyp, n, n);
		setWorldPlane(TOP_PLANE, n, location);
	}

	// set the given world plane so it's a plane with the given normal
	// that passes through pos, and then normalize it
	private void setWorldPlane(int plane, Vector3f normal, Vector3f pos) {
		setWorldPlane(plane, normal.x, normal.y, normal.z, -normal.dot(pos));
	}

	// set the given world plane, with the 4 values, and then normalize it
	private void setWorldPlane(int plane, float a, float b, float c, float d) {
		Vector4f cp = worldPlanes[plane];
		if (cp == null) {
			cp = new Vector4f(a, b, c, d);
			worldPlanes[plane] = cp;
		} else
			cp.set(a, b, c, d);
		Plane.normalize(cp);
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
	
	private static final ThreadLocal<Vector3f> n = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> p = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
}
