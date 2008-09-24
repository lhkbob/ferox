package com.ferox.core.scene;

import java.util.HashMap;
import java.util.Set;

import com.ferox.core.renderer.RenderAtomBin;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.scene.bounds.BoundingSphere;
import com.ferox.core.scene.bounds.BoundingVolume;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public class InfluenceLeaf extends SpatialNode {
	private BoundingVolume influence;
	private SpatialState state;
	private HashMap<SpatialLeaf, Boolean> exclude;
	
	public InfluenceLeaf(SpatialBranch parent) {
		this(parent, null);
	}
	
	public InfluenceLeaf(SpatialBranch parent, SpatialState state) {
		super(parent);
		this.setState(state);
		this.exclude = new HashMap<SpatialLeaf, Boolean>();
		this.setInfluence(new BoundingSphere());
	}
	
	public void setState(SpatialState state) throws IllegalArgumentException {
		if (state != null && state.leaf != null)
			throw new IllegalArgumentException("SpatialState already used by another InfluenceLeaf");
		if (this.state != null)
			this.state.leaf = null;
		this.state = state;
		if (this.state != null)
			this.state.leaf = this;
	}
	
	public SpatialState getState() {
		return this.state;
	}
	
	public void setInfluence(BoundingVolume b) throws NullPointerException {
		if (b == null)
			throw new NullPointerException("Can't have a null influence region");
		this.influence = b;
	}
	
	public BoundingVolume getInfluence() {
		return this.influence;
	}
	
	public void addExcludedLeaf(SpatialLeaf leaf) {
		if (leaf != null)
			this.exclude.put(leaf, true);
	}
	
	public void removeExcludedLeaf(SpatialLeaf leaf) {
		if (leaf != null)
			this.exclude.remove(leaf);
	}
	
	public boolean isLeafExcluded(SpatialLeaf leaf) {
		if (leaf == null)
			return false;
		return this.exclude.containsKey(leaf);
	}
	
	public void clearAllExclusions() {
		this.exclude.clear();
	}
	
	public boolean influencesSpatialLeaf(SpatialLeaf leaf) throws NullPointerException {
		if (leaf == null)
			throw new NullPointerException("Can't test influence on a null spatial leaf");
		if (this.exclude.containsKey(leaf))
			return false;
		if (this.worldBounds == null || leaf.worldBounds == null)
			return true;
		return this.worldBounds.intersects(leaf.worldBounds);
	}
	
	public boolean submit(View view, RenderManager manager, RenderAtomBin queue, boolean initiator) {
		if (this.state != null && super.submit(view, manager, queue, initiator)) {
			this.state.updateSpatial();
			queue.addInfluenceLeaf(this);
			return true;
		}
		return false;
	}
	
	@Override
	public void updateBounds() {
		this.worldBounds = this.influence.applyTransform(this.worldTransform, this.worldBounds);
	}

	@Override
	public void writeChunk(OutputChunk out) {
		super.writeChunk(out);
		
		out.set("state", this.state);
		out.set("bounds", this.influence);
		
		Set<SpatialLeaf> excludes = this.exclude.keySet();
		Chunkable[] allChunks = new Chunkable[excludes.size()];
		int i = 0;
		for (SpatialLeaf s: excludes) {
			allChunks[i] = s;
			i++;
		}
		out.set("excludes", allChunks);
	}
	
	public void readChunk(InputChunk in) {
		super.readChunk(in);
		
		this.setState((SpatialState)in.getChunk("state"));
		this.setInfluence((BoundingVolume)in.getChunk("bounds"));
		
		Chunkable[] allChunks = in.getChunkArray("excludes");
		for (int i = 0; i < allChunks.length; i++) {
			this.addExcludedLeaf((SpatialLeaf)allChunks[i]);
		}
	}
}
