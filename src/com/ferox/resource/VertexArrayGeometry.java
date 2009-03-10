package com.ferox.resource;

import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.ShortBuffer;

import com.ferox.resource.BufferData.DataType;

/** VertexArrayGeometry is a somewhat special type of Geometry.
 * Its primary difference is that changes made to its bound Buffers
 * are seen immediately in rendering, instead of having to call 
 * update() on the renderer.
 * 
 * It is only necessary to call update() when a buffer has been
 * bound or unbound.
 * 
 * All Buffers in use by a VertexArrayGeometry are accessed with a position
 * of 0 and a limit of its capacity (regardless of the actually position
 * and limit values).  The buffers must be direct and have the system's native
 * byte ordering.
 * 
 * @author Michael Ludwig
 *
 */
public class VertexArrayGeometry extends BufferedGeometry<Buffer> {
	public VertexArrayGeometry(Buffer vertices, VertexArray vertAccessor, Buffer indices, VertexArray indexAccessor, PolygonType type) {
		super(vertices, vertAccessor, indices, indexAccessor, type);
	}
	
	/**
	 * All setters are overridden to make sure the buffer
	 * object is direct and has the native byte ordering.
	 */
	
	@Override
	public void setVertices(Buffer v, VertexArray a) {
		validateBuffer(v);
		super.setVertices(v, a);
	}
	
	@Override
	public void setNormals(Buffer n, VertexArray a) {
		validateBuffer(n);
		super.setNormals(n, a);
	}
	
	@Override
	public void setFogCoordinates(Buffer f, VertexArray a) {
		validateBuffer(f);
		super.setFogCoordinates(f, a);
	}
	
	@Override
	public void setTextureCoordinates(int unit, Buffer t, VertexArray a) {
		validateBuffer(t);
		super.setTextureCoordinates(unit, t, a);
	}
	
	@Override
	public void setVertexAttributes(int unit, Buffer v, VertexArray a) {
		validateBuffer(v);
		super.setVertexAttributes(unit, v, a);
	}
	
	@Override
	public void setIndices(Buffer i, VertexArray a, PolygonType t) {
		validateBuffer(i);
		super.setIndices(i, a, t);
	}

	@Override
	protected float get(Buffer data, int index) {
		if (data instanceof FloatBuffer) {
			return ((FloatBuffer) data).get(index);
		} else if (data instanceof IntBuffer) {
			return ((IntBuffer) data).get(index);
		} else if (data instanceof ShortBuffer) {
			return ((ShortBuffer) data).get(index);
		} else if (data instanceof ByteBuffer) {
			return ((ByteBuffer) data).get(index);
		}
		
		// fallback value, we shouldn't be dealing with other types
		// of nio Buffers, though
		return 0f;
	}

	@Override
	protected int getNumElements(Buffer data, VertexArray accessor) {
		return accessor.getNumElements(data.capacity());
	}

	@Override
	protected DataType getEffectiveDataType(Buffer data, boolean asIndices) {
		if (data instanceof FloatBuffer)
			return DataType.FLOAT;
		else if (asIndices) {
			// used unsigned types for this
			if (data instanceof IntBuffer)
				return DataType.UNSIGNED_INT;
			else if (data instanceof ShortBuffer)
				return DataType.UNSIGNED_SHORT;
			else if (data instanceof ByteBuffer)
				return DataType.UNSIGNED_BYTE;
		} else {
			if (data instanceof IntBuffer)
				return DataType.INT;
			else if (data instanceof ShortBuffer)
				return DataType.SHORT;
			else if (data instanceof ByteBuffer)
				return DataType.BYTE;
		}
		
		// unsupported buffer type (probably a LongBuffer or DoubleBuffer).
		return null;
	}
	
	/* Throw an exception if b isn't null and isn't direct or have
	 * the expected byte ordering. */
	private static void validateBuffer(Buffer b) throws IllegalArgumentException {
		if (b != null) {
			if (!b.isDirect())
				throw new IllegalArgumentException("Buffer object must be direct");
			
			boolean badOrder = false;
			ByteOrder system = ByteOrder.nativeOrder();
			
			if (b instanceof FloatBuffer) {
				badOrder = ((FloatBuffer) b).order() != system;
			} else if (b instanceof IntBuffer) {
				badOrder = ((IntBuffer) b).order() != system;
			} else if (b instanceof ShortBuffer) {
				badOrder = ((ShortBuffer) b).order() != system;
			} else if (b instanceof ByteBuffer) {
				badOrder = ((ByteBuffer) b).order() != system;
			}
			
			if (badOrder)
				throw new IllegalArgumentException("Buffer object must have a byte ordering of: " + system);
		}
	}
}
