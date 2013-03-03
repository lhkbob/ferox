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
import com.ferox.input.MouseEvent;

/**
 * InputState represents a time-stamped snapshot of both the user's keyboard and mouse.
 *
 * @author Michael Ludwig
 * @see KeyboardState
 * @see MouseState
 */
public class InputState {
    private final KeyboardState keyboard;
    private final MouseState mouse;

    private final long timestamp; // nanos

    /**
     * Create a new InputState that has the empty or default keyboard and mouse states.
     * Its timestamp is set to the currently reporting system time.
     */
    public InputState() {
        keyboard = new KeyboardState();
        mouse = new MouseState();

        timestamp = System.currentTimeMillis();
    }

    /**
     * Create a new InputState that clones the mouse and keyboard states from the given
     * InputState, but has an updated timestamp to the current system time.
     *
     * @param prev The previous input state
     *
     * @throws NullPointerException if prev is null
     */
    public InputState(InputState prev) {
        keyboard = prev.keyboard;
        mouse = prev.mouse;

        timestamp = System.nanoTime();
    }

    /**
     * Create a new InputState that computes the effective state of applying the given key
     * event to the previous keyboard state, and preserving the previous mouse state.
     *
     * @param prev  The previous input state
     * @param event The key event to apply
     *
     * @throws NullPointerException if event is null
     */
    public InputState(InputState prev, KeyEvent event) {
        if (prev != null) {
            keyboard = new KeyboardState(prev.keyboard, event);
            mouse = prev.mouse;
        } else {
            keyboard = new KeyboardState(null, event);
            mouse = new MouseState();
        }

        timestamp = System.nanoTime();
    }

    /**
     * Create a new InputState that computes the effective state of applying the given
     * mouse event to the previous mouse state, and preserving the previous keyboard
     * state.
     *
     * @param prev  The previous input state
     * @param event The mouse event to apply
     *
     * @throws NullPointerException if event is null
     */
    public InputState(InputState prev, MouseEvent event) {
        if (prev != null) {
            keyboard = prev.keyboard;
            mouse = new MouseState(prev.mouse, event);
        } else {
            keyboard = new KeyboardState();
            mouse = new MouseState(null, event);
        }

        timestamp = System.nanoTime();
    }

    /**
     * @return The time stamp in nanoseconds of the event that produced this input state
     */
    public long getTimestamp() {
        return timestamp;
    }

    /**
     * @return The state of the mouse device at the time this state was current
     */
    public MouseState getMouseState() {
        return mouse;
    }

    /**
     * @return The state of the keyboard at the time this state was current
     */
    public KeyboardState getKeyboardState() {
        return keyboard;
    }
}
