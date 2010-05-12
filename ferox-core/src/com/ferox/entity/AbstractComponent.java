package com.ferox.entity;

/**
 * <p>
 * AbstractComponent extends Component and adds a parametrized type so that
 * {@link #getComponentId()} returns a correctly typed ComponentId. In addition
 * its constructor enforces the recommendation that concrete Components be
 * final. It is strongly recommended that Component implementations extend
 * AbstractComponent.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The concrete type that extends AbstractComponent
 */
public abstract class AbstractComponent<T extends AbstractComponent<T>> extends Component {
	/**
	 * <p>
	 * AbstractComponent's constructor enforces the constraint that the provided
	 * Class equal the Class returned by the instance's {@link #getClass()}
	 * method. If that is not the case, then an exception is thrown.
	 * </p>
	 * <p>
	 * To properly implement a Component this way, it would like:
	 * 
	 * <pre>
	 * public class Tac extends AbstractComponent&lt;Tac&gt; {
	 * 	public Tac() {
	 * 		super(Tac.class);
	 * 		// ...
	 * 	}
	 * }
	 * </pre>
	 * 
	 * And now, even though Tac was not declared as final, Tac is protected from
	 * being extended.
	 * </p>
	 * 
	 * @param concreteType The Class type that all constructed instances must be
	 *            of
	 */
	protected AbstractComponent(Class<T> concreteType) {
		Class<?> realType = getClass();
		if (!realType.equals(concreteType))
			throw new IllegalStateException("Class hierarchy is invalid, class type must be " 
											+ realType + " and not " + concreteType);
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public ComponentId<T> getComponentId() {
		return (ComponentId<T>) super.getComponentId();
	}
}
