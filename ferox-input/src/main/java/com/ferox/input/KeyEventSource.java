package com.ferox.input;

/**
 * <p>
 * KeyEventSource is an event source for mouse events. Most often it is a window
 * of some kind that can obtain focus from the OS and receive events either
 * through AWT, JInput, or LWJGL's native input library.
 * <p>
 * The source is then responsible for converting those low-level events into
 * {@link KeyEvent KeyEvents} and dispatching them to registered
 * {@link KeyListener KeyListeners}. There is no guarantee about the order in
 * which registered listeners are invoked when an event occurs.
 * 
 * @author Michael Ludwig
 */
public interface KeyEventSource extends EventSource {
    /**
     * Register the given KeyListener with this KeyEventSource. Nothing is
     * done if the given listener has already been added.
     * 
     * @param listener The listener to add
     * @throws NullPointerException if listener is null
     */
    public void addKeyListener(KeyListener listener);
    
    /**
     * Remove the given KeyListener from this KeyEventSource. Nothing is
     * done if the given listener has never been added, or was already removed.
     * 
     * @param listener The listener to remove
     * @throws NullPointerException if listener is null
     */
    public void removeKeyListener(KeyListener listener);
}
