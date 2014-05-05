/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.math.bounds;

import com.ferox.math.*;

/**
 * <p/>
 * Frustum represents the mathematical construction of a frustum. It is described as a 6 sided convex hull,
 * where at least two planes are parallel to each other. It supports generating frustums that represent
 * perspective projections (a truncated pyramid), or orthographic projections (a rectangular prism).
 * <p/>
 * Each frustum has a direction vector and an up vector. These vectors define an orthonormal basis for the
 * frustum. The two parallel planes of the frustum are specified as distances along the direction vector (near
 * and far). The additional planes are computed based on the locations of the four corners of the near plane
 * intersection.
 * <p/>
 * The mapping from world space to frustum space is not as straight-forward as is implied by the above state.
 * Frustum provides the functionality to get the {@link #getProjectionMatrix() project matrix} and {@link
 * #getViewMatrix() modelview matrix} suitable for use in an OpenGL system. The camera within an OpenGL system
 * looks down its local negative z-axis. Thus the provided direction in this Frustum represents the negative
 * z-axis within camera space.
 *
 * @author Michael Ludwig
 */
public class Frustum {
    /**
     * Result of a frustum test against a {@link AxisAlignedBox}.
     */
    public static enum FrustumIntersection {
        /**
         * Returned when a candidate object is fully enclosed by the Frustum.
         */
        INSIDE,
        /**
         * Returned when a candidate object is completely outside of the Frustum.
         */
        OUTSIDE,
        /**
         * Returned when a candidate object intersects the Frustum but is not completely contained.
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

    // temporary vector used during intersection queries, saved to avoid allocation
    private final Vector3 temp;

    /**
     * Instantiate a new Frustum that's positioned at the origin, looking down the negative z-axis. The given
     * values are equivalent to those described in setPerspective() and are used for the initial frustum
     * parameters.
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
     * Instantiate a new Frustum that's positioned at the origin, looking down the negative z-axis. The six
     * values are equivalent to those specified in setFrustum() and are taken as the initial frustum
     * parameters.
     *
     * @param ortho True if the frustum values are for an orthographic projection, otherwise it's a
     *              perspective projection
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

        temp = new Vector3();
    }

    /**
     * Get the left edge of the near frustum plane.
     *
     * @return The left edge of the near frustum plane
     *
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     */
    public double getFrustumLeft() {
        return frustumLeft;
    }

    /**
     * Get the right edge of the near frustum plane.
     *
     * @return The right edge of the near frustum plane
     *
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     */
    public double getFrustumRight() {
        return frustumRight;
    }

    /**
     * Get the top edge of the near frustum plane.
     *
     * @return The top edge of the near frustum plane
     *
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     */
    public double getFrustumTop() {
        return frustumTop;
    }

    /**
     * Get the bottom edge of the near frustum plane.
     *
     * @return The bottom edge of the near frustum plane
     *
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     */
    public double getFrustumBottom() {
        return frustumBottom;
    }

    /**
     * Get the distance to the near frustum plane from the origin, in camera coords.
     *
     * @return The distance to the near frustum plane
     *
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     */
    public double getFrustumNear() {
        return frustumNear;
    }

    /**
     * Get the distance to the far frustum plane from the origin, in camera coords.
     *
     * @return The distance to the far frustum plane
     *
     * @see #setFrustum(boolean, double, double, double, double, double, double)
     */
    public double getFrustumFar() {
        return frustumFar;
    }

