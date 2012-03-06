package com.ferox.scene;

import com.ferox.math.bounds.Frustum;
import com.ferox.renderer.Surface;
import com.ferox.scene.controller.CameraController;
import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.InitParams;
import com.googlecode.entreri.TypedId;
import com.googlecode.entreri.property.IntProperty;
import com.googlecode.entreri.property.ObjectProperty;
import com.googlecode.entreri.property.Parameter;

/**
 * <p>
 * Camera is a Component that specifies the viewing point and projection
 * information necessary for rendering a scene onto a {@link Surface}. Cameras
 * are linked with a Surface that acts the target for any rendering performed
 * with the Camera. Each Camera contains a single {@link Frustum} that stores
 * the location, orientation and projection information to use when rendering.
 * </p>
 * <p>
 * Initially each Frustum (and thus Camera) uses a location of (0, 0, 0) with a
 * right-handed coordinate system pointing down the negative z-axis. A Camera
 * can be positioned manually by updating the orientation of its associated
 * Frustum. Alternatively, a {@link CameraController} can be used to have a
 * Camera match the position and direction provided by a {@link Transform}.
 * </p>
 * <p>
 * Camera defines a single initialization parameter representing the Surface it
 * is attached to. By default the frustum is configured to be a perspective
 * frustum with an aspect ratio matching the ratio of the surface.
 * </p>
 * 
 * @author Michael Ludwig
 */
@InitParams(Surface.class)
public final class Camera extends Component {
    /**
     * The shared TypedId instance corresponding to Camera.
     */
    public static final TypedId<Camera> ID = Component.getTypedId(Camera.class);
    
    // Indexes into the viewport bulk property
    private static final int LEFT = 0;
    private static final int RIGHT = 1;
    private static final int BOTTOM = 2;
    private static final int TOP = 3;
    private static final int SCALE = 4;
    
    private ObjectProperty<Surface> surface;
    private ObjectProperty<Frustum> frustum;
    
    @Parameter(type=int.class, value="5")
    private IntProperty viewport;

    private Camera(EntitySystem system, int index) {
        super(system, index);
    }
    
    @Override
    protected void init(Object... initParams) {
        frustum.set(new Frustum(60f, 1f, .1f, 100f), getIndex(), 0);
        setSurface((Surface) initParams[0]);
    }

    /**
     * <p>
     * Return the Frustum instance that represents how a rendered image should
     * be projected onto the {@link #getSurface() Surface of this Camera}. The
     * Frustum may be modified to change how things are rendered.
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
        return frustum.get(getIndex(), 0);
    }

    /**
     * Return the Surface that this Camera is linked to. This will be
     * null if the camera is meant to be "disabled".
     * 
     * @return The Surface of this Camera
     */
    public Surface getSurface() {
        return surface.get(getIndex(), 0);
    }

    /**
     * Set the current Surface of this Camera, and update its Frustum to be a
     * perspective projection fitting the surface's dimensions.
     * 
     * @param surface The new surface
     * @return This camera for chaining purposes
     * @throws NullPointerException if surface is null
     */
    public Camera setSurface(Surface surface) {
        return setSurface(surface, true);
    }

    /**
     * <p>
     * Set the Surface that this Camera is linked to. If <tt>perspective</tt> is
     * true, the frustum will be updated to fit the surface's aspect ratio and
     * be a perspective projection. If it is false, the it will be set to an
     * orthographic projection fitted to the dimensions of the surface.
     * </p>
     * <p>
     * If the frustum was previously a perspective projection and it's being
     * updated to a perspective projection, the field-of-view and near and far
     * planes will be preserved; otherwise a field-of-view of 60 and planes at
     * .1 and 100 will be used. Similarly, if the frustum was previously an
     * orthographic projection and it's updated to an orthographic projection,
     * the near and far planes will be preserved; otherwise they will be updated
     * to -1 and 1.
     * </p>
     * 
     * @param surface The Surface that this camera will use
     * @param perspective True if the frustum should be a perspective transform,
     *            or false for an orthographic one
     * @return This camera for chaining purposes
     * @throws NullPointerException if surface is null
     */
    public Camera setSurface(Surface surface, boolean perspective) {
        if (surface == null)
            throw new NullPointerException("Surface cannot be null");
        
        this.surface.set(surface, getIndex(), 0);
        // update the frustum
        Frustum f = getFrustum();
        if (perspective) {
            float znear = f.getFrustumNear();
            float zfar = f.getFrustumFar();
            float fov = f.getFieldOfView();
            if (f.isOrthogonalProjection()) {
                znear = .1f;
                zfar = 100f;
                fov = 60f;
            }

            f.setPerspective(fov, surface.getWidth() / (float) surface.getHeight(), znear, zfar);
        } else {
            float znear = f.getFrustumNear();
            float zfar = f.getFrustumFar();
            if (!f.isOrthogonalProjection()) {
                znear = -1f;
                zfar = 1f;
            }
            f.setFrustum(true, 0, surface.getWidth(), 0, surface.getHeight(), znear, zfar);
        }
        
        viewport.set(0, getIndex(), LEFT);
        viewport.set(surface.getWidth(), getIndex(), RIGHT);
        viewport.set(0, getIndex(), BOTTOM);
        viewport.set(surface.getHeight(), getIndex(), TOP);
        
        return this;
    }
    
    public int getViewportLeft() {
        return viewport.get(getIndex(), LEFT);
    }
    
    public Camera setViewportLeft(int left) {
        viewport.set(left, getIndex(), LEFT);
        return this;
    }
    
    public int getViewportRight() {
        return viewport.get(getIndex(), RIGHT);
    }
    
    public Camera setViewportRight(int right) {
        viewport.set(right, getIndex(), RIGHT);
        return this;
    }
    
    public int getViewportBottom() {
        return viewport.get(getIndex(), BOTTOM);
    }
    
    public Camera setViewportBottom(int bottom) {
        viewport.set(bottom, getIndex(), BOTTOM);
        return this;
    }
    
    public int getViewportTop() {
        return viewport.get(getIndex(), TOP);
    }
    
    public Camera setViewportTop(int top) {
        viewport.set(top, getIndex(), TOP);
        return this;
    }
    
    public Camera setViewport(int left, int right, int bottom, int top) {
        return setViewportLeft(left)
              .setViewportRight(right)
              .setViewportBottom(bottom)
              .setViewportTop(top);
    }
    
    public boolean isViewportScaled() {
        return viewport.get(getIndex(), SCALE) != 0;
    }
    
    public Camera setViewportScaled(boolean scale) {
        if (scale)
            viewport.set(1, getIndex(), SCALE);
        else
            viewport.set(0, getIndex(), SCALE);
        return this;
    }
}
