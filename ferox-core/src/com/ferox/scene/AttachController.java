package com.ferox.scene;

import com.ferox.math.Transform;

public class AttachController implements UpdateController {
	private SceneElement attachment;
	private final Transform cachedWorld;
	
	public AttachController(SceneElement element) {
		cachedWorld = new Transform();
		setAttachment(element);
	}
	
	public void setAttachment(SceneElement element) {
		attachment = element;
	}
	
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
