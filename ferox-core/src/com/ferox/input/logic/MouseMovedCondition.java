package com.ferox.input.logic;

public class MouseMovedCondition implements Condition {
    @Override
    public boolean apply(InputState prev, InputState next) {
        return next.getMouseState().getDeltaX() != 0 || next.getMouseState().getDeltaY() != 0;
    }
}
