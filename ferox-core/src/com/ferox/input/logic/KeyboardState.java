package com.ferox.input.logic;

import java.util.EnumSet;

import com.ferox.input.KeyEvent;
import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.KeyEvent.Type;

public class KeyboardState {
    private final EnumSet<KeyCode> keysDown;
    
    public KeyboardState() {
        keysDown = EnumSet.noneOf(KeyCode.class);
    }
    
    public KeyboardState(KeyboardState prev, KeyEvent event) {
        if (prev != null)
            keysDown = EnumSet.copyOf(prev.keysDown);
        else
            keysDown = EnumSet.noneOf(KeyCode.class);
        
        if (event.getEventType() == Type.PRESS)
            keysDown.add(event.getKeyCode());
        else
            keysDown.remove(event.getKeyCode());
    }
    
    public boolean isKeyDown(KeyCode code) {
        return keysDown.contains(code);
    }
}
