package com.ferox.resource.geometry;

import org.openmali.vecmath.Vector3f;

import com.ferox.resource.geometry.BufferedGeometry.PolygonType;

/** A Box represents a 6 sided rectangular prism.
 * 
 * This code was ported from the identically named Box in com.jme.scene.shapes.
 * 
 * @author Michael Ludwig
 *
 */
public class Box extends AbstractBufferedGeometryDescriptor {
    private final Vector3f center = new Vector3f();
    private float xExtent, yExtent, zExtent;
    
    private final float[] vertices;
    private final float[] normals;
    private final float[] texCoords;
    
    private int[] indices;

    /** Construct a box centered on its origin, with the given
     * side length.  So, Box(1f) creates a unit cube. */
    public Box(float side) {
    	this(new Vector3f(), side / 2f, side / 2f, side / 2f);
    }
    
    /** Construct a new Box with the given minimum and maximum points. 
     * These points are opposite corners of the box. 
     * 
     * See setData(min, max). */
    public Box(Vector3f min, Vector3f max) throws NullPointerException {
    	// allocate arrays as needed
    	this.vertices = new float[72];
    	this.normals = new float[72];
    	this.texCoords = new float[48];
    	
        setData(min, max);
    }

    /**
     * Constructs a new box. The box has the given center and extends in the x,
     * y, and z out from the center (+ and -) by the given amounts. So, for
     * example, a box with extent of .5 would be the unit cube. 
     * 
     * See setData(center, x, y, z). */
    public Box(Vector3f center, float xExtent, float yExtent, float zExtent) throws NullPointerException {
    	this.vertices = new float[72];
    	this.normals = new float[72];
    	this.texCoords = new float[48];
    	this.indices = new int[36];
    	
        setData(center, xExtent, yExtent, zExtent);
    }
    
    /** Returns the current center of this box, in local space.
     * The result is stored within store.  If store is null, a new
     * vector is created. */
    public Vector3f getCenter(Vector3f store) {
    	if (store == null)
    		store = new Vector3f();
    	store.set(this.center);
    	return store;
    }

    /** Changes the data of the box so that the two opposite corners are minPoint
     * and maxPoint. The other corners are created from those two points.
     * 
     * This assumes that minPoint represents the minimum coordinate point of the
     * box, and maxPoint is the max.  If this isn't true, results are undefined.
     * 
     * Throws a NullPointerException if minPoint or maxPoint are null. */
    public void setData(Vector3f minPoint, Vector3f maxPoint) throws NullPointerException {
    	if (minPoint == null || maxPoint == null)
    		throw new NullPointerException("minPoint and maxPoint cannot be null");
    	
    	this.center.add(minPoint, maxPoint);
    	this.center.scale(.5f);

        float x = maxPoint.x - this.center.x;
        float y = maxPoint.y - this.center.y;
        float z = maxPoint.z - this.center.z;
        this.setData(this.center, x, y, z);
    }

    /** Changes the data of the box so that its center is at center
     * and it extends in the x, y, and z directions by the given extent. Note
     * that the actual sides will be 2x the given extent values because the box
     * extends in + & - from the center for each extent.
     * 
     * This assumes that the extents are positive, if they aren't then resuls
     * are undefined.
     * 
     * Throws a NullPointerException if center is null. */
    public void setData(Vector3f center, float xExtent, float yExtent, float zExtent) {
        if (center == null)
        	throw new NullPointerException("Center cannot be null");
        else
            this.center.set(center);

        this.xExtent = xExtent;
        this.yExtent = yExtent;
        this.zExtent = zExtent;

        this.updateData();
    }
    
    @Override
    public PolygonType getPolygonType() {
    	return PolygonType.TRIANGLES;
    }
    
    @Override
    protected float[] internalVertices() {
    	return this.vertices;
    }
    
    @Override
    protected float[] internalNormals() {
    	return this.normals;
    }
    
    @Override
    protected float[] internalTexCoords() {
    	return this.texCoords;
    }
    
    @Override
    protected int[] internalIndices() {
    	return this.indices;
    }
    
