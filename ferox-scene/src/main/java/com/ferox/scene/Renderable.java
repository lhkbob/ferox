package com.ferox.scene;

import com.ferox.math.Const;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.math.bounds.Frustum;
import com.ferox.math.entreri.AxisAlignedBoxProperty;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.util.geom.Geometry;
import com.lhkbob.entreri.Controller;
import com.lhkbob.entreri.Entity;
import com.lhkbob.entreri.TypeId;
import com.lhkbob.entreri.Unmanaged;
import com.lhkbob.entreri.property.ElementSize;
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
public final class Renderable extends EntitySetComponent<Renderable> {
    /**
     * The shared TypedId representing Renderable.
     */
    public static final TypeId<Renderable> ID = TypeId.get(Renderable.class);
    
    private static final int INDEX_OFFSET = 0;
    private static final int INDEX_COUNT = 1;
    
    private ObjectProperty<VertexAttribute> vertices;
    private ObjectProperty<VertexBufferObject> indices;
    private ObjectProperty<PolygonType> polyType;
    
    @ElementSize(2)
    private IntProperty indexConfig; // 0 = offset, 1 = count
    
    private AxisAlignedBoxProperty localBounds;
    private AxisAlignedBoxProperty worldBounds;
    
    @Unmanaged
    private final AxisAlignedBox localBoundsCache = new AxisAlignedBox();
    
    @Unmanaged
    private final AxisAlignedBox worldBoundsCache = new AxisAlignedBox();

    private Renderable() { }
    
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
        if (vertices == null)
            throw new NullPointerException("Vertices cannot be null");
        if (vertices.getData().getData().getDataType() != DataType.FLOAT)
            throw new IllegalArgumentException("Vertices must have a datatype of FLOAT");
        if (vertices.getElementSize() == 1)
            throw new IllegalArgumentException("Vertices can only have an element size of 2, 3, or 4");
        
        this.vertices.set(vertices, getIndex(), 0);
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
    public Renderable setIndices(PolygonType type, VertexBufferObject indices, int first, int count) {
        if (type == null)
            throw new NullPointerException("PolygonType cannot be null");
        if (indices != null && indices.getData().getDataType() == DataType.FLOAT)
            throw new IllegalArgumentException("Indices cannot have a FLOAT datatype");
        if (first < 0 || count < 0)
            throw new IllegalArgumentException("First and count must be at least 0");
        if (indices != null && (first + count) > indices.getData().getLength())
            throw new IndexOutOfBoundsException("First and count would reference out-of-bounds indices");
        
        int componentIndex = getIndex();
        
        this.indices.set(indices, componentIndex, 0);
        polyType.set(type, componentIndex, 0);
        indexConfig.set(first, componentIndex, INDEX_OFFSET);
        indexConfig.set(count, componentIndex, INDEX_COUNT);
        
        return this;
    }

    /**
     * @return The vertex attribute containing vertex information for the
     *         geometry. If the entity has a transform, the vertices are
     *         transformed before being rendered.
     */
    public VertexAttribute getVertices() {
        return vertices.get(getIndex(), 0);
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
        return indices.get(getIndex(), 0);
    }

    /**
     * @return The number of indices to render (even when indices are implicit
     *         array indices)
     */
    public int getIndexCount() {
        return indexConfig.get(getIndex(), INDEX_COUNT);
    }

    /**
     * @return The offset into the indices (even if indices are implicit array
     *         indices)
     */
    public int getIndexOffset() {
        return indexConfig.get(getIndex(), INDEX_OFFSET);
    }
    
    /**
     * @return The PolygonType rendered by this Renderable
     */
    public PolygonType getPolygonType() {
        return polyType.get(getIndex(), 0);
    }

    /**
     * Return true if this Entity has been flagged as visible to the given
     * Entity. Generally, it is assumed that <tt>e</tt> provides a Frustum
     * somehow (e.g. {@link Camera}. Implementations of {@link Controller} are
     * responsible for using this as appropriate
     * 
     * @param e The Entity to check visibility
     * @return Whether or not this component's entity is visible to e
     * @throws NullPointerException if f is null
     */
    public boolean isVisible(Entity e) {
        return contains(e.getId());
    }

    /**
     * As {@link #isVisible(Entity)} but only requires the id of an entity.
     * 
     * @param entityId The entity id to check visibility
     * @return Whether or not this component's entity is visible to entityId
     */
    public boolean isVisible(int entityId) {
        return contains(entityId);
    }

    /**
     * Set whether or not this Entity is considered visible to the Entity,
     * <tt>e</tt>. The method is provided so that Controllers can implement
     * their own visibility algorithms, instead of relying solely on
     * {@link ReadOnlyAxisAlignedBox#intersects(Frustum, com.ferox.math.bounds.PlaneState)}
     * . It is generally assumed that the input Entity somehow provides a
     * {@link Frustum}.
     * 
     * @param e The Entity whose visibility is assigned
     * @param pv Whether or not the Entity is visible to e
     * @return This component, for chaining purposes
     * @throws NullPointerException if f is null
     */
    public Renderable setVisible(Entity e, boolean pv) {
        return setVisible(e.getId(), pv);
    }

    /**
     * As {@link #setVisible(Entity, boolean)} but only requires the id of an
     * entity.
     * 
     * @param entityId The entity id to check visibility
     * @param pv Whether or not this component's entity is visible to entityId
     * @return This component, for chaining purposes
     */
    public Renderable setVisible(int entityId, boolean pv) {
        if (pv)
            put(entityId);
        else
            remove(entityId);
        return this;
    }

    /**
     * Reset the visibility flags so that the Entity is no longer visible to any
     * Frustums. Subsequent calls to {@link #isVisible(Entity)} will return
     * false until a Entity has been flagged as visible via
     * {@link #setVisible(Entity, boolean)}.
     * 
     * @return This component, for chaining purposes
     */
    public Renderable resetVisibility() {
        clear();
        return this;
    }

    /**
     * Return the local bounds of this Renderable. The returned AxisAlignedBox
     * instance is reused by this Renderable instance so it should be cloned
     * before changing which Component is referenced.
     * 
     * @return A cached local bounds instance
     */
    public @Const AxisAlignedBox getLocalBounds() {
        return localBoundsCache;
    }

    /**
     * Set the local bounds of this entity. The bounds should contain the
     * entire geometry of the Entity, including any modifications dynamic
     * animation might cause. 
     * 
     * @param bounds The new local bounds of the entity
     * @return This component, for chaining purposes
     * @throws NullPointerException if bounds is null
     */
    public Renderable setLocalBounds(@Const AxisAlignedBox bounds) {
        localBoundsCache.set(bounds);
        localBounds.set(bounds, getIndex());
        return this;
    }

    /**
     * Return the world bounds of this Renderable. The returned AxisAlignedBox
     * instance is reused by this Renderable instance so it should be cloned
     * before changing which Component is referenced.
     * 
     * @return A cached world bounds instance
     */
    public @Const AxisAlignedBox getWorldBounds() {
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
        worldBoundsCache.set(bounds);
        worldBounds.set(bounds, getIndex());
        return this;
    }
    
    @Override
    protected void onSet(int index) {
        worldBounds.get(index, worldBoundsCache);
        localBounds.get(index, localBoundsCache);
    }
}
