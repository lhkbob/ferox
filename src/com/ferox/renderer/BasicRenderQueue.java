package com.ferox.renderer;

/**
 * The simplest of implementation of RenderQueue, that accumulates atoms, but
 * performs no sorting.
 * 
 * @author Michael Ludwig
 */
public class BasicRenderQueue implements RenderQueue {
	private static final int ALLOCATION_INCREMENT = 16;

	private RenderAtom[] renderAtoms;
	private int raCount;

	private boolean cleared;

	public BasicRenderQueue() {
		renderAtoms = new RenderAtom[0];
		cleared = true;
	}

	@Override
	public void add(RenderAtom atom) {
		if (atom != null) {
			if (raCount >= renderAtoms.length) {
				RenderAtom[] temp = new RenderAtom[raCount
						+ ALLOCATION_INCREMENT];
				System.arraycopy(renderAtoms, 0, temp, 0, renderAtoms.length);
				renderAtoms = temp;
			}

			renderAtoms[raCount++] = atom;
		}
	}

	@Override
	public void clear() {
		raCount = 0;
		cleared = true;
	}

	@Override
	public int flush(Renderer renderer, View view) {
		if (renderer == null || view == null)
			return 0;

		if (cleared)
			optimizeOrder(view, renderAtoms, raCount);
		cleared = false;

		int polyCount = 0;
		for (int i = 0; i < raCount; i++)
			polyCount += renderer.renderAtom(renderAtoms[i]);
		return polyCount;
	}

	/**
	 * <p>
	 * To be efficient between frames, BasicRenderQueue internally stores the
	 * atoms in arrays. It doesn't clear the arrays in clear(), just resets the
	 * counts so it is possible for atoms to stay within the array longer than
	 * intended.
	 * </p>
	 * <p>
	 * This method resets the arrays, allowing atoms to be garbage collected.
	 * </p>
	 */
	public void clearInternalResources() {
		clear();
		renderAtoms = new RenderAtom[0];
	}

	/**
	 * Method hook allowing subclasses to sort the given array. Current
	 * implementation doesn't do anything. RenderAtoms are only valid in indices
	 * 0 to count-1, inclusive. The view parameter is the same view object
	 * passed into flush(renderer, view).
	 * 
	 * @param view The View passed into flush()
	 * @param renderAtoms Internal array used to accumulate submitted render
	 *            atoms
	 * @param raCount Index of first invalid render atom in renderAtoms
	 */
	protected void optimizeOrder(View view, RenderAtom[] renderAtoms,
			int raCount) {
		// do nothing in base class
	}
}
