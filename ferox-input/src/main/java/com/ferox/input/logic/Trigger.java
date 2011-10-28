package com.ferox.input.logic;


public interface Trigger {
    public void onTrigger(InputState prev, InputState next);
}
