package com.ferox.input.logic;

/**
 * <p>
 * Predicate is a boolean function that operates on two InputStates. The two
 * states represent the previous and current states formed when an input event
 * is formed. Button and key status can be compared between the two states to
 * identify if they are held, were just pressed, or were just released.
 * <p>
 * {@link Predicates} contains static methods to construct many of the common
 * predicates desired for input handling, such as a key or button being clicked,
 * pressed, or held down.
 * 
 * @author Michael Ludwig
 */
public interface Predicate {
    /**
     * Return true if the state transition represented by <tt>prev</tt> and
     * <tt>next</tt> match the internal rules of this predicate.
     * 
     * @param prev The previous state
     * @param next The next state
     * @return True if the predicate matches the transition
     */
    public boolean apply(InputState prev, InputState next);
}
