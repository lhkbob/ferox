package com.ferox.scene.controller;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import com.ferox.scene.Attached;
import com.ferox.scene.SceneElement;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.ComponentId;
import com.ferox.util.entity.Controller;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;

public class AttachmentController implements Controller {
	private static final ComponentId<Attached> A_ID = Component.getComponentId(Attached.class);
	private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
	
	@Override
	public void process(EntitySystem system) {
		Set<Entity> alreadyProcessed = new HashSet<Entity>();
		Iterator<Entity> it = system.iterator(A_ID);
		while(it.hasNext()) {
			process(it.next(), alreadyProcessed);
		}
	}
	
	private void process(Entity e, Set<Entity> ignore) {
		if (ignore.contains(e))
			return; // don't process it anymore
		ignore.add(e);
		
		Attached a = e.get(A_ID);
		SceneElement se = e.get(SE_ID);
		if (a != null && se != null) {
			Entity attachedTo = a.getAttachment();
			Attached pa = attachedTo.get(A_ID);
			SceneElement pse = attachedTo.get(SE_ID);
			
			if (pse != null) {
				if (pa != null)
					process(attachedTo, ignore);
				pse.getTransform().mul(a.getOffset(), se.getTransform());
			}
		}
	}
}
