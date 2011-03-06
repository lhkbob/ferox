package com.ferox.entity;

/**
 * <p>
 * ComponentProviders provide a more flexible mechanism of creating new
 * Components when a Template needs to build new Entities. A simple example
 * would be a ComponentProvider that created texture-based components from file
 * names.
 * </p>
 * <p>
 * ComponentProvider implementations do not necessarily need to be thread safe,
 * depending on how they are used. It is highly recommended to use a new
 * provider per Template so that a non-thread safe provider will not break. Of
 * course, if multiple providers share resources, it may still need to be thread
 * safe since there are no guarantees when multiple providers are in different
 * templates.
 * </p>
 * 
 * @author Michael Ludwig
 * @param <T> The type of created Component
 */
public interface ComponentProvider<T extends Component> {
    /**
     * Return a new instance of type T based on the implementation of the
     * provider. It must be a new instance that is not owned by any
     * ComponentContainer because it will be added to a new Entity by a
     * Template.
     * 
     * @return A new instance of T, configured as needed
     */
    public T get();
}
