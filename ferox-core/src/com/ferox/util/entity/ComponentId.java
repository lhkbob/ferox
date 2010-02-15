package com.ferox.util.entity;

import java.util.HashMap;
import java.util.Map;

public class ComponentId<T extends Component> {
	private static final Map<Class<? extends Component>, ComponentId<?>> typeMap = new HashMap<Class<? extends Component>, ComponentId<?>>();
	private static int idSeq = 0;
	
	private final Class<T> type;
	private final int id;
	private final boolean indexable;
	private final String descr;
	
	private ComponentId(Class<T> type, int id) {
		if (type == null)
			throw new NullPointerException("Type cannot be null");
		if (id < 0)
			throw new IllegalArgumentException("Id must be at least 0, not: " + id);
		
		this.type = type;
		this.id = id;
		
		indexable = type.getAnnotation(NonIndexable.class) == null;
		Description d = type.getAnnotation(Description.class);
		descr = (d == null ? null : d.value());
	}
	
	public Class<T> getComponentType() {
		return type;
	}
	
	public boolean isIndexable() {
		return indexable;
	}
	
	public int getId() {
		return id;
	}
	
	public String getDescription() {
		return descr;
	}
	
	@SuppressWarnings("unchecked")
	static <M extends Component>ComponentId<M> getComponentId(Class<M> type) {
		if (type == null)
			throw new NullPointerException("Type cannot be null");
		if (!Component.class.isAssignableFrom(type))
			throw new IllegalArgumentException("Type must be a subclass of Component: " + type);
		
		synchronized(typeMap) {
			ComponentId<M> id = (ComponentId<M>) typeMap.get(type);
			if (id == null) {
				id = new ComponentId<M>(type, idSeq++);
				typeMap.put(type, id);
			}
			return id;
		}
	}
}
