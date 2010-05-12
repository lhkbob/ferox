package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.math.Matrix3f;
import com.ferox.math.Transform;
import com.ferox.math.Vector3f;
import com.ferox.scene.Billboarded;
import com.ferox.scene.SceneElement;
import com.ferox.entity.Component;
import com.ferox.entity.ComponentId;
import com.ferox.entity.Controller;
import com.ferox.entity.Entity;
import com.ferox.entity.EntitySystem;

/**
 * <p>
 * The BillboardController processes an {@link EntitySystem} to implement the
 * behavior necessary that allows {@link SceneElement} to be {@link Billboarded}
 * . This controller will process all Billboarded entities that are also
 * SceneElements, and update the SceneElement's transform as required to meet
 * the constraints of the billboard.
 * </p>
 * <p>
 * Any Billboarded entities that are not SceneElements are ignored. The
 * SceneElement is required as a source for the location, and the result of the
 * orientation computation.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class BillboardController extends Controller {
	private static final ComponentId<Billboarded> B_ID = Component.getComponentId(Billboarded.class);
	private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
	
	private static final Vector3f ZERO = new Vector3f();
	
	public BillboardController(EntitySystem system) {
	    super(system);
	}
	
	@Override
	public void process() {
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
				Vector3f a = r.getCol((o + 2) % 3, null).ortho(d);
				
				updateMatrix(r, d, a, o);
			}
			if (b.getConstraintVector() != null) {
				// X = 0, Y = 1, Z = 2
				int o = b.getConstraintAxis().ordinal();
				Matrix3f r = t.getRotation();

				Vector3f d = b.getConstraintVector().normalize(null);
				Vector3f a = r.getCol((o + 2) % 3, null).ortho(d);
				
				updateMatrix(r, d, a, o);
			}
		}
	}
	
	private void updateMatrix(Matrix3f r, Vector3f d, Vector3f a, int o) {
	    if (!a.epsilonEquals(ZERO, .0001f)) {
            // properly perpendicular so we can update matrix
            a.normalize();
            
            r.setCol(o, d);
            r.setCol((o + 2) % 3, a);
            r.setCol((o + 1) % 3, a.cross(d, a));
        } else {
            // singularity for ortho axis (equals desired direction)
            r.getCol((o + 1) % 3, a).ortho(d).normalize();
            
            r.setCol(o, d);
            r.setCol((o + 1) % 3, a);
            r.setCol((o + 2) % 3, d.cross(a, a));
        }
	}
}
