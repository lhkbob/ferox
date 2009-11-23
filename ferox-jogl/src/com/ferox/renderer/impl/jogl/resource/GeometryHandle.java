package com.ferox.renderer.impl.jogl.resource;

import java.nio.IntBuffer;
import java.util.List;

import com.ferox.renderer.impl.ResourceHandle;
import com.ferox.resource.Geometry.CompileType;

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

	// if null, use elementVbo
	public IntBuffer indices;
	
	// element vbo for use with RESIDENT_x
	public int elementVboSize;
	public int elementVbo;

	public final CompileType compile;

	public GeometryHandle(CompileType type) {
		super(-1); // geom has no single id
		compile = type;
	}
}
