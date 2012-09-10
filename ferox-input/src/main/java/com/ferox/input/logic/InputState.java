package com.ferox.input.logic;

import com.ferox.input.KeyEvent;
import com.ferox.input.MouseEvent;

/**
 * InputState represents a time-stamped snapshot of both the user's keyboard and
 * mouse.
 * 
 * @see KeyboardState
 * @see MouseState
 * @author Michael Ludwig
 */
public class InputState {
    private final KeyboardState keyboard;
    private final MouseState mouse;
    
    private final long timestamp; // nanos
    
    /**
     * Create a new InputState that has the empty or default keyboard and mouse
     * states. Its timestamp is set to the currently reporting system time.
     */
    public InputState() {
        keyboard = new KeyboardState();
        mouse = new MouseState();
        
        timestamp = System.currentTimeMillis();
    }
    
    /**
     * Create a new InputState that clones the mouse and keyboard states from
     * the given InputState, but has an updated timestamp to the current system
     * time.
     * 
     * @param prev The previous input state
     * @throws NullPointerException if prev is null
     */
    public InputState(InputState prev) {
        keyboard = prev.keyboard;
        mouse = prev.mouse;
        
        timestamp = System.nanoTime();
    }
    
    /**
     * Create a new InputState that computes the effective state of applying the
     * given key event to the previous keyboard state, and preserving the
     * previous mouse state.
     * 
     * @param prev The previous input state
     * @param event The key event to apply
     * @throws NullPointerException if event is null
     */
    public InputState(InputState prev, KeyEvent event) {
        if (prev != null) {
            keyboard = new KeyboardState(prev.keyboard, event);
            mouse = prev.mouse;
        } else {
            keyboard = new KeyboardState(null, event);
            mouse = new MouseState();
        }
        
        timestamp = System.nanoTime();
    }
    
    /**
     * Create a new InputState that computes the effective state of applying the
     * given mouse event to the previous mouse state, and preserving the
     * previous keyboard state.
     * 
     * @param prev The previous input state
     * @param event The mouse event to apply
     * @throws NullPointerException if event is null
     */
    public InputState(InputState prev, MouseEvent event) {
        if (prev != null) {
            keyboard = prev.keyboard;
            mouse = new MouseState(prev.mouse, event);
        } else {
            keyboard = new KeyboardState();
            mouse = new MouseState(null, event);
        }
        
        timestamp = System.nanoTime();
    }
    
    /**
     * @return The time stamp in nanoseconds of the event that produced this
     *         input state
     */
    public long getTimestamp() {
        return timestamp;
    }
    
    /**
     * @return The state of the mouse device at the time this state was current
     */
    public MouseState getMouseState() {
        return mouse;
    }
    
    /**
     * @return The state of the keyboard at the time this state was current
     */
    public KeyboardState getKeyboardState() {
        return keyboard;
    }
}
