package com.ferox.renderer;

/**
 * <p>
 * ContextState is a tag-interface for objects that hold a complete
 * representation of a Renderer's state for a context. ContextState instances
 * are not tied to a specific context or renderer, and can be used to duplicate
 * state configuration across multiple renderers, or to restore state after a
 * surface activation.
 * <p>
 * ContextState implementations are tied to a particular Framework. A state
 * representation for one Framework's FixedFunctionRenderer will not work with
 * another's FixedFunctionRenderer.
 * 
 * @author Michael Ludwig
 * 
 */
public interface ContextState<R extends Renderer> {

}
