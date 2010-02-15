package com.ferox.util.entity;


public abstract class Component {
	private final ComponentId<?> typeId;
	
	public Component() {
		Class<? extends Component> type = getClass();
		typeId = getComponentId(type);
	}
	
	public String getDescription() {
		return typeId.getDescription();
	}
	
	public ComponentId<?> getComponentId() {
		return typeId;
	}
	
	public boolean isIndexable() {
		return typeId.isIndexable();
	}
	
	public static <T extends Component> ComponentId<T> getComponentId(Class<T> type) {
		return ComponentId.getComponentId(type);
	}
}
