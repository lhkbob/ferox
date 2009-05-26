package com.ferox.renderer;

/**
 * <p>
 * A RenderQueue keeps track of the information to render the visible objects of
 * a scene. Depending on the implementation, it may re-order the render atoms so
 * that they are actually rendered in a more optimal order.
 * </p>
 * <p>
 * As stated in RenderPass, RenderQueues cannot be shared by RenderPasses, each
 * pass must have a unique instance of a RenderQueue.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface RenderQueue {
	/**
	 * Clear or reset the RenderQueue for the next frame. Prepares the
	 * RenderQueue to be filled by render atoms and influence atoms.
	 */
	public void clear();

	/**
	 * <p>
	 * Optimize the ordering of the added render atoms (since the last call to
	 * clear()). Render each atom with the given renderer and apply the correct
	 * influences for each atom. The given view is the view from which all
	 * submitted atoms will be rendered.
	 * </p>
	 * <p>
	 * This method must only be invoked when it's acceptable to call
	 * renderAtom() on the given Renderer.
	 * </p>
	 * <p>
	 * Repeated calls to flush() without intermittent calls to clear() should
	 * perform the same operations on the Framework as the first call to
	 * flush().
	 * </p>
	 * 
	 * @param renderer The Renderer that will be rendered into
	 * @param view The current View that represents the viewing location for all
	 *            rendered atoms
	 */
	public int flush(Renderer renderer, View view);

	/**
	 * <p>
	 * Add the given atom to be rendered by this RenderQueue. If an atom is
	 * added twice, then that atom will be rendered twice. It is the user of a
	 * RenderQueue's responsibility to ensure that there are no duplicates
	 * added.
	 * </p>
	 * <p>
	 * Do nothing if the atom is null.
	 * </p>
	 * 
	 * @param atom The RenderAtom that should be added to the queue
	 */
	public void add(RenderAtom atom);
}
