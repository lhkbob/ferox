package com.ferox.core.renderer;

import com.ferox.core.states.StateAtom;

/**
 * StateAtomFilters provide a per pass way to change the state atoms as they are about to be applied.
 * However, there are a few important rules for implementations to get correct results:
 * 1. Do not modify the atom directly
 * 2. When returning a non-null atom, it must be of the same dynamic type
 * 3. For a rendering of a pass, the same input atom must always return the same value
 * 
 * @author Michael Ludwig
 *
 */
public interface StateAtomFilter {
	/**
	 * Apply this filter to the given atom.  Return null to not use the atom, return the atom instance
	 * to keep it the same (but don't modify it), or return a new instance of the same type that will
	 * always be returned when filtering this atom (the new atom shouldn't be modified by the filter
	 * either since that will damage state management).
	 */
	public StateAtom filterAtom(RenderManager manager, RenderPass pass, StateAtom atom);
}
