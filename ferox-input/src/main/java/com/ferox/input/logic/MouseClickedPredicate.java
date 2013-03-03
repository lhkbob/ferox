/*
 * Ferox, a graphics and game library in Java
 *
 * Copyright (c) 2012, Michael Ludwig
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 *
 *     Redistributions of source code must retain the above copyright notice,
 *         this list of conditions and the following disclaimer.
 *     Redistributions in binary form must reproduce the above copyright notice,
 *         this list of conditions and the following disclaimer in the
 *         documentation and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.ferox.input.logic;

import com.ferox.input.MouseEvent.MouseButton;

/**
 * MouseClickedPredicate is an advanced Predicate capable of identifying and triggering
 * actions on single clicks up to N-count clicks within a specific time period.
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
            throw new IllegalArgumentException(
                    "NONE is not a valid button for mouse clicks");
        }
        if (numClicks <= 0) {
            throw new IllegalArgumentException(
                    "Number of clicks must be at least 1, not: " + numClicks);
        }
        if (clickDuration <= 0) {
            throw new IllegalArgumentException(
                    "Click duration must be at least 1 ms, not: " + clickDuration);
        }

        this.button = button;
        this.numClicks = numClicks;
        this.clickDuration = clickDuration * 1000000; // convert from millis to nanos

        startTime = -1;
        currentClickCount = 0;
    }

    @Override
    public boolean apply(InputState prev, InputState next) {
        if (!prev.getMouseState().isButtonDown(button) &&
            next.getMouseState().isButtonDown(button)) {
            // record time of first press
            if (currentClickCount == 0) {
                startTime = next.getTimestamp();
            }

            // increase the number of 'clicks', which for our purposes is tracked on mouse down
            currentClickCount++;
            return false;
        } else if (prev.getMouseState().isButtonDown(button) &&
                   !next.getMouseState().isButtonDown(button)) {
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
