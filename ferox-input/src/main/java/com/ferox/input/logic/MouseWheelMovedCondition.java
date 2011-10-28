package com.ferox.input.logic;

public class MouseWheelMovedCondition implements Condition {
    @Override
    public boolean apply(InputState prev, InputState next) {
        return next.getMouseState().getScrollDelta() != 0;
    }
}
