package com.ferox.entity;

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Map;

/**
 * <p>
 * Template is a ComponentContainer, like {@link Entity}, except a Template's
 * main purpose is to consolidate the creation and set up of Entities. They
 * could also be used with a serialization service to easily configure game
 * entities.
 * </p>
 * <p>
 * In any case, Templates are built by composing Components just like an Entity
 * is. When an Entity is created from a Template (using {@link #createEntity()}
 * ), the Components in a Template are cloned and added to the new Entity. If
 * needed, a {@link ComponentProvider} can be used in place of a Component to
 * create components as needed for the new entities created by a template.
 * </p>
 * <p>
 * A Component and a ComponentProvider cannot be used for the same component
 * type at the same time. Adding a Component of type T will replace any
 * ComponentProvider for type T and vice versa.
 * </p>
 * 
 * @author Michael Ludwig
 */
public final class Template extends ComponentContainer {
    private final Map<TypedId<? extends Component>, ComponentProvider<?>> providers;
    
    /**
     * Create an empty Template without any Components or ComponentProviders.
     */
    public Template() {
        this((Component[]) null);
    }

    /**
     * Create a Template with the given Components. There will will be no
     * ComponentProviders attached, these must be added later with
     * {@link #addProvider(TypedId, ComponentProvider)}.
     * 
     * @param components Components to add
     * @throws NullPointerException if any of the components are null
     * @throws IllegalArgumentException if any of the component are already
     *             added to another container
     */
    public Template(Component... components) {
        providers = new HashMap<TypedId<? extends Component>, ComponentProvider<?>>();
        
        if (components != null && components.length > 0) {
            for (Component c: components)
                add(c);
        }
    }

    /**
     * Get the ComponentProvider for the given component type. If this returns a
     * non-null provider, then Components will be created by the returned
     * provider when {@link #createEntity()} is invoked. If null is returned,
     * then there is an actual Component attached to the template with the given
     * id, or the template does not use components of the given type at all.
     * 
     * @param <T> The Component type that the provider will create
     * @param id The TypedId of the given Component type
     * @return The ComponentProvider for the given type, or null if no provider
     *         is used
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentProvider<T> getProvider(TypedId<T> id) {
        if (id == null)
            throw new NullPointerException("Id cannot be null");
        
        // Just lock, the Templates can afford to be a little lazier since
        // they're not used in as performance critical places as Entities
        synchronized(lock) {
            return (ComponentProvider<T>) providers.get(id);
        }
    }

    /**
     * <p>
     * Add a ComponentProvider to this Template for the given type of component.
     * When {@link #createEntity()} is called, the ComponentProvider will be
     * used to create new instances of the component type, instead of cloning an
     * added component.
     * </p>
     * <p>
     * For a single component type, only one provider or component may be in the
     * template at a given point in time. If there was a provider or component
     * that had a matching type, <tt>provider</tt> will overwrite them in the
     * template. This functions just like when adding a component overwrites an
     * old one of the same type; ComponentProviders are essentially place
     * holders within the template for the components they create.
     * </p>
     * 
     * @param <T> The Component type created by the provider
     * @param id The TypedId for the component type
     * @param provider The ComponentProvider to use
     * @throws NullPointerException if id or provider are null
     */
    public <T extends Component> void addProvider(TypedId<T> id, ComponentProvider<T> provider) {
        if (id == null)
            throw new NullPointerException("Id cannot be null");
        if (provider == null)
            throw new NullPointerException("ComponentProvider cannot be null");

        synchronized(lock) {
            // Remove any Component that happened to be configured for the given typed id.
            remove(id);
            
            // Store the provider in the map, possibly overwriting any old provider 
            // for the given typed id.
            providers.put(id, provider);
        }
    }

