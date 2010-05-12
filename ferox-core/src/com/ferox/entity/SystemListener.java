package com.ferox.entity;

/**
 * <p>
 * SystemListener is an event listener interface for listening to changes to an
 * EntitySystem. It provides methods that are invoked when an Entity is added or
 * removed from a listened-on EntitySystem. A listener must be added to an
 * EntitySystem before it can be notified of these changes, by using
 * {@link EntitySystem#addListener(SystemListener)}. It can be later removed
 * uinsg {@link EntitySystem#removeListener(SystemListener)}.
 * </p>
 * <p>
 * Because the entity package is designed to be multi-threadable,
 * implementations of SystemListener are responsible for their own internal
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
public interface SystemListener {
    /**
     * <p>
     * Invoked by EntitySystem when any Entity is to be added to the system that
     * is being listened to by this SystemListener. This is called on the thread
     * which originally invoked {@link EntitySystem#add(Entity)}. In many ways
     * this corresponds to
     * {@link EntityListener#onSystemAdd(Entity, EntitySystem)}, but is from the
     * opposite direction where listeners are notified of every added Entity.
     * </p>
     * <p>
     * Any exception thrown by the SystemListener will prevent the Entity from
     * being added to the system, and stop the execution of listeners which have
     * not executed yet. This is a situation that should be avoided and signals
     * a failure in the implementation of the SystemListener.
     * </p>
     * 
     * @param system The system which is having toAdd added to it
     * @param toAdd The Entity that will become owned by system
     */
    public void onEntityAdd(EntitySystem system, Entity toAdd);

    /**
     * <p>
     * Invoked by the EntitySystem when an Entity will be removed from the
     * system. This can be invoked in response to
     * {@link EntitySystem#remove(Entity)} or if the Entity is removed via the
     * system's iterator. In many ways this corresponds to
     * {@link EntityListener#onSystemRemove(Entity, EntitySystem)}, but is from
     * the opposite direction where listeners are notified of every removed
     * Entity.
     * </p>
     * <p>
     * Any exception thrown by the SystemListener will prevent the Entity from
     * being removed from the system, and stop the execution of listeners which
     * have not executed yet. This is a situation that should be avoided and
     * signals a failure in the implementation of the SystemListener.
     * </p>
     * 
     * @param system The system which is having toRemove being removed
     * @param toRemove The Entity that will be removed from system
     */
    public void onEntityRemove(EntitySystem system, Entity toRemove);
}
