package com.ferox.entity;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

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
 * Every Component subclass is assigned a {@link TypedId} at runtime
 * dynamically. An id is not assigned until it is needed for that Component
 * class type. This TypedId is shared across all instances of that type and
 * is unique, thus it serves as a typed identifier suitable for using in
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
 * @see TypedId
 * @author Michael Ludwig
 */
public abstract class Component {
    private static final Map<Class<? extends Component>, TypedId<? extends Component>> typeMap = new HashMap<Class<? extends Component>, TypedId<? extends Component>>();
    private static int idSeq = 0;
    
    private final TypedId<? extends Component> id;
    private final AtomicInteger version;

    /**
     * Create a new Component.
     */
    public Component() {
        Class<? extends Component> type = getClass();
        id = getTypedId(type);
        version = new AtomicInteger(0);
    }

    /**
     * <p>
     * Return the unique TypedId associated with this Component's class type.
     * All Components of the same class will return this id, too.
     * </p>
     * <p>
     * It is recommended that implementations override this method to use the
     * proper return type. Component does not perform this cast to avoid
     * parameterizing Component. Do not change the actual returned instance,
     * though.
     * </p>
     * 
     * @return The TypedId of this Component
     */
    public TypedId<? extends Component> getTypedId() {
        return id;
    }

    /**
     * <p>
     * Each Component has a version, which changes every time a Component's
     * properties are modified. Each subclass of Component may have its own
     * definition of what constitutes "modification" or a "property", but this
     * provides a uniform way of determining whether or not something has
     * changed.
     * </p>
     * <p>
     * Component types should return the new version from a setter if possible.
     * This can be stored by interested parties to compare at later points.
     * </p>
     * 
     * @return The current version number
     */
    public final int getVersion() {
        return version.get();
    }

    /**
     * Increment the current version number. Normally this should be called
     * automatically by Component subtypes but it is exposed in situations where
     * a Component is modified in such a way that it can't auto-update its
     * version.
     * 
     * @return The new version number
     */
    public int updateVersion() {
        return version.incrementAndGet();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

    /**
     * Return the unique TypedId instance for the given <tt>type</tt>. If a
     * TypedId hasn't yet been created a new one is instantiated with the next
     * numeric id in the internal id sequence. The new TypedId is stored for
     * later, so that subsequent calls to {@link #getTypedId(Class)} with
     * <tt>type</tt> will return the same instance.
     * {@link Component#Component()} implicitly calls this method when a
     * Component is created.
     * 
     * @param <T> The Component class type
     * @param type The Class whose ComponentId is fetched, which must be a
     *            subclass of Component
     * @return A unique TypedId associated with the given type
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if type is not actually a subclass of
     *             Component, or if it is abstract
     */
    @SuppressWarnings("unchecked")
    public static <T extends Component> TypedId<T> getTypedId(Class<T> type) {
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        if (!Component.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Type must be a subclass of Component: " + type);
        if (Modifier.isAbstract(type.getModifiers()))
            throw new IllegalArgumentException("Component class type cannot be abstract: " + type);

        synchronized (typeMap) {
            TypedId<T> id = (TypedId<T>) typeMap.get(type);
            if (id == null) {
                id = new TypedId<T>(type, idSeq++);
                typeMap.put(type, id);
            }
            return id;
        }
    }
}
