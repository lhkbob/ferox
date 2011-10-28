package com.ferox.input.logic;

import com.ferox.input.KeyEvent.KeyCode;

public class KeyTypedCondition implements Condition {
    private final KeyCode code;
    private final long typeDuration;
    
    private long startTime;
    
    public KeyTypedCondition(KeyCode code) {
        this(code, 250L);
    }
    
    public KeyTypedCondition(KeyCode code, long typeDuration) {
        this.code = code;
        this.typeDuration = typeDuration;
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
            if (start < 0)
                return false;
            
            return (next.getTimestamp() - start) <= typeDuration;
        } else {
            // extraneous event so ignore it
            return false;
        }
    }
}
