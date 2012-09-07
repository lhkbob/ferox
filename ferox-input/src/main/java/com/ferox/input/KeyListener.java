package com.ferox.input;

/**
 * <p>
 * KeyListener is the event listener interface for handling keyboard input
 * events. Before a listener can receive events, it must be added to a
 * {@link KeyEventSource}. Its event handler, {@link #handleEvent(KeyEvent)}, is
 * invoked every time the user presses a key on the keyboard while the
 * listener's associated source has focus.
 * <p>
 * Depending on the OS, holding a key down might generate repeated key events of
 * the same key code without any release event until the key is finally
 * released.
 * 
 * @author Michael Ludwig
 */
public interface KeyListener extends EventListener {
    /**
     * <p>
     * Process the specified KeyEvent. This will be invoked as soon as possible
     * after the real-world event occurs but there is obviously some delay.
     * KeyListeners should strive to quickly return to allow other listeners to
     * process the event.
     * <p>
     * This method will be invoked on an internal event-queue thread managed by
     * the KeyEventSource this listener was registered to. Because of this,
     * KeyListener implementations must be thread-safe.
     * 
     * @param event The key event that just occurred
     */
    public void handleEvent(KeyEvent event);
}
