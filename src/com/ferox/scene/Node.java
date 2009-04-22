package com.ferox.scene;

import java.util.List;

import org.openmali.vecmath.Matrix3f;
import org.openmali.vecmath.Vector3f;

import com.ferox.math.BoundVolume;
import com.ferox.math.Transform;
import com.ferox.renderer.RenderAtom;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.View;
import com.ferox.renderer.View.FrustumIntersection;

/** 
 * Node represents the primary superclass for any element of a scene.  It is highly unlikely
 * that implementing SceneElement without extending Node allows interaction with Node based classes.
 * It is plausible to implement a different SceneElement hierarchy for use by the renderer.
 * 
 * It provides local and world transforms, world bound caching, and default
 * implementations of update() and visit().
 * 
 * @author Michael Ludwig
 *
 */
// TODO: add general bound volume tests (shouldn't be in scene element because it's specific to nodes, etc.)
public abstract class Node implements SceneElement {
	/** Represents the culling mode for when a Node is visited. See getCullMode(). */
	public static enum CullMode {
		NEVER, ALWAYS, DYNAMIC
	}
	
	private static final CullMode DEFAULT_CULLMODE = CullMode.DYNAMIC;
	
	private static final ThreadLocal<Transform> IDENTITY = new ThreadLocal<Transform>() {
		protected Transform initialValue() {
			return new Transform();
		}
	};
	private static final ThreadLocal<Vector3f> dirVec = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> upVec = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> leftVec = new ThreadLocal<Vector3f>() {
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	
	// package-level so that Group can modify it during add() and remove()
	Group parent;
	
	/** Represents the world transform of this Node */
	protected final Transform worldTransform;
	
	/** Represents the local transform of this Node (transform relative to its parent) */
	protected final Transform localTransform;
	
	/** Represents the world bounds of this Node.  If non-null after updateBounds(), view intersections
	 * will assume intersection with frustum. */
	protected BoundVolume worldBounds;
		
	private CullMode cullMode;
	
	private boolean lockTransform;
	private boolean lockBounds;
	
	/** Cull mode is by default DYNAMIC, nothing is locked and world bounds are null.  No parent. */
	public Node() {
		this.worldTransform = new Transform();
		this.localTransform = new Transform();
		
		this.worldBounds = null;

		this.lockTransform = false;
		this.lockBounds = false;
		
		this.setCullMode(null);
		this.parent = null;
	}
	
	/** Sets this Node's cull mode, if null it is set to default. */
	public void setCullMode(CullMode mode) throws SceneException {
		if (mode == null)
			mode = DEFAULT_CULLMODE;
		this.cullMode = mode;
	}
	
	/** Cull mode determines the culling policy used when being visited.
	 * DYNAMIC = test its bounds against the View.
	 * NEVER = process this Node, and all of its children (unless a given child has cull = ALWAYS).
	 * ALWAYS = don't process this Node or any of its children. */
	public CullMode getCullMode() {
		return this.cullMode;
	}
	
	/** See SceneElement.getParent() */
	public Group getParent() {
		return this.parent;
	}

	/** Sets this Node's parent to be the given Group.  If this Node already has a parent, it 
	 * removes itself from that Group first.  If the new Group isn't null, it then adds
	 * itself to the new Group. */
	public void setParent(Group parent) {
		if (parent != this.parent) {
			if (this.parent != null)
				this.parent.remove(this); // this.parent == null
			if (parent != null)
				parent.add(this); // this.parent == parent
		}
	}
	
	/** Updates this node's world transform to reflect changes in its local transform. This update occurs
	 * whether or not transforms have been locked (only update(...) respects those).
	 * fast has the same behavior as in localToWorld and worldToLocal. */
	public void updateTransform(boolean fast) {
		this.localToWorld(IDENTITY.get(), this.worldTransform, fast);
	}
	
	/** Re-compute this node's world bounds.  When called, it should be after the world
	 * transform has been updated. Must ensure that worldBounds isn't null. 
	 * This method should perform the update even if isBoundsLocked() is true, since only update(...) respects that boolean. */
	public abstract void updateBounds();
	
	/** Implements SceneElement.update(...), updates the world transform and bounds of this Node if they aren't locked. */
	@Override
	public void update(boolean initiator) {
		if (!this.isTransformLocked())
			this.updateTransform(!initiator);
		if (!this.isBoundsLocked())
			this.updateBounds();
	}
	
	/** Implements SceneElement visit(...) to follow contract of this Node's cull mode.  If the cull mode is DYNAMIC,
	 * it will test for visibility with the current View in order to find the correct VisitResult. */
	@Override
	public VisitResult visit(RenderQueue renderQueue, View view, VisitResult parentResult) {
		switch(this.cullMode) {
		case ALWAYS: return VisitResult.FAIL;
		case NEVER: return VisitResult.SUCCESS_ALL;
		case DYNAMIC:
			if (parentResult == VisitResult.SUCCESS_ALL)
				return VisitResult.SUCCESS_ALL;
			else {
				FrustumIntersection test = (this.worldBounds == null ? FrustumIntersection.INTERSECT : this.worldBounds.testFrustum(view));
				switch(test) {
				case INSIDE: return VisitResult.SUCCESS_ALL;
				case INTERSECT: return VisitResult.SUCCESS_PARTIAL;
				case OUTSIDE: return VisitResult.FAIL;
				}
			}
		}
		// shouldn't happen, but must please compiler
		return VisitResult.SUCCESS_PARTIAL;
	}
	
	/** Utility method to add all render atoms of the scene into the
	 * given list (in many ways it functions like visit() where everything
	 * is inside the view frustum).  This list can then be used in
	 * a Renderer's compile() method.
	 * 
	 * The scene should be updated before this method is called, to get
	 * correct results.
	 * 
	 * If atoms is null, a new list should be created and returned. */
	public abstract List<RenderAtom> compile(List<RenderAtom> atoms);
			
	/** If bounds are locked, then this Node's world bounds will not be updated each time update() is called.
	 * This also means that when locked, the bounds will not reflect any changes in the world transform of the Node. */
	public boolean isBoundsLocked() {
		return this.lockBounds;
	}
	
	/** If lock is true, it updates the bounds before locking bounds.  It is recommended
	 * that the transforms are updated before calling this, or the bounds may not be accurate.
	 * If lock = isBoundsLocked(), this call is a no-op. */
	public void lockBounds(boolean lock) {
		if (lock != this.lockBounds) {
			if (lock) {
				this.updateTransform(false);
				this.updateBounds();
				this.lockBounds = true;
			} else
				this.lockBounds = false;
		}
	}
	
	/** If transforms are locked, then this Node's world transform will not be updated to reflect changes in its local transform
	 * or parent's world transform. */
	public boolean isTransformLocked() {
		return this.lockTransform;
	}
	
	/** If lock is true, it updates this Node's transform (fast = false) before locking transforms. */
	public void lockTransform(boolean lock) {
		if (lock != this.lockTransform) {
			if (lock) {
				this.updateTransform(false);
				this.lockTransform = true;
			} else
				this.lockTransform = false;
		}
	}
	
	/** Get the BoundVolume that stores the world bounds of this Node.  The returned value may be null or stale
	 * if updateBounds() hasn't been called after modification to this Node's children or transforms. 
	 * 
	 * If this returns null after an update(), and if its cull mode is DYNAMIC, then it is assumed to intersect the view. */
	public BoundVolume getWorldBounds() {
		return this.worldBounds;
	}
	
	/** Get the world transform instance for this Node.  Changes to this instance will only be visible if the transforms are
	 * locked, otherwise this is used as a cache for computing the world transform based off of this node's local transform. */
	public Transform getWorldTransform() {
		return this.worldTransform;
	}
	
	/** Get the local transform instance of this Node.  Changes to this instance will only be visible in the world transform 
	 * if the transforms aren't locked and/or updateTransform() is called. */
	public Transform getLocalTransform() {
		return this.localTransform;
	}
	
	/** Computes the appropriate world transform for the given transform (local).  local is relative to this Node's identity space.
	 * If this node's transforms are locked, then this method relies on the cached world transform for this node.
	 * Otherwise it computes its parent's world transform in two ways, then concatenates it with its own local transform:
	 *   1. If fast is true, uses this node's parent's cached world transform
	 *   2  If fast is false, computes this node's parent's guaranteed world transform (identical to 1 during an update() traversal).
	 * 
	 * Once this node's world transform has been computed, concatenates local and world together for the result.
	 * 
	 * The result of the computation is stored in result.  If result is null, a new instance is used.  If result is local or the same
	 * instance as this node's local transform, because attempted computation would result in numeric errors.  Also fails if local = null.
	 * 
	 * Returns result. */
	public Transform localToWorld(Transform local, Transform result, boolean fast) throws SceneException {
		if (local == null)
			throw new SceneException("Can't compute world transform from null local trans");
		if (result == local || result == this.localTransform)
			throw new SceneException("Can't use this node's local transform or local as result");
		if (result == null)
			result = new Transform();
		
		if (!this.isTransformLocked()) {
			if (this.parent != null) {
				if (fast)
					result.set(this.parent.worldTransform);
				else
					this.parent.localToWorld(IDENTITY.get(), result, false);
			} else 
				result.setIdentity();
			result.mul(result, this.localTransform);
		} else
			result.set(this.worldTransform);
		result.mul(result, local);
		
		return result;
	}
	
	/** Computes the transform (local to this node's identity space) that is equivalent to the given world transform. 
	 * result stores the answer and then returns it.  If result is null, a new instance is created.  
	 * If fast is true, it uses this node's cached world transform, otherwise it calculates the guaranteed world transform
	 * (they're the same during an update() traversal).
	 * 
	 * If result is the same instance as world, this node's world cache, or local cache, it will fail because otherwise numeric errors
	 * would occur.  If world is null, it also throws an exception. */
	public Transform worldToLocal(Transform world, Transform result, boolean fast) throws SceneException {
		if (world == null)
			throw new SceneException("Can't compute local transform from null world trans");
		if (result == world || result == this.worldTransform)
			throw new SceneException("Can't use this node's world transform or world as result");
		if (result == null)
			result = new Transform();
		
		if (fast)
			result.set(this.worldTransform);
		else
			this.localToWorld(IDENTITY.get(), result, false);
		result.inverseMul(result, world);
		
		return result;
	}
	
	/** Utility method to copy the given local transform into this node's local transform and to store the correct
	 * new world transform into this node's cache. */
	public void setLocalTransform(Transform local) {
		this.localTransform.set(local);
		this.localToWorld(IDENTITY.get(), this.worldTransform, false);
	}
	
	/** Utility method to copy the given world transform into this node's world transform and to store the correct
	 * new local transform into this node's cache. */
	public void setWorldTransform(Transform world) {
		if (this.parent == null)
			this.localTransform.set(world);
		else
			this.parent.worldToLocal(world, this.localTransform, false);
		this.worldTransform.set(world);
	}
	
	/** Adjusts this Node's local transform so that it looks at position (in world coordinates), and
	 * its y-axis is aligned as closely as possible to up (not always exactly up because it has to
	 * maintain orthogonality). 
	 * 
	 * Specify negateDirection = true if the node "faces" backwards.  This is especially useful
	 * for view nodes, since a view looks down the negative z-axis. */
	public void lookAt(Vector3f position, Vector3f up, boolean negateDirection) throws NullPointerException {
		if (position == null || up == null)
			throw new SceneException("Can't call lookAt() with null input vectors: " + position + " " + up);
		
		if (this.parent != null) {
			this.parent.updateTransform(false);
			this.worldTransform.mul(this.parent.worldTransform, this.localTransform);
		} else {
			this.worldTransform.set(this.localTransform);
		}
		
		Vector3f dirVec = Node.dirVec.get();
		Vector3f leftVec = Node.leftVec.get();
		Vector3f upVec = Node.upVec.get();
		
		dirVec.sub(position, this.worldTransform.getTranslation()); dirVec.normalize();
		if (negateDirection)
			dirVec.scale(-1f);
		
		leftVec.cross(up, dirVec); leftVec.normalize();
		upVec.cross(dirVec, leftVec); upVec.normalize();
				
		Matrix3f rot = this.worldTransform.getRotation();
		rot.setColumn(0, leftVec);
		rot.setColumn(1, upVec);
		rot.setColumn(2, dirVec);
		
		if (this.parent != null) {
			this.parent.worldToLocal(this.worldTransform, this.localTransform, true);
		} else {
			this.localTransform.set(this.worldTransform);
		}
	}
}