    /**
     * <p/>
     * Sets the dimensions of the viewing frustum in camera coords. left, right, bottom, and top specify edges
     * of the rectangular near plane. This plane is positioned perpendicular to the viewing direction, a
     * distance near along the direction vector from the view's location.
     * <p/>
     * If this Frustum is using orthogonal projection, the frustum is a rectangular prism extending from this
     * near plane, out to an identically sized plane, that is distance far away. If not, the far plane is the
     * far extent of a pyramid with it's point at the location, truncated at the near plane.
     *
     * @param ortho  True if the Frustum should use an orthographic projection
     * @param left   The left edge of the near frustum plane
     * @param right  The right edge of the near frustum plane
     * @param bottom The bottom edge of the near frustum plane
     * @param top    The top edge of the near frustum plane
     * @param near   The distance to the near frustum plane
     * @param far    The distance to the far frustum plane
     *
     * @throws IllegalArgumentException if left > right, bottom > top, near > far, or near <= 0 when the view
     *                                  isn't orthographic
     */
    public void setFrustum(boolean ortho, double left, double right, double bottom, double top, double near,
                           double far) {
        if (left > right || bottom > top || near > far) {
            throw new IllegalArgumentException("Frustum values would create an invalid frustum: " + left +
                                               " " +
                                               right + " x " + bottom + " " + top + " x " + near + " " + far);
        }
        if (near <= 0 && !ortho) {
            throw new IllegalArgumentException("Illegal value for near frustum when using perspective projection: " +
                                               near);
        }

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
     * Set the frustum to be perspective projection with the given field of view (in degrees). Widths and
     * heights are calculated using the assumed aspect ration and near and far values. Because perspective
     * transforms only make sense for non-orthographic projections, it also sets this view to be
     * non-orthographic.
     *
     * @param fov    The field of view
     * @param aspect The aspect ratio of the view region (width / height)
     * @param near   The distance from the view's location to the near camera plane
     * @param far    The distance from the view's location to the far camera plane
     *
     * @throws IllegalArgumentException if fov is outside of (0, 180], or aspect is <= 0, or near > far, or if
     *                                  near <= 0
     */
    public void setPerspective(double fov, double aspect, double near, double far) {
        if (fov <= 0f || fov > 180f) {
            throw new IllegalArgumentException("Field of view must be in (0, 180], not: " + fov);
        }
        if (aspect <= 0) {
            throw new IllegalArgumentException("Aspect ration must be >= 0, not: " + aspect);
        }

        double h = Math.tan(Math.toRadians(fov * .5f)) * near;
        double w = h * aspect;
        setFrustum(false, -w, w, -h, h, near, far);
    }

    /**
     * Set the frustum to be an orthographic projection that uses the given boundary edges for the near
     * frustum plane. The near value is set to -1, and the far value is set to 1.
     *
     * @param left
     * @param right
     * @param bottom
     * @param top
     *
     * @throws IllegalArgumentException if left > right or bottom > top
     * @see #setFrustum(boolean, double, double, double, double, double, double)
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
     * <p/>
     * Get the location vector of this view, in world space. The returned vector is read-only. Modifications
     * to the frustum's view parameters must be done through {@link #setOrientation(Vector3, Vector3,
     * Vector3)}.
     *
     * @return The location of the view
     */
    public
    @Const
    Vector3 getLocation() {
        return location;
    }

    /**
     * <p/>
     * Get the up vector of this view, in world space. Together up and direction form a right-handed
     * coordinate system. The returned vector is read-only. Modifications to the frustum's view parameters
     * must be done through {@link #setOrientation(Vector3, Vector3, Vector3)}.
     *
     * @return The up vector of this view
     */
    public
    @Const
    Vector3 getUp() {
        return up;
    }

    /**
     * <p/>
     * Get the direction vector of this frustum, in world space. Together up and direction form a right-handed
     * coordinate system. The returned vector is read-only. Modifications to the frustum's view parameters
     * must be done through {@link #setOrientation(Vector3, Vector3, Vector3)}.
     *
     * @return The current direction that this frustum is pointing
     */
    public
    @Const
    Vector3 getDirection() {
        return direction;
    }

    /**
     * Compute and return the field of view along the vertical axis that this Frustum uses. This is
     * meaningless for an orthographic projection, and returns -1 in that case. Otherwise, an angle in degrees
     * is returned in the range 0 to 180. This works correctly even when the bottom and top edges of the
     * Frustum are not centered about its location.
     *
     * @return The field of view of this Frustum
     */
    public double getFieldOfView() {
        if (useOrtho) {
            return -1f;
        }

        double fovTop = Math.atan(frustumTop / frustumNear);
        double fovBottom = Math.atan(frustumBottom / frustumNear);
        return Math.toDegrees(fovTop - fovBottom);
    }

    /**
     * <p/>
     * Return the 4x4 projection matrix that represents the mathematical projection from the frustum to
     * homogenous device coordinates (essentially the unit cube).
     * <p/>
     * <p/>
     * The returned matrix is read-only and will be updated automatically as the projection of the Frustum
     * changes.
     *
     * @return The projection matrix
     */
    public
    @Const
    Matrix4 getProjectionMatrix() {
        return projection;
    }

    /**
     * <p/>
     * Return the 'view' transform of this Frustum. The view transform represents the coordinate space
     * transformation from world space to camera/frustum space. The local basis of the Frustum is formed by
     * the left, up and direction vectors of the Frustum. The left vector is <code>up X direction</code>, and
     * up and direction are user defined vectors.
     * <p/>
     * The returned matrix is read-only and will be updated automatically as {@link #setOrientation(Vector3,
     * Vector3, Vector3)} is invoked.
     *
     * @return The view matrix
     */
    public
    @Const
    Matrix4 getViewMatrix() {
        return view;
    }

    /**
     * <p/>
     * Copy the given vectors into this Frustum for its location, direction and up vectors. The orientation is
     * then normalized and orthogonalized, but the provided vectors are unmodified.
     * <p/>
     * Any later changes to the vectors' x, y, and z values will not be reflected in the frustum planes or
     * view transform, unless this method is called again.
     *
     * @param location  The new location vector
     * @param direction The new direction vector
     * @param up        The new up vector
     *
     * @throws NullPointerException if location, direction or up is null
     */
    public void setOrientation(@Const Vector3 location, @Const Vector3 direction, @Const Vector3 up) {
        if (location == null || direction == null || up == null) {
            throw new NullPointerException("Orientation vectors cannot be null: " + location + " " +
                                           direction +
                                           " " + up);
        }

        this.location.set(location);
        this.direction.set(direction);
        this.up.set(up);
        update();
    }

    /**
     * Set the orientation of this Frustum based on the affine <var>transform</var>. The 4th column's first 3
     * values encode the transformation. The 3rd column holds the direction vector, and the 2nd column defines
     * the up vector.
     *
     * @param transform The new transform of the frustum
     *
     * @throws NullPointerException if transform is null
     */
    public void setOrientation(@Const Matrix4 transform) {
        if (transform == null) {
            throw new NullPointerException("Transform cannot be null");
        }

        this.location.set(transform.m03, transform.m13, transform.m23);
        this.direction.set(transform.m02, transform.m12, transform.m22);
        this.up.set(transform.m01, transform.m11, transform.m21);
        update();
    }

    /**
     * Set the orientation of this Frustum based on the given location vector and 3x3 rotation matrix.
     * Together the vector and rotation represent an affine transform that is treated the same as in {@link
     * #setOrientation(Matrix4)}.
     *
     * @param location The location of the frustum
     * @param rotation The rotation of the frustum
     *
     * @throws NullPointerException if location or rotation are null
     */
    public void setOrientation(@Const Vector3 location, @Const Matrix3 rotation) {
        if (location == null) {
            throw new NullPointerException("Location cannot be null");
        }
        if (rotation == null) {
            throw new NullPointerException("Rotation matrix cannot be null");
        }

        this.location.set(location);
        this.direction.set(rotation.m02, rotation.m12, rotation.m22);
        this.up.set(rotation.m01, rotation.m11, rotation.m21);
        update();
    }

    /**
     * <p/>
     * Return a plane representing the given plane of the view frustum, in world coordinates. This plane
     * should not be modified. The returned plane's normal is configured so that it points into the center of
     * the Frustum. The returned {@link Vector4} is encoded as a plane as defined in {@link Plane}; it is also
     * normalized.
     * <p/>
     * <p/>
     * The returned plane vector is read-only. It will be updated automatically when the projection or view
     * parameters change.
     *
     * @param i The requested plane index
     *
     * @return The ReadOnlyVector4f instance for the requested plane, in world coordinates
     *
     * @throws IndexOutOfBoundsException if plane isn't in [0, 5]
     */
    public
    @Const
    Vector4 getFrustumPlane(int i) {
        return worldPlanes[i];
    }

    /**
     * <p/>
     * Compute and return the intersection of the AxisAlignedBox and this Frustum, <var>f</var>. It is assumed
     * that the Frustum and AxisAlignedBox exist in the same coordinate frame. {@link
     * FrustumIntersection#INSIDE} is returned when the AxisAlignedBox is fully contained by the Frustum.
     * {@link FrustumIntersection#INTERSECT} is returned when this box is partially contained by the Frustum,
     * and {@link FrustumIntersection#OUTSIDE} is returned when the box has no intersection with the Frustum.
     * <p/>
     * If <var>OUTSIDE</var> is returned, it is guaranteed that the objects enclosed by the bounds do not
     * intersect the Frustum. If <var>INSIDE</var> is returned, any object {@link
     * AxisAlignedBox#contains(AxisAlignedBox) contained} by the box will also be completely inside the
     * Frustum. When <var>INTERSECT</var> is returned, there is a chance that the true representation of the
     * objects enclosed by the box will be outside of the Frustum, but it is unlikely. This can occur when a
     * corner of the box intersects with the planes of <var>f</var>, but the shape does not exist in that
     * corner.
     * <p/>
     * The argument <var>planeState</var> can be used to hint to this function which planes of the Frustum
     * require checking and which do not. When a hierarchy of bounds is used, the planeState can be used to
     * remove unnecessary plane comparisons. If <var>planeState</var> is null it is assumed that all planes
     * need to be checked. If <var>planeState</var> is not null, this method will mark any plane that the box
     * is completely inside of as not requiring a comparison. It is the responsibility of the caller to save
     * and restore the plane state as needed based on the structure of the bound hierarchy.
     *
     * @param bounds     The bounds to test for intersection with this frustm
     * @param planeState An optional PlaneState hint specifying which planes to check
     *
     * @return A FrustumIntersection indicating how the frustum and bounds intersect
     *
     * @throws NullPointerException if bounds is null
     */
    public FrustumIntersection intersects(@Const AxisAlignedBox bounds, PlaneState planeState) {
        if (bounds == null) {
            throw new NullPointerException("Bounds cannot be null");
        }

        // early escape for potentially deeply nested nodes in a tree
        if (planeState != null && !planeState.getTestsRequired()) {
            return FrustumIntersection.INSIDE;
        }

        FrustumIntersection result = FrustumIntersection.INSIDE;
        double distMax;
        double distMin;
        int plane = 0;

        Vector4 p;
        for (int i = Frustum.NUM_PLANES - 1; i >= 0; i--) {
            if (planeState == null || planeState.isTestRequired(i)) {
                p = getFrustumPlane(plane);
                // set temp to the normal of the plane then compute the extent
                // in-place; this is safe but we'll have to reset temp to the
                // normal later if needed
                temp.set(p.x, p.y, p.z).farExtent(bounds, temp);
                distMax = Plane.getSignedDistance(p, temp, true);

                if (distMax < 0) {
                    // the point closest to the plane is behind the plane, so
                    // we know the bounds must be outside of the frustum
                    return FrustumIntersection.OUTSIDE;
                } else {
                    // the point closest to the plane is in front of the plane,
                    // but we need to check the farthest away point

                    // make sure to reset temp to the normal before computing
                    // the near extent in-place
                    temp.set(p.x, p.y, p.z).nearExtent(bounds, temp);
                    distMin = Plane.getSignedDistance(p, temp, true);

                    if (distMin < 0) {
                        // the farthest point is behind the plane, so at best
                        // this box will be intersecting the frustum
                        result = FrustumIntersection.INTERSECT;
                    } else {
                        // the box is completely contained by the plane, so
                        // the return result can be INSIDE or INTERSECT (if set by another plane)
                        if (planeState != null) {
                            planeState.setTestRequired(plane, false);
                        }
                    }
                }
            }
        }

        return result;
    }

    /*
     * Update the plane instances returned by getFrustumPlane() to reflect any
     * changes to the frustum's local parameters or orientation. Also update the
     * view transform and projection matrix.
     */
    private void update() {
        // compute the right-handed basis vectors of the frustum
        Vector3 n = new Vector3().scale(direction.normalize(), -1); // normalizes direction as well
        Vector3 u = new Vector3().cross(up, n).normalize();
        Vector3 v = up.cross(n, u).normalize(); // recompute up to properly orthogonal to direction

        // view matrix
        view.set(u.x, u.y, u.z, -location.dot(u), v.x, v.y, v.z, -location.dot(v), n.x, n.y, n.z,
                 -location.dot(n), 0f, 0f, 0f, 1f);

        // projection matrix
        if (useOrtho) {
            projection.set(2 / (frustumRight - frustumLeft), 0, 0,
                           -(frustumRight + frustumLeft) / (frustumRight - frustumLeft), 0,
                           2 / (frustumTop - frustumBottom), 0,
                           -(frustumTop + frustumBottom) / (frustumTop - frustumBottom), 0, 0,
                           2 / (frustumNear - frustumFar),
                           -(frustumFar + frustumNear) / (frustumFar - frustumNear), 0, 0, 0, 1);
        } else {
            projection.set(2 * frustumNear / (frustumRight - frustumLeft), 0,
                           (frustumRight + frustumLeft) / (frustumRight - frustumLeft), 0, 0,
                           2 * frustumNear / (frustumTop - frustumBottom),
                           (frustumTop + frustumBottom) / (frustumTop - frustumBottom), 0, 0, 0,
                           -(frustumFar + frustumNear) / (frustumFar - frustumNear),
                           -2 * frustumFar * frustumNear / (frustumFar - frustumNear), 0, 0, -1, 0);
        }

        // generate world-space frustum planes, we pass in n and u since we
        // created them to compute the view matrix and they're just garbage
        // at this point, might as well let plane generation reuse them.
        if (useOrtho) {
            computeOrthoWorldPlanes(n, u);
        } else {
            computePerspectiveWorldPlanes(n, u);
        }
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
        invHyp = 1 / Math.sqrt(frustumNear * frustumNear + frustumRight * frustumRight);
        n.scale(direction, Math.abs(frustumRight) / frustumNear).add(p).scale(frustumNear * invHyp);
        setWorldPlane(RIGHT_PLANE, n, location);

        // BOTTOM
        invHyp = 1 / Math.sqrt(frustumNear * frustumNear + frustumBottom * frustumBottom);
        n.scale(direction, Math.abs(frustumBottom) / frustumNear).add(up).scale(frustumNear * invHyp);
        setWorldPlane(BOTTOM_PLANE, n, location);

        // TOP
        invHyp = 1 / Math.sqrt(frustumNear * frustumNear + frustumTop * frustumTop);
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
        } else {
            cp.set(a, b, c, d);
        }
        Plane.normalize(cp);
    }
}
