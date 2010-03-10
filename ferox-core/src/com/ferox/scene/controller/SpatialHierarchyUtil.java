package com.ferox.scene.controller;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.ferox.math.Frustum;
import com.ferox.util.Bag;
import com.ferox.util.entity.Component;
import com.ferox.util.entity.ComponentId;
import com.ferox.util.entity.Entity;
import com.ferox.util.entity.EntitySystem;

public class SpatialHierarchyUtil {
	private static final ComponentId<SpatialHierarchyComponent> SHC_ID = Component.getComponentId(SpatialHierarchyComponent.class);
	
	
	private final List<SpatialHierarchyComponent> cells;
	
	public SpatialHierarchyUtil(EntitySystem system) {
		cells = new ArrayList<SpatialHierarchyComponent>();
		
		Iterator<Entity> it = system.iterator(SHC_ID);
		while(it.hasNext()) {
			SpatialHierarchyComponent c = it.next().get(SHC_ID);
			if (c != null)
				cells.add(c);
		}
		
		Collections.sort(cells, prioritySorter);
	}
	
	public Bag<Entity> query(Frustum f, Bag<Entity> result) {
		if (result == null)
			result = new Bag<Entity>();
		result.clear(true);
		int sz = cells.size();
		for (int i = 0; i < sz; i++)
			cells.get(i).getHierarchy().query(f, result);
		return result;
	}
	
	private static final Comparator<SpatialHierarchyComponent> prioritySorter = new Comparator<SpatialHierarchyComponent>() {
		@Override
		public int compare(SpatialHierarchyComponent o1, SpatialHierarchyComponent o2) {
			return o2.getPriority() - o1.getPriority();
		}
	};
}
