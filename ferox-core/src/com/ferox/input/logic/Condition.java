package com.ferox.input.logic;


public interface Condition {
    public boolean apply(InputState prev, InputState next);
}
