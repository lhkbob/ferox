package com.ferox.entity;

import java.lang.reflect.AccessibleObject;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * <p>
 * Component represents a set of self-consistent state that are added to an
 * {@link Entity}. Components are intended to be data storage objects, so their
 * definition should not contain methods for processing or updating (that is the
 * responsibility of a {@link Controller}).
 * </p>
 * <p>
 * The behavior or purpose of a Component should be well defined, including its
 * behavior with respect to other Components attached to the same Entity. It may
 * be that to function correctly or--more likely--usefully, related Components
 * will have to be used as well. An example of this might be a transform
 * component and a shape component for rendering.
 * </p>
 * <p>
 * Each Component class gets a {@link TypedId}, which can be looked up with
 * {@link #getTypedId(Class)}, passing in the desired class type. Because the
 * entity-component design pattern does not follow common object-oriented
 * principles, certain rules are followed when handling Component types in a
 * class hierarchy:
 * <ol>
 * <li>Any abstract type extending Component cannot get a TypedId</li>
 * <li>All concrete classes extending Component get separate TypedIds, even if
 * they extend from the same intermediate classes beneath Component.</li>
 * <li>All intermediate classes in a Component type's hierarchy must be abstract
 * or runtime exceptions will be thrown.</li>
 * </ol>
 * As an example, an abstract component could be Light, with concrete subclasses
 * SpotLight and DirectionLight. SpotLight and DirectionLight would be separate
 * component types as determined by TypedId. Light would not have any TypedId
 * and only serves to consolidate property definition among related component
 * types.
 * </p>
 * <p>
 * Implementations of Components must follow certain rules with respect to their
 * declared fields. For performance reasons, an EntitySystem packs all
 * components of the same type into the same region of memory using the
 * {@link Property} and {@link IndexedDataStore} API. To ensure that Components
 * behave correctly, a type can only declare private, final Property fields.
 * They can declare any methods they wish to expose the data these properties
 * represent. It is strongly recommended to not expose the Property objects
 * themselves. See {@link #getTypedId(Class)} for the complete contract.
 * </p>
 * <p>
 * Component instances are tied to an index into the IndexedDataStores used by
 * their properties. The index can be fetched by calling {@link #getIndex()}. An
 * instance of Component may have its index changed, effectively changing it to
 * a different "instance". This is most common when using the fast iterators.
 * Because of this, reference equality may not work, instead you should rely on
 * {@link #equals(Object)}.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class Component {
    // Use a ConcurrentHashMap to perform reads. It is still synchronized completely to do
    // an insert to make sure a type doesn't try to use two different id values.
    private static final ConcurrentHashMap<Class<? extends Component>, TypedId<?>> typeMap = new ConcurrentHashMap<Class<? extends Component>, TypedId<?>>();
    private static int idSeq = 0;
    
    /**
     * <var>index</var> is a sliding component index into the indexed data store
     * for each property of the component. It can be mutated by the EntitySystem
     * to effectively change the Component instance's values to another
     * component in the system.
     */
    int index;

    private final ComponentIndex<?> owner;
    private final TypedId<? extends Component> typedId;

    /**
     * <p>
     * Create a new Component instance that has its property data managed by the
     * given EntitySystem. Multiple Component instances may represent the same
     * "component" if their index's are the same.
     * </p>
     * <p>
     * Subclasses must call this constructor with the arguments as passed-in and
     * must not change them. Abstract subclasses can add additional arguments,
     * but concrete subclasses must have the same constructor signatures except
     * that it is private.
     * </p>
     * 
     * @param system The owning EntitySystem of the Component
     * @param index The initial index of this Component
     * @throws NullPointerException if system is null
     * @throws IllegalArgumentException if index is less than 0
     */
    @SuppressWarnings({ "rawtypes", "unchecked" })
    protected Component(EntitySystem system, int index) {
        if (system == null)
            throw new NullPointerException("EntitySystem cannot be null");
        if (index < 0)
            throw new IllegalArgumentException("Index must be at least 0: " + index);
        TypedId raw = getTypedId(getClass());

        this.owner = system.getIndex(raw);
        this.index = index;
        typedId = raw;
    }
    
    /**
     * <p>
     * Return the unique TypedId associated with this Component's class type.
     * All Components of the same class will return this id, too.
     * </p>
     * <p>
     * It is recommended that implementations override this method to use the
     * proper return type. Component does not perform this cast to avoid a
     * parameterizing Component. Do not change the actual returned instance,
     * though.
     * </p>
     * 
     * @return The TypedId of this Component
     */
    public TypedId<? extends Component> getTypedId() {
        return typedId;
    }

    /**
     * Get the Entity that owns this Component. The Entity will still be part of
     * an EntitySystem, and the component can be iterated over via
     * {@link EntitySystem#iterator(TypedId)}. If a Component is removed from an
     * Entity (or the Entity is removed from the system), this will return null.
     * 
     * @return The owning Entity
     * @throws IndexOutOfBoundsException if the Component has been removed from
     *             an Entity, or if its owning Entity has been removed from its
     *             EntitySystem.
     */
    public final Entity getEntity() {
        int entityIndex = owner.getEntityIndex(index);
        return owner.getEntitySystem().getEntityByIndex(entityIndex);
    }

    /**
     * Return the index of this Component within the IndexedDataStores that back
     * the defined properties of a Component. A Component instance may have its
     * index change if it is being used to slide over the component data (e.g.
     * in a fast iterator).
     * 
     * @return The index of the component used to access its IndexedDataStores.
     */
    public final int getIndex() {
        return index;
    }
    
    @Override
    public int hashCode() {
        return index;
    }
    
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Component))
            return false;
        Component c = (Component) o;
        if (c.owner == owner && c.typedId == typedId) {
            // We can't use index because a canonical component might have it change,
            // instead use its owning entity
            int tei = owner.getEntitySystem().getEntityId(owner.getEntityIndex(index));
            int cei = c.owner.getEntitySystem().getEntityId(c.owner.getEntityIndex(c.index));
            return tei == cei;
        } else {
            // type and owner don't match
            return false;
        }
    }

    /**
     * <p>
     * Return the unique TypedId instance for the given <tt>type</tt>. If a
     * TypedId hasn't yet been created a new one is instantiated with the next
     * numeric id in the internal id sequence. The new TypedId is stored for
     * later, so that subsequent calls to {@link #getTypedId(Class)} with
     * <tt>type</tt> will return the same instance.
     * {@link Component#Component()} implicitly calls this method when a
     * Component is created.
     * </p>
     * <p>
     * This method also performs runtime checks to ensure the validity of the
     * Component type definition. The following rules must be upheld or an
     * {@link IllegalComponentDefinitionException} is thrown.
     * <ul>
     * <li>If the class extends from a class other than Component, that class
     * must be a subclass of Component and be declared abstract. Additional
     * rules might affect these parent classes.</li>
     * <li>A concrete Component type must have only one constructor; it must be
     * private and with arguments: EntitySystem, int. Abstract Component types
     * do not have this restriction.</li>
     * <li>Any non-static fields defined in a Component (abstract or concrete)
     * must implement Property and be declared private or protected, and final.</li>
     * </ul>
     * Additionally, abstract Component types cannot have a TypedId assigned to
     * them.
     * </p>
     * 
     * @param <T> The Component class type
     * @param type The Class whose TypedId is fetched, which must be a subclass
     *            of Component
     * @return A unique TypedId associated with the given type
     * @throws NullPointerException if type is null
     * @throws IllegalArgumentException if type is not actually a subclass of
     *             Component, or if it is abstract
     * @throws IllegalComponentDefinitionException if the type does not follow
     *             the definition rules described above
     * @throws SecurityException if the reflection needed to create and analyze
     *             the Component fails
     */
    @SuppressWarnings("unchecked")
    public static <T extends Component> TypedId<T> getTypedId(Class<T> type) {
        if (type == null)
            throw new NullPointerException("Type cannot be null");
        
        // Do a look up first without locking to avoid the synchronized lock and expensive
        // error checking.  If we found one, we know it passed validation the first time, otherwise
        // we'll validate it before creating a new TypedId.
        TypedId<T> id = (TypedId<T>) typeMap.get(type);
        if (id != null)
            return id; // Found an existing id
        
        // Now we actually have to build up a new TypedId - which is sort of slow
        if (!Component.class.isAssignableFrom(type))
            throw new IllegalArgumentException("Type must be a subclass of Component: " + type);
        
        // Make sure we don't create TypedIds for abstract Component types 
        // (we don't want to try to allocate these)
        if (Modifier.isAbstract(type.getModifiers()))
            throw new IllegalArgumentException("Component class type cannot be abstract: " + type);
        
        // Accumulate all properties of type and its parents while validating the types declared fields,
        // the validity of its parent classes.
        List<Field> properties = new ArrayList<Field>();
        properties.addAll(getProperties(type)); // getProperties() validates the fields
        
        Class<? super T> parent = type.getSuperclass();
        while(!Component.class.equals(parent)) {
            if (!Modifier.isAbstract(parent.getModifiers()))
                throw new IllegalComponentDefinitionException(type, "Parent class " + parent + " is not abstract");
            
            // This cast is safe since we know type extends Component, and that we haven't
            // reached Component yet, ergo this parent class must still extend Component.
            properties.addAll(getProperties((Class<? extends Component>) parent));
            parent = parent.getSuperclass();
        }
        
        // Find and validate the constructor of the type
        Constructor<T> ctor = getConstructor(type);

        synchronized(typeMap) {
            // Must create a new id, we lock completely to prevent concurrent getTypedId() on the
            // same type using two different ids.  One would get overridden and its returned TypedId
            // would be invalid.
            // - Double check, though, before creating a new id
            id = (TypedId<T>) typeMap.get(type);
            if (id != null)
                return id; // Someone else put the type after we checked but before we locked
            
            id = new TypedId<T>(type, ctor, properties, idSeq++);
            typeMap.put(type, id);
            return id;
        }
    }
    
    private static List<Field> getProperties(Class<? extends Component> type) {
        List<Field> properties = new ArrayList<Field>();
        Field[] fields = type.getDeclaredFields();
        
        for (int i = 0; i < fields.length; i++) { 
            int mod = fields[i].getModifiers();
            if (Modifier.isStatic(mod))
                continue; // ignore static fields
            
            if (Property.class.isAssignableFrom(fields[i].getType())) {
                // Found a property field, we'll make it accessible later
                if (!Modifier.isPrivate(mod) && !Modifier.isProtected(mod))
                    throw new IllegalComponentDefinitionException(type, "The field, " + fields[i].getName() + " is not private or protected");
                if (!Modifier.isFinal(mod))
                    throw new IllegalComponentDefinitionException(type, "The field, " + fields[i].getName() + " is not final");
                properties.add(fields[i]);
            } else {
                throw new IllegalComponentDefinitionException(type, "The field, " + fields[i].getName() + ", does not implement Property");
            }
        }
        
        AccessibleObject[] securityCheck = new AccessibleObject[properties.size()];
        for (int i = 0; i < securityCheck.length; i++)
            securityCheck[i] = properties.get(i);
        
        Field.setAccessible(securityCheck, true);
        
        return properties;
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends Component> Constructor<T> getConstructor(Class<T> type) {
        // This assumes that type is the concrete type, so it will fail if there
        // are multiple constructors or it's not private with the correct arguments
        Constructor<?>[] ctors = type.getDeclaredConstructors();
        if (ctors.length != 1)
            throw new IllegalComponentDefinitionException(type, "Component type must only define a single constructor");
        
        Constructor<T> ctor = (Constructor<T>) ctors[0];
        if (!Modifier.isPrivate(ctor.getModifiers()))
            throw new IllegalComponentDefinitionException(type, "Component constructor must be private");
        
        Class<?>[] args = ctor.getParameterTypes();
        if (args.length != 2 || !EntitySystem.class.equals(args[0]) || !int.class.equals(args[1]))
            throw new IllegalComponentDefinitionException(type, "Component constructor does not have proper signature of (ComponentIndex<T>, int)");
        
        // Found it, now make it accessible (which might throw a SecurityException)
        ctor.setAccessible(true);
        return ctor;
    }
}
