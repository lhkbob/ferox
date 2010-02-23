package com.ferox.scene.controller;

import com.ferox.math.bounds.SpatialHierarchy;
import com.ferox.util.entity.AbstractComponent;
import com.ferox.util.entity.Description;
import com.ferox.util.entity.Entity;

@Description("Book-keeping Component that stores spatial hierarchy information in a scene")
public class SpatialHierarchyComponent extends AbstractComponent<SpatialHierarchyComponent> {
	private SpatialHierarchy<Entity> hierarchy;
	private int priority;
	
	public SpatialHierarchyComponent() {
		super(SpatialHierarchyComponent.class);
	}
	
	public SpatialHierarchy<Entity> getHierarchy() {
		return hierarchy;
	}
	
	public int getPriority() {
		return priority;
	}
}
