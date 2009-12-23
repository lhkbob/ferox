package com.ferox.util.geom;

import com.ferox.resource.Geometry;
import com.ferox.resource.PolygonType;
import com.ferox.resource.VectorBuffer;

/**
 * <p>
 * Sphere is an approximation of a mathematical sphere centered at the local
 * origin with a configurable radius. The accuracy of the approximation depends
 * on a parameter termed <tt>resolution</tt>. The approximated sphere is
 * constructed by rotating a number of circles in the XY-plane about the Y-axis.
 * The number of rotations equals the resolution of the sphere.
 * </p>
 * <p>
 * By default, a Sphere is configured to have its vertices, normals, and texture
 * coordinates use the default attribute names defined in {@link Geometry}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Sphere extends PrimitiveGeometry {
	private static final float PI = (float) Math.PI;

	/**
	 * Create a new Sphere with the given radius, a resolution of 8, and a
	 * CompileType of NONE.
	 * 
	 * @param radius The radius of the sphere, in local space
	 * @throws IllegalArgumentException if radius <= 0
	 */
	public Sphere(float radius) {
		this(radius, 8);
	}

	/**
	 * Create a new Sphere with the given radius and resolution. It uses a
	 * CompileType of NONE.
	 * 
	 * @param radius The radius of the sphere, in local space
	 * @param res The resolution of the sphere, the higher the value the
	 *            smoother the tesselation
	 * @throws IllegalArgumentException if radius <= 0 or if res < 4
	 */
	public Sphere(float radius, int res) {
		this(radius, res, CompileType.NONE);
	}

	/**
	 * Create a new Sphere with the given radius and CompileType. It uses a
	 * resolution of 8.
	 * 
	 * @param radius The radius of the sphere, in local space
	 * @param type The CompileType to use
	 * @throws IllegalArgumentException if radius <= 0
	 */
	public Sphere(float radius, CompileType type) {
		this(radius, 8, type);
	}

	/**
	 * Create a new Sphere with the given radius, resolution and CompileType.
	 * 
	 * @param radius The radius of the sphere, in local space
	 * @param res The resolution of the sphere
	 * @param type The CompileType to use
	 * @throws IllegalArgumentException if radius <= 0 or if res < 4
	 */
	public Sphere(float radius, int res, CompileType type) {
		this(radius, res, type, Geometry.DEFAULT_VERTICES_NAME, Geometry.DEFAULT_NORMALS_NAME, 
			 Geometry.DEFAULT_TEXCOORD_NAME);
	}

	/**
	 * Create a new Sphere with the given radius, resolution, CompileType and
	 * attribute names. Unlike the other constructors, which use the default
	 * attribute names as defined in {@link Geometry}, this constructor lets you
	 * specify the attribute names.
	 * 
	 * @param radius The radius of the sphere, in local space
	 * @param res The resolution of the sphere
	 * @param type The CompileType to use
	 * @param vertexName The name of the attribute that holds vertex data
	 * @param normalName The name of the attribute that holds normal data
	 * @param texName The name of the attribute that holds texture coordinate
	 *            data
	 * @throws IllegalArgumentException if radius <= 0 or if res < 4
	 * @throws NullPointerException if vertexName, normalName or texName are
	 *             null
	 */
	public Sphere(float radius, int res, CompileType type, 
				   String vertexName, String normalName, String texName) {
		super(type, vertexName, normalName, texName);
		setData(radius, res);
	}

	/**
	 * Re-compute the geometric data of this Sphere so that it represents an
	 * approximation of a sphere centered at the origin with the given radius.
	 * The parameter <tt>res</tt> is equal to the number of rotated "slices"
	 * through the sphere where vertices are placed.
	 * 
	 * @param radius The new radius of the sphere
	 * @param res The resolution of the sphere
	 * @throws IllegalArgumentException if radius <= 0 or res < 4
	 */
	public void setData(float radius, int res) {
		if (radius <= 0f)
			throw new IllegalArgumentException("Invalid radius, must be > 0, not: " + radius);
		if (res < 4)
			throw new IllegalArgumentException("Invalid resolution, must be > 3, not: " + res);
		
		int vertexCount = res * (res + 1);
		
		float[] xCoord = new float[res + 1];
		float[] zCoord = new float[res + 1];		
		float[] u = new float[res + 1];
		
		float xzAngle = 0;
		float dXZ = 2 * PI / res;
		for (int i = 0; i < res; i++) {
			// compute cache for slices
			xCoord[i] = (float) Math.cos(xzAngle);
			zCoord[i] = (float) Math.sin(xzAngle);
			u[i] = 1f - (float) i / res;
			xzAngle += dXZ;
		}
		
		// wrap around to connect the sphere
		xCoord[res] = xCoord[0];
		zCoord[res] = zCoord[0];
		u[res] = 1f;
		
		float[] vertices = new float[vertexCount * 3];
		float[] normals = new float[vertexCount * 3];
		float[] tcs = new float[vertexCount * 2];
		
		float yAngle = PI;
		float dY = -PI / (res - 1);
		int index = 0;
		int ri;
		float y, r, tv;
		for (int dv = 0; dv < res; dv++) {
			// compute y values, since they're constant for the whole ring
			y = (float) Math.cos(yAngle);
			r = (float) Math.sqrt(1 - y * y);
			tv = (float) dv / (res - 1);
			yAngle += dY;
			
			for (int du = 0; du <= res; du++) {
				// place vertices, normals and texcoords
				ri = index * 3;
				normals[ri] = r * xCoord[du];
				normals[ri + 1] = y;
				normals[ri + 2] = r * zCoord[du];
				
				vertices[ri] = radius * normals[ri];
				vertices[ri + 1] = radius * normals[ri + 1];
				vertices[ri + 2] = radius * normals[ri + 2];
				
				tcs[index * 2] = u[du];
				tcs[index * 2 + 1] = tv;
				
				// update index
				index++; 
			}
		}
		
		// build up indices
		int[] indices = new int[(res - 1) * (2 * res + 2)];
		index = 0;
		int v1, v2;
		for (int dv = 0; dv < res - 1; dv++) {
			v1 = dv * (res + 1);
			v2 = (dv + 1) * (res + 1);
			// start off the strip
			indices[index++] = v1++;
			indices[index++] = v2++;
			for (int du = 0; du < res; du++) {
				indices[index++] = v1++;
				indices[index++] = v2++;
			}
		}
		
		setAttribute(getVertexName(), new VectorBuffer(vertices, 3));
		setAttribute(getNormalName(), new VectorBuffer(normals, 3));
		setAttribute(getTextureCoordinateName(), new VectorBuffer(tcs, 2));
		setIndices(indices, PolygonType.TRIANGLE_STRIP);
	}
}
