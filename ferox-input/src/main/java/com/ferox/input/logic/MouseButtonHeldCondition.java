package com.ferox.input.logic;

import com.ferox.input.MouseEvent.MouseButton;

public class MouseButtonHeldCondition implements Condition {
    private final MouseButton button;
    
    public MouseButtonHeldCondition(MouseButton button) {
        this.button = button;
    }

    @Override
    public boolean apply(InputState prev, InputState next) {
        return next.getMouseState().isButtonDown(button);
    }
}
