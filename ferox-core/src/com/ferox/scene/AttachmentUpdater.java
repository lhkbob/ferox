package com.ferox.scene;

import com.ferox.math.Transform;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.ComponentId;
import com.ferox.util.entity.Entity;

/**
 * <p>
 * AttachUpdater is a SceneElementUpdater that, when applied to an Entity,
 * constrain's that Entity's SceneElement transform such that it maintains a
 * constant offset between another Entity. This causes the given Entity to
 * appear 'attached' to the other. It can also be used to simulate hierarchical
 * transforms, where each child element is attached to a parent element.
 * </p>
 * <p>
 * Mathematically, if we have <tt>P</tt> is the SceneElement of the Entity
 * returned by {@link #getAttachment()}, and <tt>C</tt> is the SceneElement
 * that's being updated, <tt>C</tt>'s transform is computed as
 * <code>P.getTransform().mul(getOffset(), C)</code>.
 * </p>
 * <p>
 * Although SceneElementUpdaters operate on Entities and AttachUpdater specifies
 * that the attachment instance is of type Entity, the updating can only be
 * performed if both the {@link #getAttachment()} Entity and the Entities that
 * use this updater have SceneElement Components. If either does not, then the
 * update performs no action until the situation is rectified.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class AttachmentUpdater implements SceneElementUpdater {
	private final static ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);

	private Entity attachTo;
	private final Transform offset;

	/**
	 * Create a new AttachUpdater such that any SceneElement using this updater
	 * will be attached <tt>attachTo</tt> separated by <tt>offset</tt>.
	 * 
	 * @param attachTo The Entity that represents the attach instance
	 * @param offset The offset that separates attachTo from the SceneElements
	 * @throws NullPointerException if attachTo or offset are null
	 */
	public AttachmentUpdater(Entity attachTo, Transform offset) {
		this.offset = new Transform();
		
		setOffset(offset);
		setAttachment(attachTo);
	}

	/**
	 * Return the Transform offset that separates any updated Entity from the
	 * the attachment Entity.
	 * 
	 * @return The offset used
	 */
	public Transform getOffset() {
		return offset;
	}

	/**
	 * Copy <tt>offset</tt> into the internal Transform that stores the offset
	 * used when computing an updated Entity's final Transform. This offset can
	 * include, or be solely rotational in nature; it does not need to just be
	 * translations.
	 * 
	 * @param offset The new offset
	 */
	public void setOffset(Transform offset) {
		if (offset == null)
			throw new NullPointerException("Offset cannot be null");
		this.offset.set(offset);
	}

	/**
	 * Return the Entity that any updated Entities are visually attached or
	 * linked to by constraining their transforms to be offset from this
	 * Entity's SceneElement's transform. This is only valid when the Entity has
	 * a SceneElement Component.
	 * 
	 * @return The attachment
	 */
	public Entity getAttachment() {
		return attachTo;
	}

	/**
	 * Set the Entity that all updated Entities will be visually attached to. If
	 * this Entity is not also a SceneElement, the updates cannot be performed
	 * and the updated Entities will not have their Transforms modified (it will
	 * not throw an exception, however).
	 * 
	 * @param attachTo The new attachment Entity
	 * @throws NullPointerException if attachTo is null
	 */
	public void setAttachment(Entity attachTo) {
		if (attachTo == null)
			throw new NullPointerException("Attached Entity cannot be null");
		this.attachTo = attachTo;
	}
	
	@Override
	public void update(Entity entity, SceneController controller, float dt) {
		SceneElement parent = attachTo.get(SE_ID);
		SceneElement child = entity.get(SE_ID);
		
		if (parent != null && child != null) {
			// make sure the parent is up to date first
			controller.process(attachTo);
			
			// compute the accumulated transform
			parent.getTransform().mul(offset, child.getTransform());
		}
	}
}
