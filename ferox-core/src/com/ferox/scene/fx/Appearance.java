package com.ferox.scene.fx;

import java.util.IdentityHashMap;

import com.ferox.util.FastMap;

public class Appearance {
	private final IdentityHashMap<Class<? extends Component>, Component> components;
	private final FastMap<SceneCompositor, Object> scData;
	
	public Appearance(Component... components) {
		scData = new FastMap<SceneCompositor, Object>(SceneCompositor.class);
		this.components = new IdentityHashMap<Class<? extends Component>, Component>();
		
		if (components != null) {
			for (int i = 0; i < components.length; i++) 
				add(components[i]);
		}
	}
	
	public void setData(SceneCompositor compositor, Object data) {
		scData.put(compositor, data);
	}
	
	public Object getData(SceneCompositor compositor) {
		return scData.get(compositor);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T get(Class<T> type) {
		return (T) components.get(type);
	}
	
	@SuppressWarnings("unchecked")
	public <T extends Component> T remove(Class<T> type) {
		return (T) components.remove(type);
	}
	
	public <T extends Component> boolean remove(T component) {
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
	public <T extends Component> T add(T component) {
		if (component == null)
			throw new NullPointerException("Cannot add a null Component");
		return (T) components.put(component.getType(), component);
	}
}
