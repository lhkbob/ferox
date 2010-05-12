package com.ferox.entity;

/**
 * <p>
 * EntityListener is an event listener interface for listening to changes to the
 * Component and EntitySystem state of particular Entities. It has methods that
 * will be invoked corresponding to when a Component is added or removed from
 * listened-on Entities, or when an Entity is added or removed from an
 * EntitySystem. The methods of an EntityListener will only be invoked by an
 * Entity if the listener is added to the Entity by using
 * {@link Entity#addListener(EntityListener)}. A listener can then be removed
 * using {@link Entity#removeListener(EntityListener)}.
 * </p>
 * <p>
 * Because the entity package is designed to be multi-threadable,
 * implementations of EntityListener are responsible for their own internal
 * synchronization. Additionally, it is assumed by the package implementations
 * that the listeners will not throw exceptions. Any thrown exceptions prevent
 * the event from occurring. This should not put the system itself in an
 * inconsistent state, but it could confuse other listeners that were notified
 * of an addition or removal that never occurred. Because of this, exceptions
 * should not be thrown. The entity package does not catch these exceptions
 * because it's believed the failure of the listener should be as visible as
 * possible.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface EntityListener {
    /**
     * <p>
     * Invoked when the given Entity will have the Component, <tt>c</tt> added
     * to it. This is invoked by the Entity on thread that called
     * {@link Entity#add(Component)} before any previously attached Component or
     * meta-Components are removed. This allows for comparison between previous
     * and future states. However, because c has yet to truly be added to the
     * Entity, meta-Components cannot be attached to c at the time the listener
     * is invoked.
     * </p>
     * <p>
     * Any exception thrown by the EntityListener will prevent the attachment of
     * the Component to the Entity, and prevent the execution of listeners which
     * have not been executed yet. This is a situation that should be avoided
     * and signals a failure in the implementation of the EntityListener.
     * </p>
     * 
     * @param e The Entity which is having the given Component added
     * @param c The Component that's being added to e
     */
	public void onComponentAdd(Entity e, Component c);

    /**
     * <p>
     * Invoked when the given Entity will have the Component, <tt>c</tt> removed
     * from it. This is only invoked if c was already attached to the Entity.
     * This will be invoked by the Entity on the thread that called
     * {@link Entity#remove(Component)} or {@link Entity#remove(ComponentId)}
     * before the Component's meta-Components have been removed. This is also
     * invoked when a Component is removed by the Entity's Component iterator or
     * by the Component-type specific iterator of an EntitySystem.
     * </p>
     * <p>
     * Any exception thrown by the EntityListener will prevent the removal of
     * the given Component, and prevent the execution of listeners which have
     * not executed yet. This is a situation that should be avoided and signals
     * a failure in the implementation of the EntityListener.
     * </p>
     * 
     * @param e The Entity which is having the given Component removed
     * @param c The Component that's being removed from e
     */
	public void onComponentRemove(Entity e, Component c);

    /**
     * <p>
     * Invoked when the given Entity will be added to the given EntitySystem.
     * This is called on the thread which invoked
     * {@link EntitySystem#add(Entity)} before the Entity has been added to the
     * system's internal data structures, so the Entity will not report that it
     * is owned by <tt>system</tt>.
     * </p>
     * <p>
     * Any exception thrown by the EntityListener will prevent the Entity from
     * being added to the system, and stop the execution of listeners which have
     * not executed yet. This is a situation that should be avoided and signals
     * a failure in the implementation of the EntityListener.
     * </p>
     * 
     * @param e The Entity that is being added to system
     * @param system The EntitySystem which subsequently own e
     */
	public void onSystemAdd(Entity e, EntitySystem system);

    /**
     * <p>
     * Invoked when the given Entity is to be removed from the given
     * EntitySystem. This is called on the thread which invoked
     * {@link EntitySystem#remove(Entity)} or when the system's Iterator had
     * remove() called. The listeners are notified before the Entity has
     * actually been removed so <tt>e</tt> will continue to report that it is
     * owned by the given system.
     * </p>
     * <p>
     * Any exception thrown by the EntityListener will prevent the Entity from
     * being removed from the system, and stop the execution of listeners which
     * have not executed yet. This is a situation that should be avoided and
     * signals a failure in the implementation of the EntityListener.
     * </p>
     * 
     * @param e The Entity that is being removed from system
     * @param system The EntitySystem that is having e removed
     */
	public void onSystemRemove(Entity e, EntitySystem system);
}
