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
package com.ferox.scene;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.geom.Geometry;
import com.lhkbob.entreri.Component;
import com.lhkbob.entreri.NoAutoVersion;
import com.lhkbob.entreri.NotNull;
import com.lhkbob.entreri.Requires;
import com.lhkbob.entreri.property.Named;
import com.lhkbob.entreri.property.SharedInstance;

/**
 * Renderable is a Component that enables an Entity to be rendered. It provides a {@link Geometry} containing
 * the vertex buffer information needed to render the Entity and {@link DrawStyle DrawStyles} determining how
 * each polygon is rendered. To enable frustum-culling, the Renderable also stores an axis-aligned bounding
 * box that contains the geometry.
 * <p/>
 * The Renderable should be combined with a {@link Transform} to place the Entity within a rendered scene.
 * Many additional Components in this package can be used to describe the materials, shaders and textures used
 * to color and light the rendered Entity.
 *
 * @author Michael Ludwig
 */
@Requires(Transform.class)
public interface Renderable extends Component {
    /**
     * @return The DrawStyle for front facing polygons
     */
    public DrawStyle getFrontDrawStyle();

    /**
     * @return The DrawStyle for back facing polygons
     */
    public DrawStyle getBackDrawStyle();

    /**
     * Set the front and back draw styles for polygons rendered by this Renderable.
     *
     * @param front The front style
     * @param back  The back style
     *
     * @return This component
     */
    public Renderable setDrawStyle(@Named("frontDrawStyle") DrawStyle front,
                                   @Named("backDrawStyle") DrawStyle back);

    /**
     * Get the Geometry instance that determines the shape and local bounds of the entity for rendering
     * purposes.
     *
     * @return The geometry of the entity
     */
    public Geometry getGeometry();

    /**
     * Assign the Geometry instance that is rendered for the entity.
     *
     * @param g The new geometry
     *
     * @return This component
     */
    public Renderable setGeometry(@NotNull Geometry g);

    /**
     * Return the world bounds of this Renderable. The returned AxisAlignedBox instance is reused by this
     * Renderable instance so it should be cloned before changing which Component is referenced.
     *
     * @return A cached world bounds instance
     */
    @Const
    @SharedInstance
    @NoAutoVersion
    public AxisAlignedBox getWorldBounds();

    /**
     * Set the world bounds of this entity. The bounds should contain the entire geometry of the Entity,
     * including any modifications dynamic animation might cause, in world space. A controller or other
     * processor must use this method to keep the world bounds in sync with any changes to the local bounds.
     * <p/>
     * Note that unlike all other properties of the renderable, setting the world bounds does not update the
     * version of the component. This is because the world bounds is dependent on the local bounds (which will
     * update the version) and the transform (not part of the renderable).
     *
     * @param bounds The new world bounds of the entity
     *
     * @return This component, for chaining purposes
     */
    public Renderable setWorldBounds(@Const AxisAlignedBox bounds);
}
