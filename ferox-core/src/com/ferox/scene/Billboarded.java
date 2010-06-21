package com.ferox.scene;

import com.ferox.entity.AbstractComponent;
import com.ferox.math.Vector3f;

public class Billboarded extends AbstractComponent<Billboarded> {
    /**
     * Axis represents each of the three local basis vectors of a SceneElement.
     * Axis is used when constraining one of the local axis to a vector in world
     * space. It is also used when determining which axis constitutes the
     * direction axis for billboarding points.
     */
    public static enum Axis {
        X, Y, Z
    }
    
    private Vector3f pointTowards;
    private Axis pointAxis;
    
    private Vector3f constrainVector;
    private Axis constrainAxis;
    
    public Billboarded() {
        super(Billboarded.class);
    }
    
    /**
     * Get the point that this SceneElement will point towards. If the returned
     * Vector3f is null, then this SceneElement will use the rotation matrix
     * that was last set by its SceneElementUpdater (or manually). When the
     * vector is non-null, the SceneController will adjust the rotation matrix
     * so that the local axis specified by {@link #getBillboardDirectionAxis()}
     * points towards this vector. After this, matrix will be updated so that
     * the constraint axis is met.
     * 
     * @return The point that this SceneElement will point towards, as best its
     *         able
     */
    public Vector3f getBillboardPoint() {
        return pointTowards;
    }

    /**
     * Get the Axis that determines which of the local axis represents the
     * 'direction' that will line up towards the point returned by
     * {@link #getBillboardPoint()}. This will return null if
     * {@link #getBillboardPoint()} is null.
     * 
     * @return The direction Axis
     */
    public Axis getBillboardDirectionAxis() {
        return pointAxis;
    }

    /**
     * Set the billboard point and direction vector. If <tt>pointTowards</tt> is
     * non-null, the SceneElement will have its local <tt>axis</tt> directed
     * towards the point after each update by the SceneController. If the
     * {@link #getConstraintVector()} is non-null, then the rotation matrix will
     * be constrained to that vector after it's been set to point towards
     * <tt>pointTowards</tt>. The vector will not be copied, any modifications
     * will be reflected in this Billboarded.
     * 
     * @param pointTowards The point that this SceneElement will be directed to
     * @param axis The direction axis
     * @throws NullPointerException if pointTowards is not null, but axis is
     *             null
     */
    public void setBillboardPoint(Vector3f pointTowards, Axis axis) {
        if (pointTowards != null && axis == null)
            throw new NullPointerException("Axis must be non-null when pointTowards is not null");
        this.pointTowards = pointTowards;
        pointAxis = (pointTowards == null ? null : axis);
    }

    /**
     * Get the Axis that determines which of the three local axis are
     * constrained to match {@link #getConstraintVector()}. This will be null if
     * the constraint vector is null.
     * 
     * @return The constrained axis
     */
    public Axis getConstraintAxis() {
        return constrainAxis;
    }

    /**
     * Get the vector that one of the local axis of this SceneElement will be
     * constrained to. This can be used to force elements to remain vertical,
     * and to limit the degrees of freedom used when billboarding points. If the
     * vector is null, then no axis are constrained.
     * 
     * @return The vector that {@link #getConstraintAxis()} will be set to
     */
    public Vector3f getConstraintVector() {
        return constrainVector;
    }

    /**
     * Set the vector and axis that will be constrained after the SceneElement
     * has been updated, and then billboarded. If the vector is null, then no
     * axis will be constrained. Note that the vector value is not copied, any
     * modifications to <tt>vector</tt> will affect this Billboarded.
     * 
     * @param vector The vector that the given axis will be set to
     * @param axis The axis that will be constrained
     */
    public void setConstraintAxis(Vector3f vector, Axis axis) {
        if (vector != null && axis == null)
            throw new NullPointerException("Axis must be non-null when vector is not null");
        constrainVector = vector;
        constrainAxis = (vector == null ? null : axis);
    }
}
