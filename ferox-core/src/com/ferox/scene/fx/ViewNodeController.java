package com.ferox.scene.fx;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.ferox.math.Frustum;
import com.ferox.math.Matrix3f;
import com.ferox.scene.SceneController;
import com.ferox.scene.SceneElement;
import com.ferox.util.Bag;
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
public class ViewNodeController extends Controller {
	private static final int VN_ID = Component.getTypeId(ViewNode.class);
	private static final int SE_ID = Component.getTypeId(SceneElement.class);

	private Map<Frustum, Bag<Entity>> visibilitySets;
	
	/**
	 * Create a ViewNodeController that is registered with the given
	 * EntitySystem.
	 * 
	 * @param system The EntitySystem to be registered with
	 * @throws NullPointerException if system is null
	 */
	public ViewNodeController(EntitySystem system) {
		super(system);
		visibilitySets = new HashMap<Frustum, Bag<Entity>>();
	}

	/**
	 * <p>
	 * Return the results of visibility query for the <tt>frustum</tt>. The
	 * returned result is computed once per frustum during this Controller's
	 * process phase, so the returned Bag should not be modified. If
	 * <tt>frustum</tt> is not the Frustum of a ViewNode in the system, then
	 * null is returned.
	 * </p>
	 * <p>
	 * The visibility query is performed using
	 * {@link SceneController#query(Frustum, Bag)}. If the system has no
	 * SceneController the queries cannot be performed, so this will return
	 * null.
	 * </p>
	 * 
	 * @param frustum The Frustum whose visibility set is returned
	 * @return The Bag of Entities that are visible within frustum.
	 * @throws NullPointerException if frustum is null
	 */
	public Bag<Entity> getVisibleEntities(Frustum frustum) {
		if (frustum == null)
			throw new NullPointerException("Frustum cannot be null");
		return visibilitySets.get(frustum);
	}

	/**
	 * Overridden to perform the following operations:
	 * <ol>
	 * <li>Update all ViewNodes to reflect their attached SceneElements, if
	 * possible.</li>
	 * <li>Update the ViewNodes' projections to match dimension changes, if
	 * necessary.</li>
	 * <li>Compute the visibility sets for each ViewNode in the system.</li>
	 * </ol>
	 * 
	 * @throws IllegalStateException if the controller is no longer part of its
	 *             system
	 */
	@Override
	public void process() {
		validate();
		
		Iterator<Entity> it = system.iterator(VN_ID);
		
		Map<Frustum, Bag<Entity>> pvs = new HashMap<Frustum, Bag<Entity>>();
		SceneController scene = system.getController(SceneController.class);
		
		while(it.hasNext()) {
			process(it.next(), scene, pvs);
		}
		
		// discard any unused bags and store computed results for this frame
		visibilitySets = pvs;
	}
	
	private void process(Entity e, SceneController scene, Map<Frustum, Bag<Entity>> pvs) {
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
		if (scene != null) {
			Bag<Entity> query = visibilitySets.get(f);
			if (query == null)
				query = new Bag<Entity>();
			
			scene.query(f, query);
			pvs.put(f, query);
			
			// modify all scene elements to be potentially visible
			int ct = query.size();
			for (int i = 0; i < ct; i++) {
				se = (SceneElement) query.get(i).get(SE_ID);
				if (se != null)
					se.setPotentiallyVisible(true);
			}
		} // else nothing we can do
	}
}
