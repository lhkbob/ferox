package com.ferox.core.renderer;

/**
 * A callback mechanism that can determine if at the last minute a RenderAtom should be rendered or not.
 * If a non-null mask is set on a RenderPass then that mask will have its method called once for each atom that
 * was added to the scene's render atom bin for the pass rendering.  For visually appealing results, don't alternate
 * return values for the same atom across subsequent frames.
 * @author Michael Ludwig
 *
 */
public interface RenderAtomMask {
	/**
	 * Return true if the given atom should be rendered, false otherwise.
	 */
	public boolean isValidForRender(RenderAtom atom, RenderManager manager, RenderPass pass);
}
