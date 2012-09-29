package com.ferox.input.logic;

import com.ferox.input.MouseEvent.MouseButton;

/**
 * MouseClickedPredicate is an advanced Predicate capable of identifying and
 * triggering actions on single clicks up to N-count clicks within a specific
 * time period.
 * 
 * @author Michael Ludwig
 */
class MouseClickedPredicate implements Predicate {
    private final MouseButton button;
    private final long clickDuration;
    private final int numClicks;

    private long startTime;
    private int currentClickCount;

    public MouseClickedPredicate(MouseButton button, int numClicks, long clickDuration) {
        if (button == null) {
            throw new NullPointerException("MouseButton cannot be null");
        }
        if (button == MouseButton.NONE) {
            throw new IllegalArgumentException("NONE is not a valid button for mouse clicks");
        }
        if (numClicks <= 0) {
            throw new IllegalArgumentException("Number of clicks must be at least 1, not: " + numClicks);
        }
        if (clickDuration <= 0) {
            throw new IllegalArgumentException("Click duration must be at least 1 ms, not: " + clickDuration);
        }

        this.button = button;
        this.numClicks = numClicks;
        this.clickDuration = clickDuration * 1000000; // convert from millis to nanos

        startTime = -1;
        currentClickCount = 0;
    }

    @Override
    public boolean apply(InputState prev, InputState next) {
        if (!prev.getMouseState().isButtonDown(button) && next.getMouseState().isButtonDown(button)) {
            // record time of first press
            if (currentClickCount == 0) {
                startTime = next.getTimestamp();
            }

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
                if (start < 0) {
                    return false;
                }

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
