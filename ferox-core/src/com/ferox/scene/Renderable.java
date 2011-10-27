package com.ferox.scene;

import com.ferox.entity2.Component;
import com.ferox.entity2.Template;
import com.ferox.entity2.TypedComponent;
import com.ferox.entity2.TypedId;
import com.ferox.math.bounds.AxisAlignedBox;
import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.BufferData.DataType;
import com.ferox.util.geom.Geometry;

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
public final class Renderable extends TypedComponent<Renderable> {
    /**
     * The shared TypedId representing Renderable.
     */
    public static final TypedId<Renderable> ID = Component.getTypedId(Renderable.class);
    
    private DrawStyle frontStyle;
    private DrawStyle backStyle;

    private VertexAttribute vertices;
    private VertexBufferObject indices;
    private PolygonType polyType;
    private int indexOffset;
    private int indexCount;

    private final AxisAlignedBox localBounds;

    /**
     * Create a Renderable that renders the given vertices, and renders only
     * front facing polygons. It will use the provided bounds as the initial
     * local bounds. An index offset of 0 is used. If indices is not null, the
     * number of indices will equal the size of the indices buffer. If it is
     * null, the number of indices will equal the number of vertices.
     * 
     * @param vertices The vertices that will be rendered
     * @param indices The indices used to access the vertices, may be null
     * @param polyType The type of polygons formed by the indices
     * @param bounds The local bounds surrounding g
     * @throws NullPointerException if any argument except for indices is null
     */
    public Renderable(VertexAttribute vertices, VertexBufferObject indices, 
                      PolygonType polyType, AxisAlignedBox bounds) {
        this(vertices, indices, polyType, bounds, DrawStyle.SOLID, DrawStyle.NONE);
    }

    /**
     * Create a Renderable that renders the given vertices, and uses the given
     * DrawStyles for front and back facing polygons. It will use the provided
     * bounds as the initial local bounds. An index offset of 0 is used. If
     * indices is not null, the number of indices will equal the size of the
     * indices buffer. If it is null, the number of indices will equal the
     * number of vertices.
     * 
     * @param vertices The vertices that will be rendered
     * @param indices The indices used to access the vertices, may be null
     * @param polyType The type of polygons formed by the indices
     * @param bounds The local bounds surrounding g
     * @param front The DrawStyle for front facing polygons
     * @param back The DrawStyle for back facing polygons
     * @throws NullPointerException if any arguments except for indices is null
     */
    public Renderable(VertexAttribute vertices, VertexBufferObject indices, PolygonType polyType, 
                      AxisAlignedBox bounds, DrawStyle front, DrawStyle back) {
        this(vertices, indices, polyType, 0, 
             (indices == null ? vertices.getMaximumNumVertices() : indices.getData().getLength()), 
             bounds, front, back);
    }

    /**
     * Create a Renderable that renders the given vertices, and uses the given
     * DrawStyles for front and back facing polygons. It will use the provided
     * bounds as the initial local bounds. It will use <tt>first</tt> and
     * <tt>count</tt> to access the indices provided.
     * 
     * @param vertices The vertices that will be rendered
     * @param indices The indices used to access the vertices, may be null
     * @param polyType The type of polygons formed by the indices
     * @param first The offset into indices (or the array offset into vertices
     *            if indices is null)
     * @param count The number of indices to render
     * @param bounds The local bounds surrounding g
     * @param front The DrawStyle for front facing polygons
     * @param back The DrawStyle for back facing polygons
     * @throws NullPointerException if any arguments except for indices is null
     * @throws IllegalArgumentException if first or count are negative
     * @throws IndexOutOfBoundsException if (first + count) is larger than the
     *             number of renderable indices
     */
    public Renderable(VertexAttribute vertices, VertexBufferObject indices, PolygonType polyType,
                      int first, int count, AxisAlignedBox bounds, DrawStyle front, DrawStyle back) {
        super(null, false);
        localBounds = new AxisAlignedBox();
        
        setVertices(vertices);
        setIndices(indices, polyType, first, count);
        setLocalBounds(bounds);
        setDrawStyleFront(front);
        setDrawStyleBack(back);
    }

    /**
     * Create a Renderable that is a clone of <tt>clone</tt>, for use with a
     * {@link Template}.
     * 
     * @param clone The Renderable to clone
     * @throws NullPointerException if clone is null
     */
    public Renderable(Renderable clone) {
        super(clone, true);
        
        localBounds = new AxisAlignedBox(clone.localBounds);
        vertices = clone.vertices;
        indices = clone.indices;
        indexCount = clone.indexCount;
        indexOffset = clone.indexOffset;
        frontStyle = clone.frontStyle;
        backStyle = clone.backStyle;
    }

