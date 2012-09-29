package com.ferox.input.logic;

import com.ferox.input.KeyEvent.KeyCode;

/**
 * KeyTypedPredicate is an advanced Predicate capable of identifying when a key
 * is typed (e.g. pressed and released quickly).
 * 
 * @author Michael Ludwig
 */
class KeyTypedPredicate implements Predicate {
    private final KeyCode code;
    private final long typeDuration;

    private long startTime;

    public KeyTypedPredicate(KeyCode code, long typeDuration) {
        if (code == null) {
            throw new NullPointerException("KeyCode cannot be null");
        }
        if (typeDuration <= 0) {
            throw new IllegalArgumentException("Type duration must be a positive number of milliseconds, not: " + typeDuration);
        }

        this.code = code;
        this.typeDuration = typeDuration * 1000000; // convert from millis to nanoseconds
        startTime = -1;
    }

    @Override
    public boolean apply(InputState prev, InputState next) {
        if (!prev.getKeyboardState().isKeyDown(code) && next.getKeyboardState().isKeyDown(code)) {
            // record time of first press
            startTime = next.getTimestamp();
            return false;
        } else if (prev.getKeyboardState().isKeyDown(code) && !next.getKeyboardState().isKeyDown(code)) {
            // key is released, see if it was fast enough
            long start = startTime;
            startTime = -1;

            // check for faulty data (i.e. we missed the 1st press somehow)
            if (start < 0) {
                return false;
            }

            return (next.getTimestamp() - start) <= typeDuration;
        } else {
            // extraneous event so ignore it
            return false;
        }
    }
}
