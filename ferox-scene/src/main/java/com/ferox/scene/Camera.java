package com.ferox.scene;

import com.ferox.renderer.Surface;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.annot.DefaultValue;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.ObjectProperty;

/**
 * <p>
 * Camera is a Component that specifies the viewing settings for a "camera" into
 * the scene of the EntitySystem. It represents a perspective projection and
 * stores the field of view, near and far z planes. It is also attached to a
 * {@link Surface} to actually render into. This surface determines the aspect
 * ratio that must be used when rendering with this camera. Additionally, the
 * camera takes its position and orientation from any transform-providing
 * component attached to the same entity.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Camera extends ComponentData<Camera> {
    /**
     * The shared TypedId instance corresponding to Camera.
     */
    public static final TypeId<Camera> ID = TypeId.get(Camera.class);
    
    private ObjectProperty<Surface> surface;
    
    @DefaultValue(defaultDouble=60.0)
    private DoubleProperty fov;
    
    @DefaultValue(defaultDouble=0.01)
    private DoubleProperty znear;
    
    @DefaultValue(defaultDouble=100.0)
    private DoubleProperty zfar;
    
    private Camera() { }
    
    /**
     * @return The field of view for this Camera, in degrees
     */
    public double getFieldOfView() {
        return fov.get(getIndex(), 0);
    }
    
    /**
     * Set the field of view for this Camera, in degrees.
     * 
     * @param fov The new field of view
     * @return This Camera for chaining purposes
     * @throws IllegalArgumentException if fov is less than 0 or greater than
     *             180
     */
    public Camera setFieldOfView(double fov) {
        if (fov < 0.0 || fov > 180.0)
            throw new IllegalArgumentException("Field of view must be in [0, 180]: " + fov);
        this.fov.set(fov, getIndex(), 0);
        return this;
    }
    
    /**
     * @return The distance to the near z plane of the camera
     */
    public double getNearZDistance() {
        return znear.get(getIndex(), 0);
    }
    
    /**
     * Set the distance to the near and far z planes.
     * 
     * @param znear The new near distance
     * @param zfar The new far distance
     * @return This Camera for chaining purposes
     * @throws IllegalArgumentException if znear is less than or equal to 0, or
     *             if zfar is less than znear
     */
    public Camera setZDistances(double znear, double zfar) {
        if (znear <= 0.0)
            throw new IllegalArgumentException("Near distances must be greater than 0: " + znear);
        if (znear > zfar)
            throw new IllegalArgumentException("Near distance must be less than far: " + znear + ", " + zfar);
        this.znear.set(znear, getIndex(), 0);
        this.zfar.set(zfar, getIndex(), 0);
        return this;
    }
    
    /**
     * @return The distance to the far z plane of the camera
     */
    public double getFarZDistance() {
        return zfar.get(getIndex(), 0);
    }

    /**
     * Return the Surface that this Camera is linked to.
     * 
     * @return The Surface of this Camera
     */
    public Surface getSurface() {
        return surface.get(getIndex(), 0);
    }

    /**
     * Set the current Surface of this Camera.
     * 
     * @param surface The new surface
     * @return This camera for chaining purposes
     * @throws NullPointerException if surface is null
     */
    public Camera setSurface(Surface surface) {
        this.surface.set(surface, getIndex(), 0);
        return this;
    }
}
