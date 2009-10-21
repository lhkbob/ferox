package com.ferox.scene;

import com.ferox.math.Transform;

/**
 * <p>
 * The AttachController resembles the HierarchicalTransformController except
 * that it is streamlined for attaching some number of elements to a parent
 * element. Each element's world transform is computed as [parent's world
 * transform] X [element's local transform].
 * </p>
 * <p>
 * It is possible to build entire hierarchies where each SceneElement that acts
 * as a parent is also being updated by another AttachController.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class AttachController implements UpdateController {
	private SceneElement attachment;
	private final Transform cachedWorld;

	/**
	 * Create a new AttachController that uses the given element as the initial
	 * attachment SceneElement.
	 * 
	 * @param element The SceneElement to attach others to
	 */
	public AttachController(SceneElement element) {
		cachedWorld = new Transform();
		setAttachment(element);
	}

	/**
	 * <p>
	 * Set the SceneElement that all affected elements are attached to. Each
	 * SceneElement that has this as its UpdateController will have its world
	 * transform computed as the concatenation of the attachment's world
	 * transform and the element's local transform.
	 * </p>
	 * <p>
	 * As the attachment element changes its world transform, each attached
	 * element will be moved with it.
	 * </p>
	 * 
	 * @param element The SceneElement that acts as a parent to any scene
	 *            element affected by this UpdateController
	 */
	public void setAttachment(SceneElement element) {
		attachment = element;
	}

	/**
	 * Return the SceneElement that acts as a parent to any SceneElement which
	 * uses this as its UpdateController. If null is returned, an child element
	 * will be updated as per the contract of SceneElement.
	 * 
	 * @return The SceneElement that children are 'attached' to
	 */
	public SceneElement getAttachment() {
		return attachment;
	}
	
	@Override
	public boolean update(SceneElement element, float timeDelta) {
		if (attachment == null) {
			// default behavior
			if (element.isDirty()) {
				element.getWorldTransform().set(element.getLocalTransform());
				return true;
			}
			
			return false;
		} else {
			boolean update = false;
			if (attachment.isDirty()) {
				attachment.update(timeDelta);
				update = true;
			}
			
			if (!cachedWorld.equals(attachment.getWorldTransform())) {
				cachedWorld.set(attachment.getWorldTransform());
				update = true;
			}
			
			if (update || element.isDirty()) {
				cachedWorld.mul(element.getLocalTransform(), element.getWorldTransform());
				update = true;
			}
			
			return update;
		}
	}
}
