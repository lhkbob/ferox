package com.ferox.entity;

/**
 * <p>
 * Component represents the abstract super-type for any bean-like object that
 * can be attached to an {@link Entity} within an {@link EntitySystem}.
 * Components are intended to be purely data, they should not contain functions
 * that act on them in way. This is the purpose of {@link Controller
 * Controllers}, which process the Components of an Entity.
 * </p>
 * <p>
 * The set of Components on a given Entity transform the Entity into a modular
 * object with much more flexibility than statically defining all the
 * combinations of component data. An entity system is not an object-oriented
 * paradigm, so it is strongly recommended that Component implementations be
 * declared as final to re-iterate this point. The entity system implemented
 * within this package assumes that Component implementations do not have
 * subclasses of their own.
 * </p>
 * <p>
 * The one exception to this rule is the {@link AbstractComponent} which
 * provides "syntactic sugar" not useful at the Component level. It is
 * recommended that most Component types extend from AbstractComponent.
 * </p>
 * <p>
 * Every Component subclass is assigned a {@link ComponentId} at runtime
 * dynamically. An id is not assigned until it is needed for that Component
 * class type. This ComponentId is shared across all instances of that type and
 * is unique, thus it serves as an typed identifier suitable for using in
 * array-based caching schemes. The numeric id assigned to a Component type
 * should not be used for cross-JVM or cross-lifetime guarantees because the
 * order in which Components are instantiated can impact the exact id used.
 * </p>
 * <p>
 * The entity package is designed to be thread-safe and support parallelized
 * access and processing of components. Component implementations should strive
 * to be as thread-safe as possible but because they are intended to only store
 * data, this should be relatively easy and may not even require synchronization
 * in cases where parallel changes do not overly damage the state of the system.
 * It is the intention that the organization of Controllers and the order that
 * they process the system should prevent these situations from occurring.
 * </p>
 * 
 * @see Entity
 * @see EntitySystem
 * @see Controller
 * @see ComponentId
 * @author Michael Ludwig
 */
public abstract class Component {
	private final ComponentId<?> id;

	/**
	 * Create a new Component that determines its ComponentId by storing the
	 * result of {@link #getComponentId(Class)}.
	 */
	public Component() {
		Class<? extends Component> type = getClass();
		id = getComponentId(type);
	}

	/**
	 * <p>
	 * Return the unique ComponentId associated with this Component's class
	 * type. If the concrete Component is of type T, then the returned id is
	 * actually of type ComponentId<T>. All Components of the same class will
	 * return this id, too.
	 * </p>
	 * <p>
	 * It is recommended that implementations override this method to use the
	 * proper return type. Component does not perform this cast to avoid a
	 * parametrizing Component.
	 * </p>
	 * 
	 * @return The ComponentId of this Component
	 */
	public ComponentId<?> getComponentId() {
		return id;
	}
	
	@Override
	public String toString() {
	    return getClass().getSimpleName();
	}

	/**
	 * Return the unique ComponentId instance for the given <tt>type</tt>. If a
	 * ComponentId hasn't yet been created a new one is instantiated with the
	 * next numeric id in the internal id sequence. The new ComponentId is
	 * stored for later, so that subsequent calls to
	 * {@link #getComponentId(Class)} with <tt>type</tt> will return the same
	 * instance. {@link Component#Component()} implicitly calls this method when
	 * a Component is created.
	 * 
	 * @param <T> The Component class type
	 * @param type The Class whose ComponentId is fetched, which must be a
	 *            subclass of Component
	 * @return A unique ComponentId associated with the given type
	 * @throws NullPointerException if type is null
	 * @throws IllegalArgumentException if type is not actual
	 */
	public static <T extends Component> ComponentId<T> getComponentId(Class<T> type) {
		return ComponentId.getComponentId(type);
	}
}
