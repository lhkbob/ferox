package com.ferox.renderer.util;

import com.ferox.renderer.InfluenceAtom;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.RenderException;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.Renderer;
import com.ferox.renderer.View;


/** The simplest of implementation of RenderQueue, that accumulates 
 * both types of atoms, but performs no sorting.
 * 
 * @author Michael Ludwig
 *
 */
public class BasicRenderQueue implements RenderQueue {
	private static final int ALLOCATION_INCREMENT = 16;
	
	private RenderAtom[] renderAtoms;
	private int raCount;
	
	private InfluenceAtom[] influenceAtoms;
	private int iaCount;
	
	private boolean cleared;
	
	public BasicRenderQueue() {
		this.renderAtoms = new RenderAtom[0];
		this.influenceAtoms = new InfluenceAtom[0];
		this.cleared = true;
	}
	
	@Override
	public void add(RenderAtom atom) {
		if (atom != null) {
			if (this.raCount >= this.renderAtoms.length) {
				RenderAtom[] temp = new RenderAtom[this.raCount + ALLOCATION_INCREMENT];
				System.arraycopy(this.renderAtoms, 0, temp, 0, this.renderAtoms.length);
				this.renderAtoms = temp;
			}
			
			this.renderAtoms[this.raCount++] = atom;
		}
	}

	@Override
	public void add(InfluenceAtom atom) {
		if (atom != null) {
			if (this.iaCount >= this.influenceAtoms.length) {
				InfluenceAtom[] temp = new InfluenceAtom[this.iaCount + ALLOCATION_INCREMENT];
				System.arraycopy(this.influenceAtoms, 0, temp, 0, this.influenceAtoms.length);
				this.influenceAtoms = temp;
			}
			
			this.influenceAtoms[this.iaCount++] = atom;
		}
	}

	@Override
	public void clear() {
		this.raCount = 0;
		this.iaCount = 0;
		this.cleared = true;
	}

	@Override
	public int flush(Renderer renderer, View view) throws RenderException {
		if (renderer == null || view == null)
			return 0;
		
		if (this.cleared) // only optimize the order once
			this.optimizeOrder(view, this.renderAtoms, this.raCount);
		this.cleared = false;
		
		int polyCount = 0;
		
		RenderAtom atom;
		InfluenceAtom iAtom;
		float influence;
		for (int i = 0; i < this.raCount; i++) {
			atom = this.renderAtoms[i];
			// apply any influences
			for (int u = 0; u < this.iaCount; u++) {
				// ideally it'd be nice to remember the results of influence checking
				// so repeated flushes get some speed boost, but it may not be cost effective,
				// since repeated flushes of the same RenderQueue is a somewhat unlikely scenario
				iAtom = this.influenceAtoms[u];
				influence = iAtom.influences(atom);
				if (influence > 0)
					renderer.applyInfluence(iAtom, influence);
			}
			
			polyCount += renderer.renderAtom(atom);
		}
		
		return polyCount;
	}

	/** To be efficient between frames, BasicRenderQueue internally stores
	 * the atoms in arrays.  It doesn't clear the arrays in clear(), just
	 * resets the counts so it is possible for atoms to stay within the 
	 * array longer than intended.
	 * 
	 * This method resets the arrays, allowing atoms to be garbage collected. */
	public void clearInternalResources() {
		this.clear();
		this.renderAtoms = new RenderAtom[0];
		this.influenceAtoms = new InfluenceAtom[0];
	}
	
	/** Method hook allowing subclasses to sort the given array.  Current implementation
	 * doesn't do anything.  RenderAtoms are only valid in indices 0 to count-1, inclusive.
	 * The view parameter is the same view object passed into flush(renderer, view). */
	protected void optimizeOrder(View view, RenderAtom[] renderAtoms, int raCount) {
		// do nothing in base class
	}
}
