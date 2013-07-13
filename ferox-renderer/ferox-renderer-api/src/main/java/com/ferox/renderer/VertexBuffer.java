package com.ferox.renderer;


/**
 * VertexBuffer is a {@link Buffer} that holds coordinate information for the vertex attributes used when
 * rendering. These attributes are fixed with a FixedFunctionRenderer, such as vertices, normals, and texture
 * coordinates. They can also be defined within a shader.
 * <p/>
 * VertexBuffer can store floating point data, signed integer data normalized to a decimal. Additionally, it
 * can store regular integer coordinates in conjunction with shaders using integer vector attributes, such as
 * 'ivec3', etc.
 *
 * @author Michael Ludwig
 */
public interface VertexBuffer extends Buffer {
}
