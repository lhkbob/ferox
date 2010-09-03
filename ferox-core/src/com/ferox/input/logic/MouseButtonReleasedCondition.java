package com.ferox.input.logic;

import com.ferox.input.MouseEvent.MouseButton;

public class MouseButtonReleasedCondition implements Condition {
    private final MouseButton button;
    
    public MouseButtonReleasedCondition(MouseButton button) {
        this.button = button;
    }
    
    @Override
    public boolean apply(InputState prev, InputState next) {
        return prev.getMouseState().isButtonDown(button) && !next.getMouseState().isButtonDown(button);
    }

}
