/**
 * 
 */
package com.ferox.scene;

import java.util.HashMap;
import java.util.Map;

import com.ferox.util.Bag;
import com.ferox.util.entity.Entity;

public class VisibilityResults {
	private Map<ViewNode, Bag<Entity>> entities = new HashMap<ViewNode, Bag<Entity>>();
	
	public Bag<Entity> getVisibleEntities(ViewNode view) {
		if (view == null)
			throw new NullPointerException("ViewNode cannot be null");
		return entities.get(view);
	}
}