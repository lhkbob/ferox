package com.ferox.renderer;

/**
 * ElementBuffer is a {@link Buffer} that holds unsigned integer data for performing indexed drawing.  The
 * integer data contains the indices into the configured {@link VertexAttribute vertex attributes} that are
 * processed by calls to {@link Renderer#render(com.ferox.renderer.Renderer.PolygonType, int, int)}.
 *
 * @author Michael Ludwig
 */
public interface ElementBuffer extends Buffer {
}
