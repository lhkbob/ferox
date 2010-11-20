package com.ferox.physics.collision;

import java.util.BitSet;

import com.ferox.math.ReadOnlyMatrix4f;
import com.ferox.math.Transform;
import com.ferox.math.bounds.AxisAlignedBox;

/**
 * <p>
 * Collidable represents an instance of an object in a physics simulation
 * capable of being collided with. It has both {@link Shape shape} and a
 * {@link ReadOnlyMatrix4f transform} to describe its local geometry and its
 * world position and orientation. Additionally, it has pseudo-physical
 * parameters for determining its surface's friction coefficients and collision
 * response (e.g. elastic or inelastic).
 * </p>
 * <p>
 * Collidables also have configurable "groups" which can restrict the sets of
 * objects that can collide with each other. This is useful for supporting
 * no-clip like features in multiplyer games for members of the same team. Each
 * Collidable belongs to some number of integer groups; they also have a bit
 * mask specifying which groups they can collide against. When considering a
 * pair of Collidables for collision, the pair can collide if any of either
 * instance's groups are in the opposite's collision mask. A Collidable with no
 * group cannot collide with anything.
 * </p>
 * <p>
 * Individual instances of Collidable are not thread-safe.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Collidable {
    private final BitSet collisionGroups;
    private final BitSet collisionMask;

    private final Transform worldTransform;
    private final AxisAlignedBox worldAabb;
    private Shape bounds;
    
    private float restitution;
    private float friction;

    /**
     * Create a Collidable initially placed at the given position and
     * orientation stored in <tt>t</tt>, using the provided Shape. It is assumed
     * that the transform is an affine transform. It is recommended that
     * Collidables share the same instance of Shapes when possible. The created
     * Collidable initially uses a friction value of .5 and a restitution of 0.
     * 
     * @param t The initial location
     * @param shape The initial shape
     * @throws NullPointerException if t or shape are null
     */
    public Collidable(ReadOnlyMatrix4f t, Shape shape) {
        if (t == null)
            throw new NullPointerException("Transform cannot be null");
        if (shape == null)
            throw new NullPointerException("Shape cannot be null");
        
        worldTransform = new Transform(t);
        bounds = shape;
        worldAabb = new AxisAlignedBox();
        
        collisionGroups = new BitSet();
        collisionMask = new BitSet();
        
        // every Collidable starts out in a group
        setMemberOfGroup(0, true);
        setCollidesWithGroup(0, true);
        
        setFriction(.5f);
        setRestitution(0f);
        updateBounds();
    }

    /**
     * Return the friction coefficient for the surface of this Collidable. This
     * is a pseudo-physical value combining both static and kinetic friction. A
     * value of 0 represents no friction and higher values represent rougher
     * surfaces.
     * 
     * @return The current friction coefficient
     */
    public float getFriction() {
        return friction;
    }

    /**
     * Set the friction coefficient. See {@link #getFriction()} for more
     * details.
     * 
     * @param friction The new friction value
     * @throws IllegalArgumentException if friction is less than 0
     */
    public void setFriction(float friction) {
        if (friction < 0f)
            throw new IllegalArgumentException("Friction coefficient must be at least 0, not: " + friction);
        this.friction = friction;
    }

    /**
     * Get the restitution coefficient of this Collidable. Restitution is an
     * approximation of the elasticity of a surface. A restitution of 0
     * represents a fully inelastic collision. Higher values represent more
     * elastic collisions.
     * 
     * @return The current restitution coefficient
     */
    public float getRestitution() {
        return restitution;
    }

    /**
     * Set the new restitution coefficient. See {@link #getRestitution()} for
     * more details.
     * 
     * @param restitution The new restitution value
     * @throws IllegalArgumentException if restitution is less than 0
     */
    public void setRestitution(float restitution) {
        if (restitution < 0f)
            throw new IllegalArgumentException("Restitution coefficient must be at least 0, not: " + restitution);
        this.restitution = restitution;
    }

    /**
     * Compares the collision groups and masks of this Collidable and
     * <tt>other</tt> and returns true if the two instances are capable of
     * colliding. It is always true that
     * <code>objA.canCollide(objB) == objB.canCollide(objA)</code>.
     * 
     * @param other The other Collidable to check
     * @return True if the pair can collide
     * @throws NullPointerException if other is null
     */
    public boolean canCollide(Collidable other) {
        if (other == null)
            throw new NullPointerException("Collidable cannot be null");
        
        return collisionGroups.intersects(other.collisionMask) ||
               collisionMask.intersects(other.collisionGroups);
    }

    /**
     * Check if this Collidable is the member of the given integer group.
     * 
     * @param group The group, must be at least 0
     * @return True if the Collidable is in the group
     * @throws IndexOutOfBoundsException if the group is negative
     */
    public boolean isMemberOfGroup(int group) {
        return collisionGroups.get(group);
    }

    /**
     * Check if this Collidable is allowed to collide with specified group.
     * 
     * @param group The group to check
     * @return True if the Collidable can collide with the group
     * @throws IndexOutOfBoundsException if the group is negative
     */
    public boolean canCollideWithGroup(int group) {
        return collisionMask.get(group);
    }

    /**
     * Set whether or not this Collidable is the member of the provided group.
     * 
     * @param group The group that this Collidable is added to or removed from
     * @param member True if the Collidable should be a member
     * @throws IndexOutOfBoundsException if group is negative
     */
    public void setMemberOfGroup(int group, boolean member) {
        collisionGroups.set(group, member);
    }
    
    /**
     * Set whether or not this Collidable can collide with the provided group.
     * 
     * @param group The group that this Collidable can collide against
     * @param member True if the Collidable should collide with the group
     * @throws IndexOutOfBoundsException if group is negative
     */
    public void setCollidesWithGroup(int group, boolean collide) {
        collisionMask.set(group, collide);
    }

    /**
     * Return the matrix instance holding this Collidable's world transform. The
     * returned instance can be modified at any time as this is not a defensive
     * copy.
     * 
     * @return The world transform of the Collidable
     */
    public ReadOnlyMatrix4f getTransform() {
        return worldTransform;
    }

    /**
     * Assign a new Shape to use for this Collidable.
     * 
     * @param shape The new Shape
     * @throws NullPointerException if shape is null
     */
    public void setShape(Shape shape) {
        if (shape == null)
            throw new NullPointerException("Shape cannot be null");
        bounds = shape;
        updateBounds();
    }

    /**
     * Return the current Shape of this Collidable.
     * 
     * @return The shape of this Collidable
     */
    public Shape getShape() {
        return bounds;
    }

    /**
     * Copy <tt>t</tt> into this Collidable's transform, updating its location
     * and orientation. This will also recompute the Collidable's world bounds.
     * 
     * @param t The transform to copy
     * @throws NullPointerException if t is null
     */
    public void setTransform(ReadOnlyMatrix4f t) {
        if (t == null)
            throw new NullPointerException("Transform cannot be null");
        worldTransform.set(t);
        updateBounds();
    }

    /**
     * Return the current world bounds of this Collidable. This is computed
     * based off of the Shape's local bounds and the current world transform.
     * The returned instance can be updated at any point in time to reflect
     * changes to the shape or transform, and should be considered read-only.
     * 
     * @return The world bounds of this Collidable
     */
    public AxisAlignedBox getWorldBounds() {
        return worldAabb;
    }
    
    private void updateBounds() {
        bounds.getBounds().transform(worldTransform, worldAabb);
    }
}
