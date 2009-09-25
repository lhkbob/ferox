package com.ferox.scene;

import com.ferox.math.Frustum;
import com.ferox.math.Frustum.FrustumIntersection;
import com.ferox.math.bounds.BoundVolume;
import com.ferox.util.Bag;

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
public class UnboundedCell extends AbstractCell {
	private final Bag<SceneElement> elements;
	
	/**
	 * Create a new UnboundedCell with a priority of 0.
	 */
	public UnboundedCell() {
		elements = new Bag<SceneElement>();
	}

	@Override
	public boolean add(SceneElement element) {
		if (element == null)
			return false;
		// element already within this cell
		if (element.getCell() == this)
			return true;
		
		// must place a new element
		if (element.getCell() != null)
			element.getCell().remove(element);
		elements.add(element);
		element.setCell(this);
		
		// we always add the element
		return true;
	}
	
	@Override
	public void remove(SceneElement element) {
		if (element == null || element.getCell() != this)
			return;
		
		boolean rem = elements.remove(element);
		// this shouldn't happen, but just in case
		if (!rem)
			throw new RuntimeException("Inconsistent state in UnboundedCell");
		element.setCell(null);
	}

	@Override
	public void clear() {
		int ct = elements.size();
		for (int i = 0; i < ct; i++)
			elements.get(i).setCell(null);
		
		elements.clear(false);
	}

	@Override
	public void query(Frustum query, Class<? extends SceneElement> index, Bag<SceneElement> result) {
		int ct = elements.size();
		
		SceneElement e;
		BoundVolume v;
		for (int i = 0; i < ct; i++) {
			e = elements.get(i);
			if (index == null || index.isInstance(e)) {
				v = e.getWorldBounds();
				if (v == null || v.testFrustum(query, null) != FrustumIntersection.OUTSIDE)
					result.add(e);
			}
		}
	}

	@Override
	public void query(BoundVolume query, Class<? extends SceneElement> index, Bag<SceneElement> result) {
		int ct = elements.size();
		
		SceneElement e;
		BoundVolume v;
		for (int i = 0; i < ct; i++) {
			e = elements.get(i);
			if (index == null || index.isInstance(e)) {
				v = e.getWorldBounds();
				if (v == null || v.intersects(query))
					result.add(e);
			}
		}
	}
}
