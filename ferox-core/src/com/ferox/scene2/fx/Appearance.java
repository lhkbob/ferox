package com.ferox.scene2.fx;

import java.util.IdentityHashMap;

public class Appearance {
	private IdentityHashMap<Class<? extends Component>, Component> components;
	
	public Appearance(Component... components) {
		this.components = new IdentityHashMap<Class<? extends Component>, Component>();
		
		if (components != null) {
			for (int i = 0; i < components.length; i++) 
				addComponent(components[i]);
		}
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T getComponent(Class<T> type) {
		return (T) components.get(type);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T removeComponent(Class<T> type) {
		return (T) components.remove(type);
	}
	
	public <T extends Component> boolean removeComponent(T component) {
		if (component == null)
			throw new NullPointerException("Cannot remove a null Component");
		
		Component t = components.get(component.getType());
		if (t == component) {
			components.remove(component.getType());
			return true;
		} else
			return false;
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T addComponent(T component) {
		if (component == null)
			throw new NullPointerException("Cannot add a null Component");
		return (T) components.put(component.getType(), component);
	}
}
