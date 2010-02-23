package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.math.Matrix3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.scene.Billboarded;
import com.ferox.scene.SceneElement;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.ComponentId;
import com.ferox.util.entity.Controller;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;

public class BillboardController implements Controller {
	private static final ComponentId<Billboarded> B_ID = Component.getComponentId(Billboarded.class);
	private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
	
	@Override
	public void process(EntitySystem system) {
		Iterator<Entity> it = system.iterator(B_ID);
		while(it.hasNext()) {
			process(it.next());
		}
	}

	private void process(Entity e) {
		Billboarded b = e.get(B_ID);
		SceneElement se = e.get(SE_ID);
		if (b != null && se != null) {
			Transform t = se.getTransform();
			if (b.getBillboardPoint() != null) {
				// X = 0, Y = 1, Z = 2
				int o = b.getBillboardDirectionAxis().ordinal();
				Matrix3f r = t.getRotation();

				Vector3f d = b.getBillboardPoint().sub(t.getTranslation(), null).normalize();
				Vector3f a = r.getCol((o + 1) % 3, null).ortho(d).normalize();
				
				r.setCol(o, d);
				r.setCol((o + 1) % 3, a);
				r.setCol((o + 2) % 3, d.cross(a, a));
			}
			if (b.getConstraintVector() != null) {
				// X = 0, Y = 1, Z = 2
				int o = b.getConstraintAxis().ordinal();
				Matrix3f r = t.getRotation();

				Vector3f d = b.getConstraintVector().normalize(null);
				Vector3f a = r.getCol((o + 1) % 3, null).ortho(d).normalize();
				
				r.setCol(o, d);
				r.setCol((o + 1) % 3, a);
				r.setCol((o + 2) % 3, d.cross(a, a));
			}
		}
	}
}
