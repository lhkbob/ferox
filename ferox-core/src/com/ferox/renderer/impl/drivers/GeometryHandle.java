package com.ferox.renderer.impl.drivers;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import com.ferox.renderer.PolygonType;
import com.ferox.renderer.impl.resource.ResourceHandle;
import com.ferox.util.geom.Geometry.CompileType;

/**
 * GeometryHandle is a concrete subclass of ResourceHangle that is used by the
 * JoglGeometryResourceDriver when it manages Geometry instances. There are a
 * number of exposed, low-level fields that assume that the GeometryHandle will
 * be used in an OpenGL system that supports VBOs or vertex buffer objects.
 * 
 * @author Michael Ludwig
 */
public class GeometryHandle extends ResourceHandle {
    public final List<VertexArray> compiledPointers;
    
    // array vbo to use for RESIDENT_x
    public int arrayVbo;
    public int arrayVboSize;

    // values for polygon tracking, used by vertex arrays and vbos
    public PolygonType polyType;
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

    public GeometryHandle(CompileType type) {
        super(-1); // geom has no single id
        compile = type;
        compiledPointers = new ArrayList<VertexArray>();
    }
}
