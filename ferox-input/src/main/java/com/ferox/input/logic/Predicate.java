package com.ferox.input.logic;


public interface Predicate {
    public boolean apply(InputState prev, InputState next);
}
