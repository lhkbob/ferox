package com.ferox.resource;

/**
 * Represents how consecutive elements in a Geometry's indices form
 * polygons.
 */
public enum PolygonType {
    /** Every index is treated as a single point. */
    POINTS,
    /**
     * Every two indices form a line, so [i0, i1, i2, i3] creates 2 lines,
     * one from i0 to i1 and another from i2 to i3.
     */
    LINES,
    /**
     * Every three indices form an individual triangle.
     */
    TRIANGLES,
    /**
     * Every four indices form a quadrilateral (should be planar and
     * convex).
     */
    QUADS,
    /**
     * The first three indices form a triangle, and then every subsequent
     * indices forms a triangle with the previous two indices.
     */
    TRIANGLE_STRIP,
    /**
     * The first four indices form a quad, and then every two indices form a
     * quad with the previous two indices.
     */
    QUAD_STRIP;

    /**
     * Compute the number of polygons, based on the number of indices. This
     * assumes that numVertices > 0.
     * 
     * @param numIndices The number of indices that build a shape with this
     *            PolygonType.
     * @return The polygon count
     */
    public int getPolygonCount(int numIndices) {
        switch (this) {
        case POINTS:
            return numIndices;
        case LINES:
            return numIndices >> 1;
        case TRIANGLES:
            return numIndices / 3;
        case QUADS:
            return numIndices >> 2;

        case TRIANGLE_STRIP:
            return numIndices - 2;
        case QUAD_STRIP:
            return (numIndices - 2) >> 1;
        }

        return -1;
    }
}