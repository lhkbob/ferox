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
import com.ferox.math.entreri.AxisAlignedBoxProperty;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.util.geom.Geometry;
import com.lhkbob.entreri.ComponentData;
import com.lhkbob.entreri.Requires;
import com.lhkbob.entreri.SharedInstance;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.IntProperty;
import com.lhkbob.entreri.property.ObjectProperty;

/**
 * Renderable is a Component that enables an Entity to be rendered. It provides
 * a {@link Geometry} containing the vertex buffer information needed to render
 * the Entity and {@link DrawStyle DrawStyles} determining how each polygon is
 * rendered. To enable frustum-culling, the Renderable also stores an
 * axis-aligned bounding box that contains the geometry.</p>
 * <p>
 * The Renderable should be combined with a {@link Transform} to place the
 * Entity within a rendered scene. Many additional Components in this package
 * can be used to describe the materials, shaders and textures used to color and
 * light the rendered Entity.
 * </p>
 * 
 * @author Michael Ludwig
 */
@Requires(Transform.class)
public final class Renderable extends ComponentData<Renderable> {
    private ObjectProperty<VertexAttribute> vertices;
    private ObjectProperty<VertexBufferObject> indices;
    private ObjectProperty<PolygonType> polyType;

    private IntProperty indexOffset;
    private IntProperty indexCount;

    private AxisAlignedBoxProperty localBounds;
    private AxisAlignedBoxProperty worldBounds;

    @Unmanaged
    private final AxisAlignedBox localBoundsCache = new AxisAlignedBox();

    @Unmanaged
    private final AxisAlignedBox worldBoundsCache = new AxisAlignedBox();

    private Renderable() {}

    /**
     * Set the vertex attribute that holds the vertex position information for
     * the Renderable. The way the vertices are combined in 3D primitives
     * depends on the indices and polygon type configured for the renderable.
     * See {@link #getIndices()} for more details.
     * 
     * @param vertices The new vertex attribute of vertices
     * @return This component, for chaining purposes
     * @throws NullPointerException if vertices is null
     * @throws IllegalArgumentException if the data type isn't FLOAT or if the
     *             element size is 1
     */
    public Renderable setVertices(VertexAttribute vertices) {
        if (vertices == null) {
            throw new NullPointerException("Vertices cannot be null");
        }
        if (vertices.getData().getData().getDataType() != DataType.FLOAT) {
            throw new IllegalArgumentException("Vertices must have a datatype of FLOAT");
        }
        if (vertices.getElementSize() == 1) {
            throw new IllegalArgumentException("Vertices can only have an element size of 2, 3, or 4");
        }

        this.vertices.set(vertices, getIndex());
        return this;
    }

    /**
     * Convenience function to set the indices of this Renderable to null,
     * causing it to use implicit array indices. Calling this method with
     * arguments <tt>first</tt> and <tt>count</tt> is the same as calling
     * {@link #setIndices(VertexBufferObject, PolygonType)} with a buffer
     * created as <code>[i for i in range(first, first + count)]</code>.
     * 
     * @param type The polygon type to render
     * @param first The offset into the vertex attributes
     * @param count The number of consecutive vertices to turn into polygons
     * @return This component, for chaining purposes
     */
    public Renderable setArrayIndices(PolygonType type, int first, int count) {
        return setIndices(type, null, first, count);
    }

    /**
     * Set the indices of this Renderable to <tt>indices</tt> and use the given
     * PolygonType to construct primitives. The index offset will be 0 and the
     * index count will the size of the VertexBufferObject.
     * 
     * @param type The new polygon type
     * @param indices The new indices VBO
     * @return This component, for chaining purposes
     * @throws NullPointerException if indices or type are null
     * @throws IllegalArgumentException if indices data type is FLOAT
     */
    public Renderable setIndices(PolygonType type, VertexBufferObject indices) {
        return setIndices(type, indices, 0, indices.getData().getLength());
    }

