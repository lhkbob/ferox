package com.ferox.math.bounds;

import com.ferox.math.Matrix4f;
import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.ReadOnlyVector4f;
import com.ferox.math.Vector3f;
import com.ferox.math.Vector4f;


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
 * {@link #getProjectionMatrix(Matrix4f) project matrix} and
 * {@link #getViewMatrix(Matrix4f) modelview matrix} suitable for use in an
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
    
    private final Matrix4f projection;
    private final Matrix4f view;
    
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
        
        view = new Matrix4f();
        projection = new Matrix4f();
    }
    
    /**
     * Get the left edge of the near frustum plane.
     * 
     * @see #setFrustum(boolean, float, float, float, float, float, float)
     * @return The left edge of the near frustum plane
     */
    public float getFrustumLeft() {
        return frustumLeft;
    }

    /**
     * Get the right edge of the near frustum plane.
     * 
     * @see #setFrustum(boolean, float, float, float, float, float, float)
     * @return The right edge of the near frustum plane
     */
    public float getFrustumRight() {
        return frustumRight;
    }

    /**
     * Get the top edge of the near frustum plane.
     * 
     * @see #setFrustum(boolean, float, float, float, float, float, float)
     * @return The top edge of the near frustum plane
     */
    public float getFrustumTop() {
        return frustumTop;
    }

    /**
     * Get the bottom edge of the near frustum plane.
     * 
     * @see #setFrustum(boolean, float, float, float, float, float, float)
     * @return The bottom edge of the near frustum plane
     */
    public float getFrustumBottom() {
        return frustumBottom;
    }

    /**
     * Get the distance to the near frustum plane from the origin, in camera
     * coords.
     * 
     * @see #setFrustum(boolean, float, float, float, float, float, float)
     * @return The distance to the near frustum plane
     */
    public float getFrustumNear() {
        return frustumNear;
    }

    /**
     * Get the distance to the far frustum plane from the origin, in camera
     * coords.
     * 
     * @see #setFrustum(boolean, float, float, float, float, float, float)
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
     * Get the location vector of this view, in world space. The returned vector
     * is read-only. Modifications to the frustum's view paratmers must be done
     * through {@link #setOrientation(Vector3f, Vector3f, Vector3f)}.
     * </p>
     * 
     * @return The location of the view
     */
    public ReadOnlyVector3f getLocation() {
        return location;
    }

    /**
     * <p>
     * Get the up vector of this view, in world space. Together up and direction
     * form a right-handed coordinate system. The returned vector is read-only.
     * Modifications to the frustum's view parameters must be done through
     * {@link #setOrientation(Vector3f, Vector3f, Vector3f)}.
     * </p>
     * 
     * @return The up vector of this view
     */
    public ReadOnlyVector3f getUp() {
        return up;
    }

    /**
     * <p>
     * Get the direction vector of this frustum, in world space. Together up and
     * direction form a right-handed coordinate system. The returned vector is
     * read-only. Modifications to the frustum's view parameters must be done
     * through {@link #setOrientation(Vector3f, Vector3f, Vector3f)}.
     * </p>
     * 
     * @return The current direction that this frustum is pointing
     */
    public ReadOnlyVector3f getDirection() {
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
    public ReadOnlyMatrix4f getProjectionMatrix() {
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
     * {@link #setOrientation(Vector3f, Vector3f, Vector3f)} is invoked.
     * </p>
     * 
     * @return The view matrix
     */
    public ReadOnlyMatrix4f getViewMatrix() {
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
     * reflected in the frustum planes or view transform, unless this method is
     * called again.
     * </p>
     * 
     * @param location The new location vector
     * @param direction The new direction vector
     * @param up The new up vector
     * @throws NullPointerException if location, direction or up is null
     */
    public void setOrientation(ReadOnlyVector3f location, ReadOnlyVector3f direction, ReadOnlyVector3f up) {
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
     * The returned {@link Vector4f} is encoded as a plane as defined in
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
    public ReadOnlyVector4f getFrustumPlane(int i) {
        return worldPlanes[i];
    }
    
    /*
     * Update the plane instances returned by getFrustumPlane() to reflect any
     * changes to the frustum's local parameters or orientation. Also update the
     * view transform and projection matrix.
     */
    private void update() {
        // compute the right-handed basis vectors of the frustum
        MutableVector3f n = direction.normalize().scale(-1f, Frustum.n.get());
        MutableVector3f u = up.normalize().cross(n, Frustum.p.get());
        MutableVector3f v = n.cross(u, up);
        
        // view matrix
        view.set(u.getX(), u.getY(), u.getZ(), -location.dot(u),
                 v.getX(), v.getY(), v.getZ(), -location.dot(v),
                 n.getX(), n.getY(), n.getZ(), -location.dot(n),
                 0f, 0f, 0f, 1f);
        
        // projection matrix
        if (useOrtho)
            projection.set(2f / (frustumRight - frustumLeft), 0f, 0f, -(frustumRight + frustumLeft) / (frustumRight - frustumLeft),
                           0f, 2f / (frustumTop - frustumBottom), 0f, -(frustumTop + frustumBottom) / (frustumTop - frustumBottom),
                           0f, 0f, 2f / (frustumNear - frustumFar), -(frustumFar + frustumNear) / (frustumFar - frustumNear),
                           0f, 0f, 0f, 1f);
        else
            projection.set(2f * frustumNear / (frustumRight - frustumLeft), 0f, (frustumRight + frustumLeft) / (frustumRight - frustumLeft), 0f,
                           0f, 2f * frustumNear / (frustumTop - frustumBottom), (frustumTop + frustumBottom) / (frustumTop - frustumBottom), 0f,
                           0f, 0f, -(frustumFar + frustumNear) / (frustumFar - frustumNear), -2f * frustumFar * frustumNear / (frustumFar - frustumNear),
                           0f, 0f, -1f, 0f);
        
        // generate world-space frustum planes
        if (useOrtho)
            computeOrthoWorldPlanes();
        else
            computePerspectiveWorldPlanes();
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
        setWorldPlane(plane, normal.getX(), normal.getY(), normal.getZ(), -normal.dot(pos));
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