    /**
     * Set the vertex attribute that holds the vertex position information for
     * the Renderable. The way the vertices are combined in 3D primitives
     * depends on the indices and polygon type configured for the renderable.
     * See {@link #getIndices()} for more details.
     * 
     * @param vertices The new vertex attribute of vertices
     * @return The new version of the component
     * @throws NullPointerException if vertices is null
     * @throws IllegalArgumentException if the data type isn't FLOAT or if the
     *             element size is 1
     */
    public int setVertices(VertexAttribute vertices) {
        if (vertices == null)
            throw new NullPointerException("Vertices cannot be null");
        if (vertices.getData().getData().getDataType() != DataType.FLOAT)
            throw new IllegalArgumentException("Vertices must have a datatype of FLOAT");
        if (vertices.getElementSize() == 1)
            throw new IllegalArgumentException("Vertices can only have an element size of 2, 3, or 4");
        
        this.vertices = vertices;
        return notifyChange();
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
     * @return The new version of the component
     */
    public int setArrayIndices(PolygonType type, int first, int count) {
        return setIndices(null, type, first, count);
    }

    /**
     * Set the indices of this Renderable to <tt>indices</tt> and use the given
     * PolygonType to construct primitives. The index offset will be 0 and the
     * index count will the size of the VertexBufferObject.
     * 
     * @param indices The new indices VBO
     * @param type The new polygon type
     * @return The new version of the component
     * @throws NullPointerException if indices or type are null
     * @throws IllegalArgumentException if indices data type is FLOAT
     */
    public int setIndices(VertexBufferObject indices, PolygonType type) {
        return setIndices(indices, type, 0, indices.getData().getLength());
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
     * @param indices The new indices to use, may be null
     * @param type The new polygon type
     * @param first The offset into the indices or vertices (if indices is null)
     * @param count The number of indices or vertices to put together to create
     *            polygons (this is not the number of polygons)
     * @return The new version of the component
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if the indices data type is FLOAT
     * @throws IllegalArgumentException if first or count are less than 0
     * @throws IndexOutOfBoundsException if (first + count) is larger than the
     *             size of the indices
     */
    public int setIndices(VertexBufferObject indices, PolygonType type, int first, int count) {
        if (type == null)
            throw new NullPointerException("PolygonType cannot be null");
        if (indices != null && indices.getData().getDataType() == DataType.FLOAT)
            throw new IllegalArgumentException("Indices cannot have a FLOAT datatype");
        if (first < 0 || count < 0)
            throw new IllegalArgumentException("First and count must be at least 0");
        if (indices != null && (first + count) > indices.getData().getLength())
            throw new IndexOutOfBoundsException("First and count would reference out-of-bounds indices");
        
        this.indices = indices;
        polyType = type;
        indexCount = count;
        indexOffset = first;
        
        return notifyChange();
    }

    /**
     * @return The vertex attribute containing vertex information for the
     *         geometry. If the entity has a transform, the vertices are
     *         transformed before being rendered.
     */
    public VertexAttribute getVertices() {
        return vertices;
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
        return indices;
    }

    /**
     * @return The number of indices to render (even when indices are implicit
     *         array indices)
     */
    public int getIndexCount() {
        return indexCount;
    }

    /**
     * @return The offset into the indices (even if indices are implicit array
     *         indices)
     */
    public int getIndexOffset() {
        return indexOffset;
    }
    
    /**
     * @return The PolygonType rendered by this Renderable
     */
    public PolygonType getPolygonType() {
        return polyType;
    }
    
    /**
     * Return the local bounds of the Renderable. This will always return the
     * same instance, and the instance will be updated based on any calls to
     * {@link #setLocalBounds(AxisAlignedBox)}.
     * 
     * @return The local bounds
     */
    public AxisAlignedBox getLocalBounds() {
        // FIXME: update signature when we have read-only aabb's
        return localBounds;
    }

    /**
     * Set the local bounds of this Renderable. The bounds should contain the
     * entire Geometry of this Renderable, including any modifications dynamic
     * animation might cause. If a Visibility component is attached to an
     * entity, the local bounds can be used in frustum-culling.
     * 
     * @param bounds The new local bounds of the Renderable
     * @return The new version of the component, via {@link #notifyChange()}
     * @throws NullPointerException if bounds is null
     */
    public int setLocalBounds(AxisAlignedBox bounds) {
        localBounds.set(bounds);
        return notifyChange();
    }

    /**
     * Set both front and back draw styles for this Renderable.
     * 
     * @param front The DrawStyle for front-facing polygons
     * @param back The DrawStyle for back-facing polygons
     * @return The new version of the component
     * @throws NullPointerException if front or back are null
     */
    public int setDrawStyle(DrawStyle front, DrawStyle back) {
        if (front == null || back == null)
            throw new NullPointerException("DrawStyles cannot be null");
        frontStyle = front;
        backStyle = back;
        return notifyChange();
    }

    /**
     * Set the DrawStyle used when rendering front-facing polygons of this
     * Renderable.
     * 
     * @param front The front-facing DrawStyle
     * @return The new version of the Renderable
     * @throws NullPointerException if front is null
     */
    public int setDrawStyleFront(DrawStyle front) {
        if (front == null)
            throw new NullPointerException("DrawStyle cannot be null");
        frontStyle = front;
        return notifyChange();
    }

    /**
     * @return The DrawStyle used for front-facing polygons
     */
    public DrawStyle getDrawStyleFront() {
        return frontStyle;
    }

    /**
     * Set the DrawStyle used when rendering back-facing polygons of this
     * Renderable.
     * 
     * @param back The back-facing DrawStyle
     * @return The new version of the Renderable
     * @throws NullPointerException if back is null
     */
    public int setDrawStyleBack(DrawStyle back) {
        if (back == null)
            throw new NullPointerException("DrawStyle cannot be null");
        backStyle = back;
        return notifyChange();
    }

    /**
     * @return The DrawStyle used for back-facing polygons
     */
    public DrawStyle getDrawStyleBack() {
        return backStyle;
    }
}
