package com.ferox.scene2;

import com.ferox.math.Transform;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.Entity;

public class AttachUpdater implements SceneElementUpdater {
	private final static int SE_ID = Component.getTypeId(SceneElement.class);

	private Entity attachTo;
	private final Transform offset;
	
	public AttachUpdater(Entity attachTo, Transform offset) {
		this.offset = new Transform();
		
		setOffset(offset);
		setAttachment(attachTo);
	}
	
	public Transform getOffset() {
		return offset;
	}
	
	public void setOffset(Transform offset) {
		if (offset == null)
			throw new NullPointerException("Offset cannot be null");
		this.offset.set(offset);
	}
	
	public Entity getAttachment() {
		return attachTo;
	}
	
	public void setAttachment(Entity attachTo) {
		if (attachTo == null)
			throw new NullPointerException("Attached Entity cannot be null");
		this.attachTo = attachTo;
	}
	
	@Override
	public void update(Entity entity, SceneController controller, float dt) {
		SceneElement parent = (SceneElement) attachTo.get(SE_ID);
		SceneElement child = (SceneElement) entity.get(SE_ID);
		
		if (parent != null && child != null) {
			// make sure the parent is up to date first
			controller.process(attachTo);
			
			// compute the accumulated transform
			parent.getTransform().mul(offset, child.getTransform());
		}
	}
}
