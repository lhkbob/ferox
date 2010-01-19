package com.ferox.scene;

import com.ferox.math.Frustum;
import com.ferox.math.Frustum.FrustumIntersection;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.util.Bag;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.Entity;

/**
 * <p>
 * UnboundedCell is a simple Cell implementation that accepts any SceneElement
 * and performs all queries in O(n), where n is the number of elements assigned
 * to it.
 * </p>
 * <p>
 * UnboundedCell's purpose is to be a low-priority Cell that is a catch-all for
 * SceneElements that can't be assigned to better performing elements.  It is not
 * necessary to add an UnboundedCell for this purpose, as Scene already does this.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class UnboundedCell extends Cell {
	private static final Object SUCCESS = new Object();
	private static final int SE_ID = Component.getTypeId(SceneElement.class);
	
	private final Bag<Entity> elements;
	
	/**
	 * Create a new UnboundedCell with a priority of 0.
	 */
	public UnboundedCell() {
		elements = new Bag<Entity>();
	}

	@Override
	public Object add(Entity e, SceneElement element, Object cellData) {
		// element already within this cell
		if (cellData != null)
			return SUCCESS;
		
		// we always add the element
		elements.add(e);
		return SUCCESS;
	}
	
	@Override
	public void remove(Entity e, SceneElement element, Object cellData) {
		boolean rem = elements.remove(element);
		// this shouldn't happen, but just in case
		if (!rem)
			throw new RuntimeException("Inconsistent state in UnboundedCell");
	}

	@Override
	public void clear() {
		elements.clear();
	}

	@Override
	public void query(Frustum query, Bag<Entity> result) {
		int ct = elements.size();
		
		Entity e;
		SceneElement s;
		BoundVolume v;
		for (int i = 0; i < ct; i++) {
			e = elements.get(i);
			s = (SceneElement) e.get(SE_ID);
			if (s != null) {
				v = s.getWorldBounds();
				if (v == null || v.testFrustum(query, null) != FrustumIntersection.OUTSIDE)
					result.add(e);
			}
		}
	}

	@Override
	public void query(BoundVolume query, Bag<Entity> result) {
		int ct = elements.size();
		
		Entity e;
		SceneElement s;
		BoundVolume v;
		for (int i = 0; i < ct; i++) {
			e = elements.get(i);
			s = (SceneElement) e.get(SE_ID);
			if (s != null) {
				v = s.getWorldBounds();
				if (v == null || v.intersects(query))
					result.add(e);
			}
		}
	}
}
