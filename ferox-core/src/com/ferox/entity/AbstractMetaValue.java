package com.ferox.entity;

/**
 * <p>
 * AbstractMetaValue extends MetaValue and adds a parameterized type so that
 * {@link #getTypedId()} returns a correctly typed TypedId. In addition
 * its constructor enforces the recommendation that concrete MetaAvalues be
 * final. It is strongly recommended that MetaValue implementations extend
 * AbstractComponent.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type that extends AbstractMetaValue
 */
public abstract class AbstractMetaValue<T extends AbstractMetaValue<T>> extends MetaValue {
    /**
     * <p>
     * AbstractMetaValues's constructor enforces the constraint that the
     * provided Class equal the Class returned by the instance's
     * {@link #getClass()} method. If that is not the case, then an exception is
     * thrown.
     * </p>
     * <p>
     * To properly implement a MetaValue this way, it should be:
     * 
     * <pre>
     * public class Tam extends AbstractMetaValue&lt;Tac&gt; {
     *     public Tam() {
     *         super(Tam.class);
     *         // ...
     *     }
     * }
     * </pre>
     * 
     * And now, even though Tam was not declared as final, Tam is protected from
     * being extended.
     * </p>
     * 
     * @param concreteType The Class type that all constructed instances must be
     *            of
     */
    protected AbstractMetaValue(Class<T> concreteType) {
        Class<?> realType = getClass();
        if (!realType.equals(concreteType))
            throw new IllegalStateException("Class hierarchy is invalid, class type must be " 
                                            + realType + " and not " + concreteType);
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public TypedId<T> getTypedId() {
        return (TypedId<T>) super.getTypedId();
    }
}
