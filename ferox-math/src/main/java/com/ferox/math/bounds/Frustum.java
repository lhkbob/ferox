package com.ferox.math.bounds;

import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.Vector3;
import com.ferox.math.Vector4;

/**
 * <p>
 * Frustum represents the mathematical construction of a frustum. It is
 * described as a 6 sided convex hull, where at least two planes are parallel to
 * each other. It supports generating frustums that represent perspective
 * projections (a truncated pyramid), or orthographic projections (a rectangular
 * prism).
 * </p>
 * <p>
 * Each frustum has a direction vector and an up vector. These vectors define an
 * orthonormal basis for the frustum. The two parallel planes of the frustum are
 * specified as distances along the direction vector (near and far). The
 * additional planes are computed based on the locations of the four corners of
 * the near plane intersection.
 * </p>
 * <p>
 * The mapping from world space to frustum space is not as straight-forward as
 * is implied by the above state. Frustum provides the functionality to get the
 * {@link #getProjectionMatrix(Matrix4) project matrix} and
 * {@link #getViewMatrix(Matrix4) modelview matrix} suitable for use in an
 * OpenGL system. The camera within an OpenGL system looks down its local
 * negative z-axis. Thus the provided direction in this Frustum represents the
 * negative z-axis within camera space.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Frustum {
    /** Result of a frustum test against a {@link ReadOnlyAxisAlignedBox}. */
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
    private double frustumLeft;
    private double frustumRight;
    private double frustumTop;
    private double frustumBottom;
    private double frustumNear;
    private double frustumFar;
    
    // frustum orientation
    private final Vector3 up;
    private final Vector3 direction;
    private final Vector3 location;
    
    private final Matrix4 projection;
    private final Matrix4 view;
    
    // planes representing frustum, adjusted for
    // position, direction and up
    private final Vector4[] worldPlanes;
    
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
    public Frustum(double fov, double aspect, double znear, double zfar) {
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
    public Frustum(boolean ortho, double fl, double fr, double fb, double ft, double fn, double ff) {
        this();
        setFrustum(ortho, fl, fr, fb, ft, fn, ff);
    }
    
    // initialize everything
    private Frustum() {
        worldPlanes = new Vector4[6];

        location = new Vector3();
        up = new Vector3(0, 1, 0);
        direction = new Vector3(0, 0, -1);
        
        view = new Matrix4();
        projection = new Matrix4();
    }
    
    /**
     * Get the left edge of the near frustum plane.
     * 
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     * @return The left edge of the near frustum plane
     */
    public double getFrustumLeft() {
        return frustumLeft;
    }

    /**
     * Get the right edge of the near frustum plane.
     * 
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     * @return The right edge of the near frustum plane
     */
    public double getFrustumRight() {
        return frustumRight;
    }

    /**
     * Get the top edge of the near frustum plane.
     * 
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     * @return The top edge of the near frustum plane
     */
    public double getFrustumTop() {
        return frustumTop;
    }

    /**
     * Get the bottom edge of the near frustum plane.
     * 
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     * @return The bottom edge of the near frustum plane
     */
    public double getFrustumBottom() {
        return frustumBottom;
    }

    /**
     * Get the distance to the near frustum plane from the origin, in camera
     * coords.
     * 
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     * @return The distance to the near frustum plane
     */
    public double getFrustumNear() {
        return frustumNear;
    }

    /**
     * Get the distance to the far frustum plane from the origin, in camera
     * coords.
     * 
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     * @return The distance to the far frustum plane
     */
    public double getFrustumFar() {
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
    public void setFrustum(boolean ortho, double left, double right, double bottom, double top, double near, double far) {
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
        
        update();
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
    public void setPerspective(double fov, double aspect, double near, double far) {
        if (fov <= 0f || fov > 180f)
            throw new IllegalArgumentException("Field of view must be in (0, 180], not: " + fov);
        if (aspect <= 0)
            throw new IllegalArgumentException("Aspect ration must be >= 0, not: " + aspect);
        
        double h = (double) Math.tan(Math.toRadians(fov * .5f)) * near;
        double w = h * aspect;
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
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     * @throws IllegalArgumentException if left > right or bottom > top
     */
    public void setOrtho(double left, double right, double bottom, double top) {
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
     * Get the location vector of this view, in world space. The returned vector
     * is read-only. Modifications to the frustum's view parameters must be done
     * through {@link #setOrientation(Vector3, Vector3, Vector3)}.
     * </p>
     * 
     * @return The location of the view
     */
    public @Const Vector3 getLocation() {
        return location;
    }

    /**
     * <p>
     * Get the up vector of this view, in world space. Together up and direction
     * form a right-handed coordinate system. The returned vector is read-only.
     * Modifications to the frustum's view parameters must be done through
     * {@link #setOrientation(Vector3, Vector3, Vector3)}.
     * </p>
     * 
     * @return The up vector of this view
     */
    public @Const Vector3 getUp() {
        return up;
    }

    /**
     * <p>
     * Get the direction vector of this frustum, in world space. Together up and
     * direction form a right-handed coordinate system. The returned vector is
     * read-only. Modifications to the frustum's view parameters must be done
     * through {@link #setOrientation(Vector3, Vector3, Vector3)}.
     * </p>
     * 
     * @return The current direction that this frustum is pointing
     */
    public @Const Vector3 getDirection() {
        return direction;
    }

    /**
     * Compute and return the field of view along the vertical axis that this
     * Frustum uses. This is meaningless for an orthographic projection, and
     * returns -1 in that case. Otherwise, an angle in degrees is returned in
     * the range 0 to 180. This works correctly even when the bottom and top
     * edges of the Frustum are not centered about its location.
     * 
     * @return The field of view of this Frustum
     */
    public double getFieldOfView() {
        if (useOrtho)
            return -1f;
        
        double fovTop = Math.atan(frustumTop / frustumNear);
        double fovBottom = Math.atan(frustumBottom / frustumNear);
        return Math.toDegrees(fovTop - fovBottom);
    }

    /**
     * <p>
     * Return the 4x4 projection matrix that represents the mathematical
     * projection from the frustum to homogenous device coordinates (essentially
     * the unit cube).
     * </p>
     * <p>
     * The returned matrix is read-only and will be updated automatically as the
     * projection of the Frustum changes.
     * </p>
     * 
     * @return The projection matrix
     */
    public @Const Matrix4 getProjectionMatrix() {
        return projection;
    }

    /**
     * <p>
     * Return the 'view' transform of this Frustum. The view transform
     * represents the coordinate space transformation from world space to
     * camera/frustum space. The local basis of the Frustum is formed by the
     * left, up and direction vectors of the Frustum. The left vector is
     * <code>up X
     * direction</code>, and up and direction are user defined vectors.
     * </p>
     * <p>
     * The returned matrix is read-only and will be updated automatically as
     * {@link #setOrientation(Vector3, Vector3, Vector3)} is invoked.
     * </p>
     * 
     * @return The view matrix
     */
    public @Const Matrix4 getViewMatrix() {
        return view;
    }

    /**
     * <p>
     * Copy the given vectors into this Frustum for its location, direction and
     * up vectors. The orientation is then normalized and orthogonalized, but
     * the provided vectors are unmodified.
     * </p>
     * <p>
     * Any later changes to the vectors' x, y, and z values will not be
     * reflected in the frustum planes or view transform, unless this method is
     * called again.
     * </p>
     * 
     * @param location The new location vector
     * @param direction The new direction vector
     * @param up The new up vector
     * @throws NullPointerException if location, direction or up is null
     */
    public void setOrientation(@Const Vector3 location, @Const Vector3 direction, @Const Vector3 up) {
        if (location == null || direction == null || up == null)
            throw new NullPointerException("Orientation vectors cannot be null: " + 
                                           location + " " + direction + " " + up);
        
        this.location.set(location);
        this.direction.set(direction);
        this.up.set(up);
        
        update();
    }

    /**
     * <p>
     * Return a plane representing the given plane of the view frustum, in world
     * coordinates. This plane should not be modified. The returned plane's
     * normal is configured so that it points into the center of the Frustum.
     * The returned {@link Vector4} is encoded as a plane as defined in
     * {@link Plane}; it is also normalized.
     * </p>
     * <p>
     * The returned plane vector is read-only. It will be updated automatically
     * when the projection or view parameters change.
     * </p>
     * 
     * @param i The requested plane index
     * @return The ReadOnlyVector4f instance for the requested plane, in world
     *         coordinates
     * @throws IndexOutOfBoundsException if plane isn't in [0, 5]
     */
    public @Const Vector4 getFrustumPlane(int i) {
        return worldPlanes[i];
    }
    
    /*
     * Update the plane instances returned by getFrustumPlane() to reflect any
     * changes to the frustum's local parameters or orientation. Also update the
     * view transform and projection matrix.
     */
    private void update() {
        // compute the right-handed basis vectors of the frustum
        Vector3 n = new Vector3().scale(direction.normalize(), -1); // normalize direction as well
        Vector3 u = new Vector3().cross(up.normalize(), n); // normalize up as well
        Vector3 v = up.cross(n, u); // recompute up to properly orthogonal to direction 
        
        // view matrix
        view.set(u.x, u.y, u.z, -location.dot(u),
                 v.x, v.y, v.z, -location.dot(v),
                 n.x, n.y, n.z, -location.dot(n),
                 0f, 0f, 0f, 1f);
        
        // projection matrix
        if (useOrtho)
            projection.set(2 / (frustumRight - frustumLeft), 0, 0, -(frustumRight + frustumLeft) / (frustumRight - frustumLeft),
                           0, 2 / (frustumTop - frustumBottom), 0, -(frustumTop + frustumBottom) / (frustumTop - frustumBottom),
                           0, 0, 2 / (frustumNear - frustumFar), -(frustumFar + frustumNear) / (frustumFar - frustumNear),
                           0, 0, 0, 1);
        else
            projection.set(2 * frustumNear / (frustumRight - frustumLeft), 0, (frustumRight + frustumLeft) / (frustumRight - frustumLeft), 0,
                           0, 2 * frustumNear / (frustumTop - frustumBottom), (frustumTop + frustumBottom) / (frustumTop - frustumBottom), 0,
                           0, 0, -(frustumFar + frustumNear) / (frustumFar - frustumNear), -2 * frustumFar * frustumNear / (frustumFar - frustumNear),
                           0, 0, -1, 0);
        
        // generate world-space frustum planes, we pass in n and u since we
        // created them to compute the view matrix and they're just garbage
        // at this point, might as well let plane generation reuse them.
        if (useOrtho)
            computeOrthoWorldPlanes(n, u);
        else
            computePerspectiveWorldPlanes(n, u);
    }

    private void computeOrthoWorldPlanes(Vector3 n, Vector3 p) {
        // FAR
        p.scale(direction, frustumFar).add(location);
        n.scale(direction, -1);
        setWorldPlane(FAR_PLANE, n, p);

        // NEAR
        p.scale(direction, frustumNear).add(location);
        n.set(direction);
        setWorldPlane(NEAR_PLANE, n, p);

        // compute right vector for LEFT and RIGHT usage
        n.cross(direction, up);
        
        // LEFT
        p.scale(n, frustumLeft).add(location);
        setWorldPlane(LEFT_PLANE, n, p);

        // RIGHT
        p.scale(n, frustumRight).add(location);
        n.scale(-1);
        setWorldPlane(RIGHT_PLANE, n, p);

        // BOTTOM
        p.scale(up, frustumBottom).add(location);
        setWorldPlane(BOTTOM_PLANE, up, p);

        // TOP
        n.scale(up, -1);
        p.scale(up, frustumTop).add(location);
        setWorldPlane(TOP_PLANE, n, p);
    }

    private void computePerspectiveWorldPlanes(Vector3 n, Vector3 p) {
        // FAR
        p.scale(direction, frustumFar).add(location);
        p.scale(direction, -1);
        setWorldPlane(FAR_PLANE, n, p);
        
        // NEAR
        p.scale(direction, frustumNear).add(location);
        n.set(direction);
        setWorldPlane(NEAR_PLANE, n, p);

        // compute left vector for LEFT and RIGHT usage
        p.cross(up, direction);

        // LEFT
        double invHyp = 1 / Math.sqrt(frustumNear * frustumNear + frustumLeft * frustumLeft);
        n.scale(direction, Math.abs(frustumLeft) / frustumNear).sub(p).scale(frustumNear * invHyp);
        setWorldPlane(LEFT_PLANE, n, location);

        // RIGHT
        invHyp = 1 / (double) Math.sqrt(frustumNear * frustumNear + frustumRight * frustumRight);
        n.scale(direction, Math.abs(frustumRight) / frustumNear).add(p).scale(frustumNear * invHyp);
        setWorldPlane(RIGHT_PLANE, n, location);

        // BOTTOM
        invHyp = 1 / (double) Math.sqrt(frustumNear * frustumNear + frustumBottom * frustumBottom);
        n.scale(direction, Math.abs(frustumBottom) / frustumNear).add(up).scale(frustumNear * invHyp);
        setWorldPlane(BOTTOM_PLANE, n, location);

        // TOP
        invHyp = 1 / (double) Math.sqrt(frustumNear * frustumNear + frustumTop * frustumTop);
        n.scale(direction, Math.abs(frustumTop) / frustumNear).sub(up).scale(frustumNear * invHyp);
        setWorldPlane(TOP_PLANE, n, location);
    }

    // set the given world plane so it's a plane with the given normal
    // that passes through pos, and then normalize it
    private void setWorldPlane(int plane, @Const Vector3 normal, @Const Vector3 pos) {
        setWorldPlane(plane, normal.x, normal.y, normal.z, -normal.dot(pos));
    }

    // set the given world plane, with the 4 values, and then normalize it
    private void setWorldPlane(int plane, double a, double b, double c, double d) {
        Vector4 cp = worldPlanes[plane];
        if (cp == null) {
            cp = new Vector4(a, b, c, d);
            worldPlanes[plane] = cp;
        } else
            cp.set(a, b, c, d);
        Plane.normalize(cp);
    }
}