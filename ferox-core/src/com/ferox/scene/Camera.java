package com.ferox.scene;

import com.ferox.entity.Component;
import com.ferox.entity.Template;
import com.ferox.entity.TypedComponent;
import com.ferox.entity.TypedId;
import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.Surface;

/**
 * <p>
 * Camera is a Component that specifies the viewing point and projection
 * information necessary for rendering a scene onto a {@link Surface}. Cameras
 * are linked with a Surface that acts the target for any rendering performed
 * with the Camera. Each Camera contains a single {@link Frustum} that stores
 * the location, orientation and projection information to use when rendering.
 * </p>
 * <p>
 * The constructors provided are designed for the common use cases of
 * perspective projections that are centered on the Surface, or of orthographic
 * projections that span the Surface (useful for UI's and 2D pixel work). The
 * Frustum instance returned by {@link #getFrustum()} can be edited further if
 * these options are not acceptable, although {@link #notifyChange()} must be
 * invoked afterwards to signal any controller tracking changes to the
 * component.
 * </p>
 * <p>
 * Initially each Frustum (and thus Camera) uses a location of (0, 0, 0) with a
 * right-handed coordinate system pointing down the negative z-axis. A Camera
 * can be positioned manually by updating the orientation of its associated
 * Frustum. Alternatively, a {@link CameraController} can be used to have a
 * Camera match the position and direction provided by a {@link Transform}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Camera extends TypedComponent<Camera> {
    /**
     * The shared TypedId instance corresponding to Camera.
     */
    public static final TypedId<Camera> ID = Component.getTypedId(Camera.class);
    
    private Surface surface;
    private final Frustum frustum;

    /**
     * Create a Camera linked with the given Surface. Its initial
     * Frustum will use a field-of-view of 60 degrees, a near distance of .1 and
     * a far distance of 100. The aspect ratio will match that of the
     * Surface.
     * 
     * @param surface The Surface initially attached to this Camera
     */
    public Camera(Surface surface) {
        this(surface, 60f, .1f, 100f);
    }

    /**
     * Create a Camera linked with the given Surface, that uses the given values
     * for field-of-view, near distance and far distance. The aspect ratio will
     * match that of the Surface.
     * 
     * @param surface The Surface initially attached to this Camera
     * @param fov Field-of-view in degrees of the perspective projection of the
     *            frustum
     * @param znear Distance from camera location to near clipping plane
     * @param zfar Distance from camera location to far clipping plane
     * @throws IllegalArgumentException if znear >= zfar, or if znear <= 0, or
     *             if fov is outside of (0, 180]
     * @throws NullPointerException if surface is null
     */
    public Camera(Surface surface, float fov, float znear, float zfar) {
        super(null, false);
        setSurface(surface);
        frustum = new Frustum(fov, surface.getWidth() / (float) surface.getHeight(), znear, zfar);
    }

    /**
     * Create a Camera linked with the given Surface, that uses the given values
     * for near and far clipping plane distance. Unlike the other constructor,
     * the frustum is configured to be an orthographic frustum. The frustum
     * boundaries are configured to be from (0, 0) to (width, height).
     * 
     * @param surface The Surface initially attached to this Camera
     * @param znear The near clipping plane distance
     * @param zfar The far clipping plane distance
     * @throws IllegalArgumentException if znear >= zfar
     * @throws NullPointerException if surface is null
     */
    public Camera(Surface surface, float znear, float zfar) {
        super(null, false);
        setSurface(surface);
        frustum = new Frustum(true, 0f, surface.getWidth(), 0f, surface.getHeight(), znear, zfar);
    }

    /**
     * Create a Camera component that is a clone of <tt>clone</tt>, for use with
     * a {@link Template}.
     * 
     * @param clone The Component to clone
     * @throws NullPointerException if clone is null
     */
    public Camera(Camera clone) {
        super(clone, true);
        this.surface = clone.surface;
        frustum = new Frustum(clone.frustum.isOrthogonalProjection(), 
                              clone.frustum.getFrustumLeft(), clone.frustum.getFrustumRight(), 
                              clone.frustum.getFrustumBottom(), clone.frustum.getFrustumTop(), 
                              clone.frustum.getFrustumNear(), clone.frustum.getFrustumFar());
    }

    /**
     * <p>
     * Return the Frustum instance that represents how a rendered image should
     * be projected onto the {@link #getSurface() Surface of this Camera}. The
     * Frustum may be modified to change how things are rendered. Because the
     * Frustum is mutable, it is necessary to call {@link #notifyChange()}
     * manually.
     * </p>
     * <p>
     * However, keep in mind that the default behavior of the
     * {@link CameraController} is to preserve aspect ratios of the Frustum when
     * the Surface changes size.
     * </p>
     * 
     * @return The Frustum representing the projection for this Camera
     */
    public Frustum getFrustum() {
        return frustum;
    }

    /**
     * Return the Surface that this Camera is linked to. This will not
     * be null.
     * 
     * @return The Surface of this Camera
     */
    public Surface getSurface() {
        return surface;
    }

    /**
     * Set the Surface that this Camera is linked to.
     * 
     * @param surface The Surface that this
     * @return The new version of this Camera, via {@link #notifyChange()}
     */
    public int setSurface(Surface surface) {
        this.surface = surface;
        return notifyChange();
    }
}
