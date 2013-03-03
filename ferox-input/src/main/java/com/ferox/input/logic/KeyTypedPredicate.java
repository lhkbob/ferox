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

import com.ferox.input.KeyEvent.KeyCode;

/**
 * KeyTypedPredicate is an advanced Predicate capable of identifying when a key is typed
 * (e.g. pressed and released quickly).
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
            throw new IllegalArgumentException(
                    "Type duration must be a positive number of milliseconds, not: " +
                    typeDuration);
        }

        this.code = code;
        this.typeDuration = typeDuration * 1000000; // convert from millis to nanoseconds
        startTime = -1;
    }

    @Override
    public boolean apply(InputState prev, InputState next) {
        if (!prev.getKeyboardState().isKeyDown(code) &&
            next.getKeyboardState().isKeyDown(code)) {
            // record time of first press
            startTime = next.getTimestamp();
            return false;
        } else if (prev.getKeyboardState().isKeyDown(code) &&
                   !next.getKeyboardState().isKeyDown(code)) {
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
