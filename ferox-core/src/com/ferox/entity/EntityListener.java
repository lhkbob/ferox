package com.ferox.entity;

/**
 * <p>
 * EntityListener is a listener interface that can be used to listen for changes
 * to an EntitySystem. It supports listening to adds and removes of Entities to
 * an EntitySystem, and the adds and removes of Components on Entities already
 * in an EntitySystem. Entities that are not owned by any EntitySystem do not
 * generate events until they are added to an EntitySystem.
 * </p>
 * <p>
 * EntityListeners are invoked such that the event timeline for a single entity
 * is consistent across threads. Thus if two threads add a Component to the same
 * Entity, the listeners invoked for the first add event will complete before
 * the listeners are run for the second event (where the order of the adds is
 * determined by the scheduling of threads). This is more critical when an add
 * and remove occurs. As an example, if an Entity is removed from one system on
 * a thread and added to another system on a second thread, it is guaranteed
 * that the listeners of the remove will have completed before the listeners of
 * the add will run.
 * </p>
 * <p>
 * There is no ordering guarantee for events that affect different Entities. If
 * two threads generate entity or component events at the same time, but for
 * different entities (even on the same system), their listeners may be run in
 * different order compared to when the adds completed. Given that
 * EntityListeners should generally only be processing a single Entity at a
 * time, this is not a problem.
 * </p>
 * <p>
 * EntityListeners must be thread safe if they modify internal storage shared by
 * multiple entities.
 * </p>
 * 
 * @author Michael Ludwig
 */
public interface EntityListener {
    /**
     * Invoked when an Entity is added to an EntitySystem that this listener is
     * listening to. The EntityEvent will contain the added Entity and the
     * EntitySystem it was added to.
     * 
     * @param e The EntityEvent containing all necessary information
     */
    public void onEntityAdd(EntityEvent e);

    /**
     * Invoked when an Entity is removed from an EntitySystem that this listener
     * is listening to. The EntityEvent will contain the removed Entity and the
     * EntitySystem it was removed from.
     * 
     * @param e The EntityEvent containing all necessary information
     */
    public void onEntityRemove(EntityEvent e);

    /**
     * Invoked when a Component is added to an Entity that's owned by an
     * EntitySystem that this listener is listening to. The ComponentEvent will
     * contain the added Component, potentially the old replaced Component, and
     * both the Entity and EntitySystem involved.
     * 
     * @param c The ComponentEvent containing all necessary information
     */
    public void onComponentAdd(ComponentEvent c);

    /**
     * Invoked when a Component is removed from an Entity that's owned by
     * EntitySystem that this listener is listening to. The ComponentEvent will
     * contain the removed Component, the Entity it was removed from and the
     * EntitySystem that owns the Entity.
     * 
     * @param c The ComponentEvent containing all necessary information
     */
    public void onComponentRemove(ComponentEvent c);
}
