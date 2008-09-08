package com.ferox.core.states.manager;

import java.util.*;

import com.ferox.core.renderer.RenderContext;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.renderer.RenderPass;
import com.ferox.core.scene.bounds.AxisAlignedBox;
import com.ferox.core.scene.bounds.BoundingSphere;
import com.ferox.core.scene.bounds.BoundingVolume;
import com.ferox.core.states.StateAtom;
import com.ferox.core.states.StateAtomPeer;
import com.ferox.core.states.StateManager;
import com.ferox.core.states.atoms.BufferData;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.atoms.VertexArray.VertexArrayTarget;
import com.ferox.core.states.atoms.VertexArray.VertexArrayUnit;
import com.ferox.core.system.SystemCapabilities;
import com.ferox.core.util.FeroxException;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.ChunkableInstantiator;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;


/**
 * Geometry is the representation of a collection of polygons on screen.  It is a collection of vertex buffers
 * where each buffer has a designated role: vertices, normals, texture coordinates, or user defined vertex attributes,
 * and indices.  A Geometry only needs vertices, the others are applied and used if available.  If indices are null,
 * the geometry is rendered using drawArrays(), otherwise it used drawElements(indices).
 * @author Michael Ludwig
 *
 */
public class Geometry extends StateManager implements ChunkableInstantiator {
	public static enum PolygonType {
		TRIANGLES, TRIANGLE_STRIP, TRIANGLE_FAN, QUADS, QUAD_STRIP, POINTS, LINES
	}
	
	public static enum InterleavedType {
		NONE, N3F_V3F, T2F_V3F, T2F_N3F_V3F
	}
	
	/**
	 * Private class used to store vertex buffers that can have multiple units.
	 *
	 */
	private static class UnitVertexBuffer {
		VertexArray buffer;
		int logicalUnit;
		VertexArrayUnit peerUnit;
		int arrayIndex;
	}
	
	private static int maxVertexAttributes = -1;
	private static boolean maxVASet = false;
	private static int maxTextureCoordinates = -1;
	private static boolean maxTCSet = false;
	
	private BoundingSphere sphereBounds;
	private AxisAlignedBox aaBounds;
	
	private VertexArray vertices;
	private VertexArray normals;
	private ArrayList<UnitVertexBuffer> texCoords;
	private ArrayList<UnitVertexBuffer> vertAttribs;
	private ArrayList<UnitVertexBuffer> compiledBuffers;
	private VertexArray[] allBuffers;
	
	private VertexArray indices;
	private PolygonType connectivity;
	
	private InterleavedType interleaved;
	
