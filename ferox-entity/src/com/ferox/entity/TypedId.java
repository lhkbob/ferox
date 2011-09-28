package com.ferox.entity;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;

/**
 * <p>
 * TypedId is a dynamically assigned identifier unique to the desired class
 * types. Every instance of a type T will use the same TypedId. TypedId is a
 * glorified integer id assigned to set of class types that share a common
 * parent class. Each class will be assigned a unique id within the currently
 * executing JVM.
 * </p>
 * <p>
 * The parent class of a set of TypedIds is responsible for exposing a method to
 * create TypedIds and ensuring the uniqueness of the integer ids assigned. In
 * general this should be a static method, with
 * {@link Component#getTypedId(Class)} as an example.
 * </p>
 * <p>
 * TypedId's enable typed, random-access lookups.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The identified type
 */
public class TypedId<T> {
    private final Class<T> type;
    private final Constructor<T> ctor;
    private final List<Field> properties;
    
    private final int id;

    /**
     * Create a new Id for the given type, with the given numeric id. Subclasses
     * are responsible for guaranteeing the uniqueness of <tt>id</tt>. This
     * constructor is protected so that TypedIds cannot be created directly,
     * instead another class or system should manage their id's and uniqueness.
     * 
     * @param type The type that is identified
     * @param ctor The constructor to be used to create instances of T
     * @param properties The identified property fields of the type T
     * @param id The unique numeric id
     */
    protected TypedId(Class<T> type, Constructor<T> ctor, List<Field> properties, int id) {
        // Sanity checks, shouldn't happen if constructed by Component.getTypedId()
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        if (ctor == null)
            throw new NullPointerException("Constructor cannot be null");
        if (id < 0)
            throw new IllegalArgumentException("Id must be at least 0, not: " + id);

        this.type = type;
        this.id = id;
        this.ctor = ctor;
        this.properties = Collections.unmodifiableList(properties);
    }

    /**
     * Return the type that this TypedId corresponds to. All instances of the
     * returned type will have the same TypedId.
     * 
     * @return The type that corresponds to this id
     */
    public Class<T> getType() {
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
    
    /**
     * @return The constructor provided at construction time
     */
    protected Constructor<T> getConstructor() {
        return ctor;
    }
    
    /**
     * @return The number of identified Properties in this type
     */
    protected int getFieldCount() {
        return properties.size();
    }
    
    /**
     * @return An unmodifiable list of fields in the type
     */
    protected List<Field> getFields() {
        return properties;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof TypedId))
            return false;
        TypedId<?> cid = (TypedId<?>) o;
        return cid.id == id && cid.type.equals(type);
    }

    @Override
    public int hashCode() {
        return id;
    }
    
    @Override
    public String toString() {
        return "TypedId (" + type.getSimpleName() + ", id=" + id + ")";
    }
}
