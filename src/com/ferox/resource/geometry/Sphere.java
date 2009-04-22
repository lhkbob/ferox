package com.ferox.resource.geometry;

import org.openmali.FastMath;
import org.openmali.vecmath.Vector3f;

import com.ferox.resource.geometry.BufferedGeometry.PolygonType;

/** A Sphere represents, well, a sphere.
 * 
 * This code was ported from the identically named Sphere in com.jme.scene.shapes.
 * 
 * @author Michael Ludwig
 *
 */
public class Sphere extends AbstractBufferedGeometryDescriptor {
	/** Enum describing how texture coordinates are generated
	 * for a sphere. */
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
	
	private final float[] vertices;
	private final float[] normals;
	private final float[] texCoords;
	private final int[] indices;
	
	public Sphere(float radius) {
		this(radius, 16, 16);
	}
	
	public Sphere(float radius, int zSamples, int radialSamples) {
		this(radius, zSamples, radialSamples, SphereTextureMode.ORIGINAL);
	}
	
	public Sphere(float radius, int zSamples, int radialSamples, SphereTextureMode mode) {
		this(null, radius, zSamples, radialSamples, mode);
	}
	
	public Sphere(Vector3f center, float radius, int zSamples, int radialSamples, SphereTextureMode mode) {
		if (zSamples < 0 || radialSamples <= 0)
			throw new IllegalArgumentException("zSamples and radialSamples must be > 0");
		
		int vertexCount = (zSamples - 2) * (radialSamples + 1) + 2;
		this.vertices = new float[vertexCount * 3];
		this.normals = new float[vertexCount * 3];
		this.texCoords = new float[vertexCount * 2];
		
		int triCount = 6 * radialSamples * (zSamples - 2);
		this.indices = new int[triCount];

		this.center = new Vector3f();
		this.zSamples = zSamples;
		this.radialSamples = radialSamples;
		this.setData(center, radius, mode);
	}
	
	public void setData(Vector3f center, float radius, SphereTextureMode mode) {
		if (radius <= 0f)
			throw new IllegalArgumentException("Radius must be > 0");
		
		if (center != null)
			this.center.set(center);
		else
			this.center.set(0f, 0f, 0f);
		
		this.radius = radius;
		this.mode = (mode == null ? SphereTextureMode.ORIGINAL : mode);
		this.setGeometryData();
		this.setIndexData();
	}
	
	public float getRadius() {
		return this.radius;
	}
	
	public Vector3f getCenter(Vector3f store) {
		if (store == null)
			store = new Vector3f();
		store.set(this.center);
		return store;
	}
	
	public SphereTextureMode getSphereTextureMode() {
		return this.mode;
	}
	
	public int getZSamples() {
		return this.zSamples;
	}
	
	public int getRadialSamples() {
		return this.radialSamples;
	}
	
