package com.ferox.util.entity;

public class AbstractComponent<T extends AbstractComponent<T>> extends Component {
	protected AbstractComponent(Class<T> concreteType) {
		Class<?> realType = getClass();
		if (realType.equals(concreteType))
			throw new IllegalStateException("Class hierarchy is invalid, class type must be " 
											+ realType.getSimpleName() + " and not " + concreteType.getSimpleName());
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public ComponentId<T> getComponentId() {
		return (ComponentId<T>) super.getComponentId();
	}
}
