package com.ferox.input;

/**
 * <p>
 * MouseListener is the event listener interface for handling mouse input
 * events. Before a listener can receive events, it must be added to a
 * {@link MouseEventSource}. Its event handler, {@link #handleEvent(MouseEvent)}
 * , is invoked every time the user presses or releases a mouse button, scrolls
 * a mouse wheel, or moves the mouse.
 * <p>
 * The MouseEventSource may only be able to produce events while the mouse is
 * inside the bounds of its window. Some OS's support reporting mouse movement
 * events if the mouse is being "dragged" outside the window as long as the drag
 * began within the source.
 * 
 * @author Michael Ludwig
 */
public interface MouseListener extends EventListener {
    /**
     * <p>
     * Process the specified MouseEvent. This will be invoked as soon as
     * possible after the real-world event occurs but there is obviously some
     * delay. MouseListeners should strive to quickly return to allow other
     * listeners to process the event.
     * <p>
     * Because mouse movement generates a new event for each slight movement,
     * many events can be processed in succession.
     * <p>
     * This method will be invoked on an internal event-queue thread managed by
     * the MouseEventSource this listener was registered to. Because of this,
     * MouseListener implementations must be thread-safe.
     * 
     * @param event The mouse event that just occurred
     */
    public void handleEvent(MouseEvent event);
}
