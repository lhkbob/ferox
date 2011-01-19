package com.ferox.entity;

import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <p>
 * MetaValue represents the abstract super-type for any bean-like object that
 * can be attached as meta values to {@link Component Components} on an
 * {@link Entity}. MetaValues are intended to be purely data, and should not
 * contain functions that act on them in way. This is the purpose of
 * {@link Controller Controllers}, which process the Components and MetaValues
 * of an Entity.
 * </p>
 * <p>
 * MetaValue's can be attached to Components to store meta-data needed by
 * Controllers, or to tweak the behavior of a Component. Different Entities with
 * the same Component instances can have different MetaValues attached to the
 * Components. The MetaValues for a Component are removed when the Component is
 * detached from an Entity.
 * </p>
 * <p>
 * The one exception to this rule is the {@link AbstractMetaValue} which
 * provides "syntactic sugar" not useful at the MetaValue level. It is
 * recommended that most MetaValue types extend from AbstractMetaValue.
 * </p>
 * <p>
 * Every MetaValue subclass is assigned a {@link TypedId} at runtime
 * dynamically. An id is not assigned until it is needed for that MetaValue
 * class type. This TypedId is shared across all instances of that type and is
 * unique, thus it serves as a typed identifier suitable for using in
 * array-based caching schemes. The numeric id assigned to a MetaValue type
 * should not be used for cross-JVM or cross-lifetime guarantees because the
 * order in which MetaValues are instantiated can impact the exact id used.
 * </p>
 * <p>
 * The entity package is designed to be thread-safe and support parallelized
 * access and processing of components. MetaValue implementations should strive
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
public abstract class MetaValue {
    private static final Map<Class<? extends MetaValue>, TypedId<? extends MetaValue>> typeMap = new HashMap<Class<? extends MetaValue>, TypedId<? extends MetaValue>>();
    private static int idSeq = 0;
    
    private final TypedId<? extends MetaValue> id;
    private final AtomicInteger version;

    /**
     * Create a new Component.
     */
    public MetaValue() {
        Class<? extends MetaValue> type = getClass();
        id = getTypedId(type);
        version = new AtomicInteger(0);
    }

    /**
     * <p>
     * Return the unique TypedId associated with this MetaValues's class type.
     * All MetaValuess of the same class will return this id, too.
     * </p>
     * <p>
     * It is recommended that implementations override this method to use the
     * proper return type. MetaValue does not perform this cast to avoid
     * parameterizing MetaValue. Do not change the actual returned instance,
     * though.
     * </p>
     * 
     * @return The TypedId of this MetaValue
     */
    public TypedId<? extends MetaValue> getTypedId() {
        return id;
    }

    /**
     * <p>
     * Each MetaValue has a version, which changes every time a MetaValue's
     * properties are modified. Each subclass of MetaValue may have its own
     * definition of what constitutes "modification" or a "property", but this
     * provides a uniform way of determining whether or not something has
     * changed.
     * </p>
     * <p>
     * MetaValue types should return the new version from a setter if possible.
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
     * automatically by MetaValue subtypes but it is exposed in situations where
     * a MetaValue is modified in such a way that it can't auto-update its
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
     * {@link MetaValue#MetaValue()} implicitly calls this method when a
     * MetaValue is created.
     * 
     * @param <T> The MetaValue class type
     * @param type The Class whose TypedId is fetched, which must be a subclass
     *            of MetaValue
     * @return A unique TypedId associated with the given type
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if type is not actually a subclass of
     *             MetaValue, or if it is abstract
     */
    @SuppressWarnings("unchecked")
    public static <T extends MetaValue> TypedId<T> getTypedId(Class<T> type) {
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        if (!MetaValue.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Type must be a subclass of MetaValue: " + type);
        if (Modifier.isAbstract(type.getModifiers()))
            throw new IllegalArgumentException("MetaValue class type cannot be abstract: " + type);

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
