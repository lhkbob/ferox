/**
 * 
 */
package com.ferox.renderer.impl2.drivers;

import java.nio.FloatBuffer;

/**
 * A VertexArray represents the persisted state of a VertexAttribute within a
 * Geometry. It either contains access information pointing to a VBO holding the
 * vector data, or a native FloatBuffer holding the vector data.
 * 
 * @author Michael Ludwig
 */
public class VertexArray {
    // if null, use offset with a vbo
    public FloatBuffer buffer;
    public int offset; // only used for vbo
    public int vboLen; // portion of vbo dedicated to this va
    
    public int elementSize; // current size of the array
    public final String name;
    
    public VertexArray(String name) {
        if (name == null)
            throw new NullPointerException("Cannot have a null name");
        this.name = name;
    }
}