package com.ferox.core.renderer;

import com.ferox.core.scene.SpatialLeaf;
import com.ferox.core.states.StateLeaf;
import com.ferox.core.states.StateManager;
import com.ferox.core.states.atoms.BlendState;
import com.ferox.core.states.atoms.VertexArray;
import com.ferox.core.states.manager.BlendStateManager;
import com.ferox.core.states.manager.Geometry;

/**
 * Represents something to be rendered onto the screen, given a location (SpatialLeaf) and a visual representation
 * (StateLeaf).  Any geometry must be stored in the StateLeaf.  If there is no geometry found after the StateLeaf
 * has merged states from its parent, this RenderAtom will not be rendered.  It is not recommended to instanciate
 * these yourself, but instead let a SpatialLeaf manage them.
 * 
 * @author Michael Ludwig
 *
 */
public class RenderAtom  {
	private static int gDT = -1;
	private static int bDT = -1;
	
	StateManager[] states;
	private final SpatialLeaf spatialLink;
	private StateLeaf stateLink;
	int stateSortedIndex;
	
	/**
	 * Constructs a RenderAtom with the given Spatial and State leaves.  A RenderAtom can't have a null
	 * SpatialLeaf, and its linked SpatialLeaf is final.
	 */
	public RenderAtom(SpatialLeaf link, StateLeaf stateLink) throws NullPointerException {
		if (link == null)
			throw new NullPointerException("Can't have a null spatial link");
		
		this.spatialLink = link;
		this.stateLink = stateLink;
	}
	
	/**
	 * Return the SpatialLeaf that represents this atom's location.
	 */
	public SpatialLeaf getSpatialLink() {
		return this.spatialLink;
	}
	
	/**
	 * Return the StateLeaf that represents this atom's visual appearance, it can be null.
	 */
	public StateLeaf getStateLink() {
		return this.stateLink;
	}
	
	/**
	 * Set this atom's visual appearance.  If link is null, or if link doesn't have a Geometry instance 
	 * in its merged states, then this atom will not be rendered.
	 */
	public void setStateLink(StateLeaf link) {
		this.stateLink = link;
	}
	
	/**
	 * Get a cached representation of this atom's StateLeaf's merged states.  This may be null or invalid
	 * if accessed outside of a RenderAtomBin rendering operation.  You should not modify the contents
	 * of this array directly, instead use StateAtom and StateManager masks.
	 */
	public StateManager[] getCachedStates() {
		return this.states;
	}
	
	/**
	 * Return the Geometry instance in this atom's linked StateLeaf.  Null is returned if it can't be 
	 * accessed for any reason.
	 */
	public Geometry getGeometry() {
		if (this.stateLink == null)
			return null;
		if (gDT < 0)
			gDT = RenderContext.registerStateAtomType(VertexArray.class);
		StateManager[] s = this.stateLink.getMergedStates();
		if (s == null || gDT >= s.length)
			return null;
		return (Geometry)s[gDT];
	}
	
	/**
	 * Whether or not this atom has a non-null BlendState present in its StateLeaf that has blending enabled.
	 * If this is true, then this atom is subject to a more rigorous and expensive rendering algorithm to 
	 * increase the chances of a visually correct rendering (see TransparentAtomBin).
	 */
	public boolean isTransparent() {
		if (this.stateLink == null)
			return false;
		if (bDT < 0)
			bDT = RenderContext.registerStateAtomType(BlendState.class);
		StateManager[] sm = this.stateLink.getMergedStates();
		if (bDT >= sm.length || sm[bDT] == null)
			return false;
		BlendState s = ((BlendStateManager)sm[bDT]).getStateAtom();
		return s != null && s.isBlendEnabled();
	}
	
	/**
	 * Don't call directly, should be used by RenderAtomBin (and subclasses) to correctly apply and restore
	 * states given the previously rendered atom and the upcoming atom.  This method obeys the state manager
	 * masking in pass.
	 */
	public static void applyStates(RenderAtom prev, RenderAtom next, RenderManager manager, RenderPass pass) {
		StateManager[] p = (prev != null ? prev.states : null);
		StateManager[] n = (next != null ? next.states : null);
		
		if (n != null) {
			for (int i = 0; i < n.length; i++) {
				if (n[i] != null && !pass.isStateManagerMasked(i)) {
					n[i].apply(manager, pass);
				}
			}
		}
		if (p != null) {
			for (int i = 0; i < p.length; i++) {
				if (p[i] != null && (n == null || i >= n.length || n[i] == null) && !pass.isStateManagerMasked(i)) {
					p[i].restore(manager, pass);
				}
			}
		}
	}
}
