package com.ferox.core.scene;

import com.ferox.core.renderer.RenderAtomBin;
import com.ferox.core.renderer.RenderManager;
import com.ferox.core.scene.bounds.BoundingVolume;
import com.ferox.core.util.FeroxException;
import com.ferox.core.util.io.Chunkable;
import com.ferox.core.util.io.InputChunk;
import com.ferox.core.util.io.OutputChunk;

public abstract class SpatialNode implements Chunkable {
	public static enum CullMode {
		NEVER, ALWAYS, DYNAMIC
	}
	
	public static final int LOCK_TRANSFORM_BIT = 1 << 0;
	public static final int LOCK_BOUNDS_BIT = 1 << 1;
	
	private static final Transform identity = new Transform();
	private static final Transform temp = new Transform();
	
	protected SpatialBranch parent;
	
	protected final Transform worldTransform;
	protected final Transform localTransform;
	
	protected BoundingVolume worldBounds;
	
	protected int visibility;
	
	private CullMode cullMode;
	private int locks;
	
	public SpatialNode() {
		this(null);
	}
	
	public SpatialNode(SpatialBranch parent) {
		this.worldTransform = new Transform();
		this.localTransform = new Transform();
		
		this.worldBounds = null;
		this.locks = 0;
		this.cullMode = CullMode.DYNAMIC;
		
		if (parent != null)
			parent.add(this);
	}
	
	public void setCullMode(CullMode mode) throws NullPointerException {
		if (mode == null)
			throw new NullPointerException("Cull mode can't be null");
		this.cullMode = mode;
	}
	
	public CullMode getCullMode() {
		return this.cullMode;
	}
	
	public SpatialBranch getParent() {
		return this.parent;
	}
	
	public void detach() {
		if (this.parent != null)
			this.parent.remove(this);
	}
	
	public void setParent(SpatialBranch b) {
		if (b != this.parent) {
			this.detach();
			if (b != null)
				b.add(this);
		}
	}
	
	public void updateTransform(boolean fast) {
		this.localToWorld(identity, this.worldTransform, fast);
	}
	
	public abstract void updateBounds();
	
	public void update(boolean initiator) {
		if (!this.isLocked(LOCK_TRANSFORM_BIT))
			this.updateTransform(!initiator);
		if (!this.isLocked(LOCK_BOUNDS_BIT))
			this.updateBounds();
	}
	
	public boolean submit(View view, RenderManager manager, RenderAtomBin queue, boolean initiator) throws NullPointerException {
		if (view == null || manager == null || queue == null)
			throw new NullPointerException("Arguments can't be null");
		if (!initiator && this.parent != null && this.parent.visibility == View.INSIDE)
			this.visibility = View.INSIDE;
		else
			this.computeVisibility(view);
		return this.visibility != View.OUTSIDE;
	}
	
	public int computeVisibility(View view) throws FeroxException {	
		switch(this.cullMode) {
		case ALWAYS: this.visibility = View.OUTSIDE; break;
		case NEVER: this.visibility = View.INSIDE; break;
		case DYNAMIC:
			if (this.worldBounds != null)
				this.visibility = this.worldBounds.testFrustum(view, view.getPlaneState());
			else
				this.visibility = View.INTERSECT;
			break;
		default:
			throw new FeroxException("Illegal cull mode specified for a spatial node");
		}
		return this.visibility;
	}
	
	public void lockBounds(boolean lock) {
		if (lock != this.isLocked(LOCK_BOUNDS_BIT)) {
			if (lock) {
				this.updateTransform(false);
				this.updateBounds();
				this.locks |= LOCK_BOUNDS_BIT;
			} else
				this.locks &= ~LOCK_BOUNDS_BIT;
		}
	}
	
	public void lockTransform(boolean lock) {
		if (lock != this.isLocked(LOCK_TRANSFORM_BIT)) {
			if (lock) {
				this.updateTransform(false);
				this.locks |= LOCK_TRANSFORM_BIT;
			} else
				this.locks &= ~LOCK_TRANSFORM_BIT;
		}
	}
	
	public void lock(boolean lock) {
		this.lockTransform(lock);
		this.lockBounds(lock);
	}
	
	public boolean isLocked(int bits) {
		return (this.locks & bits) == bits;
	}
	
	public BoundingVolume getWorldBounds() {
		return this.worldBounds;
	}
	
	public Transform getWorldTransform() {
		return this.worldTransform;
	}
	
	public Transform getLocalTransform() {
		return this.localTransform;
	}
	
	public Transform localToWorld(Transform local, Transform result) {
		return this.localToWorld(local, result, false);
	}
	
	public Transform localToWorld(Transform local, Transform result, boolean fast) throws NullPointerException, IllegalArgumentException {
		if (local == null)
			throw new NullPointerException("Can't compute world transform from null local trans");
		if (result == local || result == this.localTransform)
			throw new IllegalArgumentException("Can't use this node's local transform or local as result");
		if (result == null)
			result = new Transform();
		
		if (!this.isLocked(LOCK_TRANSFORM_BIT)) {
			if (this.parent != null) {
				if (fast)
					result.set(this.parent.worldTransform);
				else
					this.parent.localToWorld(identity, result, false);
			} else 
				result.setIdentity();
			result.mul(result, this.localTransform);
		} else
			result.set(this.worldTransform);
		result.mul(result, local);
		
		return result;
	}
	
	public Transform worldToLocal(Transform world, Transform result) {
		return this.worldToLocal(world, result, false);
	}
	
	public Transform worldToLocal(Transform world, Transform result, boolean fast) throws NullPointerException, IllegalArgumentException {
		if (world == null)
			throw new NullPointerException("Can't compute local transform from null world trans");
		if (result == world || result == this.worldTransform)
			throw new IllegalArgumentException("Can't use this node's world transform or world as result");
		if (result == null)
			result = new Transform();
		
		if (fast)
			result.set(this.worldTransform);
		else
			this.localToWorld(identity, result, false);
		result.inverseMul(result, world);
		
		return result;
	}
	
	public void setLocalTransform(Transform local) {
		this.localTransform.set(local);
		this.localToWorld(identity, this.worldTransform, false);
	}
	
	public void setWorldTransform(Transform world) {
		this.worldToLocal(world, temp, false);
		this.localTransform.set(temp);
		this.worldTransform.set(world);
	}
	
	public void writeChunk(OutputChunk out) {
		// parent will be set when this node is added to the parent
		// similarly with scene
		
		out.setObject("transform", this.localTransform);
		out.setEnum("cullMode", this.cullMode);
		out.setInt("locks", this.locks);
	}
	
	public void readChunk(InputChunk in) {
		// parent and scene are set later on
		
		this.setLocalTransform((Transform)in.getObject("transform"));
		this.cullMode = in.getEnum("cullMode", CullMode.class);
		this.locks = in.getInt("locks");
	}
}
