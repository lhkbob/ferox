package com.ferox.entity;

/**
 * <p>
 * TypedComponent extends Component and adds a parameterized type so that
 * {@link #getTypedId()} returns a correctly typed TypedId.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type that extends TypedComponent
 */
public abstract class TypedComponent<T extends TypedComponent<T>> extends Component {
    
    @Override
    @SuppressWarnings("unchecked")
    public TypedId<T> getTypedId() {
        return (TypedId<T>) super.getTypedId();
    }
}
