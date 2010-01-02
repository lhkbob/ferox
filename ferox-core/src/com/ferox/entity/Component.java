package com.ferox.entity;

import java.util.HashMap;
import java.util.Map;

public abstract class Component {
	private static final Map<Class<? extends Component>, Integer> typeMap = new HashMap<Class<? extends Component>, Integer>();
	private static int idSeq = 0;
	
	private final int typeId;
	private final String description;
	
	public Component(String description) {
		synchronized(typeMap) {
			Integer id = typeMap.get(getClass());
			if (id == null) {
				id = Integer.valueOf(idSeq++);
				typeMap.put(getClass(), id);
			}
			
			typeId = id.intValue();
		}
		this.description = description;
	}
	
	public final String getDescription() {
		return description;
	}
	
	public final int getTypeId() {
		return typeId;
	}
	
	public String toString() {
		if (description == null)
			return getClass().getSimpleName() + "(type id=" + typeId + ")";
		else
			return getClass().getSimpleName() + "(type id=" + typeId + ") - " + description;
	}
	
	public static int getTypeId(Class<? extends Component> type) {
		if (type == null)
			throw new NullPointerException("Type cannot be null");
		
		synchronized(typeMap) {
			Integer id = typeMap.get(type);
			return (id == null ? -1 : id.intValue());
		}
	}
}