	/**
	 * Get the maximum allowed number of vertex attributes for this system.  -1 if a manager hasn't been
	 * created yet.  0 implies vertex attributes aren't supported.
	 */
	public static int getMaxVertexAttributes() {
		if (!maxVASet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				maxVertexAttributes = caps.getMaxVertexAttributes();
				maxVASet = true;
			}
		}
		return maxVertexAttributes;
	}
	
	/**
	 * Get the maximum number of texture coordinate units allowed for this system. -1 if a manager hasn't been
	 * created yet.  Should always be at least 1.
	 */
	public static int getMaxTextureCoordinates() {
		if (!maxTCSet) {
			SystemCapabilities caps = RenderManager.getSystemCapabilities();
			if (caps != null) {
				maxTextureCoordinates = caps.getMaxTextureCoordinates();
				maxTCSet = true;
			}
		}
		return maxTextureCoordinates;
	}
	
	/**
	 * Same as Geometry(vertices, null, null, null, indices, connectivity).
	 */
	public Geometry(VertexArray vertices, VertexArray indices, PolygonType connectivity) {
		this(vertices, null, null, null, indices, connectivity);
	}
	
	/**
	 * Same as Geometry(vertices, normals, null, null, indices, connectivity).
	 */
	public Geometry(VertexArray vertices, VertexArray normals, VertexArray indices, PolygonType connectivity) {
		this(vertices, normals, null, null, indices, connectivity);
	}
	
	/**
	 * Same as Geometry(vertices, normals, texCoords, null, indices, connectivity).
	 */
	public Geometry(VertexArray vertices, VertexArray normals, VertexArray[] texCoords, VertexArray indices, PolygonType connectivity) {
		this(vertices, normals, texCoords, null, indices, connectivity);
	}
	
	/**
	 * Creates a Geometry with the given vertices, normals, array of texCoords (one for each unit, can
	 * have multiples of the same instance, null means no tex coords), array of vertex atrributes
	 * (one for each vertex attributes slot, same rules as texCoords), indices and the connectivity
	 * of those indices.  If indices is null, glDrawArrays() is used instead of glDrawElements()
	 * and connectivity represents the connections between the vertices.
	 * Every BufferDataAccessor, if not null must have the same number of elements (except for the indices
	 * buffer).
	 */
	public Geometry(VertexArray vertices, VertexArray normals, VertexArray[] texCoords, VertexArray[] vertAttribs, VertexArray indices, PolygonType connectivity) {		
		this();
		
		this.setVertices(vertices);
		this.setNormals(normals);
		if (texCoords != null) {
			int size = texCoords.length;
			if (Geometry.getMaxTextureCoordinates() >= 0)
				size = Math.min(size, Geometry.getMaxTextureCoordinates());
			for (int i = 0; i < size; i++)
				this.setTexCoords(texCoords[i], i);
		}
		if (vertAttribs != null) {
			int size = vertAttribs.length;
			if (Geometry.getMaxVertexAttributes() >= 0)
				size = Math.min(size, Geometry.getMaxVertexAttributes());
			for (int i = 0; i < size; i++)
				this.setVertexAttributes(vertAttribs[i], i);	
		}
		this.setIndices(indices);
		this.setConnectivity(connectivity);
	}
	
	private Geometry() {
		super();
		
		this.texCoords = new ArrayList<UnitVertexBuffer>();
		this.vertAttribs = new ArrayList<UnitVertexBuffer>();
	}
	
	public InterleavedType getInterleavedType() {
		return this.interleaved;
	}
	
	public boolean isDataInterleaved() {
		return this.interleaved != InterleavedType.NONE;
	}
	
	/**
	 * Tells each RenderAtom that this Geometry requires a bounds update.  It will automatically be called when
	 * the vertices vertex buffer object is set.  However, if you modify the contents of the vertices vertex buffer,
	 * then you should call this yourself.
	 */
	public void forceBoundsUpdate() {
		if (this.vertices.getBufferData().isDataInClientMemory()) {
			this.sphereBounds = null;
			this.aaBounds = null;
		}
	}
	
	public void getBoundingVolume(BoundingVolume bounds) {
		if (bounds == null)
			return;
		switch (bounds.getBoundType()) {
		case SPHERE:
			if (this.sphereBounds == null) {
				this.sphereBounds = new BoundingSphere();
				this.sphereBounds.enclose(this);
			}
			this.sphereBounds.clone(bounds);
			break;
		case AA_BOX:
			if (this.aaBounds == null) {
				this.aaBounds = new AxisAlignedBox();
				this.aaBounds.enclose(this);
			}
			this.aaBounds.clone(bounds);
			break;
		}
	}
	
	/**
	 * Get the vertices pointer.
	 */
	public VertexArray getVertices() {
		return this.vertices;
	}

	/**
	 * Sets the vertex pointer, must not be null, must have element size of 2, 3, or 4,
	 * must have a target of ARRAY_BUFFER.
	 */
	public void setVertices(VertexArray vertices) throws NullPointerException, IllegalArgumentException {
		if (vertices == null)
			throw new NullPointerException("Vertices aren't allowed to be null");
		if (vertices != null && (vertices.getElementSize() != 2 && vertices.getElementSize() != 3 && vertices.getElementSize() != 4))
			throw new IllegalArgumentException("A BufferDataAccessor intended for vertices specification must have an element size of 2, 3, or 4");
		
		this.vertices = vertices;
		this.detectInterleavedType();
		this.invalidateAssociatedStateTrees();
		this.forceBoundsUpdate();
	}

	/**
	 * Get the normals pointer.
	 */
	public VertexArray getNormals() {
		return this.normals;
	}

	/**
	 * Sets the normal pointer, must not be null, must have element size of 3,
	 * must have a target of ARRAY_BUFFER, must have the same number of elements as the currently
	 * bound vertex pointer.
	 */
	public void setNormals(VertexArray normals) throws IllegalArgumentException {
		if (normals != null && normals.getNumElements() != this.vertices.getNumElements())
			throw new IllegalArgumentException("Normal element count doesn't match vertex element count");
		if (normals != null && normals.getElementSize() != 3)
			throw new IllegalArgumentException("A BufferDataAccessor intended for normals specification must have an element size of 3");
		
		this.normals = normals;
		this.detectInterleavedType();
		this.invalidateAssociatedStateTrees();
	}

	/**
	 * Get the tex coordinate pointer for the given texture unit.
	 */
	public VertexArray getTexCoords(int unit) {
		for (int i = 0; i < this.texCoords.size(); i++) {
			if (this.texCoords.get(i).logicalUnit == unit)
				return this.texCoords.get(i).buffer;
		}
		return null;
	}

	/**
	 * Sets the tex coord pointer for the unit, must not be null, must have element size of 1, 2, 3, or 4,
	 * must have a target of ARRAY_BUFFER, must have the same number of elements as the currently
	 * bound vertex pointer.  Any tex coords with a unit over getMaxTextureCoordinates() will be ignored if a
	 * valid max unit count is returned.
	 */
	public void setTexCoords(VertexArray texCoords, int unit) throws IllegalArgumentException {
		if (texCoords != null && texCoords.getNumElements() != this.vertices.getNumElements())
			throw new IllegalArgumentException("Texture coord element count doesn't match vertex element count");
		if (texCoords != null && (texCoords.getElementSize() != 1 && texCoords.getElementSize() != 2 && texCoords.getElementSize() != 3 && texCoords.getElementSize() != 4))
			throw new IllegalArgumentException("A BufferDataAccessor intended for texCoords specification must have an element size of 1, 2, 3, or 4");
				
		if (unit < 0)
			return;
		if (Geometry.getMaxTextureCoordinates() >= 0 && unit >= Geometry.getMaxTextureCoordinates())
			return;
		
		UnitVertexBuffer prev = null;

		int removeIndex = -1;
		for (int i = 0; i < this.texCoords.size(); i++) {
			if (this.texCoords.get(i).logicalUnit == unit) {
				removeIndex = i;
				break;
			}
		}
		if (removeIndex >= 0) 
			prev = this.texCoords.get(removeIndex);
		if (prev != null && texCoords == null) 
			this.texCoords.remove(removeIndex);
		else if (prev != null && texCoords != null) 
			this.texCoords.get(removeIndex).buffer = texCoords;
		else if (prev == null && texCoords != null) {
			UnitVertexBuffer n = new UnitVertexBuffer();
			n.buffer = texCoords;
			n.logicalUnit = unit;
			this.texCoords.add(n);
		}

		this.detectInterleavedType();
		this.invalidateAssociatedStateTrees();
	}

	/**
	 * Get the vertex attribute pointer for the given slot.
	 */
	public VertexArray getVertexAttributes(int unit) {
		for (int i = 0; i < this.vertAttribs.size(); i++) {
			if (this.vertAttribs.get(i).logicalUnit == unit)
				return this.vertAttribs.get(i).buffer;
		}	
		return null;
	}

	/**
	 * Sets the vertex attrib pointer for the slot, must not be null, must have element size of 1, 2, 3, or 4,
	 * must have a target of ARRAY_BUFFER, must have the same number of elements as the currently
	 * bound vertex pointer.  unit must be less than getMaxVertexAttributes() if that number is >= 0.
	 */
	public void setVertexAttributes(VertexArray vertAttribs, int unit) throws IllegalArgumentException {
		if (vertAttribs != null && vertAttribs.getNumElements() != this.vertices.getNumElements())
			throw new IllegalArgumentException("Vertex attribute element count doesn't match vertex element count");
		if (vertAttribs != null && (vertAttribs.getElementSize() != 1 && vertAttribs.getElementSize() != 2 && vertAttribs.getElementSize() != 3 && vertAttribs.getElementSize() != 4))
			throw new IllegalArgumentException("A BufferDataAccessor intended for vertAttribs specification must have an element size of 1, 2, 3, or 4");
		
		if (unit < 0)
			return;
		if (Geometry.getMaxVertexAttributes() >= 0 && unit >= Geometry.getMaxVertexAttributes())
			return;
		
		UnitVertexBuffer prev = null;

		int removeIndex = -1;
		for (int i = 0; i < this.vertAttribs.size(); i++) {
			if (this.vertAttribs.get(i).logicalUnit == unit) {
				removeIndex = i;
				break;
			}
		}
		if (removeIndex >= 0) 
			prev = this.vertAttribs.get(removeIndex);
		if (prev != null && vertAttribs == null) 
			this.vertAttribs.remove(removeIndex);
		else if (prev != null && vertAttribs != null) 
			this.vertAttribs.get(removeIndex).buffer = vertAttribs;
		else if (prev == null && vertAttribs != null) {
			UnitVertexBuffer n = new UnitVertexBuffer();
			n.buffer = vertAttribs;
			n.logicalUnit = unit;
			this.vertAttribs.add(n);
		}

		this.invalidateAssociatedStateTrees();
	}

	/**
	 * Get the indices pointer.
	 */
	public VertexArray getIndices() {
		return this.indices;
	}

	/**
	 * Set the indices pointer for this Geometry, if it's not null, it must have a target of
	 * GL_ELEMENT_ARRAY_BUFFER.
	 */
	public void setIndices(VertexArray indices) throws IllegalArgumentException {
		if (indices != null && indices.getElementSize() != 1)
			throw new IllegalArgumentException("Geometry indices must have an element size of 1");
		if (indices != null && indices.getStride() != 0)
			throw new IllegalArgumentException("Geometry indices must have a stride of 0");
		
		this.indices = indices;
		this.invalidateAssociatedStateTrees();
	}

	/**
	 * Get the connectivity for this Geometry (basically polygon type).
	 */
	public PolygonType getConnectivity() {
		return this.connectivity;
	}

	/**
	 * Set the connectivity for this Geometry, see above for valid arguments.
	 */
	public void setConnectivity(PolygonType connectivity) {
		this.connectivity = connectivity;
	}
	
	/**
	 * Get the number of polygons that will be rendered by this geometry.
	 */
	public int getPolygonCount() {
		int count = 0;
		if (this.indices != null) {
			count = this.indices.getNumElements();
		} else
			count = this.vertices.getNumElements();
		switch(this.connectivity) {
		case TRIANGLES:
			return count / 3;
		case QUADS:
			return count / 4;
		case LINES:
			return count / 2;
		case POINTS:
			return count;
		case TRIANGLE_STRIP:case TRIANGLE_FAN:
			return count - 2;
		case QUAD_STRIP:
			return (count - 2) / 2;
		}
		return 0;
	}
	
	@Override
	public void update() {
		if (Geometry.getMaxTextureCoordinates() < 0 || Geometry.getMaxVertexAttributes() < 0)
			throw new FeroxException("Can't apply geometry states if the context hasn't created any capabilities");
		
		int mT = Geometry.getMaxTextureCoordinates();
		int mV = Geometry.getMaxVertexAttributes();
		
		this.allBuffers = new VertexArray[1 + 1 + 1 + mT + mV];
		this.compiledBuffers = new ArrayList<UnitVertexBuffer>();
		
		UnitVertexBuffer vb = new UnitVertexBuffer();
		vb.buffer = this.vertices;
		vb.logicalUnit = 0;
		vb.peerUnit = VertexArrayUnit.get(VertexArrayTarget.VERTEX, 0);
		vb.arrayIndex = 0;
		
		this.compiledBuffers.add(vb);
		
		if (this.normals != null) {
			UnitVertexBuffer nb = new UnitVertexBuffer();
			nb.buffer = this.normals;
			nb.logicalUnit = 0;
			nb.arrayIndex = 1;
			nb.peerUnit = VertexArrayUnit.get(VertexArrayTarget.NORMAL, 0);
			this.compiledBuffers.add(nb);
		}			
		if (this.indices != null) {
			UnitVertexBuffer ib = new UnitVertexBuffer();
			ib.buffer = this.indices;
			ib.logicalUnit = 0;
			ib.arrayIndex = 2;
			ib.peerUnit = VertexArrayUnit.get(VertexArrayTarget.INDEX, 0);
			this.compiledBuffers.add(ib);		
		}
			
		Iterator<UnitVertexBuffer> it = this.texCoords.iterator();
		while (it.hasNext()) {
			UnitVertexBuffer uvb = it.next();
			if (uvb.logicalUnit >= mT)
				it.remove();
			else {
				uvb.arrayIndex = 3 + uvb.logicalUnit;
				uvb.peerUnit = VertexArrayUnit.get(VertexArrayTarget.TEXCOORD, uvb.logicalUnit);
				this.compiledBuffers.add(uvb);
			}
		}
		it = this.vertAttribs.iterator();
		while (it.hasNext()) {
			UnitVertexBuffer uvb = it.next();
			if (uvb.logicalUnit >= mV)
				it.remove();
			else {
				uvb.arrayIndex = 3 + mT + uvb.logicalUnit;
				uvb.peerUnit = VertexArrayUnit.get(VertexArrayTarget.ATTRIB, uvb.logicalUnit);
				this.compiledBuffers.add(uvb);
			}
		}
		
		for (int i = 0; i < this.compiledBuffers.size(); i++)
			this.allBuffers[this.compiledBuffers.get(i).arrayIndex] = this.compiledBuffers.get(i).buffer;
		Collections.sort(this.compiledBuffers, new Comparator<UnitVertexBuffer>() {
			public int compare(UnitVertexBuffer arg0, UnitVertexBuffer arg1) {
				int hash1 = arg0.buffer.getBufferData().hashCode();
				int hash2 = arg1.buffer.getBufferData().hashCode();
				return hash1 - hash2;
			}	
		});
		
		this.detectInterleavedType();
	}
	
	private void detectInterleavedType() {
		this.interleaved = InterleavedType.NONE;
		if (this.vertAttribs.size() != 0)
			return;
		
		if (this.normals != null && this.texCoords.size() == 0) {
			if (this.normals.getElementSize() == 3 && this.vertices.getElementSize() == 3) {
				if (this.normals.getOffset() == 0 && this.vertices.getOffset() == 3) {
					if (this.normals.getStride() == 3 && this.vertices.getStride() == 3) {
						if (this.normals.getBufferData() == this.vertices.getBufferData())
							this.interleaved = InterleavedType.N3F_V3F;
					}
				}
			}
		} else if (this.normals != null && this.texCoords.size() == 1) {
			VertexArray tex = this.texCoords.get(0).buffer;
			if (tex.getElementSize() == 2 && this.normals.getElementSize() == 3 && this.vertices.getElementSize() == 3) {
				if (tex.getOffset() == 0 && this.normals.getOffset() == 2 && this.vertices.getOffset() == 5) {
					if (tex.getStride() == 6 && this.normals.getStride() == 5 && this.vertices.getStride() == 5) {
						if (this.normals.getBufferData() == this.vertices.getBufferData() && tex.getBufferData() == this.normals.getBufferData())
							this.interleaved = InterleavedType.T2F_N3F_V3F;
					}
				}
			}
		} else if (this.normals == null && this.texCoords.size() == 1) {
			VertexArray tex = this.texCoords.get(0).buffer;
			if (tex.getElementSize() == 2 && this.vertices.getElementSize() == 3) {
				if (tex.getOffset() == 0 && this.vertices.getOffset() == 2) {
					if (tex.getStride() == 3 && this.vertices.getStride() == 2) {
						if (tex.getBufferData() == this.vertices.getBufferData())
							this.interleaved = InterleavedType.T2F_V3F;
					}
				}
			}
		}
	}
	
	public int getInterleavedTextureUnit() {
		if (this.texCoords.size() != 1 || this.interleaved == InterleavedType.N3F_V3F || this.interleaved == InterleavedType.NONE)
			return -1;
		return this.texCoords.get(0).logicalUnit;
	}
	
	@Override
	protected void applyStateAtoms(StateManager prev, RenderManager manager, RenderPass pass) throws FeroxException {
		if (prev == this)
			return;
		Geometry previous = (Geometry)prev;
		
		if (this.isDataInterleaved()) {
			if (this.vertices.getBufferData().getDataType() != BufferData.DataType.FLOAT)
				this.interleaved = InterleavedType.NONE;
		}
		
		switch(this.vertices.getBufferData().getDataType()) {
		case FLOAT: case DOUBLE: 
		case SHORT: case INT:
			break;
		default:
			throw new FeroxException("Invalid data type for BufferData used as vertices");
		}
		if (this.normals != null) {
			switch(this.normals.getBufferData().getDataType()) {
			case FLOAT: case DOUBLE: 
			case SHORT: case INT:
			case BYTE:
				break;
			default:
				throw new FeroxException("Invalid data type for BufferData used as normals");
			}
		}
		if (this.indices != null) {
			switch(this.indices.getBufferData().getDataType()) {
			case UNSIGNED_SHORT: case UNSIGNED_INT: 
			case UNSIGNED_BYTE:
				break;
			default:
				throw new FeroxException("Invalid data type for BufferData used as indices");
			}
		}
		
		UnitVertexBuffer vb;
		for (int i = 0; i < this.texCoords.size(); i++) {
			vb = this.texCoords.get(i);
			switch(vb.buffer.getBufferData().getDataType()) {
			case FLOAT: case DOUBLE: 
			case SHORT: case INT:
				break;
			default:
				throw new FeroxException("Invalid data type for BufferData used as tex coords");
			}
		}
		
		RenderContext context = manager.getRenderContext();
		StateAtomPeer peer = context.getStateAtomPeer(VertexArray.class);
		
		peer.prepareManager(this, previous);
		for (int i = 0; i < this.compiledBuffers.size(); i++) {
			vb = this.compiledBuffers.get(i);
			pass.applyState(manager, vb.buffer, VertexArray.class, vb.peerUnit);
		}
		if (previous != null) {
			for (int i = 0; i < previous.compiledBuffers.size(); i++) {
				vb = previous.compiledBuffers.get(i);
				if (this.allBuffers[vb.arrayIndex] == null) 
					pass.applyState(manager, null, VertexArray.class, vb.peerUnit);
			}
		}
	}

	@Override
	public Class<? extends StateAtom> getAtomType() {
		return VertexArray.class;
	}

	@Override
	protected void restoreStateAtoms(RenderManager manager, RenderPass pass) {
		RenderContext context = manager.getRenderContext();
		StateAtomPeer peer = context.getStateAtomPeer(VertexArray.class);
		
		UnitVertexBuffer vb;
		for (int i = 0; i < this.compiledBuffers.size(); i++) {
			vb = this.compiledBuffers.get(i);
			pass.applyState(manager, null, VertexArray.class, vb.peerUnit);
		}
		
		peer.disableManager(this);
	}

	@Override
	public int getSortingIdentifier() {		
		int hash = this.vertices.hashCode();
		if (this.normals != null)
			hash ^= this.normals.hashCode();
		if (this.indices != null)
			hash ^= this.indices.hashCode();
		for (int i = 0; i < this.texCoords.size(); i++)
			hash ^= this.texCoords.get(i).hashCode();
		for (int i = 0; i < this.vertAttribs.size(); i++)
			hash ^= this.vertAttribs.get(i).hashCode();
		
		return hash;
	}
	
	@Override
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.connectivity = in.getEnum("connectivity", PolygonType.class);
		this.setVertices((VertexArray)in.getObject("vertices"));
		this.setNormals((VertexArray)in.getObject("normals"));
		this.setIndices((VertexArray)in.getObject("indices"));
		
		int tc = in.getInt("tex_count");
		for (int i = 0; i < tc; i++) 
			this.setTexCoords((VertexArray)in.getObject("texture_" + i), in.getInt("tex_unit_" + i));
		int va = in.getInt("va_count");
		for (int i = 0; i < va; i++)
			this.setVertexAttributes((VertexArray)in.getObject("va_" + i), in.getInt("va_unit_" + i));
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setEnum("connectivity", this.connectivity);
		out.setObject("vertices", this.vertices);
		out.setObject("normals", this.normals);
		out.setObject("indices", this.indices);
		
		out.setInt("tex_count", this.texCoords.size());
		for (int i = 0; i < this.texCoords.size(); i++) {
			out.setInt("tex_unit_" + i, this.texCoords.get(i).logicalUnit);
			out.setObject("texture_" + i, this.texCoords.get(i).buffer);
		}
		
		out.setInt("va_count", this.vertAttribs.size());
		for (int i = 0; i < this.vertAttribs.size(); i++) {
			out.setInt("va_unit_" + i, this.vertAttribs.get(i).logicalUnit);
			out.setObject("va_" + i, this.vertAttribs.get(i).buffer);
		}
	}

	public Class<? extends Chunkable> getChunkableClass() {
		return Geometry.class;
	}

	public Chunkable newInstance() {
		return new Geometry();
	}

	@Override
	public StateManager merge(StateManager manager) {
		return this;
	}
}
