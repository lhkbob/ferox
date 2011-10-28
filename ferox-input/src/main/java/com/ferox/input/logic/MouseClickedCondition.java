package com.ferox.input.logic;

import com.ferox.input.MouseEvent.MouseButton;

public class MouseClickedCondition implements Condition {
    private final MouseButton button;
    private final long clickDuration;
    private final int numClicks;
    
    private long startTime;
    private int currentClickCount;
    
    public MouseClickedCondition(MouseButton button) {
        this(button, 1);
    }
    
    public MouseClickedCondition(MouseButton button, int numClicks) {
        this(button, 1, 150L);
    }
    
    public MouseClickedCondition(MouseButton button, int numClicks, long clickDuration) {
        this.button = button;
        this.numClicks = numClicks;
        this.clickDuration = clickDuration;
        
        startTime = -1;
        currentClickCount = 0;
    }
    
    @Override
    public boolean apply(InputState prev, InputState next) {
        if (!prev.getMouseState().isButtonDown(button) && next.getMouseState().isButtonDown(button)) {
            // record time of first press
            if (currentClickCount == 0)
                startTime = next.getTimestamp();
            
            // increase the number of 'clicks', which for our purposes is tracked on mouse down
            currentClickCount++;
            return false;
        } else if (prev.getMouseState().isButtonDown(button) && !next.getMouseState().isButtonDown(button)) {
            // button was released, see if we reached our click goal and were fast enough
            if (currentClickCount == numClicks) {
                long start = startTime;
                startTime = -1;
                currentClickCount = 0;
                
                // check for faulty data (i.e. we missed the 1st press somehow)
                if (start < 0)
                    return false;
                
                return (next.getTimestamp() - start) <= clickDuration;
            } else {
                // haven't reach click count yet, but if we've taken too long
                // we should reset now
                if (next.getTimestamp() - startTime > clickDuration) {
                    startTime = -1;
                    currentClickCount = 0;
                }
                
                return false;
            }
        } else {
            // extraneous event so ignore it
            return false;
        }
    }
}
