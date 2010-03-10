package com.ferox.scene.controller;

import java.util.Iterator;

import com.ferox.math.Frustum;
import com.ferox.math.Matrix3f;
import com.ferox.scene.SceneElement;
import com.ferox.scene.ViewNode;
import com.ferox.util.Bag;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.ComponentId;
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
 * <li>Use the EntitySystem's attached {@link SceneController} to determine the
 * visible entities within the ViewNode and flag them as visible:
 * {@link SceneElement#isPotentiallyVisible()}.</li>
 * </ol>
 * 
 * @author Michael Ludwig
 */
public class ViewNodeController implements Controller {
	private static final ComponentId<ViewNode> VN_ID = Component.getComponentId(ViewNode.class);
	private static final ComponentId<SceneElement> SE_ID = Component.getComponentId(SceneElement.class);
	private static final ComponentId<VisibleEntities> VE_ID = Component.getComponentId(VisibleEntities.class);

	/**
	 * Create a ViewNodeController.
	 */
	public ViewNodeController() { }

	/**
	 * Overridden to perform the following operations:
	 * <ol>
	 * <li>Update all ViewNodes to reflect their attached SceneElements, if
	 * possible.</li>
	 * <li>Update the ViewNodes' projections to match dimension changes, if
	 * necessary.</li>
	 * <li>Compute the visibility sets for each ViewNode in the system.</li>
	 * </ol>
	 * After process() completes, {@link #getVisibleEntities(ViewNode)} will
	 * return meaningful results for the ViewNodes encountered within the
	 * EntitySystem. Subsequent processing will overwrite the previous results.
	 * 
	 * @throws NullPointerException if system is null
	 */
	@Override
	public void process(EntitySystem system) {
		SpatialHierarchyUtil scene = new SpatialHierarchyUtil(system);
		Iterator<Entity> it = system.iterator(VN_ID);
		while(it.hasNext()) {
			process(it.next(), scene);
		}
		
		// FIXME: how do we clean up these book-keeping components that aren't managed
		// by the user but are shared across multiple controllers (complication is that
		// not just the viewnodecontroller creates the visibleentities components, so
		// we can't just iterate and remove all ve's that are no longer vn's)
		//
		// we could make it so that ve has a creator, and this controller only cleans
		// up those that it owns.
	}
	
	private void process(Entity e, SpatialHierarchyUtil scene) {
		ViewNode vn = e.get(VN_ID);
		SceneElement se = e.get(SE_ID);
		
		Frustum f = vn.getFrustum();
		boolean needsUpdate = true;
		if (se != null) {
			// modify the frustum so that it matches the scene element 
			// location and orientation
			Matrix3f m = se.getTransform().getRotation();
			m.getCol(1, f.getUp());
			m.getCol(2, f.getDirection());
			f.getLocation().set(se.getTransform().getTranslation());
		}
		
		if (vn.getAutoUpdateProjection()) {
			int width = vn.getRenderSurface().getWidth();
			int height = vn.getRenderSurface().getHeight();
			
			if (f.isOrthogonalProjection()) {
				// modify the frustum so that it spans the entire RS
				f.setFrustum(0, width, 0, height, f.getFrustumNear(), f.getFrustumFar());
			} else {
				// modify the frustum to use the correct aspect ratio
				float oldHeight = ((f.getFrustumTop() - f.getFrustumBottom()) / 2f);
				float fov = (float) Math.toDegrees(Math.atan(2 * oldHeight / f.getFrustumNear()));
				f.setPerspective(fov, width / (float) height, f.getFrustumNear(), f.getFrustumFar());
			}
			
			// setFrustum and setPerspective auto update the frustum, so 
			// set needsUpdate to false so we don't do it again
			needsUpdate = false;
		}
		
		if (needsUpdate)
			f.updateFrustumPlanes();
		
		// frustum is up-to-date, so now we perform a visibility query
		VisibleEntities ve = e.get(VE_ID);
		if (ve == null) {
			ve = new VisibleEntities();
			e.add(ve);
		}
		ve.resetVisibility();
		
		Bag<Entity> query = ve.getEntities();
		scene.query(f, query);

		// modify all scene elements to be potentially visible
		int ct = query.size();
		for (int i = 0; i < ct; i++) {
			se = (SceneElement) query.get(i).get(SE_ID);
			if (se != null)
				se.setPotentiallyVisible(true);
		}
	}
}
