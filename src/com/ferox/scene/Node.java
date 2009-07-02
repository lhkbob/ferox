package com.ferox.scene;

import java.util.ArrayList;
import java.util.List;

import com.ferox.math.Matrix3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.renderer.RenderQueue;
import com.ferox.renderer.View;
import com.ferox.renderer.View.FrustumIntersection;

/**
 * <p>
 * Node represents the superclass for any element of a scene. It provides local
 * and world transforms, world bounds and can keep track of the lights and fog
 * affecting it.
 * </p>
 * <p>
 * A scene (built up of Nodes in a tree) goes through two phases before
 * rendering: updating and then visiting. To guarantee that a Node's world
 * transform, bounds, lights and fog stats are consistent, the scene must be
 * updated. After a scene is updated, each node can be visited with a View and
 * RenderQueue to add any required render atoms. After visiting is completed,
 * the render queue is prepared for flushing to a Framework.
 * </p>
 * 
 * @author Michael Ludwig
 */
public abstract class Node {
	/**
	 * Represents the culling mode for when a Node is visited. See
	 * getCullMode().
	 */
	public static enum CullMode {
		NEVER, ALWAYS, FRUSTUM
	}

	/**
	 * Represents the different outcomes of a Node's visit() method.
	 */
	public static enum VisitResult {
		FAIL, SUCCESS_PARTIAL, SUCCESS_ALL
	}

	private static final CullMode DEFAULT_CULLMODE = CullMode.FRUSTUM;

	private CullMode cullMode;

	// package-level so that Group can modify it during add() and remove()
	Group parent;

	/** Represents the world transform of this Node */
	protected final Transform worldTransform;

	/**
	 * Represents the local transform of this Node (transform relative to its
	 * parent)
	 */
	protected final Transform localTransform;

	/**
	 * Represents the world bounds of this Node.
	 */
	protected BoundVolume worldBounds;

	/**
	 * Closest FogNode to this Node. Null implies no fog intersection. If a Node
	 * implementation doesn't desire attached fogs, they should override
	 * updateFog() or ignore this value.
	 */
	protected FogNode fog;

	/**
	 * A list of all LightNodes that intersect this Node. If this list is null,
	 * then LightNodes will not be attached during updateLight().
	 */
	protected List<LightNode<?>> lights;

	/**
	 * Create a Node with default state: identity transforms, no bounds, and the
	 * default cull mode.
	 */
	public Node() {
		worldTransform = new Transform();
		localTransform = new Transform();

		worldBounds = null;

		setCullMode(null);
		parent = null;
	}

	/**
	 * Sets this Node's cull mode, which is used to determine which nodes are
	 * processed during visit().
	 * 
	 * @param mode The cull mode to use, null = FRUSTUM
	 */
	public void setCullMode(CullMode mode) {
		if (mode == null)
			mode = DEFAULT_CULLMODE;
		cullMode = mode;
	}

	/**
	 * Cull mode determines the culling policy used when a Node is processed in
	 * visit(). If the cull mode is FRUSTUM, it is process if its world bounds
	 * intersect the View's frustum. If it is NEVER, the Node (or any of its
	 * children) will not be processed. If it's ALWAYS, then this Node (and all
	 * of its children) will be processed. Nodes with a cull mode of ALWAYS
	 * still require that their parents were successfully processed.
	 * 
	 * @return The CullMode to use for this Node
	 */
	public CullMode getCullMode() {
		return cullMode;
	}

	/**
	 * Get the parent of this Node in the tree hierarchy of the scene. If null
	 * is returned, then this Node is the root of its tree.
	 * 
	 * @return The parent of this Node
	 */
	public Group getParent() {
		return parent;
	}

	/**
	 * Sets this Node's parent to be the given Group. If this Node already has a
	 * parent, it removes itself from that Group first. If the new Group isn't
	 * null, it then adds itself to the new Group, otherwise it becomes the root
	 * of a newly detached tree.
	 * 
	 * @param parent The Group that will be this Node's new parent
	 */
	public void setParent(Group parent) {
		if (parent != this.parent) {
			if (this.parent != null)
				this.parent.remove(this); // this.parent == null
			if (parent != null)
				parent.add(this); // this.parent == parent
		}
	}

	/**
	 * Updates this node's world transform to reflect changes in its local
	 * transform. fast has the same behavior as in localToWorld().
	 * 
	 * @see #localToWorld(Transform, Transform, boolean)
	 * @param fast If true, it assumes that this Node's parent's world transform
	 *            is valid
	 */
	public void updateTransform(boolean fast) {
		// localToWorld(IDENTITY.get(), worldTransform, fast);
		if (parent != null)
			parent.worldTransform.mul(localTransform, worldTransform);
		else
			worldTransform.set(localTransform);
	}

