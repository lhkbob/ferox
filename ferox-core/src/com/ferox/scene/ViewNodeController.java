package com.ferox.scene;

import java.util.Iterator;

import com.ferox.math.Frustum;
import com.ferox.math.Matrix3f;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.Controller;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;

/**
 * ViewNodeController is a Controller implementation that handles the processing
 * necessary to update each ViewNode within an EntitySystem. For each Entity
 * that is a ViewNode, a ViewNodeController performs the following operations:
 * <ol>
 * <li>If the entity is also a SceneElement, modify the ViewNode's Frustum so
 * that it reflects the location and orientation of the SceneElement. The y-axis
 * is considered up and the z-axis is considered to be the direction.</li>
 * <li>If {@link ViewNode#getAutoUpdateProjection()} returns true, modify the
 * projection as described in ViewNode to match its RenderSurface's dimensions.</li>
 * <li>Invoke {@link Frustum#updateFrustumPlanes()} so the Frustum is up to
 * date.</li>
 * </ol>
 * 
 * @author Michael Ludwig
 */
public class ViewNodeController implements Controller {
	private static final int VN_ID = Component.getTypeId(ViewNode.class);
	private static final int SE_ID = Component.getTypeId(SceneElement.class);

	/**
	 * Create a ViewNodeController that is registered with the given
	 * EntitySystem. If the system is null, then the controller will not be
	 * registered and should be registered later.
	 * 
	 * @param system The EntitySystem to be registered with if not null
	 */
	public ViewNodeController(EntitySystem system) {
		if (system != null)
			system.registerController(this);
	}
	
	@Override
	public void process(EntitySystem system) {
		Iterator<Entity> it = system.iterator(VN_ID);
		
		while(it.hasNext()) {
			process(it.next());
		}
	}
	
	private void process(Entity e) {
		ViewNode vn = (ViewNode) e.get(VN_ID);
		SceneElement se = (SceneElement) e.get(SE_ID);
		
		Frustum f = vn.getFrustum();
		boolean needsUpdate = true;
		if (se != null) {
			// modify the frustum so that it matches the scene element 
			// location and orientation
			Matrix3f m = se.getTransform().getRotation();
			m.getCol(1, f.getUp());
			m.getCol(2, f.getDirection());
			se.getTransform().getTranslation().set(f.getLocation());
		}
		
		if (vn.getAutoUpdateProjection()) {
			int width = vn.getRenderSurface().getWidth();
			int height = vn.getRenderSurface().getHeight();
			
			if (f.isOrthogonalProjection()) {
				// modify the frustum so that it spans the entire RS
				f.setFrustum(0, width, 0, height, f.getFrustumNear(), f.getFrustumFar());
			} else {
				// modify the frustum to use the correct aspect ratio
				int oldHeight = (int) ((f.getFrustumTop() - f.getFrustumBottom()) / 2f);
				float fov = (float) Math.toDegrees(Math.atan(2 * oldHeight / f.getFrustumNear()));
				f.setPerspective(fov, width / (float) height, f.getFrustumNear(), f.getFrustumFar());
			}
			
			// setFrustum and setPerspective auto update the frustum, so 
			// set needsUpdate to false so we don't do it again
			needsUpdate = false;
		}
		
		if (needsUpdate)
			f.updateFrustumPlanes();
	}
}
