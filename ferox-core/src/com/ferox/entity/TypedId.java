package com.ferox.entity;

/**
 * <p>
 * TypedId is a dynamically assigned identifier unique to the desired class
 * types. Every instance of a type T will use the same TypedId. TypedId is a
 * glorified integer id assigned to a class type. TypedId's are managed by third
 * parties that maintain the uniqueness of the integer ids. These managers often
 * narrow the possible types, such as managing TypedIds only for
 * {@link Component} or {@link MetaValue} types.
 * </p>
 * <p>
 * TypedId's enable typed, random-access lookups.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The identified type
 */
public final class TypedId<T> {
    private final Class<T> type;
    
    private final int id;

    /**
     * Create a new Id for the given type, with the given numeric id. Subclasses
     * are responsible for guaranteeing the uniqueness of <tt>id</tt>.
     * 
     * @param type The type that is identified
     * @param id The numeric id
     */
    protected TypedId(Class<T> type, int id) {
        // Sanity checks, shouldn't happen if constructed by getTypedId()
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        if (id < 0)
            throw new IllegalArgumentException("Id must be at least 0, not: " + id);

        this.type = type;
        this.id = id;
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
