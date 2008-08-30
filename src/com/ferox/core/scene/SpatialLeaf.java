package com.ferox.core.scene;

import com.ferox.core.renderer.RenderAtom;
import com.ferox.core.renderer.RenderAtomBin;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.scene.bounds.AxisAlignedBox;
import com.ferox.core.scene.bounds.BoundingVolume;
import com.ferox.core.states.StateLeaf;
import com.ferox.core.states.manager.Geometry;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

/**
 * A SpatialLeaf represents a node in the SpatialTree that has no children.  It does however maintain an
 * array list of RenderAtoms.  SpatialLeaf can have multiple render atoms attached to it, but it only 
 * ever supplies ONE to the renderer at a time.  Subclasses implement the manner in which the correct attached
 * atom is selected.  This class provides a naive but unlimited means of attaching RenderAtoms, it is recommended
 * for implementations to create more user intuitive methods that delegate to the already defined functions.
 * @author Michael Ludwig
 *
 */
public class SpatialLeaf extends SpatialNode {
	protected StateLeaf state;
	private final RenderAtom atom;
	private BoundingVolume modelBounds;
	
	/**
	 * Create a SpatialLeaf with no current parent.
	 */
	public SpatialLeaf() {
		this(null, null);
	}
	
	/**
	 * Creates a SpatialLeaf with the given parent.
	 */
	public SpatialLeaf(SpatialBranch parent) {
		this(parent, null);
	}
	
	public SpatialLeaf(SpatialBranch parent, StateLeaf states) {
		super(parent);
		this.state = states;
		this.atom = new RenderAtom(this, states);
		this.setModelBounds(new AxisAlignedBox());
	}
	
	public void setStates(StateLeaf states) {
		this.state = states;
		this.atom.setStateLink(states);
	}
	
	public BoundingVolume getModelBounds() {
		return this.modelBounds;
	}
	
	public void setModelBounds(BoundingVolume model) {
		if (model == null)
			throw new NullPointerException("Model bounds can't be null");
		this.modelBounds = model;
	}
	
	@Override
	public void updateBounds() {
		Geometry g = this.atom.getGeometry();
		if (g == null) {
			this.worldBounds = null;
			return;
		}
		
		g.getBoundingVolume(this.modelBounds);
		this.worldBounds = this.modelBounds.applyTransform(this.worldTransform, this.worldBounds);
	}
	
	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.setObject("state", this.state);
		out.setObject("bounds", this.modelBounds);
	}
	
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.setStates((StateLeaf)in.getObject("state"));
		this.setModelBounds((BoundingVolume)in.getObject("bounds"));
	}
	
	@Override
	public boolean submit(View view, RenderManager manager, RenderAtomBin queue, boolean initiator) {
		if (super.submit(view, manager, queue, initiator)) {
			queue.addRenderAtom(this.atom, manager);
			return true;
		}
		return false;
	}
}