    /**
     * <p>
     * Remove the current ComponentProvider for the given type of Component from
     * this Template. If there was no configured ComponentProvider, null is
     * returned, otherwise the removed provider is returned. It is important to
     * note that this will not remove a Component of the given type if an actual
     * Component has been added to the Template, instead of a provider.
     * </p>
     * <p>
     * Additionally, {@link #remove(TypedId)} will not remove
     * ComponentProviders. That method will only remove Components. Use
     * {@link #removeAll(TypedId)} to completly remove any Component or
     * ComponentProvider from the Template for a given type.
     * </p>
     * 
     * @param <T> The Component type of the provider that is being removed
     * @param id The TypedId specifying the type
     * @return The ComponentProvider that was removed, or null if there was no
     *         provider for the given type
     * @throws NullPointerException if id is null
     */
    @SuppressWarnings("unchecked")
    public <T extends Component> ComponentProvider<T> removeProvider(TypedId<T> id) {
        if (id == null)
            throw new NullPointerException("Id cannot be null");
        
        synchronized(lock) {
            // Simply remove and return the old provider
            return (ComponentProvider<T>) providers.remove(id);
        }
    }

    /**
     * Remove either a ComponentProvider or Component from this Template for the
     * given type of component. This is equivalent to calling both
     * {@link #removeProvider(TypedId)} and {@link #remove(TypedId)}, except
     * that it is an atomic operation.
     * 
     * @param <T> The type of Component that is being removed
     * @param id The TypedId for the component being removed
     * @return True if a component or provider was removed, or false if neither
     *         a component nor provider were in the template when this was
     *         invoked
     * @throws NullPointerException if id is null
     */
    public <T extends Component> boolean removeAll(TypedId<T> id) {
        synchronized(lock) {
            // Must have an outer lock over both of these removes so
            // that they are in they same atomic unit
            ComponentProvider<T> oldProvider = removeProvider(id);
            T oldComponent = remove(id);
            
            return oldProvider != null || oldComponent != null;
        }
    }
    
    @Override
    public boolean add(Component c) {
        // Must have an outer lock over super.add() so that the removal from
        // providers happens within the same atomic unit
        synchronized(lock) {
            if (super.add(c)) {
                // Remove any provider that had previously been set for the component's type
                providers.remove(c.getTypedId());
                return true;
            } else
                return false; // Don't remove provider if add failed
        }
    }

    /**
     * <p>
     * Create an Entity based off of the current state of this Template. The
     * created Entity will have a Component for each Component or
     * ComponentProvider that is in the Template. If the template has a
     * Component, a new Component is created of the same type. If the template
     * has a ComponentProvider for a given type, the ComponentProvider's
     * {@link ComponentProvider#get() get} method is invoked to create a new
     * instance.
     * </p>
     * <p>
     * When creating a Component of type T, the Template looks for a constructor
     * defined in T that takes a single instance of T as an argument (i.e. a
     * copy constructor). It is not possible to use {@link Object#clone()} with
     * Components so this convention must be followed or
     * {@link TemplateException} will be thrown.
     * </p>
     * 
     * @return A new Entity, with new Components matching the current state of
     *         the template
     * @throws TemplateException if a ComponentProvider threw an exception, or
     *             if a Component did not expose a proper copy constructor to
     *             facilitate cloning
     */
    public Entity createEntity() {
        synchronized(lock) {
            // Operate within the lock so the Template doesn't change on us
            Entity fromTemplate = new Entity();
            
            for (Component c: this) {
                // First add clones of all components in the template
                Component clone = clone(c);
                if (clone != null)
                    fromTemplate.add(clone);
            }
            
            for (ComponentProvider<?> provider: providers.values()) {
                // Now add newly created components from the configured providers
                try {
                    Component c = provider.get();
                    if (c != null)
                        fromTemplate.add(c);
                } catch(Exception e) {
                    throw new TemplateException("ComponentProvider threw an exception", e);
                }
            }
            
            // Entity creation is completed
            return fromTemplate;
        }
    }
    
    private Component clone(Component c) {
        Class<? extends Component> type = c.getClass();
        Constructor<? extends Component> constructor;
        try {
            // We clone a Component by following the convention that each Component
            // class defines a copy constructor for its specific type.
            constructor = type.getConstructor(type);
            return constructor.newInstance(c);
        } catch(Exception e) {
            throw new TemplateException("Unable to clone Component of type " + c.getTypedId(), e);
        }
    }
}
