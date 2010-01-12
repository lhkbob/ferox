package com.ferox.util.entity;

import java.util.HashMap;
import java.util.Map;

public abstract class Component {
	private static final Map<Class<? extends Component>, Integer> typeMap = new HashMap<Class<? extends Component>, Integer>();
	private static int idSeq = 0;
	
	private final int typeId;
	private final String description;
	
	public Component(String description) {
		typeId = getTypeId(getClass());
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
		
		// even though an EntitySystem is not multi-threaded, this static
		// method must be since multiple systems could use it at once
		synchronized(typeMap) {
			Integer id = typeMap.get(type);
			if (id == null) {
				id = Integer.valueOf(idSeq++);
				typeMap.put(type, id);
			}
			return id.intValue();
		}
	}
}
