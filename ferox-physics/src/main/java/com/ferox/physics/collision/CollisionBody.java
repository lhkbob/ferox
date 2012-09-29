package com.ferox.physics.collision;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.entreri.AxisAlignedBoxProperty;
import com.ferox.math.entreri.Matrix4Property;
import com.ferox.math.entreri.Matrix4Property.DefaultMatrix4;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;
import com.lhkbob.entreri.property.LongProperty;
import com.lhkbob.entreri.property.LongProperty.DefaultLong;
import com.lhkbob.entreri.property.ObjectProperty;

/**
 * <p>
 * CollisionBody represents an instance of an object in a physics simulation
 * capable of being collided with. It has both {@link Shape shape} and a
 * {@link Matrix4 transform} to describe its local geometry and its world
 * position and orientation. Additionally, it has pseudo-physical parameters for
 * determining its surface's friction coefficients and collision response (e.g.
 * elastic or inelastic).
 * <p>
 * CollisionBodies also have configurable "groups" which can restrict the sets
 * of objects that can collide with each other. This is useful for supporting
 * no-clip like features in multiplyer games for members of the same team. Each
 * CollisionBody belongs to some number of integer groups; they also have a bit
 * mask specifying which groups they can collide against. When considering a
 * pair of CollisionBodies for collision, the pair can collide if any of either
 * instance's groups are in the opposite's collision mask. A CollisionBody with
 * no group cannot collide with anything.
 * 
 * @author Michael Ludwig
 */
public class CollisionBody extends ComponentData<CollisionBody> {
    /**
     * Internally CollisionBody stores the groups inside a single long, so group
     * ids are limited to be between 0 and 63.
     */
    public static final int MAX_GROUPS = 64;

    /**
     * TypeId for CollisionBody's.
     */
    public static final TypeId<CollisionBody> ID = TypeId.get(CollisionBody.class);

    @DefaultMatrix4(m00=1.0, m01=0.0, m02=0.0, m03=0.0,
            m10=0.0, m11=1.0, m12=0.0, m13=0.0,
            m20=0.0, m21=0.0, m22=1.0, m23=0.0,
            m30=0.0, m31=0.0, m32=0.0, m33=1.0)
    private Matrix4Property worldTransform;

    private AxisAlignedBoxProperty worldBounds;
    private ObjectProperty<Shape> shape;

    @DefaultDouble(0.0)
    private DoubleProperty restitution;

    @DefaultDouble(0.5)
    private DoubleProperty friction;

    @DefaultLong(1)
    private LongProperty collisionGroups;

    @DefaultLong(1)
    private LongProperty collisionMask;

    @Unmanaged private final AxisAlignedBox boundsCache = new AxisAlignedBox();
    @Unmanaged private final Matrix4 transformCache = new Matrix4();

    private CollisionBody() { }

    /**
     * Return the friction coefficient for the surface of this CollisionBody. This
     * is a pseudo-physical value combining both static and kinetic friction. A
     * value of 0 represents no friction and higher values represent rougher
     * surfaces.
     * 
     * @return The current friction coefficient
     */
    public double getFriction() {
        return friction.get(getIndex());
    }

    /**
     * Set the friction coefficient. See {@link #getFriction()} for more
     * details.
     * 
     * @param friction The new friction value
     * @return This instance
     * @throws IllegalArgumentException if friction is less than 0
     */
    public CollisionBody setFriction(double friction) {
        if (friction < 0.0) {
            throw new IllegalArgumentException("Friction coefficient must be at least 0, not: " + friction);
        }
        this.friction.set(friction, getIndex());
        return this;
    }

    /**
     * Get the restitution coefficient of this CollisionBody. Restitution is an
     * approximation of the elasticity of a surface. A restitution of 0
     * represents a fully inelastic collision. Higher values represent more
     * elastic collisions.
     * 
     * @return The current restitution coefficient
     */
    public double getRestitution() {
        return restitution.get(getIndex());
    }

    /**
     * Set the new restitution coefficient. See {@link #getRestitution()} for
     * more details.
     * 
     * @param restitution The new restitution value
     * @return This instance
     * @throws IllegalArgumentException if restitution is less than 0
     */
    public CollisionBody setRestitution(double restitution) {
        if (restitution < 0.0) {
            throw new IllegalArgumentException("Restitution coefficient must be at least 0, not: " + restitution);
        }
        this.restitution.set(restitution, getIndex());
        return this;
    }

    /**
     * Compares the collision groups and masks of this CollisionBody and
     * <tt>other</tt> and returns true if the two instances are capable of
     * colliding. It is always true that
     * <code>objA.canCollide(objB) == objB.canCollide(objA)</code>.
     * 
     * @param other The other CollisionBody to check
     * @return True if the pair can collide
     * @throws NullPointerException if other is null
     */
    public boolean canCollide(CollisionBody other) {
        if (other == null) {
            throw new NullPointerException("Collidable cannot be null");
        }

        return isSet(collisionGroups.get(getIndex()), other.collisionMask.get(other.getIndex())) ||
                isSet(other.collisionGroups.get(other.getIndex()), collisionMask.get(getIndex()));
    }

