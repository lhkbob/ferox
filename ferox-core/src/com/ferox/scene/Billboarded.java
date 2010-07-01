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
        X, Y, Z,
    }
    
    private final Vector3f[] constrainVectors; // as a direction or location
    private final boolean[] positionConstraint;
    private final boolean[] negateAxisDirection;

    /**
     * Create a new Billboarded instance, with its initial state to have no axis
     * constrained.
     */
    public Billboarded() {
        super(Billboarded.class);
        constrainVectors = new Vector3f[Axis.values().length];
        positionConstraint = new boolean[constrainVectors.length];
        negateAxisDirection = new boolean[constrainVectors.length];
    }

    /**
     * <p>
     * Get the Vector3f that represents the constraint on the provided axis. The
     * returned Vector3f is null when the axis is unconstrained. When not null,
     * the returned vector represents the a constraint. This constraint is
     * interpreted in different ways depending on
     * {@link #isPositionConstraint(Axis)} and
     * {@link #isConstraintAxisNegated(Axis)}.
     * </p>
     * <p>
     * If the constraint is a position constraint, the local axis' direction
     * will be constrained such that it points towards the constraint. When
     * false, the axis' direction will be the normalized vector parallel to the
     * constraint. If {@link #isConstraintAxisNegated(Axis)} is true, the
     * computed axis as described above are simply negated.
     * </p>
     * 
     * @param axis The Axis that may be constrained
     * @return A Vector3f describing the constraint, or null
     * @throws NullPointerException if axis is null
     */
    public Vector3f getConstraint(Axis axis) {
        return constrainVectors[axis.ordinal()];
    }

    /**
     * Return true if the vector returned by {@link #getConstraint(Axis)}
     * represents a position the Billboarded entity must face. If false, the
     * vector represents a simple direction vector.
     * 
     * @param axis The constrained axis
     * @return Whether or not the given axis' constraint is a position
     *         constraint
     */
    public boolean isPositionConstraint(Axis axis) {
        return constrainVectors[axis.ordinal()] != null && positionConstraint[axis.ordinal()];
    }

    /**
     * Return true if computed axis direction for the given axis should be
     * negated before updating the entity's transform.
     * 
     * @param axis The constrained axis
     * @return Whether or not the given axis' direction is reversed from what it
     *         would normally be, e.g. it faces away from instead of towards
     */
    public boolean isConstraintAxisNegated(Axis axis) {
        return constrainVectors[axis.ordinal()] != null && negateAxisDirection[axis.ordinal()];
    }

    /**
     * Set the constraint on the given axis to be a directional constraint that
     * is not negated. The constraint will use the provided vector. Subsequent
     * changes to <tt>vec</tt> will correctly update the desired position for a
     * Billboarded entity. If <tt>vec</tt> is null, the constraint is cleared
     * for the provided axis.
     * 
     * @param axis The axis to constrain
     * @param vec The Vector3f specifying the desired axis direction in world
     *            space
     * @throws NullPointerException if axis is null
     */
    public void setConstraint(Axis axis, Vector3f vec) {
        setConstraint(axis, vec, false);
    }

    /**
     * Set the constraint on the given axis to use the provided Vector3f. If
     * <tt>isPosition</tt> is true, the provided vector will be a position
     * constraint. The created constraint will not negate the axis direction. If
     * <tt>vec</tt> is null, the constraint is cleared for the provided axis.
     * 
     * @param axis The axis to constrain
     * @param vec The Vector3f specifying the desired axis direction or facing
     *            location
     * @param isPosition True if the constraint vector is a position
     * @throws NullPointerException if axis is null
     */
    public void setConstraint(Axis axis, Vector3f vec, boolean isPosition) {
        setConstraint(axis, vec, isPosition, false);
    }

    /**
     * Set the constraint on the given axis to use the provided Vector3f. If
     * <tt>isPosition</tt> is true, the provided vector will be a position
     * constraint. If <tt>negate</tt> is true, the computed axis' directions for
     * this axis will be automatically negated. If <tt>vec</tt> is null, the
     * constraint is cleared for the provided axis.
     * 
     * @param axis The axis to constrain
     * @param vec The Vector3f specifying the desired axis direction or facing
     *            location
     * @param isPosition True if the constraint vector is a position
     * @param negate True if computed axis directions should be negated
     * @throws NullPointerException if axis is null
     */
    public void setConstraint(Axis axis, Vector3f vec, boolean isPosition, boolean negate) {
        constrainVectors[axis.ordinal()] = vec;
        positionConstraint[axis.ordinal()] = isPosition;
        negateAxisDirection[axis.ordinal()] = negate;
    }
}
