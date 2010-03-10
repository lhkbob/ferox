package com.ferox.util.entity;

import java.util.HashMap;
import java.util.Map;

/**
 * ComponentId is a dynamically assigned identifier unique to a concrete
 * subclass of {@link Component}. Every instance of a Component type T will
 * contain the same ComponentId. ComponentId is a glorified integer id assigned
 * to each Component type. It enables random-access when looking up Component
 * instances in an {@link Entity} or iterating over Entities of a given type,
 * instead of requiring a hash-map lookup if Classes were to be used. It also
 * allows for correctly typed Component returns from an Entity.
 * 
 * @see Component
 * @see Entity
 * @author Michael Ludwig
 * @param <T> Component type a ComponentId corresponds to
 */
public final class ComponentId<T extends Component> {
	private static final Map<Class<? extends Component>, ComponentId<?>> typeMap = new HashMap<Class<? extends Component>, ComponentId<?>>();
	private static int idSeq = 0;

	private final Class<T> type;
	private final int id;

	private ComponentId(Class<T> type, int id) {
		// sanity checks, shouldn't happen if constructed by getComponentId()
		if (type == null)
			throw new NullPointerException("Type cannot be null");
		if (id < 0)
			throw new IllegalArgumentException("Id must be at least 0, not: " + id);

		this.type = type;
		this.id = id;
	}

	/**
	 * Return the {@link Component} type that this ComponentId corresponds to.
	 * All instances of the returned type will return this ComponentId from
	 * their {@link Component#getComponentId()}.
	 * 
	 * @return The Component type that corresponds to this id
	 */
	public Class<T> getComponentType() {
		return type;
	}

	/**
	 * Return the numeric id corresponding to this ComponentId. This id is
	 * unique such that a ComponentId corresponding to a different
	 * {@link Component} implementation will not have the same id.
	 * 
	 * @return The numeric id, which will be at least 0
	 */
	public int getId() {
		return id;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof ComponentId))
			return false;
		ComponentId<?> cid = (ComponentId<?>) o;
		return cid.id == id && cid.type.equals(type);
	}

	@Override
	public int hashCode() {
		return type.hashCode();
	}

	/**
	 * Return the unique ComponentId instance for the given <tt>type</tt>. If a
	 * ComponentId hasn't yet been created a new one is instantiated with the
	 * next numeric id in the internal id sequence. The new ComponentId is
	 * stored for later, so that subsequent calls to
	 * {@link #getComponentId(Class)} with <tt>type</tt> will return the same
	 * instance.
	 * 
	 * @param <M> The Component class type
	 * @param type The Class whose ComponentId is fetched, which must be a
	 *            subclass of Component
	 * @return A unique ComponentId associated with the given type
	 * @throws NullPointerException if type is null
	 * @throws IllegalArgumentException if type is not actual
	 */
	@SuppressWarnings("unchecked")
	static <M extends Component> ComponentId<M> getComponentId(Class<M> type) {
		if (type == null)
			throw new NullPointerException("Type cannot be null");
		if (!Component.class.isAssignableFrom(type))
			throw new IllegalArgumentException("Type must be a subclass of Component: " + type);

		synchronized (typeMap) {
			ComponentId<M> id = (ComponentId<M>) typeMap.get(type);
			if (id == null) {
				id = new ComponentId<M>(type, idSeq++);
				typeMap.put(type, id);
			}
			return id;
		}
	}
}