	/**
	 * Re-compute this node's world bounds. This should only be called after a
	 * valid and consistent world transform is guaranteed.
	 */
	public abstract void updateBounds();

	/**
	 * Update this Node's world transform, world bounds, and detect any
	 * LightNode's and FogNode's that affect it. After this method is called,
	 * the Node's world state is valid.
	 */
	public void update() {
		updateTransformAndBounds(true);

		List<LightNode<?>> lights = sceneLights.get();
		List<FogNode> fogs = sceneFogs.get();

		// reset the list
		lights.clear();
		fogs.clear();
		// fill the list
		prepareLightsAndFog(lights, fogs);

		// update this node for all detected lights
		int size = lights.size();
		for (int i = 0; i < size; i++)
			updateLight(lights.get(i));

		// update this node for all detected fogs
		size = fogs.size();
		for (int i = 0; i < size; i++)
			updateFog(fogs.get(i));
	}

	/**
	 * <p>
	 * Signal to a Node that it should prepare itself for upcoming updateLight()
	 * and updateFog() calls.
	 * </p>
	 * <p>
	 * The default implementation resets the attached FogNodes and LightNodes
	 * (for LightNodes, the Node's lights list is only cleared if it's not
	 * null).
	 * </p>
	 * <p>
	 * Subclasses with children should invoke this method on each child. Groups
	 * or other branch nodes should not update sceneLights or sceneFogs, it is
	 * the responsibility of LightNode and FogNode to add themselves to these
	 * lists when this method is called.
	 * </p>
	 * 
	 * @param sceneLights A list that will accumulate all LightNodes in a scene
	 *            tree
	 * @param sceneFogs A list that will accumulate all FogNodes in a scene tree
	 */
	protected void prepareLightsAndFog(List<LightNode<?>> sceneLights, List<FogNode> sceneFogs) {
		// always reset fog
		fog = null;

		// reset the lights list if it's not null
		if (lights != null)
			lights.clear();
	}

	/**
	 * <p>
	 * Potentially attach the given LightNode to this Node. The current
	 * implementation will not attach the light if its lights list is set to
	 * null. Even if the list is null, subclasses that contain children should
	 * still invoke updateLight() on all children if necessary.
	 * </p>
	 * <p>
	 * The light is only attached if its bounds intersect with this Node's world
	 * bounds. If either light's or this Node's world bounds are null, then it
	 * is assumed to intersect.
	 * </p>
	 * 
	 * @param light The light that could be attached, assume not null
	 */
	protected void updateLight(LightNode<?> light) {
		if (lights != null) {
			BoundVolume lightBounds = light.getWorldBounds();
			if (lightBounds == null || worldBounds == null || worldBounds.intersects(lightBounds))
				lights.add(light);
		}
	}

	/**
	 * <p>
	 * Perform the identical operation as in updateLight(light), but for
	 * FogNodes. The only additional rule is that if a FogNode is already
	 * attached, the closest fog node should be chosen.
	 * </p>
	 * <p>
	 * Also there is no signal such as setting the lights list to null to
	 * disable the updating of Fogs. If subclasses do not want to hold onto a
	 * reference, they must override this method to not call
	 * super.updateFog(fog).
	 * </p>
	 * 
	 * @param fog The fog that could be attached, assume not null
	 */
	protected void updateFog(FogNode fog) {
		BoundVolume fogBounds = fog.getWorldBounds();
		if (fogBounds == null || worldBounds == null || worldBounds.intersects(fogBounds)) {
			if (this.fog != null) {
				if (worldTransform.distanceSquared(this.fog.worldTransform) 
					> worldTransform.distanceSquared(fog.worldTransform))
					this.fog = fog;
			} else {
				// just attach the fog
				this.fog = fog;
			}
		}
	}

	/**
	 * Actually perform the operations required for the transform and bounds
	 * portion of update(). If this Node has children, it should not call
	 * update(), but use updateTransformAndBounds(false).
	 * 
	 * @see #update()
	 * @param initiator True if this Node is first invocation in a chain of
	 *            updates necessary to get the scene tree updated.
	 */
	protected void updateTransformAndBounds(boolean initiator) {
		updateTransform(!initiator);
		updateBounds();
	}

