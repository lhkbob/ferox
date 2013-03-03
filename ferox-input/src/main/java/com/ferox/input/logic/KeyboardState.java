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

import com.ferox.input.KeyEvent;
import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.KeyEvent.Type;

import java.util.EnumSet;

/**
 * KeyboardState represents an immutable snapshot of the user's keyboard. It can be used
 * to query if any key is held down or released.
 *
 * @author Michael Ludwig
 */
public class KeyboardState {
    private final EnumSet<KeyCode> keysDown;

    /**
     * Create a new KeyboardState that has zero keys marked as down.
     */
    public KeyboardState() {
        keysDown = EnumSet.noneOf(KeyCode.class);
    }

    /**
     * Create a new KeyboardState that will mark a key as down if the event is a PRESS, or
     * will mark it as up if the event is a RELEASE. Any other key marked as up or down
     * from the previous state will be kept unchanged.
     *
     * @param prev  The previous keyboard state before the event
     * @param event The key event to update to the new state
     *
     * @throws NullPointerException if event is null
     */
    public KeyboardState(KeyboardState prev, KeyEvent event) {
        if (prev != null) {
            keysDown = EnumSet.copyOf(prev.keysDown);
        } else {
            keysDown = EnumSet.noneOf(KeyCode.class);
        }

        if (event.getEventType() == Type.PRESS) {
            keysDown.add(event.getKeyCode());
        } else {
            keysDown.remove(event.getKeyCode());
        }
    }

    /**
     * Return whether or not the given key is down. The PRESS event that marked the key as
     * down could have happened any number of frames in the past, but it is guaranteed
     * that it has not been released as of this state.
     *
     * @param code The key code to query
     *
     * @return True if the key is held down
     */
    public boolean isKeyDown(KeyCode code) {
        return keysDown.contains(code);
    }
}
