package com.ferox.scene;

import com.ferox.math.Matrix3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.util.ReadOnly;

/**
 * <p>
 * HierarchicalTransformController is an UpdateController that assumes the role
 * of a Group or Node (or similarly named) structure would in a conventional
 * scene graph. It allows for hierarchies of transforms with potentially many
 * SceneElements attached at each level of the tree. It is not necessary for
 * every hierarchy in the Scene to have the same root, making it easy to setup
 * hierarchies only when needed.
 * </p>
 * <p>
 * To determine the final world transform of an attached SceneElement, the
 * element's local transform will be multiplied on the left by its
 * HierarchicalTransformController's local transform. Subsequently, the
 * controller's parent's transform will be multiplied onto the left, etc. until
 * the root controller has been processed.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class HierarchicalTransformController implements UpdateController {
	private final Transform accumulatedTransform;
	private final Transform stepTransform;
	
	private boolean dirty;
	private HierarchicalTransformController parent;
	
	/**
	 * Create a HierarchicalTransformController that's not attached to any
	 * parent and has the identity transform.
	 */
	public HierarchicalTransformController() {
		this(null);
	}
	
	/**
	 * Create a HierarchicalTransformController that copies t as its initial
	 * transform value. If t is null, the transform is initialized to the
	 * identity.
	 * 
	 * @param t Initial transform
	 */
	public HierarchicalTransformController(Transform t) {
		this(null, null);
	}
	
	/**
	 * Creates a HierarchicalTransformController that copies t for its initial
	 * transform and uses the given parent. If parent is not null, then the
	 * created Controller is at the root of its hierarchy.
	 * 
	 * @param t Initial transform
	 * @param parent Parent controller in the transform hierarchy
	 */
	public HierarchicalTransformController(Transform t, HierarchicalTransformController parent) {
		stepTransform = new Transform();
		accumulatedTransform = new Transform();
		
		setTransform(t);
		setParent(parent);
	}
	
	/**
	 * Convenience function to return the translation vector of the transform
	 * returned by geTransform(). For the same reasons that getTransform() is
	 * read-only, so is the returned vector. Any changes to it should be
	 * followed by a call to setDirty().
	 * 
	 * @return The translation offset
	 */
	@ReadOnly
	public Vector3f getTranslation() {
		return stepTransform.getTranslation();
	}
	
	/**
	 * Convenience function to return the rotation matrix of the transform
	 * returned by getTransform(). For the same reasons that getTransform() is
	 * read-only, so is the returned matrix. Any changes to it should be
	 * followed by a call to setDirty().
	 * 
	 * @return The rotation matrix
	 */
	@ReadOnly
	public Matrix3f getRotation() {
		return stepTransform.getRotation();
	}
	
	/**
	 * Set the translation that this Controller will use as on offset to each
	 * controlled SceneElement's local transform. If this Controller is part of
	 * a hierarchy, the vector <x, y, z> is translation for this current step of
	 * the transform. This will flag the element as dirty.
	 * 
	 * @param x The new x-coordinate
	 * @param y The new y-coordinate
	 * @param z The new z-coordinate
	 */
	public void setTranslation(float x, float y, float z) {
		stepTransform.setTranslation(x, y, z);
		setDirty();
	}
	
	/**
	 * Copy t into the translation for this HierarchicalTransformController. If
	 * t is null, it will be set to <0, 0, 0>. This will flag the element as
	 * dirty.
	 * 
	 * @param t The new translation vector
	 */
	public void setTranslation(Vector3f translation) {
		stepTransform.setTranslation(translation);
		setDirty();
	}
	
	/**
	 * Copy m into the rotation for this Controller's transform step. If m is
	 * null, then it is set to the identity rotation matrix. This will flag the
	 * element as dirty.
	 * 
	 * @param m The new local rotation
	 */
	public void setRotation(Matrix3f rotation) {
		stepTransform.setRotation(rotation);
		setDirty();
	}

	/**
	 * <p>
	 * Return the local transform instance used by this
	 * HierarchicalTransformController. Any SceneElements that have this as
	 * their UpdateController will have a final world transform that's their
	 * local transform multiplied by the accumulation of this Controller's
	 * transform and its (possible) chain of parent Controllers.
	 * </p>
	 * <p>
	 * In this way a HierarchicalTransformController's transform resembles the
	 * local transform of a group or node in conventional scene graphs.
	 * </p>
	 * <p>
	 * The returned instance need not be a defensive copy and it should be
	 * considered as read-only. However, if it is modified, use setDirty() to
	 * ensure that the element's update() has the correct behavior.
	 * </p>
	 * 
	 * @return The local transform for this HierarchicalTransformController
	 */
	@ReadOnly
	public Transform getTransform() {
		return stepTransform;
	}
	
	/**
	 * Copy t into this Controller's local transform. . If t is null, the
	 * transform will be set to the identity. Calling this method will also
	 * invoke setDirty().
	 * 
	 * @param t The new local transform values
	 */
	public void setTransform(Transform t) {
		stepTransform.set(t);
		setDirty();
	}
	
	/**
	 * <p>
	 * Return the current parent for this HierarchicalTransformController. If
	 * this is not null, any controlled SceneElement will have its transform
	 * modified by the parent after it's been modified by this controller's
	 * transform. This process will continue as long as there are parents in the
	 * chain.
	 * </p>
	 * <p>
	 * In this way, HierarchicalTransformControllers can be used to form
	 * hierarchies of transforms that group SceneElements.
	 * </p>
	 * 
	 * @return This Controller's parent
	 */
	public HierarchicalTransformController getParent() {
		return parent;
	}
	
	/**
	 * Assign a new HierarchicalTransformController as this controller's parent
	 * in the hierarchy. If parent is null, then this controller becomes the
	 * root of its hierarchy (assuming that there are other controllers using it
	 * as their parent). See getParent() for more details. This will invoke
	 * setDirty().
	 * 
	 * @see #getParent()
	 * @param parent
	 */
	public void setParent(HierarchicalTransformController parent) {
		this.parent = parent;
		setDirty();
	}
	
	/**
	 * Notify the HierarchicalTransformController that its transform has been
	 * modified externally and that it should return true from its next
	 * update().
	 */
	public void setDirty() {
		dirty = true;
	}
	
	@Override
	public boolean update(SceneElement element, float timeDelta) {
		if (updateTransform() || element.isDirty()) {
			accumulatedTransform.mul(element.getLocalTransform(), element.getWorldTransform());
			return true;
		}
		
		return false;
	}
	
	// ensures that accumulatedTransform holds the proper values and resets dirty
	private boolean updateTransform() {
		boolean updated = false;
		
		if (parent != null) {
			if (parent.updateTransform() || dirty) {
				parent.accumulatedTransform.mul(stepTransform, accumulatedTransform);
				updated = true;
			}
		} else {
			if (dirty) {
				accumulatedTransform.set(stepTransform);
				updated = true;
			}
		}
		
		dirty = false;
		return updated;
	}
}
