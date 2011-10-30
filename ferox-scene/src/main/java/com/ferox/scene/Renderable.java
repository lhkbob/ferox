package com.ferox.scene;

import com.ferox.renderer.Renderer.DrawStyle;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.BufferData.DataType;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.util.geom.Geometry;
import com.googlecode.entreri.Component;
import com.googlecode.entreri.EntitySystem;
import com.googlecode.entreri.InitParams;
import com.googlecode.entreri.TypedId;
import com.googlecode.entreri.property.IntProperty;
import com.googlecode.entreri.property.ObjectProperty;
import com.googlecode.entreri.property.Parameter;

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
 * <p>
 * Renderable defines one initialization parameter: the VertexAttribute
 * representing its vertices. It defaults to using no indices, a PolygonType of
 * POINT, and an index offset and count of 0. It is highly recommended that
 * adding a Renderable, is immediately followed up with a call to
 * {@link #setIndices(PolygonType, VertexBufferObject, int, int)}.
 * </p>
 * 
 * @author Michael Ludwig
 */
@InitParams({ VertexAttribute.class })
public final class Renderable extends Component {
    /**
     * The shared TypedId representing Renderable.
     */
    public static final TypedId<Renderable> ID = Component.getTypedId(Renderable.class);
    
    @Parameter(type=int.class, value="2")
    private ObjectProperty<DrawStyle> drawStyles; // 0 = front, 1 = back

    
    private ObjectProperty<VertexAttribute> vertices;
    private ObjectProperty<VertexBufferObject> indices;
    private ObjectProperty<PolygonType> polyType;
    
    @Parameter(type=int.class, value="2")
    private IntProperty indexConfig; // 0 = offset, 1 = count

    private Renderable(EntitySystem system, int index) {
        super(system, index);
    }
    
    @Override
    protected void init(Object... initParams) {
        setVertices((VertexAttribute) initParams[0]);
        setDrawStyle(DrawStyle.SOLID, DrawStyle.NONE);
        
        // This is a little lame, but it will result in entirely valid
        // geometry, so it's a good default
        setArrayIndices(PolygonType.POINTS, 0, 0);
    }
    
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
        indexConfig.set(first, componentIndex, 0);
        indexConfig.set(count, componentIndex, 1);
        
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
        return indexConfig.get(getIndex(), 1);
    }

    /**
     * @return The offset into the indices (even if indices are implicit array
     *         indices)
     */
    public int getIndexOffset() {
        return indexConfig.get(getIndex(), 0);
    }
    
    /**
     * @return The PolygonType rendered by this Renderable
     */
    public PolygonType getPolygonType() {
        return polyType.get(getIndex(), 0);
    }

    /**
     * Set both front and back draw styles for this Renderable.
     * 
     * @param front The DrawStyle for front-facing polygons
     * @param back The DrawStyle for back-facing polygons
     * @return This component, for chaining purposes
     * @throws NullPointerException if front or back are null
     */
    public Renderable setDrawStyle(DrawStyle front, DrawStyle back) {
        return setDrawStyleFront(front).setDrawStyleBack(back);
    }

    /**
     * Set the DrawStyle used when rendering front-facing polygons of this
     * Renderable.
     * 
     * @param front The front-facing DrawStyle
     * @return This component, for chaining purposes
     * @throws NullPointerException if front is null
     */
    public Renderable setDrawStyleFront(DrawStyle front) {
        if (front == null)
            throw new NullPointerException("DrawStyle cannot be null");
        drawStyles.set(front, getIndex(), 0);
        return this;
    }

    /**
     * @return The DrawStyle used for front-facing polygons
     */
    public DrawStyle getDrawStyleFront() {
        return drawStyles.get(getIndex(), 0);
    }

    /**
     * Set the DrawStyle used when rendering back-facing polygons of this
     * Renderable.
     * 
     * @param back The back-facing DrawStyle
     * @return This component, for chaining purposes
     * @throws NullPointerException if back is null
     */
    public Renderable setDrawStyleBack(DrawStyle back) {
        if (back == null)
            throw new NullPointerException("DrawStyle cannot be null");
        drawStyles.set(back, getIndex(), 1);
        return this;
    }

    /**
     * @return The DrawStyle used for back-facing polygons
     */
    public DrawStyle getDrawStyleBack() {
        return drawStyles.get(getIndex(), 1);
    }
}
