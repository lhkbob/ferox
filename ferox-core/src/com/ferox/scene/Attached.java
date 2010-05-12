package com.ferox.scene;

import com.ferox.entity.AbstractComponent;
import com.ferox.entity.Entity;
import com.ferox.math.Transform;

public class Attached extends AbstractComponent<Attached> {
	private Entity attachedTo;
	private final Transform offset;
	
	public Attached(Entity attachedTo) {
		this(attachedTo, new Transform());
	}
	
	public Attached(Entity attachedTo, Transform offset) {
		super(Attached.class);
		this.offset = new Transform();
		setAttachment(attachedTo);
		setOffset(offset);
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
		return attachedTo;
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
	 * Set the Entity that all updated Entities will be visually attached to. If
	 * this Entity is not also a SceneElement, the updates cannot be performed
	 * and the updated Entities will not have their Transforms modified (it will
	 * not throw an exception, however).
	 * 
	 * @param attachedTo The new attachment Entity
	 * @throws NullPointerException if attachedTo is null
	 */
	public void setAttachment(Entity attachedTo) {
		if (attachedTo == null)
			throw new NullPointerException("Entity attachedTo cannot be null");
		this.attachedTo = attachedTo;
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
			throw new NullPointerException("Transform cannot be null");
		this.offset.set(offset);
	}
}
