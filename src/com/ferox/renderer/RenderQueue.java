package com.ferox.renderer;

/** A RenderQueue keeps track of the information to render the visible objects of a
 * scene.  Depending on the implementation, it may re-order the render atoms
 * so that they are actually rendered in a more optimal order. 
 * 
 * As stated in RenderPass, RenderQueues cannot be shared by RenderPasses, each
 * pass must have a unique instance of a RenderQueue.
 * 
 * @author Michael Ludwig
 *
 */
public interface RenderQueue {
	/** Clear or reset the RenderQueue for the next frame.  Prepares the RenderQueue
	 * to be filled by render atoms and influence atoms. */
	public void clear();
	
	/** Optimize the ordering of the added render atoms (since the last call to clear()).
	 * Render each atom with the given renderer and apply the correct influences for each atom.
	 * Implementations are allowed to invoke the given renderer's applyInfluence() and renderAtom()
	 * methods.  The given view is the view object of the render pass being processed.  It can
	 * be assumed that the view object is not null.
	 * 
	 * Repeated calls to flush() without intermittent calls to clear() should perform the
	 * same operations on the Renderer as the first call to flush().
	 * 
	 * Do nothing if renderer or view are null.
	 * Return the total number of polygons rendered. */
	public int flush(Renderer renderer, View view) throws RenderException;
	
	/** Add the given atom to be rendered by this RenderQueue.  If an atom is added twice, then 
	 * that atom will be rendered twice. It is the user of a RenderQueue's responsibility to
	 * ensure that there are no duplicates added (Shapes properly achieve this). 
	 * 
	 * Do nothing if the atom is null.  It can be assumed that the render atom properly 
	 * returns a geometry and a transform. */
	public void add(RenderAtom atom);
	
	/** Add the given atom to the pool of influences of this RenderQueue.  Any influence atom
	 * added between calls to clear() and flush() can possibly affect render atoms, 
	 * depending on the atom's implementation of influence(render atom). 
	 * 
	 * Do nothing if the atom is null.  It can be assumed that the influence atom properly
	 * returns a state. */
	public void add(InfluenceAtom atom);
}
