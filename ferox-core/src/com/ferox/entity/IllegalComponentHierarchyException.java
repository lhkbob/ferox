package com.ferox.entity;

/**
 * IllegalComponetnHierarchyException is an exception thrown if a Component
 * implementation does not follow the class hierarchy rules defined in
 * {@link Component}.
 * 
 * @author Michael Ludwig
 */
public class IllegalComponentHierarchyException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public IllegalComponentHierarchyException(Class<? extends Component> type, Class<?> offendingParent) {
        super("Component type has an invalid class hierarchy: " + type + ", parent class is not abstract: " + offendingParent);
    }
}
