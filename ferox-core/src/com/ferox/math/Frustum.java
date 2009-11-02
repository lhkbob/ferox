package com.ferox.math;

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
		INSIDE, OUTSIDE, INTERSECT
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
	private Vector3f up;
	private Vector3f direction;
	private Vector3f location;
	
	// planes representing frustum, adjusted for
	// position, direction and up
	private final Plane[] worldPlanes;
	
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
	 * @param fl
	 * @param fr
	 * @param fb
	 * @param ft
	 * @param fn
	 * @param ff
	 */
	public Frustum(float fl, float fr, float fb, float ft, float fn, float ff) {
		this();
		setFrustum(fl, fr, fb, ft, fn, ff);
	}
	
	// initialize everything
	private Frustum() {
		worldPlanes = new Plane[6];

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
	 * Sets the dimensions of the viewing frustum in camera coords. left, right,
	 * bottom, and top specify edges of the rectangular near plane. This plane
	 * is positioned perpendicular to the viewing direction, a distance near
	 * along the direction vector from the view's location. If this view is
	 * using orthogonal projection, the frustum is a rectangular prism extending
	 * from this near plane, out to an identically sized plane, that is distance
	 * far away. If not, the far plane is the far extent of a pyramid with it's
	 * point at the location, truncated at the near plane.
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
	public void setFrustum(float left, float right, float bottom, float top, float near, float far) {
		if (left > right || bottom > top || near > far)
			throw new IllegalArgumentException("Frustum values would create an invalid frustum: " + 
											   left + " " + right + " x " + bottom + " " + top + " x " + near + " " + far);
		if (near <= 0 && !useOrtho)
			throw new IllegalArgumentException("Illegal value for near frustum when using perspective projection: " + near);

		frustumLeft = left;
		frustumRight = right;
		frustumBottom = bottom;
		frustumTop = top;
		frustumNear = near;
		frustumFar = far;
		
		updateFrustumPlanes();
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
			throw new IllegalStateException("Calling setOrthogonalProjection(false) when near frustum distance <= 0 is illegal");

		if (useOrtho != ortho) {
			useOrtho = ortho;
			updateFrustumPlanes();
		}
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
	
	// compute the projection matrix
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
	 * Assign the given vectors to this Frustum for its location, direction and
	 * up vectors. These references will replace any previously assigned
	 * vectors. direction and up will be normalized and modified to be
	 * orthogonal to each other.
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
		
		this.location = location;
		this.direction = direction;
		this.up = up;
		
		updateFrustumPlanes();
	}
	
	/**
	 * Update the plane instances returned by getFrustumPlane() to reflect any
	 * changes to the frustum's local parameters or orientation.
	 */
	public void updateFrustumPlanes() {
		// compute the right-handed basis vectors of the frustum
		Vector3f left = up.normalize().cross(direction, Frustum.p.get()).normalize();
		direction.normalize().cross(left, up).normalize();
		
		if (useOrtho)
			computeOrthoWorldPlanes();
		else
			computePerspectiveWorldPlanes();
	}
	
	/**
	 * <p>
	 * Return a plane representing the given plane of the view frustum, in world
	 * coordinates. This plane should not be modified.  The returned plane's normal
	 * is configured so that it points into the center of the Frustum.
	 * </p>
	 * <p>
	 * This will be stale if the location, direction, and up vectors are changed
	 * without a subsequent call to updateFrustumPlanes(). A call to
	 * setOrientation(), setFrustum(), setPerspective, or
	 * setOrthogonalProjection() will automatically update the frustum planes.
	 * </p>
	 * 
	 * @param plane The requested plane
	 * @return The Plane instance for the requested plane, in world coordinates
	 * @throws IndexOutOfBoundsException if plane isn't in [0, 5]
	 */
	public Plane getFrustumPlane(int i) {
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
		Plane cp = worldPlanes[plane];
		if (cp == null) {
			cp = new Plane(a, b, c, d);
			worldPlanes[plane] = cp;
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
