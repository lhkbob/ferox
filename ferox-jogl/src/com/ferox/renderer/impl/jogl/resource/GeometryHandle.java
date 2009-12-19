package com.ferox.renderer.impl.jogl.resource;

import java.nio.IntBuffer;
import java.util.List;

import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.resource.Geometry.CompileType;

/**
 * GeometryHandle is a concrete subclass of ResourceHangle that is used by the
 * JoglGeometryResourceDriver when it manages Geometry instances.
 * 
 * @author Michael Ludwig
 */
public class GeometryHandle extends ResourceHandle {
	// layout is as follows:
	public List<VertexArray> compiledPointers;
	
	// array vbo to use for RESIDENT_x
	public int arrayVbo;
	public int arrayVboSize;

	// values for polygon tracking, used by vertex arrays and vbos
	public int glPolyType;
	public int polyCount;

	public int indexCount;
	
	public int minIndex;
	public int maxIndex;

	// if null, use elementVbo
	public IntBuffer indices;
	
	// element vbo for use with RESIDENT_x
	public int elementVboSize;
	public int elementVbo;

	public final CompileType compile;
	
	public volatile int version;

	public GeometryHandle(CompileType type) {
		super(-1); // geom has no single id
		compile = type;
		version = 0;
	}
}