	/**
	 * <p>
	 * This method recursively goes through the scene tree, rooted at this Node,
	 * and adds any render atoms to renderQueue that are necessary to properly
	 * render the scene.
	 * </p>
	 * <p>
	 * The default implementation just returns a VisitResult that respects the
	 * Node's cull mode (performing frustum intersection tests if necessary).
	 * Sub-classes should override this method to actually add atoms to
	 * renderQueue.
	 * </p>
	 * 
	 * @param renderQueue The RenderQueue that should be filled with RenderAtoms
	 *            representing this scene
	 * @param view The View that will be used to render renderQueue
	 * @param parentResult The VisitResult returned by this Node's parent its
	 *            visit() call, or null if this Node is the first to be visited.
	 */
	public VisitResult visit(RenderQueue renderQueue, View view, VisitResult parentResult) {
		switch (cullMode) {
		case ALWAYS:
			return VisitResult.FAIL;
		case NEVER:
			return VisitResult.SUCCESS_ALL;
		case FRUSTUM:
			if (parentResult == VisitResult.SUCCESS_ALL)
				return VisitResult.SUCCESS_ALL;
			else {
				FrustumIntersection test = (worldBounds == null ? FrustumIntersection.INTERSECT 
																: worldBounds.testFrustum(view));
				switch (test) {
				case INSIDE:
					return VisitResult.SUCCESS_ALL;
				case INTERSECT:
					return VisitResult.SUCCESS_PARTIAL;
				case OUTSIDE:
					return VisitResult.FAIL;
				}
			}
		}
		// shouldn't happen, but must please compiler
		return VisitResult.SUCCESS_PARTIAL;
	}

	/**
	 * <p>
	 * Get the BoundVolume that stores the world bounds of this Node. The
	 * returned value may be null or stale if updateBounds() hasn't been called
	 * after modification to this Node's children or transforms.
	 * </p>
	 * <p>
	 * If this returns null after an update(), and if its cull mode is FRUSTUM,
	 * then it is assumed to intersect the view.
	 * </p>
	 * 
	 * @return The BoundVolume instance holding this Node's bounds
	 */
	public BoundVolume getWorldBounds() {
		return worldBounds;
	}

	/**
	 * Get the world transform instance for this Node. Changes made to this
	 * transform will be overwritten by the proper world transform when the
	 * Node's transforms are updated. The proper world transform is considered
	 * to be the concatentation of this Node's parent's world transform and this
	 * Node's local transform.
	 * 
	 * @return The Transform instance holding this Node's world transform.
	 */
	public Transform getWorldTransform() {
		return worldTransform;
	}

	/**
	 * Get the local transform instance of this Node. Changes to this instance
	 * will be visible in the world transform after a call to
	 * updateTransform(boolean) or update().
	 * 
	 * @return The Transform instance holding this Node's local transform
	 */
	public Transform getLocalTransform() {
		return localTransform;
	}

	/**
	 * <p>
	 * Computes the appropriate world transform for the given transform (local).
	 * local is relative to this Node's identity space. The result of the
	 * computation is stored in result. If result is null, a new instace is
	 * created.
	 * </p>
	 * <p>
	 * Because the Node's parent's world transform may be in an invalid state,
	 * this method offers the boolean parameter 'fast'. If fast is true, then it
	 * is assumed that this Node's parent's world transform is valid and can be
	 * used directly. If it is false, the parent's world transform is recomputed
	 * from scratch and is guaranteed to be correct. It is recommended to
	 * specify fast = false when the updated state of a Node is unknown.
	 * </p>
	 * 
	 * @param local The transform that is to be converted to world space
	 * @param result The transform that will hold the converted local transform
	 * @param fast True if this Node's parent's world transform is valid and
	 *            up-to-date
	 * @return result, or a new Transform if result was null
	 * @throws NullPointerException if local is null
	 * @throws IllegalArgumentException if result is local, or this Node's local
	 *             transform
	 */
	public Transform localToWorld(Transform local, Transform result, boolean fast) {
		if (local == null)
			throw new NullPointerException("Can't compute world transform from null local trans");
		if (result == local || result == localTransform)
			throw new IllegalArgumentException("Can't use this node's local transform or local as result");
		if (result == null)
			result = new Transform();

		if (parent != null) {
			if (fast)
				result.set(parent.worldTransform);
			else
				parent.localToWorld(IDENTITY.get(), result, false);
		} else
			result.setIdentity();
		return result.mul(localTransform, result).mul(local, result);
	}

