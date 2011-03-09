package com.ferox.entity;

/**
 * <p>
 * TypedComponent extends Component and adds a parameterized type so that
 * {@link #getTypedId()} returns a correctly typed TypedId. It also adds a
 * constructor that takes a single argument of type T so that subclasses can be
 * used correctly with Templates. Subclasses should still have additional
 * constructors for regular Component creation.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type that extends TypedComponent
 */
public abstract class TypedComponent<T extends TypedComponent<T>> extends Component {
    /**
     * This constructor does nothing, subclasses should expose a constructor
     * with the same argument as a clone constructor. However, they should also
     * provide constructors that are useful for creating the Component from
     * scratch. In these situations, they may call
     * <code>super(null, false);</code> without any side effects.
     * 
     * @param clone The Component to clone, or null if called from a non-clone
     *            constructor
     * @throws NullPointerException if clone is null and failIfNull is true
     */
    protected TypedComponent(T clone, boolean failIfNull) {
        // Convenience to check for a null instance
        if (failIfNull && clone == null)
            throw new NullPointerException("Component to clone cannot be null");
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public TypedId<T> getTypedId() {
        return (TypedId<T>) super.getTypedId();
    }
}
