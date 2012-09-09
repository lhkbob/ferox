package com.ferox.input.logic;


public interface Action {
    public void perform(InputState prev, InputState next);
}
