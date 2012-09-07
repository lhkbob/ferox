package com.ferox.input;

/**
 * <p>
 * Event is a generic interface to signify a type as an "event" of some kind.
 * Events are produced by {@link EventSource sources}. Event instances must be
 * immutable objects. The produced events are then passed to
 * {@link EventListener listeners} registered with the sources.
 * 
 * @see MouseEvent
 * @see KeyEvent
 * @author Michael Ludwig
 */
public interface Event {
    /**
     * @return The EventSource that produced this event
     */
    public EventSource getSource();
}
