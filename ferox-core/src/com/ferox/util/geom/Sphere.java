package com.ferox.util.geom;

import com.ferox.math.Vector3f;
import com.ferox.resource.IndexedArrayGeometry;
import com.ferox.resource.VectorBuffer;

/**
 * <p>
 * A Sphere represents, well, a sphere.
 * </p>
 * <p>
 * This code was ported from com.jme.scene.shapes.Sphere.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Sphere extends IndexedArrayGeometry {
	/**
	 * Enum describing how texture coordinates are generated for a sphere.
	 */
	public static enum SphereTextureMode {
		ORIGINAL, PROJECTION
	}

	private static final Vector3f tempVa = new Vector3f();
	private static final Vector3f tempVb = new Vector3f();
	private static final Vector3f tempVc = new Vector3f();

	// size and location of the sphere
	private final Vector3f center;
	private float radius;

	// how tesselated the sphere is
	private int zSamples;
	private int radialSamples;

	private SphereTextureMode mode;

	/**
	 * Create a sphere with the given radius, and 16 radial and zSamples with
	 * original texture mode, compiled as vertex arrays.
	 * 
	 * @param radius The radius of the sphere
	 * @throws IllegalArgumentException if radius <= 0
	 */
	public Sphere(float radius) {
		this(radius, 16, 16);
	}

	/**
	 * Create a sphere with the given radius, 16 radial and z samples with the
	 * original texture mode. The sphere is configured to use the given compile
	 * type.
	 * 
	 * @param radius Radius of the sphere
	 * @param compile Compile type
	 * @throws IllegalArgumentException if radius <= 0
	 */
	public Sphere(float radius, CompileType compile) {
		this(radius, 16, 16, SphereTextureMode.ORIGINAL, compile);
	}

	/**
	 * Create a sphere with the given radius, and samples with original texture
	 * mode, compiled as vertex arrays.
	 * 
	 * @param radius The radius of the sphere
	 * @param zSamples The number of bands in the sphere
	 * @param radialSamples The number of slices in the sphere
	 * @throws IllegalArgumentException if radius, zSamples, or radialSamples <=
	 *             0
	 */
	public Sphere(float radius, int zSamples, int radialSamples) {
		this(radius, zSamples, radialSamples, SphereTextureMode.ORIGINAL, 
			CompileType.VERTEX_ARRAY);
	}

	/**
	 * Create a sphere with the given radius, samples, textured mode and compile
	 * type.
	 * 
	 * @param radius The radius of the sphere
	 * @param zSamples The number of bands in the sphere
	 * @param radialSamples The number of slices in the sphere
	 * @param mode The texture mode used to generate texture coordinates
	 * @param type The compile type to use
	 * @throws IllegalArgumentException if radius, zSamples, or radialSamples <=
	 *             0
	 */
	public Sphere(float radius, int zSamples, int radialSamples, 
				  SphereTextureMode mode, CompileType type) {
		this(null, radius, zSamples, radialSamples, mode, type);
	}

	/**
	 * Create a sphere with the given radius, samples, textured mode and compile
	 * type. It is centered at the given vector, if null it uses the local
	 * origin.
	 * 
	 * @param radius The radius of the sphere
	 * @param zSamples The number of bands in the sphere
	 * @param radialSamples The number of slices in the sphere
	 * @param mode The texture mode used to generate texture coordinates
	 * @param type The compile type to use
	 * @throws IllegalArgumentException if radius, zSamples, or radialSamples <=
	 *             0
	 */
	public Sphere(Vector3f center, float radius, int zSamples, int radialSamples, 
				  SphereTextureMode mode, CompileType type) {
		super(type);

		this.center = new Vector3f();
		setData(center, radius, zSamples, radialSamples, mode);
	}

	/**
	 * Update the Sphere's data so it's a sphere with the given radius, samples,
	 * textured mode and compile type. It is centered at the given vector, if
	 * null it uses the local origin.
	 * 
	 * @param radius The radius of the sphere
	 * @param zSamples The number of bands in the sphere
	 * @param radialSamples The number of slices in the sphere
	 * @param mode The texture mode used to generate texture coordinates
	 * @throws IllegalArgumentException if radius, zSamples, or radialSamples <=
	 *             0
	 */
	public void setData(Vector3f center, float radius, int zSamples, 
						int radialSamples, SphereTextureMode mode) {
		if (radius <= 0f)
			throw new IllegalArgumentException("Radius must be > 0");
		if (zSamples <= 0 || radialSamples <= 0)
			throw new IllegalArgumentException("zSamples and radialSamples must be > 0");

		if (center != null)
			this.center.set(center);
		else
			this.center.set(0f, 0f, 0f);

		this.zSamples = zSamples;
		this.radialSamples = radialSamples;
		this.radius = radius;
		this.mode = (mode == null ? SphereTextureMode.ORIGINAL : mode);
		setGeometryData();
		setIndexData();
	}

	/**
	 * @return The radius of the sphere
	 */
	public float getRadius() {
		return radius;
	}

	/**
	 * Returns the current center of this sphere, in local space. The result is
	 * stored within store. If store is null, a new vector is created.
	 * 
	 * @param store The vector to hold the center
	 * @return store, or a new Vector3f holding the center
	 */
	public Vector3f getCenter(Vector3f store) {
		if (store == null)
			store = new Vector3f();
		store.set(center);
		return store;
	}

	/**
	 * @return The texture mode used to generate tex coords
	 */
	public SphereTextureMode getSphereTextureMode() {
		return mode;
	}

	/**
	 * @return The number bands in the tesselated sphere
	 */
	public int getZSamples() {
		return zSamples;
	}

	/**
	 * @return The number or radial slices in the tesselated sphere
	 */
	public int getRadialSamples() {
		return radialSamples;
	}

	/*
	 * Compute and initialize vertices[], normals[], and texCoords[] for this
	 * sphere.
	 */
	private void setGeometryData() {
		int vertexCount = (zSamples - 2) * (radialSamples + 1) + 2;
		float[] vertices = new float[vertexCount * 3];
		float[] normals = new float[vertexCount * 3];
		float[] texCoords = new float[vertexCount * 2];

		// generate geometry
		float fInvRS = 1.0f / radialSamples;
		float fZFactor = 2.0f / (zSamples - 1);

		// Generate points on the unit circle to be used in computing the mesh
		// points on a sphere slice.
		float[] afSin = new float[radialSamples + 1];
		float[] afCos = new float[radialSamples + 1];
		for (int iR = 0; iR < radialSamples; iR++) {
			float fAngle = (float) (2 * Math.PI * fInvRS * iR);
			afCos[iR] = (float) Math.cos(fAngle);
			afSin[iR] = (float) Math.sin(fAngle);
		}
		afSin[radialSamples] = afSin[0];
		afCos[radialSamples] = afCos[0];

		// generate the sphere itself
		int i = 0;
		for (int iZ = 1; iZ < (zSamples - 1); iZ++) {
			float fZFraction = -1.0f + fZFactor * iZ; // in (-1,1)
			float fZ = radius * fZFraction;

			// compute center of slice
			Vector3f kSliceCenter = tempVb.set(center);
			kSliceCenter.z += fZ;

			// compute radius of slice
			float fSliceRadius = (float) Math.sqrt(Math.abs(radius * radius - fZ * fZ));

			// compute slice vertices with duplication at end point
			Vector3f kNormal;
			int iSave = i;
			for (int iR = 0; iR < radialSamples; iR++) {
				float fRadialFraction = iR * fInvRS; // in [0,1)

				// vertices
				Vector3f kRadial = tempVc.set(afCos[iR], afSin[iR], 0);
				kRadial.scale(fSliceRadius, tempVa);

				vertices[i * 3 + 0] = kSliceCenter.x + tempVa.x;
				vertices[i * 3 + 1] = kSliceCenter.y + tempVa.y;
				vertices[i * 3 + 2] = kSliceCenter.z + tempVa.z;

				// normals
				kNormal = tempVa.set(vertices[i * 3], vertices[i * 3 + 1], vertices[i * 3 + 2]);
				kNormal.sub(center, kNormal).normalize(kNormal);

				normals[i * 3 + 0] = kNormal.x;
				normals[i * 3 + 1] = kNormal.y;
				normals[i * 3 + 2] = kNormal.z;

				if (mode == SphereTextureMode.ORIGINAL) {
					texCoords[i * 2 + 0] = fRadialFraction;
					texCoords[i * 2 + 1] = .5f * (fZFraction + 1f);
				} else { // PROJECTED
					texCoords[i * 2 + 0] = fRadialFraction;
					texCoords[i * 2 + 1] = (float) ((Math.PI / 2.0 + Math.asin(fZFraction)) / Math.PI);
				}

				i++;
			}
			// copy from iSave back to i, but with updated tex coords
			vertices[i * 3 + 0] = vertices[iSave * 3 + 0];
			vertices[i * 3 + 1] = vertices[iSave * 3 + 1];
			vertices[i * 3 + 2] = vertices[iSave * 3 + 2];

			normals[i * 3 + 0] = normals[iSave * 3 + 0];
			normals[i * 3 + 1] = normals[iSave * 3 + 1];
			normals[i * 3 + 2] = normals[iSave * 3 + 2];

			if (mode == SphereTextureMode.ORIGINAL) {
				texCoords[i * 2 + 0] = 1f;
				texCoords[i * 2 + 1] = .5f * (fZFraction + 1f);
			} else { // PROJECTED
				texCoords[i * 2 + 0] = 1f;
				texCoords[i * 2 + 1] = (float) ((Math.PI / 2.0 + Math.asin(fZFraction)) / Math.PI);
			}

			i++;
		}

		// south pole
		vertices[i * 3 + 0] = center.x;
		vertices[i * 3 + 1] = center.y;
		vertices[i * 3 + 2] = center.z - radius;

		normals[i * 3 + 0] = 0f;
		normals[i * 3 + 1] = 0f;
		normals[i * 3 + 2] = -1f;

		texCoords[i * 2 + 0] = .5f;
		texCoords[i * 2 + 1] = 0f;
		i++;

		// north pole
		vertices[i * 3 + 0] = center.x;
		vertices[i * 3 + 1] = center.y;
		vertices[i * 3 + 2] = center.z + radius;

		normals[i * 3 + 0] = 0f;
		normals[i * 3 + 1] = 0f;
		normals[i * 3 + 2] = 1f;

		texCoords[i * 2 + 0] = .5f;
		texCoords[i * 2 + 1] = 0f;

		setVertices(vertices);
		setNormals(normals);
		setTextureCoordinates(0, new VectorBuffer(texCoords, 2));
	}

	/*
	 * Compute and initialize the indices array for this Sphere. Must be called
	 * after setGeometryData().
	 */
	private void setIndexData() {
		int triCount = 6 * radialSamples * (zSamples - 2);
		int[] indices = new int[triCount];

		// generate connectivity
		int index = 0;
		for (int iZ = 0, iZStart = 0; iZ < (zSamples - 3); iZ++) {
			int i0 = iZStart;
			int i1 = i0 + 1;
			iZStart += (radialSamples + 1);
			int i2 = iZStart;
			int i3 = i2 + 1;
			for (int i = 0; i < radialSamples; i++) {
				indices[index + 0] = i0++;
				indices[index + 1] = i1;
				indices[index + 2] = i2;
				indices[index + 3] = i1++;
				indices[index + 4] = i3++;
				indices[index + 5] = i2++;

				index += 6;
			}
		}

		// south pole triangles
		for (int i = 0; i < radialSamples; i++) {
			indices[index + 0] = i;
			indices[index + 1] = getVertexCount() - 2;
			indices[index + 2] = i + 1;

			index += 3;
		}

		// north pole triangles
		int iOffset = (zSamples - 3) * (radialSamples + 1);
		for (int i = 0; i < radialSamples; i++) {
			indices[index + 0] = i + iOffset;
			indices[index + 1] = i + iOffset + 1;
			indices[index + 2] = getVertexCount() - 1;

			index += 3;
		}

		setIndices(indices, PolygonType.TRIANGLES);
	}
}
