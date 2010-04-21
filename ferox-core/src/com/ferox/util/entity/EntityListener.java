package com.ferox.util.entity;

public interface EntityListener {
	public void onComponentAdd(Entity e, Component c);
	
	// FIXME: for this and other methods, make sure it's called before
	// actual change to entity is made
	public void onComponentRemove(Entity e, Component c);
	
	public void onSystemAdd(Entity e, EntitySystem system);
	
	public void onSystemRemove(Entity e, EntitySystem system);
}