    /* Set all of the BufferedGeometry's fields to be valid. */
    private void updateData() {
    	float[] v = this.vertices;
    	float[] n = this.normals;
    	float[] t = this.texCoords;
    	
        float minX = this.center.x - this.xExtent;
        float maxX = this.center.x + this.xExtent;
        float minY = this.center.y - this.yExtent;
        float maxY = this.center.y + this.yExtent;
        float minZ = this.center.z - this.zExtent;
        float maxZ = this.center.z + this.zExtent;
        
        int ti = 0;
        int ni = 0;
        int vi = 0;
        
        // back
        t[ti++] = 1f; t[ti++] = 0f; n[ni++] = 0f; n[ni++] = 0f; n[ni++] = -1f; v[vi++] = minX; v[vi++] = minY; v[vi++] = minZ;
        t[ti++] = 0f; t[ti++] = 0f; n[ni++] = 0f; n[ni++] = 0f; n[ni++] = -1f; v[vi++] = maxX; v[vi++] = minY; v[vi++] = minZ;
        t[ti++] = 0f; t[ti++] = 1f; n[ni++] = 0f; n[ni++] = 0f; n[ni++] = -1f; v[vi++] = maxX; v[vi++] = maxY; v[vi++] = minZ;
        t[ti++] = 1f; t[ti++] = 1f; n[ni++] = 0f; n[ni++] = 0f; n[ni++] = -1f; v[vi++] = minX; v[vi++] = maxY; v[vi++] = minZ;
        
        // right
        t[ti++] = 1f; t[ti++] = 0f; n[ni++] = 1f; n[ni++] = 0f; n[ni++] = 0f; v[vi++] = maxX; v[vi++] = minY; v[vi++] = minZ;
        t[ti++] = 0f; t[ti++] = 0f; n[ni++] = 1f; n[ni++] = 0f; n[ni++] = 0f; v[vi++] = maxX; v[vi++] = minY; v[vi++] = maxZ;
        t[ti++] = 0f; t[ti++] = 1f; n[ni++] = 1f; n[ni++] = 0f; n[ni++] = 0f; v[vi++] = maxX; v[vi++] = maxY; v[vi++] = maxZ;
        t[ti++] = 1f; t[ti++] = 1f; n[ni++] = 1f; n[ni++] = 0f; n[ni++] = 0f; v[vi++] = maxX; v[vi++] = maxY; v[vi++] = minZ;
        
        // front
        t[ti++] = 1f; t[ti++] = 0f; n[ni++] = 0f; n[ni++] = 0f; n[ni++] = 1f; v[vi++] = maxX; v[vi++] = minY; v[vi++] = maxZ;
        t[ti++] = 0f; t[ti++] = 0f; n[ni++] = 0f; n[ni++] = 0f; n[ni++] = 1f; v[vi++] = minX; v[vi++] = minY; v[vi++] = maxZ;
        t[ti++] = 0f; t[ti++] = 1f; n[ni++] = 0f; n[ni++] = 0f; n[ni++] = 1f; v[vi++] = minX; v[vi++] = maxY; v[vi++] = maxZ;
        t[ti++] = 1f; t[ti++] = 1f; n[ni++] = 0f; n[ni++] = 0f; n[ni++] = 1f; v[vi++] = maxX; v[vi++] = maxY; v[vi++] = maxZ;
        
        // left
        t[ti++] = 1f; t[ti++] = 0f; n[ni++] = -1f; n[ni++] = 0f; n[ni++] = 0f; v[vi++] = minX; v[vi++] = minY; v[vi++] = maxZ;
        t[ti++] = 0f; t[ti++] = 0f; n[ni++] = -1f; n[ni++] = 0f; n[ni++] = 0f; v[vi++] = minX; v[vi++] = minY; v[vi++] = minZ;
        t[ti++] = 0f; t[ti++] = 1f; n[ni++] = -1f; n[ni++] = 0f; n[ni++] = 0f; v[vi++] = minX; v[vi++] = maxY; v[vi++] = minZ;
        t[ti++] = 1f; t[ti++] = 1f; n[ni++] = -1f; n[ni++] = 0f; n[ni++] = 0f; v[vi++] = minX; v[vi++] = maxY; v[vi++] = maxZ;
        
        // top
        t[ti++] = 1f; t[ti++] = 0f; n[ni++] = 0f; n[ni++] = 1f; n[ni++] = 0f; v[vi++] = maxX; v[vi++] = maxY; v[vi++] = maxZ;
        t[ti++] = 0f; t[ti++] = 0f; n[ni++] = 0f; n[ni++] = 1f; n[ni++] = 0f; v[vi++] = minX; v[vi++] = maxY; v[vi++] = maxZ;
        t[ti++] = 0f; t[ti++] = 1f; n[ni++] = 0f; n[ni++] = 1f; n[ni++] = 0f; v[vi++] = minX; v[vi++] = maxY; v[vi++] = minZ;
        t[ti++] = 1f; t[ti++] = 1f; n[ni++] = 0f; n[ni++] = 1f; n[ni++] = 0f; v[vi++] = maxX; v[vi++] = maxY; v[vi++] = minZ;
        
        // bottom
        t[ti++] = 1f; t[ti++] = 0f; n[ni++] = 0f; n[ni++] = -1f; n[ni++] = 0f; v[vi++] = minX; v[vi++] = minY; v[vi++] = maxZ;
        t[ti++] = 0f; t[ti++] = 0f; n[ni++] = 0f; n[ni++] = -1f; n[ni++] = 0f; v[vi++] = maxX; v[vi++] = minY; v[vi++] = maxZ;
        t[ti++] = 0f; t[ti++] = 1f; n[ni++] = 0f; n[ni++] = -1f; n[ni++] = 0f; v[vi++] = maxX; v[vi++] = minY; v[vi++] = minZ;
        t[ti++] = 1f; t[ti++] = 1f; n[ni++] = 0f; n[ni++] = -1f; n[ni++] = 0f; v[vi++] = minX; v[vi++] = minY; v[vi++] = minZ;
        
        // indices
        this.indices = new int[] { 2, 1, 0, 3, 2, 0, // back
        						   6, 5, 4, 7, 6, 4, // right
        						   10, 9, 8, 11, 10, 8, // front
        						   14, 13, 12, 15, 14, 12, // left
        						   18, 17, 16, 19, 18, 16, // top
        						   22, 21, 20, 23, 22, 20 }; // bottom
    }
}
