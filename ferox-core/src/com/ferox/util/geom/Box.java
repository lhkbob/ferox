package com.ferox.util.geom;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import com.ferox.math.ReadOnlyVector3f;
import com.ferox.math.Vector3f;
import com.ferox.resource.Geometry;
import com.ferox.resource.PolygonType;
import com.ferox.resource.VectorBuffer;

/**
 * <p>
 * A Box represents a 6 sided rectangular prism. By default, a Box is configured
 * to have its vertices, normals and texture coordinates use the default
 * attribute names defined in Geometry.
 * </p>
 * <p>
 * This code was ported from com.jme.scene.shapes.Box
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Box extends PrimitiveGeometry {
    /**
     * Construct a box centered on its origin, with the given side length. So,
     * Box(1f) creates a unit cube. Uses CompileType.NONE.
     * 
     * @param side The side length of the created cube
     */
    public Box(float side) {
        this(side, CompileType.NONE);
    }

    /**
     * Construct a new Box with the given minimum and maximum points. These
     * points are opposite corners of the box. Uses CompileType.NONE.
     * 
     * @param min Minimum corner of the box
     * @param max Maximum corner of the box
     * @throws NullPointerException if min or max are null
     */
    public Box(ReadOnlyVector3f min, ReadOnlyVector3f max) {
        this(min, max, CompileType.NONE);
    }

    /**
     * Construct a box centered on its origin, with the given side length. So,
     * Box(1f) creates a unit cube.
     * 
     * @param side The side length of the created cube
     * @param type The compile type to use for the Box
     */
    public Box(float side, CompileType type) {
        this(new Vector3f(-side / 2f, -side / 2f, -side / 2f), 
             new Vector3f(side / 2f, side / 2f, side / 2f), type);
    }

    /**
     * Construct a new Box with the given minimum and maximum points. These
     * points are opposite corners of the box.
     * 
     * @param min Minimum corner of the box
     * @param max Maximum corner of the box
     * @param type The compile type to use for the Box
     * @throws NullPointerException if min or max are null
     */
    public Box(ReadOnlyVector3f min, ReadOnlyVector3f max, CompileType type) {
        this(min, max, type, Geometry.DEFAULT_VERTICES_NAME, Geometry.DEFAULT_NORMALS_NAME, 
             Geometry.DEFAULT_TEXCOORD_NAME);
    }

    /**
     * Construct a new Box with the given minimum and maximum points. These
     * points are opposite corners of the box. Unlike other constructors which
     * use the default attribute names, this constructor allows you to configure
     * them as you wish.
     * 
     * @param min Minimum corner of the box
     * @param max Maximum corner
     * @param type The CompileType to use
     * @param vertexName The name for vertices
     * @param normalName The name for normals
     * @param tcName The name for texture coordinates
     * @throws NullPointerException if min, max, vertexName, normalName or
     *             tcName are null
     */
    public Box(ReadOnlyVector3f min, ReadOnlyVector3f max, CompileType type, String vertexName, String normalName, String tcName) {
        super(type, vertexName, normalName, tcName);
        setData(min, max);
    }

    /**
     * <p>
     * Changes the data of the box so that the two opposite corners are minPoint
     * and maxPoint. The other corners are created from those two points.
     * </p>
     * <p>
     * This assumes that minPoint represents the minimum coordinate point of the
     * box, and maxPoint is the max. If this isn't true, results are undefined.
     * </p>
     * <p>
     * The vertices, normals and texture coordinates will be placed in
     * attributes with the default names defined in Geometry. If you wish to use
     * different attribute names, invoke
     * {@link #redefineAttributes(String, String, String)} to use the new names.
     * </p>
     * 
     * @param minPoint Minimum corner of the box
     * @param maxPoint Maximum corner of the box
     * @throws NullPointerException if minPoint or maxPoint are null
     */
    public void setData(ReadOnlyVector3f minPoint, ReadOnlyVector3f maxPoint) {
        if (minPoint == null || maxPoint == null)
            throw new NullPointerException("minPoint and maxPoint cannot be null");

        Vector3f center = minPoint.add(maxPoint, null).scale(.5f);

        float xExtent = maxPoint.getX() - center.getX();
        float yExtent = maxPoint.getY() - center.getY();
        float zExtent = maxPoint.getZ() - center.getZ();

        
        FloatBuffer v = newFloatBuffer(72);
        FloatBuffer n = newFloatBuffer(72);
        FloatBuffer t = newFloatBuffer(48);

        float minX = center.getX() - xExtent;
        float maxX = center.getX() + xExtent;
        float minY = center.getY() - yExtent;
        float maxY = center.getY() + yExtent;
        float minZ = center.getZ() - zExtent;
        float maxZ = center.getZ() + zExtent;

        int ti = 0;
        int ni = 0;
        int vi = 0;

        // back
        t.put(ti++, 1f); t.put(ti++, 0f); n.put(ni++, 0f); n.put(ni++, 0f); n.put(ni++, -1f); v.put(vi++, minX); v.put(vi++, minY); v.put(vi++, minZ);
        t.put(ti++, 0f); t.put(ti++, 0f);   n.put(ni++, 0f); n.put(ni++, 0f); n.put(ni++, -1f); v.put(vi++, maxX); v.put(vi++, minY); v.put(vi++, minZ);
        t.put(ti++, 0f); t.put(ti++, 1f); n.put(ni++, 0f); n.put(ni++, 0f); n.put(ni++, -1f); v.put(vi++, maxX); v.put(vi++, maxY); v.put(vi++, minZ);
        t.put(ti++, 1f); t.put(ti++, 1f);   n.put(ni++, 0f); n.put(ni++, 0f); n.put(ni++, -1f); v.put(vi++, minX); v.put(vi++, maxY); v.put(vi++, minZ);

        // right
        t.put(ti++, 1f); t.put(ti++, 0f); n.put(ni++, 1f); n.put(ni++, 0f); n.put(ni++, 0f); v.put(vi++, maxX); v.put(vi++, minY); v.put(vi++, minZ);
        t.put(ti++, 0f); t.put(ti++, 0f); n.put(ni++, 1f); n.put(ni++, 0f); n.put(ni++, 0f); v.put(vi++, maxX); v.put(vi++, minY); v.put(vi++, maxZ);
        t.put(ti++, 0f); t.put(ti++, 1f); n.put(ni++, 1f); n.put(ni++, 0f); n.put(ni++, 0f); v.put(vi++, maxX); v.put(vi++, maxY); v.put(vi++, maxZ);
        t.put(ti++, 1f); t.put(ti++, 1f); n.put(ni++, 1f); n.put(ni++, 0f); n.put(ni++, 0f); v.put(vi++, maxX); v.put(vi++, maxY); v.put(vi++, minZ);

        // front
        t.put(ti++, 1f); t.put(ti++, 0f); n.put(ni++, 0f); n.put(ni++, 0f); n.put(ni++, 1f); v.put(vi++, maxX); v.put(vi++, minY); v.put(vi++, maxZ);
        t.put(ti++, 0f); t.put(ti++, 0f); n.put(ni++, 0f); n.put(ni++, 0f); n.put(ni++, 1f); v.put(vi++, minX); v.put(vi++, minY); v.put(vi++, maxZ);
        t.put(ti++, 0f); t.put(ti++, 1f); n.put(ni++, 0f); n.put(ni++, 0f); n.put(ni++, 1f); v.put(vi++, minX); v.put(vi++, maxY); v.put(vi++, maxZ);
        t.put(ti++, 1f); t.put(ti++, 1f); n.put(ni++, 0f); n.put(ni++, 0f); n.put(ni++, 1f); v.put(vi++, maxX); v.put(vi++, maxY); v.put(vi++, maxZ);

        // left
        t.put(ti++, 1f); t.put(ti++, 0f); n.put(ni++, -1f); n.put(ni++, 0f); n.put(ni++, 0f); v.put(vi++, minX); v.put(vi++, minY); v.put(vi++, maxZ);
        t.put(ti++, 0f); t.put(ti++, 0f); n.put(ni++, -1f); n.put(ni++, 0f); n.put(ni++, 0f); v.put(vi++, minX); v.put(vi++, minY);v.put(vi++, minZ);
        t.put(ti++, 0f); t.put(ti++, 1f); n.put(ni++, -1f); n.put(ni++, 0f); n.put(ni++, 0f); v.put(vi++, minX); v.put(vi++, maxY); v.put(vi++, minZ);
        t.put(ti++, 1f); t.put(ti++, 1f); n.put(ni++, -1f); n.put(ni++, 0f); n.put(ni++, 0f); v.put(vi++, minX); v.put(vi++, maxY); v.put(vi++, maxZ);

        // top
        t.put(ti++, 1f); t.put(ti++, 0f); n.put(ni++, 0f); n.put(ni++, 1f); n.put(ni++, 0f); v.put(vi++, maxX); v.put(vi++, maxY); v.put(vi++, maxZ);
        t.put(ti++, 0f); t.put(ti++, 0f); n.put(ni++, 0f); n.put(ni++, 1f); n.put(ni++, 0f); v.put(vi++, minX); v.put(vi++, maxY); v.put(vi++, maxZ);
        t.put(ti++, 0f); t.put(ti++, 1f); n.put(ni++, 0f); n.put(ni++, 1f); n.put(ni++, 0f); v.put(vi++, minX); v.put(vi++, maxY); v.put(vi++, minZ);
        t.put(ti++, 1f); t.put(ti++, 1f); n.put(ni++, 0f); n.put(ni++, 1f); n.put(ni++, 0f); v.put(vi++, maxX); v.put(vi++, maxY); v.put(vi++, minZ);

        // bottom
        t.put(ti++, 1f); t.put(ti++, 0f); n.put(ni++, 0f); n.put(ni++, -1f); n.put(ni++, 0f); v.put(vi++, minX); v.put(vi++, minY); v.put(vi++, maxZ);
        t.put(ti++, 0f); t.put(ti++, 0f); n.put(ni++, 0f); n.put(ni++, -1f); n.put(ni++, 0f); v.put(vi++, maxX); v.put(vi++, minY); v.put(vi++, maxZ);
        t.put(ti++, 0f); t.put(ti++, 1f); n.put(ni++, 0f); n.put(ni++, -1f); n.put(ni++, 0f); v.put(vi++, maxX); v.put(vi++, minY); v.put(vi++, minZ);
        t.put(ti++, 1f); t.put(ti++, 1f); n.put(ni++, 0f); n.put(ni++, -1f); n.put(ni++, 0f); v.put(vi++, minX); v.put(vi++, minY); v.put(vi++, minZ);

        // indices
        IntBuffer indices = newIntBuffer(36);
        indices.put(new int[] { 2, 1, 0, 3, 2, 0, // back
                                6, 5, 4, 7, 6, 4, // right
                                10, 9, 8, 11, 10, 8, // front
                                14, 13, 12, 15, 14, 12, // left
                                18, 17, 16, 19, 18, 16, // top
                                22, 21, 20, 23, 22, 20 // bottom
                    });

        setAttribute(getVertexName(), new VectorBuffer(v, 3));
        setAttribute(getNormalName(), new VectorBuffer(n, 3));
        setAttribute(getTextureCoordinateName(), new VectorBuffer(t, 2));
        setIndices(indices, PolygonType.TRIANGLES);
    }
}