    /**
     * <p>
     * Set the indices of this Renderable to <tt>indices</tt> and use the given
     * PolygonType to construct primitives. The first index used will be at
     * <tt>first</tt> in the buffer. Then the remaining <tt>count - 1</tt>
     * indices will be read consecutively from the buffer.
     * </p>
     * <p>
     * If <tt>indices</tt> is null, implicit array indices are used to render
     * the geometry instead. In this case, <tt>first</tt> represents the first
     * vertex used, and <tt>count</tt> vertices are consecutively read from the
     * vertices and other attributes. See {@link #getIndices()}.
     * </p>
     * 
     * @param type The new polygon type
     * @param indices The new indices to use, may be null
     * @param first The offset into the indices or vertices (if indices is null)
     * @param count The number of indices or vertices to put together to create
     *            polygons (this is not the number of polygons)
     * @return This component, for chaining purposes
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if the indices data type is FLOAT
     * @throws IllegalArgumentException if first or count are less than 0
     * @throws IndexOutOfBoundsException if (first + count) is larger than the
     *             size of the indices
     */
    public Renderable setIndices(PolygonType type, VertexBufferObject indices, int first,
                                 int count) {
        if (type == null) {
            throw new NullPointerException("PolygonType cannot be null");
        }
        if (indices != null && indices.getData().getDataType() == DataType.FLOAT) {
            throw new IllegalArgumentException("Indices cannot have a FLOAT datatype");
        }
        if (first < 0 || count < 0) {
            throw new IllegalArgumentException("First and count must be at least 0");
        }
        if (indices != null && (first + count) > indices.getData().getLength()) {
            throw new IndexOutOfBoundsException("First and count would reference out-of-bounds indices");
        }

        int componentIndex = getIndex();

        this.indices.set(indices, componentIndex);
        polyType.set(type, componentIndex);
        indexOffset.set(first, componentIndex);
        indexCount.set(count, componentIndex);

        updateVersion();
        return this;
    }

    /**
     * @return The vertex attribute containing vertex information for the
     *         geometry. If the entity has a transform, the vertices are
     *         transformed before being rendered.
     */
    public VertexAttribute getVertices() {
        return vertices.get(getIndex());
    }

    /**
     * Return the indices used access the vertices of the renderable, and any
     * other vertex attributes that are associated with this entity from other
     * components. If the indices are null, the vertices are accessed in order,
     * starting at 0 (effectively the indices are
     * <code>[i for i in range(first, first + count)]</code>).
     * 
     * @return The indices, may be null
     */
    public VertexBufferObject getIndices() {
        return indices.get(getIndex());
    }

    /**
     * @return The number of indices to render (even when indices are implicit
     *         array indices)
     */
    public int getIndexCount() {
        return indexCount.get(getIndex());
    }

    /**
     * @return The offset into the indices (even if indices are implicit array
     *         indices)
     */
    public int getIndexOffset() {
        return indexOffset.get(getIndex());
    }

    /**
     * @return The PolygonType rendered by this Renderable
     */
    public PolygonType getPolygonType() {
        return polyType.get(getIndex());
    }

    /**
     * Return the local bounds of this Renderable. The returned AxisAlignedBox
     * instance is reused by this Renderable instance so it should be cloned
     * before changing which Component is referenced.
     * 
     * @return A cached local bounds instance
     */
    @Const
    @SharedInstance
    public AxisAlignedBox getLocalBounds() {
        localBounds.get(getIndex(), localBoundsCache);
        return localBoundsCache;
    }

    /**
     * Set the local bounds of this entity. The bounds should contain the entire
     * geometry of the Entity, including any modifications dynamic animation
     * might cause.
     * 
     * @param bounds The new local bounds of the entity
     * @return This component, for chaining purposes
     * @throws NullPointerException if bounds is null
     */
    public Renderable setLocalBounds(@Const AxisAlignedBox bounds) {
        localBounds.set(bounds, getIndex());
        updateVersion();
        return this;
    }

    /**
     * Return the world bounds of this Renderable. The returned AxisAlignedBox
     * instance is reused by this Renderable instance so it should be cloned
     * before changing which Component is referenced.
     * 
     * @return A cached world bounds instance
     */
    @Const
    @SharedInstance
    public AxisAlignedBox getWorldBounds() {
        worldBounds.get(getIndex(), worldBoundsCache);
        return worldBoundsCache;
    }

    /**
     * Set the world bounds of this entity. The bounds should contain the entire
     * geometry of the Entity, including any modifications dynamic animation
     * might cause, in world space. A controller or other processor must use
     * this method to keep the world bounds in sync with any changes to the
     * local bounds.
     * 
     * @param bounds The new world bounds of the entity
     * @return This component, for chaining purposes
     * @throws NullPointerException if bounds is null
     */
    public Renderable setWorldBounds(@Const AxisAlignedBox bounds) {
        worldBounds.set(bounds, getIndex());
        updateVersion();
        return this;
    }
}
