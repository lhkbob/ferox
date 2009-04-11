package com.ferox.resource.geometry;

import com.ferox.renderer.Renderer;
import com.ferox.resource.BufferData;
import com.ferox.resource.geometry.VertexBufferObject.UsageHint;

/** This abstract implementation provides many of the 
 * functions in BufferedGeometryDescriptor. It assumes
 * that the geometry is floating point, and the indices
 * are ints.
 * 
 * @author Michael Ludwig
 *
 */
public abstract class AbstractBufferedGeometryDescriptor implements BufferedGeometryDescriptor {
	private static final VertexArray V3_ARRAY = new VertexArray(3);
	private static final VertexArray V2_ARRAY = new VertexArray(2);
	private static final VertexArray V1_ARRAY = new VertexArray(1);
	
	private BufferData vertices;
	private BufferData normals;
	private BufferData texCoords;
	private BufferData indices;
	
	private VertexBufferObject vboVertices;
	private VertexBufferObject vboNormals;
	private VertexBufferObject vboTexCoords;
	private VertexBufferObject vboIndices;
	
	/** Return the arrays to be used with BufferDatas and VertexBufferObjects
	 * for this descriptor. */
	protected abstract float[] internalVertices();
	protected abstract float[] internalNormals();
	protected abstract float[] internalTexCoords();
	protected abstract int[] internalIndices();
	
	/** Convenience method to invoke requestUpdate() with the
	 * given renderer for this descriptor's vbos. 
	 * 
	 * Returns itself, so this can be embedded in constructors. */
	public AbstractBufferedGeometryDescriptor requestVboUpdate(Renderer renderer, boolean forceUpdate) {
		renderer.requestUpdate(this.getVboIndices(), forceUpdate);
		renderer.requestUpdate(this.getVboVertices(), forceUpdate);
		renderer.requestUpdate(this.getVboNormals(), forceUpdate);
		renderer.requestUpdate(this.getVboTextureCoordinates(), forceUpdate);
		
		return this;
	}
	
	/** Convenience method to invoke reqeustCleanUp() with
	 * the given renderer for this descriptor's vbos. 
	 * 
	 * Return itself, so this can be embedded in constructors. */
	public AbstractBufferedGeometryDescriptor requestVboCleanUp(Renderer renderer) {
		renderer.requestCleanUp(this.getVboIndices());
		renderer.requestCleanUp(this.getVboVertices());
		renderer.requestCleanUp(this.getVboNormals());
		renderer.requestCleanUp(this.getVboTextureCoordinates());

		return this;
	}
	
	@Override
	public BufferData getIndices() {
		if (this.indices == null) {
			this.indices = new BufferData(this.internalIndices(), true);
		}
		
		return this.indices;
	}

	@Override
	public VertexArray getIndicesVertexArray() {
		return V1_ARRAY;
	}

	@Override
	public BufferData getNormals() {
		if (this.normals == null) {
			this.normals = new BufferData(this.internalNormals(), false);
		}
		
		return this.normals;
	}

	@Override
	public VertexArray getNormalsVertexArray() {
		return V3_ARRAY;
	}

	@Override
	public BufferData getTextureCoordinates() {
		if (this.texCoords == null) {
			this.texCoords = new BufferData(this.internalTexCoords(), false);
		}
		
		return this.texCoords;
	}

	@Override
	public VertexArray getTextureVertexArray() {
		return V2_ARRAY;
	}

	@Override
	public VertexBufferObject getVboIndices() {
		if (this.vboIndices == null) {
			this.vboIndices = new VertexBufferObject(this.getIndices(), UsageHint.STATIC);
		}
		
		return this.vboIndices;
	}

	@Override
	public VertexBufferObject getVboNormals() {
		if (this.vboNormals == null) {
			this.vboNormals = new VertexBufferObject(this.getNormals(), UsageHint.STATIC);
		}
		
		return this.vboNormals;
	}

	@Override
	public VertexBufferObject getVboTextureCoordinates() {
		if (this.vboTexCoords == null) {
			this.vboTexCoords = new VertexBufferObject(this.getTextureCoordinates(), UsageHint.STATIC);
		}
		
		return this.vboTexCoords;
	}

	@Override
	public VertexBufferObject getVboVertices() {
		if (this.vboVertices == null) {
			this.vboVertices = new VertexBufferObject(this.getVertices(), UsageHint.STATIC);
		}
		
		return this.vboVertices;
	}

	@Override
	public BufferData getVertices() {
		if (this.vertices == null) {
			this.vertices = new BufferData(this.internalVertices(), false);
		}
		
		return this.vertices;
	}

	@Override
	public VertexArray getVerticesVertexArray() {
		return V3_ARRAY;
	}
}