	/* Compute and initialize vertices[], normals[], and texCoords[] for this sphere. */
    private void setGeometryData() {
    	// generate geometry
        float fInvRS = 1.0f / this.radialSamples;
        float fZFactor = 2.0f / (this.zSamples - 1);

        // Generate points on the unit circle to be used in computing the mesh
        // points on a sphere slice.
        float[] afSin = new float[this.radialSamples + 1];
        float[] afCos = new float[this.radialSamples + 1];
        for (int iR = 0; iR < this.radialSamples; iR++) {
            float fAngle = FastMath.TWO_PI * fInvRS * iR;
            afCos[iR] = FastMath.cos(fAngle);
            afSin[iR] = FastMath.sin(fAngle);
        }
        afSin[this.radialSamples] = afSin[0];
        afCos[this.radialSamples] = afCos[0];
        
        // generate the sphere itself
        int i = 0;
        for (int iZ = 1; iZ < (this.zSamples - 1); iZ++) {
            float fZFraction = -1.0f + fZFactor * iZ; // in (-1,1)
            float fZ = this.radius * fZFraction;

            // compute center of slice
            Vector3f kSliceCenter = tempVb;
            kSliceCenter.set(this.center);
            kSliceCenter.z += fZ;

            // compute radius of slice
            float fSliceRadius = FastMath.sqrt(Math.abs(this.radius * this.radius - fZ * fZ));

            // compute slice vertices with duplication at end point
            Vector3f kNormal;
            int iSave = i;
            for (int iR = 0; iR < this.radialSamples; iR++) {
                float fRadialFraction = iR * fInvRS; // in [0,1)
                
                // vertices
                Vector3f kRadial = tempVc;
                kRadial.set(afCos[iR], afSin[iR], 0);
                tempVa.scale(fSliceRadius, kRadial);

                this.vertices[i * 3 + 0] = kSliceCenter.x + tempVa.x;
                this.vertices[i * 3 + 1] = kSliceCenter.y + tempVa.y;
                this.vertices[i * 3 + 2] = kSliceCenter.z + tempVa.z;

                // normals
                tempVa.set(this.vertices[i * 3], 
                		   this.vertices[i * 3 + 1],
                		   this.vertices[i * 3 + 2]);
                kNormal = tempVa;
                kNormal.sub(this.center);
                kNormal.normalize();
                
                this.normals[i * 3 + 0] = kNormal.x;
                this.normals[i * 3 + 1] = kNormal.y;
                this.normals[i * 3 + 2] = kNormal.z;

                if (this.mode == SphereTextureMode.ORIGINAL) {
                	this.texCoords[i * 2 + 0] = fRadialFraction;
                	this.texCoords[i * 2 + 1] = .5f * (fZFraction + 1f);
                } else { // PROJECTED
                	this.texCoords[i * 2 + 0] = fRadialFraction;
                	this.texCoords[i * 2 + 1] = (FastMath.PI_HALF + FastMath.asin(fZFraction)) / FastMath.PI;
                }

                i++;
            }
            // copy from iSave back to i, but with updated tex coords
            this.vertices[i * 3 + 0] = this.vertices[iSave * 3 + 0];
            this.vertices[i * 3 + 1] = this.vertices[iSave * 3 + 1];
            this.vertices[i * 3 + 2] = this.vertices[iSave * 3 + 2];

            this.normals[i * 3 + 0] = this.normals[iSave * 3 + 0];
            this.normals[i * 3 + 1] = this.normals[iSave * 3 + 1];
            this.normals[i * 3 + 2] = this.normals[iSave * 3 + 2];

            if (this.mode == SphereTextureMode.ORIGINAL) {
            	this.texCoords[i * 2 + 0] = 1f;
            	this.texCoords[i * 2 + 1] = .5f * (fZFraction + 1f);
            } else { // PROJECTED
            	this.texCoords[i * 2 + 0] = 1f;
            	this.texCoords[i * 2 + 1] = (FastMath.PI_HALF + FastMath.asin(fZFraction)) / FastMath.PI;
            }

            i++;
        }

        // south pole
        this.vertices[i * 3 + 0] = this.center.x;
        this.vertices[i * 3 + 1] = this.center.y;
        this.vertices[i * 3 + 2] = this.center.z - this.radius;
        
        this.normals[i * 3 + 0] = 0f;
        this.normals[i * 3 + 1] = 0f;
        this.normals[i * 3 + 2] = -1f;
        
        this.texCoords[i * 2 + 0] = .5f;
        this.texCoords[i * 2 + 1] = 0f;
        i++;

        // north pole
        this.vertices[i * 3 + 0] = this.center.x;
        this.vertices[i * 3 + 1] = this.center.y;
        this.vertices[i * 3 + 2] = this.center.z + this.radius;
        
        this.normals[i * 3 + 0] = 0f;
        this.normals[i * 3 + 1] = 0f;
        this.normals[i * 3 + 2] = 1f;
        
        this.texCoords[i * 2 + 0] = .5f;
        this.texCoords[i * 2 + 1] = 0f;
    }

    /* Compute and initialize the indices array for this Sphere. 
     * Must be called after setGeometryData(). */
    private void setIndexData() {
    	// generate connectivity
        int index = 0;
        for (int iZ = 0, iZStart = 0; iZ < (this.zSamples - 3); iZ++) {
            int i0 = iZStart;
            int i1 = i0 + 1;
            iZStart += (this.radialSamples + 1);
            int i2 = iZStart;
            int i3 = i2 + 1;
            for (int i = 0; i < this.radialSamples; i++) {
            	this.indices[index + 0] = i0++;
            	this.indices[index + 1] = i1;
            	this.indices[index + 2] = i2;
            	this.indices[index + 3] = i1++;
            	this.indices[index + 4] = i3++;
            	this.indices[index + 5] = i2++;
            	
            	index += 6;
            }
        }

        // south pole triangles
        for (int i = 0; i < this.radialSamples; i++) {
        	this.indices[index + 0] = i;
        	this.indices[index + 1] = this.vertices.length / 3 - 2;
        	this.indices[index + 2] = i + 1;
        	
        	index += 3;
        }

        // north pole triangles
        int iOffset = (this.zSamples - 3) * (this.radialSamples + 1);
        for (int i = 0; i < this.radialSamples; i++) {
        	this.indices[index + 0] = i + iOffset;
        	this.indices[index + 1] = i + iOffset + 1;
        	this.indices[index + 2] = this.vertices.length / 3 - 1;
        	
        	index += 3;
        }
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
}
