package com.ferox.input.logic;

import com.ferox.input.KeyEvent.KeyCode;

public class KeyPressedCondition implements Condition {
    private final KeyCode code;
    
    public KeyPressedCondition(KeyCode code) {
        this.code = code;
    }
    
    @Override
    public boolean apply(InputState prev, InputState next) {
        return !prev.getKeyboardState().isKeyDown(code) && next.getKeyboardState().isKeyDown(code);
    }
}
