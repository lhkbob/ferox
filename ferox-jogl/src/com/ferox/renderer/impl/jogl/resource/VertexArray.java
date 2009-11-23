/**
 * 
 */
package com.ferox.renderer.impl.jogl.resource;

import java.nio.FloatBuffer;

public class VertexArray {
	// if null, use offset with a vbo
	public FloatBuffer buffer;
	public int offset; // only used for vbo
	public int vboLen; // portion of vbo dedicated to this va
	
	public int elementSize;
	public final String name;
	
	public VertexArray(String name) {
		if (name == null)
			throw new NullPointerException("Cannot have a null name");
		this.name = name;
	}
}