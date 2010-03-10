package com.ferox.scene.controller;

import com.ferox.util.Bag;
import com.ferox.util.entity.AbstractComponent;
import com.ferox.util.entity.Description;
import com.ferox.util.entity.Entity;

@Description("Book-keeping Component that stores which Entities are visible from its attached Entity")
public class VisibleEntities extends AbstractComponent<VisibleEntities> {
	private final Bag<Entity> entities;
	
	public VisibleEntities() {
		super(VisibleEntities.class);
		entities = new Bag<Entity>();
	}
	
	public Bag<Entity> getEntities() {
		return entities;
	}
	
	public void resetVisibility() {
		entities.clear(true);
	}
	
	public void markVisible(Entity e) {
		entities.add(e);
	}
}
