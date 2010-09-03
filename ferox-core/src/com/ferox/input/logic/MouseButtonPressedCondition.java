package com.ferox.input.logic;

import com.ferox.input.MouseEvent.MouseButton;

public class MouseButtonPressedCondition implements Condition {
    private final MouseButton button;
    
    public MouseButtonPressedCondition(MouseButton button) {
        this.button = button;
    }

    @Override
    public boolean apply(InputState prev, InputState next) {
        return !prev.getMouseState().isButtonDown(button) && next.getMouseState().isButtonDown(button);
    }
}
