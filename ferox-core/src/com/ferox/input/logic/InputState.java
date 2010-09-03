package com.ferox.input.logic;

import com.ferox.input.KeyEvent;
import com.ferox.input.MouseEvent;

public class InputState {
    private final KeyboardState keyboard;
    private final MouseState mouse;
    
    private final long timestamp;
    
    public InputState() {
        keyboard = new KeyboardState();
        mouse = new MouseState();
        
        timestamp = System.currentTimeMillis();
    }
    
    public InputState(InputState prev, KeyEvent event) {
        if (prev != null) {
            keyboard = new KeyboardState(prev.keyboard, event);
            mouse = prev.mouse;
        } else {
            keyboard = new KeyboardState(null, event);
            mouse = new MouseState();
        }
        
        timestamp = System.currentTimeMillis();
    }
    
    public InputState(InputState prev, MouseEvent event) {
        if (prev != null) {
            keyboard = prev.keyboard;
            mouse = new MouseState(prev.mouse, event);
        } else {
            keyboard = new KeyboardState();
            mouse = new MouseState(null, event);
        }
        
        timestamp = System.currentTimeMillis();
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public MouseState getMouseState() {
        return mouse;
    }
    
    public KeyboardState getKeyboardState() {
        return keyboard;
    }
}