    private static boolean isSet(long groups, long mask) {
        return (groups & mask) != 0;
    }

    private static long set(long groups, long group, boolean set) {
        if (set) {
            return groups | group;
        } else {
            return groups & ~group;
        }
    }

    /**
     * Check if this CollisionBody is the member of the given integer group. All
     * CollisionBodies are in group 0 by default.
     * 
     * @param group The group, must be at least 0
     * @return True if the CollisionBody is in the group
     * @throws IndexOutOfBoundsException if the group is negative or greater
     *             than MAX_GROUPS
     */
    public boolean isMemberOfGroup(int group) {
        if (group < 0 || group >= MAX_GROUPS) {
            throw new IndexOutOfBoundsException("Group must be in range [0, " + MAX_GROUPS + "), but was " + group);
        }
        long groups = collisionGroups.get(getIndex());
        return isSet(groups, 1 << group);
    }

    /**
     * Check if this CollisionBody is allowed to collide with specified group.
     * 
     * @param group The group to check
     * @return True if the CollisionBody can collide with the group
     * @throws IndexOutOfBoundsException if the group is negative or greater
     *             than MAX_GROUPS
     */
    public boolean canCollideWithGroup(int group) {
        if (group < 0 || group >= MAX_GROUPS) {
            throw new IndexOutOfBoundsException("Group must be in range [0, " + MAX_GROUPS + "), but was " + group);
        }
        long mask = collisionMask.get(getIndex());
        return isSet(1 << group, mask);
    }

    /**
     * Set whether or not this CollisionBody is the member of the provided
     * group.
     * 
     * @param group The group that this CollisionBody is added to or removed
     *            from
     * @param member True if the CollisionBody should be a member
     * @return This component
     * @throws IndexOutOfBoundsException if group is negative or greater than
     *             MAX_GROUPS
     */
    public CollisionBody setMemberOfGroup(int group, boolean member) {
        if (group < 0 || group >= MAX_GROUPS) {
            throw new IndexOutOfBoundsException("Group must be in range [0, " + MAX_GROUPS + "), but was " + group);
        }
        collisionGroups.set(set(collisionGroups.get(getIndex()), 1 << group, member), getIndex());
        return this;
    }

    /**
     * Set whether or not this CollisionBody can collide with the provided
     * group.
     * 
     * @param group The group that this CollisionBody can collide against
     * @param member True if the CollisionBody should collide with the group
     * @return This component
     * @throws IndexOutOfBoundsException if group is negative or greater than
     *             MAX_GROUPS
     */
    public CollisionBody setCollidesWithGroup(int group, boolean collide) {
        if (group < 0 || group >= MAX_GROUPS) {
            throw new IndexOutOfBoundsException("Group must be in range [0, " + MAX_GROUPS + "), but was " + group);
        }
        collisionMask.set(set(collisionMask.get(getIndex()), 1 << group, collide), getIndex());
        return this;
    }

    /**
     * Return the matrix instance holding this Collidable's world transform.
     * 
     * @return The world transform of the Collidable
     */
    public @Const Matrix4 getTransform() {
        return transformCache;
    }

    /**
     * Assign a new Shape to use for this Collidable.
     * 
     * @param shape The new Shape
     * @return This component
     * @throws NullPointerException if shape is null
     */
    public CollisionBody setShape(Shape shape) {
        if (shape == null) {
            throw new NullPointerException("Shape cannot be null");
        }
        this.shape.set(shape, getIndex());
        updateBounds();
        return this;
    }

    /**
     * Return the current Shape of this Collidable.
     * 
     * @return The shape of this Collidable
     */
    public Shape getShape() {
        return shape.get(getIndex());
    }

    /**
     * Copy <tt>t</tt> into this Collidable's transform, updating its location
     * and orientation. This will also recompute the Collidable's world bounds.
     * 
     * @param t The transform to copy
     * @return This component
     * @throws NullPointerException if t is null
     */
    public CollisionBody setTransform(@Const Matrix4 t) {
        transformCache.set(t);
        worldTransform.set(t, getIndex());
        updateBounds();
        return this;
    }

    /**
     * Return the current world bounds of this CollisionBody. This is computed
     * based off of the Shape's local bounds and the current world transform.
     * 
     * @return The world bounds of this CollisionBody
     */
    public @Const AxisAlignedBox getWorldBounds() {
        return boundsCache;
    }

    private void updateBounds() {
        Shape shape = getShape();
        if (shape != null) {
            // do the if check just to be nice, if someone calls the
            // setTransform() before setShape(), we really don't want to throw
            // a weird exception
            boundsCache.transform(shape.getBounds(), getTransform());
            worldBounds.set(boundsCache, getIndex());
        }
    }

    @Override
    protected void onSet(int index) {
        worldTransform.get(index, transformCache);
        worldBounds.get(index, boundsCache);
    }
}