	/**
	 * <p>
	 * Computes the local transform, relative to this Node's coordinate space,
	 * based on the given Transform, world. The computed transform is stored in
	 * result, or a new instance if result is null.
	 * </p>
	 * <p>
	 * worldToLocal() uses a 'fast' parameter just like localToWorld(), but
	 * there is a subtle difference. With worldToLocal(), if fast is true, it
	 * assumes that this Node's world transform is valid (compapred to
	 * localToWorld(), which assumed that this Node's parent's world transform
	 * was valid).
	 * </p>
	 * 
	 * @param world The transform that should be converted into this Node's
	 *            local space
	 * @param result The transform that will hold onto the result
	 * @return result, or a new Transform if result was null
	 * @throws NullPointerException if world is null
	 * @throws IllegalArgumentException if result is world or this Node's world
	 *             transform instance.
	 */
	public Transform worldToLocal(Transform world, Transform result, boolean fast) {
		if (world == null)
			throw new NullPointerException("Can't compute local transform from null world trans");
		if (result == world || result == worldTransform)
			throw new IllegalArgumentException("Can't use this node's world transform or world as result");
		if (result == null)
			result = new Transform();

		if (fast)
			result.set(worldTransform);
		else
			localToWorld(IDENTITY.get(), result, false);
		result.inverseMul(world, result);

		return result;
	}

	/**
	 * Copy the transform values held in local into this Node's local transform
	 * instance. It will be necessary to call updateTransform() (directly or
	 * during the update process) to see these changes reflected in the Node's
	 * world transform.
	 * 
	 * @param local The transform to copy, null == identity
	 */
	public void setLocalTransform(Transform local) {
		localTransform.set(local);
	}

	/**
	 * <p>
	 * Compute the local transform necessary for this Node to have the given
	 * world position. The computed result is stored in this Node's local
	 * transform instance.
	 * </p>
	 * <p>
	 * It is safe to call this method with this Node's world transform method.
	 * This just have the affect of updating the Node's local transform so that
	 * the values in the world transform are preserved (assuming that the
	 * transforms in any of this Node's parents are not changed later).
	 * </p>
	 * 
	 * @param world The world transform that is used in computing an appropriate
	 *            local transform
	 * @throws NullPointerException if world is null
	 */
	public void setWorldTransform(Transform world) {
		if (parent == null) {
			// fail here, to be consistent with worldToLocal()'s
			// NullPointerException
			if (world == null)
				throw new NullPointerException("Cannot set a null world transform");
			localTransform.set(world);
		} else
			parent.worldToLocal(world, localTransform, false);
		worldTransform.set(world);
	}

	/**
	 * <p>
	 * Adjusts this Node's local transform so that it looks at position (in
	 * world coordinates), and its y-axis is aligned as closely as possible to
	 * up (not always exactly up because it has to maintain orthogonality).
	 * </p>
	 * 
	 * @param position The position that is being pointed at
	 * @param up The desired vertical vector (e.g. local y axis) of this node
	 * @throws NullPointerException if position or up are null
	 */
	public void lookAt(Vector3f position, Vector3f up) {
		if (position == null || up == null)
			throw new NullPointerException("Can't call lookAt() with null input vectors: " 
										   + position + " " + up);
		if (parent != null) {
			parent.updateTransform(false);
			parent.worldTransform.mul(localTransform, worldTransform);
		} else
			worldTransform.set(localTransform);

		Vector3f dirVec = position.sub(worldTransform.getTranslation(), Node.dirVec.get()).normalize();
		Vector3f leftVec = up.cross(dirVec, Node.leftVec.get()).normalize();
		Vector3f upVec = dirVec.cross(leftVec, Node.upVec.get()).normalize();

		Matrix3f rot = worldTransform.getRotation();
		rot.setCol(0, leftVec);
		rot.setCol(1, upVec);
		rot.setCol(2, dirVec);

		if (parent != null)
			parent.worldToLocal(worldTransform, localTransform, true);
		else
			localTransform.set(worldTransform);
	}

	/* Internal variables used for computations. */

	private static final ThreadLocal<List<LightNode<?>>> sceneLights = new ThreadLocal<List<LightNode<?>>>() {
		@Override
		protected List<LightNode<?>> initialValue() {
			return new ArrayList<LightNode<?>>();
		}
	};

	private static final ThreadLocal<List<FogNode>> sceneFogs = new ThreadLocal<List<FogNode>>() {
		@Override
		protected List<FogNode> initialValue() {
			return new ArrayList<FogNode>();
		}
	};

	private static final ThreadLocal<Transform> IDENTITY = new ThreadLocal<Transform>() {
		@Override
		protected Transform initialValue() {
			return new Transform();
		}
	};
	private static final ThreadLocal<Vector3f> dirVec = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> upVec = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
	private static final ThreadLocal<Vector3f> leftVec = new ThreadLocal<Vector3f>() {
		@Override
		protected Vector3f initialValue() {
			return new Vector3f();
		}
	};
}
