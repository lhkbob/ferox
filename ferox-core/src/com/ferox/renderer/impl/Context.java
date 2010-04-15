package com.ferox.renderer.impl;

import com.ferox.renderer.Renderer;

/**
 * Context represents an OpenGL context. When a context is current, OpenGL calls
 * can be made on the calling Thread. It is left up to implementations to
 * specify this behavior and manage it properly. The only assertion that Context
 * makes is that it has a single {@link Renderer} that can be used for
 * RenderPasses.
 * 
 * @author Michael Ludwig
 */
public interface Context {
	/**
	 * Return the unique Renderer instance associated with this Context.
	 * 
	 * @return This Context's Renderer
	 */
	public Renderer getRenderer();
}
