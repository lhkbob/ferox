package com.ferox.util.geom;

import com.ferox.math.MutableVector3f;
import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.BufferData;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * <p>
 * A Rectangle is a single quad aligned with a specified x and y axis, in three
 * dimensions. It is very useful for fullscreen effects that require rendering a
 * rectangle across the entire screen.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Rectangle implements Geometry {
    // Holds vertices, normals, texture coordinates packed as V3F_N3F_T2F
    // ordered in such a way as to not need indices
    private final VertexBufferObject vertexAttributes;
    
    private final VertexAttribute vertices;
    private final VertexAttribute normals;
    private final VertexAttribute texCoords;

    /**
     * Create a Rectangle with an x basis vector of (1, 0, 0) and a y basis
     * vector of (0, 1, 0), and the given edge dimensions. The storage mode is
     * IN_MEMORY.
     * 
     * @param left The left edge of the rectangle
     * @param right The right edge of the rectangle
     * @param bottom The bottom edge of the rectangle
     * @param top The top edge of the rectangle
     * @throws IllegalArgumentException if left > right or bottom > top
     */
    public Rectangle(float left, float right, float bottom, float top) {
        this(left, right, bottom, top,
             new Vector3f(1f, 0f, 0f), new Vector3f(0f, 1f, 0f));
    }

    /**
     * Create a Rectangle with the given basis vectors and edge dimensions and a
     * storage mode of IN_MEMORY.
     * 

     * @param left The left edge of the rectangle
     * @param right The right edge of the rectangle
     * @param bottom The bottom edge of the rectangle
     * @param top The top edge of the rectangle
     * @param xAxis Local x-axis of the rectangle
     * @param yAxis Local y-axis of the rectangle
     * @throws IllegalArgumentException if left > right or bottom > top
     * @throws NullPointerException if xAxis or yAxis are null
     */
    public Rectangle(float left, float right, float bottom, float top,
                     ReadOnlyVector3f xAxis, ReadOnlyVector3f yAxis) {
        this(left, right, bottom, top, xAxis, yAxis, StorageMode.IN_MEMORY);
    }

    /**
     * Create a Rectangle with the given basis vectors, edge dimensions
     * and storage mode
     * 
     * @param xAxis
     * @param yAxis
     * @param left
     * @param right
     * @param bottom
     * @param top
     * @param type The compile type to use
     * @param vertexName The name for the vertex attribute
     * @param normalName The name for the normals attribute
     * @param tcName The name for the texture coordinates attribute
     * @throws IllegalArgumentException if left > right or bottom > top
     * @throws NullPointerException if xAxis, yAxis, or mode are null
     */
    public Rectangle(float left, float right, float bottom, float top, 
                     ReadOnlyVector3f xAxis, ReadOnlyVector3f yAxis,
                     StorageMode mode) {
        if (left > right || bottom > top)
            throw new IllegalArgumentException("Side positions of the square are incorrect");
        if (xAxis == null || yAxis == null)
            throw new NullPointerException("Axis cannot be null");
        if (mode == null)
            throw new NullPointerException("StorageMode cannot be null");
        
        MutableVector3f normal = xAxis.cross(yAxis, null);

        float[] va = new float[32];
        int i = 0;
        
        // lower-left
        va[i++] = xAxis.getX() * left + yAxis.getX() * bottom;
        va[i++] = xAxis.getY() * left + yAxis.getY() * bottom;
        va[i++] = xAxis.getZ() * left + yAxis.getZ() * bottom;

        va[i++] = normal.getX();
        va[i++] = normal.getY();
        va[i++] = normal.getZ();

        va[i++] = 0f;
        va[i++] = 0f;

        // lower-right
        va[i++] = xAxis.getX() * right + yAxis.getX() * bottom;
        va[i++] = xAxis.getY() * right + yAxis.getY() * bottom;
        va[i++] = xAxis.getZ() * right + yAxis.getZ() * bottom;

        va[i++] = normal.getX();
        va[i++] = normal.getY();
        va[i++] = normal.getZ();

        va[i++] = 1f;
        va[i++] = 0f;

        // uppper-right
        va[i++] = xAxis.getX() * right + yAxis.getX() * top;
        va[i++] = xAxis.getY() * right + yAxis.getY() * top;
        va[i++] = xAxis.getZ() * right + yAxis.getZ() * top;

        va[i++] = normal.getX();
        va[i++] = normal.getY();
        va[i++] = normal.getZ();

        va[i++] = 1f;
        va[i++] = 1f;

        // upper-left
        va[i++] = xAxis.getX() * left + yAxis.getX() * top;
        va[i++] = xAxis.getY() * left + yAxis.getY() * top;
        va[i++] = xAxis.getZ() * left + yAxis.getZ() * top;

        va[i++] = normal.getX();
        va[i++] = normal.getY();
        va[i++] = normal.getZ();

        va[i++] = 0f;
        va[i++] = 1f;

        vertexAttributes = new VertexBufferObject(new BufferData(va), mode);
        vertices = new VertexAttribute(vertexAttributes, 3, 0, 5);
        normals = new VertexAttribute(vertexAttributes, 3, 3, 5);
        texCoords = new VertexAttribute(vertexAttributes, 2, 6, 6);
    }

    @Override
    public PolygonType getPolygonType() {
        return PolygonType.QUADS;
    }

    @Override
    public VertexBufferObject getIndices() {
        return null;
    }

    @Override
    public int getIndexOffset() {
        return 0;
    }

    @Override
    public int getIndexCount() {
        return 4;
    }

    @Override
    public VertexAttribute getVertices() {
        return vertices;
    }

    @Override
    public VertexAttribute getNormals() {
        return normals;
    }

    @Override
    public VertexAttribute getTextureCoordinates() {
        return texCoords;
    }

    @Override
    public VertexAttribute getTangents() {
        throw new UnsupportedOperationException("NOT IMPLEMENTED YET");
    }
}
