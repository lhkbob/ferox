package com.ferox.util.geom;

import com.ferox.math.AxisAlignedBox;
import com.ferox.math.Const;
import com.ferox.math.Vector3;
import com.ferox.renderer.Renderer.PolygonType;
import com.ferox.resource.BufferData;
import com.ferox.resource.VertexAttribute;
import com.ferox.resource.VertexBufferObject;
import com.ferox.resource.VertexBufferObject.StorageMode;

/**
 * <p>
 * A Box represents a 6 sided rectangular prism. By default, a Box is configured
 * to have its vertices, normals and texture coordinates use the default
 * attribute names defined in Geometry.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Box implements Geometry {
    // Holds vertices, normals, texture coordinates packed as V3F_N3F_T2F
    // ordered in such a way as to not need indices
    private final VertexBufferObject vertexAttributes;
    
    private final VertexAttribute vertices;
    private final VertexAttribute normals;
    private final VertexAttribute texCoords;
    
    private final AxisAlignedBox bounds;
    
    /**
     * Construct a box centered on its origin, with the given side length. So,
     * Box(1f) creates a unit cube. Uses StorageMode.IN_MEMORY for its
     * VertexBufferObjects.
     * 
     * @param side The side length of the created cube
     * @throws IllegalArgumentException if side is negative
     */
    public Box(float side) {
        this(side, StorageMode.IN_MEMORY);
    }

    /**
     * Construct a new Box with the given minimum and maximum points. These
     * points are opposite corners of the box. Uses StorageMode.IN_MEMORY for
     * its VertexBufferObjects.
     * 
     * @param min Minimum corner of the box
     * @param max Maximum corner of the box
     * @throws NullPointerException if min or max are null
     * @throws IllegalArgumentException if min has any coordinate less than the
     *             corresponding coordinate of max
     */
    public Box(@Const Vector3 min, @Const Vector3 max) {
        this(min, max, StorageMode.IN_MEMORY);
    }

    /**
     * Construct a box centered on its origin, with the given side length. So,
     * Box(1f) creates a unit cube.
     * 
     * @param side The side length of the created cube
     * @param mode The storage mode to use for the Box
     * @throws NullPointerException if mode is null
     * @throws IllegalArgumentException if side is negative
     */
    public Box(float side, StorageMode mode) {
        this(new Vector3(-side / 2f, -side / 2f, -side / 2f), 
             new Vector3(side / 2f, side / 2f, side / 2f), mode);
    }

    /**
     * Construct a new Box with the given minimum and maximum points. These
     * points are opposite corners of the box.
     * 
     * @param min Minimum corner of the box
     * @param max Maximum corner of the box
     * @param mode The compile type to use for the Box
     * @throws NullPointerException if min, max or mode are null
     * @throws IllegalArgumentException if min has any coordinate less than the
     *             corresponding coordinate of max
     */
    public Box(@Const Vector3 min, @Const Vector3 max, StorageMode mode) {
        if (min == null || max == null)
            throw new NullPointerException("Min and max vectors cannot be null");
        if (mode == null)
            throw new NullPointerException("StorageMode cannot be null");
        
        if (min.x > max.x || min.y > max.y || min.z > max.z)
            throw new IllegalArgumentException("Min vertex has coordinate greater than 'max': " + min + " - " + max);
        
        float maxX = (float) max.x;
        float maxY = (float) max.y;
        float maxZ = (float) max.z;
        float minX = (float) min.x;
        float minY = (float) min.y;
        float minZ = (float) min.z;
        
        int i = 0;
        float[] va = new float[192]; // 72v + 72n + 48t
        
        // back
        /*v*/ va[i++] = minX; va[i++] = maxY; va[i++] = minZ; /*n*/ va[i++] = 0f; va[i++] = 0f; va[i++] = -1f; /*t*/ va[i++] = 1f; va[i++] = 1f;
        /*v*/ va[i++] = maxX; va[i++] = maxY; va[i++] = minZ; /*n*/ va[i++] = 0f; va[i++] = 0f; va[i++] = -1f; /*t*/ va[i++] = 0f; va[i++] = 1f;
        /*v*/ va[i++] = maxX; va[i++] = minY; va[i++] = minZ; /*n*/ va[i++] = 0f; va[i++] = 0f; va[i++] = -1f; /*t*/ va[i++] = 0f; va[i++] = 0f;
        /*v*/ va[i++] = minX; va[i++] = minY; va[i++] = minZ; /*n*/ va[i++] = 0f; va[i++] = 0f; va[i++] = -1f; /*t*/ va[i++] = 1f; va[i++] = 0f;

        // right
        /*v*/ va[i++] = maxX; va[i++] = maxY; va[i++] = minZ; /*n*/ va[i++] = 1f; va[i++] = 0f; va[i++] = 0f; /*t*/ va[i++] = 1f; va[i++] = 1f;
        /*v*/ va[i++] = maxX; va[i++] = maxY; va[i++] = maxZ; /*n*/ va[i++] = 1f; va[i++] = 0f; va[i++] = 0f; /*t*/ va[i++] = 0f; va[i++] = 1f;
        /*v*/ va[i++] = maxX; va[i++] = minY; va[i++] = maxZ; /*n*/ va[i++] = 1f; va[i++] = 0f; va[i++] = 0f; /*t*/ va[i++] = 0f; va[i++] = 0f;
        /*v*/ va[i++] = maxX; va[i++] = minY; va[i++] = minZ; /*n*/ va[i++] = 1f; va[i++] = 0f; va[i++] = 0f; /*t*/ va[i++] = 1f; va[i++] = 0f;
        
        // front
        /*v*/ va[i++] = maxX; va[i++] = maxY; va[i++] = maxZ; /*n*/ va[i++] = 0f; va[i++] = 0f; va[i++] = 1f; /*t*/ va[i++] = 1f; va[i++] = 1f;
        /*v*/ va[i++] = minX; va[i++] = maxY; va[i++] = maxZ; /*n*/ va[i++] = 0f; va[i++] = 0f; va[i++] = 1f; /*t*/ va[i++] = 0f; va[i++] = 1f;
        /*v*/ va[i++] = minX; va[i++] = minY; va[i++] = maxZ; /*n*/ va[i++] = 0f; va[i++] = 0f; va[i++] = 1f; /*t*/ va[i++] = 0f; va[i++] = 0f;
        /*v*/ va[i++] = maxX; va[i++] = minY; va[i++] = maxZ; /*n*/ va[i++] = 0f; va[i++] = 0f; va[i++] = 1f; /*t*/ va[i++] = 1f; va[i++] = 0f;

        // left
        /*v*/ va[i++] = minX; va[i++] = maxY; va[i++] = maxZ; /*n*/ va[i++] = -1f; va[i++] = 0f; va[i++] = 0f; /*t*/ va[i++] = 1f; va[i++] = 1f;
        /*v*/ va[i++] = minX; va[i++] = maxY; va[i++] = minZ; /*n*/ va[i++] = -1f; va[i++] = 0f; va[i++] = 0f; /*t*/ va[i++] = 0f; va[i++] = 1f;
        /*v*/ va[i++] = minX; va[i++] = minY; va[i++] = minZ; /*n*/ va[i++] = -1f; va[i++] = 0f; va[i++] = 0f; /*t*/ va[i++] = 0f; va[i++] = 0f;
        /*v*/ va[i++] = minX; va[i++] = minY; va[i++] = maxZ; /*n*/ va[i++] = -1f; va[i++] = 0f; va[i++] = 0f; /*t*/ va[i++] = 1f; va[i++] = 0f;
        
        // top
        /*v*/ va[i++] = maxX; va[i++] = maxY; va[i++] = minZ; /*n*/ va[i++] = 0f; va[i++] = 1f; va[i++] = 0f; /*t*/ va[i++] = 1f; va[i++] = 1f;
        /*v*/ va[i++] = minX; va[i++] = maxY; va[i++] = minZ; /*n*/ va[i++] = 0f; va[i++] = 1f; va[i++] = 0f; /*t*/ va[i++] = 0f; va[i++] = 1f;
        /*v*/ va[i++] = minX; va[i++] = maxY; va[i++] = maxZ; /*n*/ va[i++] = 0f; va[i++] = 1f; va[i++] = 0f; /*t*/ va[i++] = 0f; va[i++] = 0f;
        /*v*/ va[i++] = maxX; va[i++] = maxY; va[i++] = maxZ; /*n*/ va[i++] = 0f; va[i++] = 1f; va[i++] = 0f; /*t*/ va[i++] = 1f; va[i++] = 0f;
        
        // bottom
        /*v*/ va[i++] = minX; va[i++] = minY; va[i++] = minZ; /*n*/ va[i++] = 0f; va[i++] = -1f; va[i++] = 0f; /*t*/ va[i++] = 1f; va[i++] = 1f;
        /*v*/ va[i++] = maxX; va[i++] = minY; va[i++] = minZ; /*n*/ va[i++] = 0f; va[i++] = -1f; va[i++] = 0f; /*t*/ va[i++] = 0f; va[i++] = 1f;
        /*v*/ va[i++] = maxX; va[i++] = minY; va[i++] = maxZ; /*n*/ va[i++] = 0f; va[i++] = -1f; va[i++] = 0f; /*t*/ va[i++] = 0f; va[i++] = 0f;
        /*v*/ va[i++] = minX; va[i++] = minY; va[i++] = maxZ; /*n*/ va[i++] = 0f; va[i++] = -1f; va[i++] = 0f; /*t*/ va[i++] = 1f; va[i++] = 0f;
        
        vertexAttributes = new VertexBufferObject(new BufferData(va), mode);
        vertices = new VertexAttribute(vertexAttributes, 3, 0, 5);
        normals = new VertexAttribute(vertexAttributes, 3, 3, 5);
        texCoords = new VertexAttribute(vertexAttributes, 2, 6, 6);
        
        bounds = new AxisAlignedBox(new Vector3(minX, minY, minZ), new Vector3(maxX, maxY, maxZ));
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
        return 24;
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

    @Override
    public @Const AxisAlignedBox getBounds() {
        return bounds;
    }
}
