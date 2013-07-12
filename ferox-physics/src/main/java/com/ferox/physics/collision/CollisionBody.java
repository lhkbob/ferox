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
package com.ferox.physics.collision;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Matrix4;
import com.ferox.math.entreri.AxisAlignedBoxProperty;
import com.ferox.math.entreri.Matrix4Property;
import com.ferox.math.entreri.Matrix4Property.DefaultMatrix4;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.DoubleProperty;
import com.lhkbob.entreri.property.DoubleProperty.DefaultDouble;
import com.lhkbob.entreri.property.LongProperty;
import com.lhkbob.entreri.property.LongProperty.DefaultLong;
import com.lhkbob.entreri.property.ObjectProperty;

/**
 * <p/>
 * CollisionBody represents an instance of an object in a physics simulation capable of
 * being collided with. It has both {@link Shape shape} and a {@link Matrix4 transform} to
 * describe its local geometry and its world position and orientation. Additionally, it
 * has pseudo-physical parameters for determining its surface's friction coefficients and
 * collision response (e.g. elastic or inelastic).
 * <p/>
 * CollisionBodies also have configurable "groups" which can restrict the sets of objects
 * that can collide with each other. This is useful for supporting no-clip like features
 * in multiplyer games for members of the same team. Each CollisionBody belongs to some
 * number of integer groups; they also have a bit mask specifying which groups they can
 * collide against. When considering a pair of CollisionBodies for collision, the pair can
 * collide if any of either instance's groups are in the opposite's collision mask. A
 * CollisionBody with no group cannot collide with anything.
 *
 * @author Michael Ludwig
 */
public interface CollisionBody extends Component {
    /**
     * Return the friction coefficient for the surface of this CollisionBody. This is a
     * pseudo-physical value combining both static and kinetic friction. A value of 0
     * represents no friction and higher values represent rougher surfaces.
     *
     * @return The current friction coefficient
     */
    @DefaultDouble(0.5)
    public double getFriction();

    /**
     * Set the friction coefficient. See {@link #getFriction()} for more details.
     *
     * @param friction The new friction value
     *
     * @return This instance
     *
     */
    public CollisionBody setFriction(double friction);

    /**
     * Get the restitution coefficient of this CollisionBody. Restitution is an
     * approximation of the elasticity of a surface. A restitution of 0 represents a fully
     * inelastic collision. Higher values represent more elastic collisions.
     *
     * @return The current restitution coefficient
     */
    @DefaultDouble(0.0)
    public double getRestitution();

    /**
     * Set the new restitution coefficient. See {@link #getRestitution()} for more
     * details.
     *
     * @param restitution The new restitution value
     *
     * @return This instance
     *
     * @throws IllegalArgumentException if restitution is less than 0
     */
    public CollisionBody setRestitution(double restitution);

    /**
     * Return the matrix instance holding this Collidable's world transform.
     *
     * @return The world transform of the Collidable
     */
    @Const
    @DefaultMatrix4(m00 = 1.0, m01 = 0.0, m02 = 0.0, m03 = 0.0, m10 = 0.0, m11 = 1.0,
                    m12 = 0.0, m13 = 0.0, m20 = 0.0, m21 = 0.0, m22 = 1.0, m23 = 0.0,
                    m30 = 0.0, m31 = 0.0, m32 = 0.0, m33 = 1.0)
    public Matrix4 getTransform();

    /**
     * Assign a new Shape to use for this Collidable.
     *
     * @param shape The new Shape
     *
     * @return This component
     *
     * @throws NullPointerException if shape is null
     */
    public CollisionBody setShape(Shape shape);

    /**
     * Return the current Shape of this Collidable.
     *
     * @return The shape of this Collidable
     */
    public Shape getShape();

    /**
     * Copy <var>t</var> into this Collidable's transform, updating its location and
     * orientation. This will also recompute the Collidable's world bounds.
     *
     * @param t The transform to copy
     *
     * @return This component
     *
     * @throws NullPointerException if t is null
     */
    public CollisionBody setTransform(@Const Matrix4 t);

    /**
     * Return the current world bounds of this CollisionBody. This is computed based off
     * of the Shape's local bounds and the current world transform.
     *
     * @return The world bounds of this CollisionBody
     */
    @Const
    public AxisAlignedBox getWorldBounds();

    public CollisionBody setWorldBounds(@Const AxisAlignedBox bounds);

    public static class Utils {
        // FIXME this is so simple, we just might move it into a task
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
    }
}
