package com.ferox.input.logic;

import com.ferox.input.KeyEvent.KeyCode;

public class KeyHeldCondition implements Condition {
    private final KeyCode code;
    
    public KeyHeldCondition(KeyCode code) {
        this.code = code;
    }
    
    @Override
    public boolean apply(InputState prev, InputState next) {
        return next.getKeyboardState().isKeyDown(code);
    }
}
