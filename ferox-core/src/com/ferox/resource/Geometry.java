package com.ferox.resource;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Geometry extends Resource {
	public static enum CompileType {
		NONE, RESIDENT_DYNAMIC, RESIDENT_STATIC
	}
	
	public static final String DEFAULT_VERTICES_NAME = "vertices";
	public static final String DEFAULT_NORMALS_NAME = "normals";
	public static final String DEFAULT_TEXCOORD_NAME = "texcoords";
	
	private final CompileType compile;
	private final Map<String, VectorBuffer> attributes;
	private final Map<String, VectorBuffer> readOnlyAttributes;
	
	private int[] indices;
	private PolygonType polyType;
	
	private GeometryDirtyState dirty;
	
	public Geometry(CompileType compileType) {
		compile = (compileType != null ? compileType : CompileType.NONE);
		attributes = new HashMap<String, VectorBuffer>();
		readOnlyAttributes = Collections.unmodifiableMap(attributes);
	}
	
	public int[] getIndices() {
		return indices;
	}
	
	public PolygonType getPolygonType() {
		return polyType;
	}
	
	public void setIndices(int[] indices, PolygonType type) {
		this.indices = indices;
		polyType = (type == null ? PolygonType.POINTS : type);
		markIndicesDirty(0, indices.length);
	}
	
	public VectorBuffer getAttribute(String name) {
		if (name == null)
			throw new NullPointerException("Cannot access an attribute with a null name");
		return attributes.get(name);
	}
	
	public void setAttribute(String name, VectorBuffer values) {
		if (name == null)
			throw new NullPointerException("Cannot assign an attribute with a null name");
		
		if (values != null) {
			VectorBuffer old = attributes.put(name, values);
			
			if (dirty == null)
				dirty = new GeometryDirtyState();
			dirty = dirty.updateAttribute(name, 0, values.getBuffer().length, old == null);
		} else
			removeAttribute(name);
	}
	
	public VectorBuffer removeAttribute(String name) {
		if (name == null)
			throw new NullPointerException("Cannot remove an attribute with a null name");
		
		VectorBuffer rem = attributes.remove(name);
		if (rem != null) {
			if (dirty == null)
				dirty = new GeometryDirtyState();
			dirty = dirty.removeAttribute(name);
		}
		
		return rem;
	}
	
	public Map<String, VectorBuffer> getAttributes() {
		return readOnlyAttributes;
	}
	
	public final CompileType getCompileType() {
		return compile;
	}
	
	public void markIndicesDirty(int offset, int length) {
		if (dirty == null)
			dirty = new GeometryDirtyState();
		dirty = dirty.updateIndices(offset, length);
	}
	
	public void markAttributeDirty(String name, int offset, int length) {
		if (!attributes.containsKey(name))
			return;
		
		if (dirty == null)
			dirty = new GeometryDirtyState();
		dirty.updateAttribute(name, offset, length, false);
	}

	@Override
	public GeometryDirtyState getDirtyState() {
		GeometryDirtyState d = dirty;
		dirty = null;
		return d;
	}
}
