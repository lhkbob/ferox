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
package com.ferox.input;

/**
 * <p>
 * MouseEvent is the concrete event type representing a user's interactions with
 * a mouse. MouseEvent can represent mouse movements, button presses, and scroll
 * wheel changes.
 * <p>
 * The specific type of event is represented by the {@link Type} enum. Depending
 * on the type, the event's active mouse button or scroll wheel delta are not
 * meaningful.
 * <p>
 * <ul>
 * <li>MOVE and SCROLL events will not have a mouse button active.</li>
 * <li>PRESS and RELEASE events will have an active button.</li>
 * <li>SCROLL is the only event type to have a non-zero scroll delta.</li>
 * <li>All event types have valid x and y positions.</li>
 * </ul>
 * 
 * @author Michael Ludwig
 */
public class MouseEvent implements Event {
    /**
     * Type represents the various mouse event types. Some other mouse event
     * systems might support the concept of a 'drag' but this can be reproduced
     * by tracking when buttons are pressed and then listening to mouse movement
     * events.
     */
    public static enum Type {
        /**
         * Only the mouse's position changed. There was no button or scroll
         * wheel interaction.
         */
        MOVE,
        /**
         * One of the mouse's buttons was pressed down and has not been released
         * yet. Use {@link MouseEvent#getButton()} to determine which button was
         * pressed.
         */
        PRESS,
        /**
         * One of the mouse's buttons was released that has previously been
         * pressed. Use {@link MouseEvent#getButton()} to determine which button
         * was released.
         */
        RELEASE,
        /**
         * The scroll wheel was rolled forwards or backwards. Use
         * {@link MouseEvent#getScrollDelta()} to determine the magnitude and
         * direction.
         */
        SCROLL
    }

    /**
     * MouseButton is an enum representing all supported buttons in this event
     * framework. Some mice might not be capable of using the right or center
     * buttons.
     */
    public static enum MouseButton {
        /**
         * Does not represent an actual button, but is used when an event has no
         * button interaction.
         */
        NONE,
        /**
         * The left button.
         */
        LEFT,
        /**
         * The right button.
         */
        RIGHT,
        /**
         * The center button, often built into the scroll wheel.
         */
        CENTER
    }

    private final MouseEventSource source;
    private final Type type;

    private final int x;
    private final int y;
    private final int scrollDelta;

    private final MouseButton button;

    /**
     * Create a new MouseEvent with the given arguments.
     * 
     * @param type The type of event
     * @param source The event's source
     * @param x The mouse's current x position as of the event
     * @param y The mouse's current y position as of the event
     * @param scrollDelta The change in wheel position
     * @param button Any mouse button pressed
     * @throws NullPointerException if source, type, or button are null
     * @throws IllegalArgumentException if type, scrollDelta and button are
     *             incompatible
     */
    public MouseEvent(Type type, MouseEventSource source, int x, int y, int scrollDelta,
                      MouseButton button) {
        if (source == null) {
            throw new NullPointerException("Event source cannot be null");
        }
        if (type == null) {
            throw new NullPointerException("Type cannot be null");
        }
        if (button == null) {
            throw new IllegalArgumentException("Mouse button cannot be null");
        }

        // verify state
        if (type == Type.PRESS || type == Type.RELEASE) {
            if (button == MouseButton.NONE) {
                throw new IllegalArgumentException("Button cannot be NONE for a " + type + " event");
            }
        } else {
            if (button != MouseButton.NONE) {
                throw new IllegalArgumentException("Button must be NONE for a " + type + " event");
            }
        }
        if (type == Type.SCROLL) {
            if (scrollDelta == 0) {
                throw new IllegalArgumentException("Scroll delta must be non-zero for a " + type + " event");
            }
        } else {
            if (scrollDelta != 0) {
                throw new IllegalArgumentException("Scroll delta must be 0 for a " + type + " event");
            }
        }

        this.source = source;
        this.type = type;
        this.x = x;
        this.y = y;
        this.scrollDelta = scrollDelta;
        this.button = button;
    }

    /**
     * @return The type of mouse event
     */
    public Type getEventType() {
        return type;
    }

    /**
     * <p>
     * Get the x position of the mouse at the time of the event. Given that most
     * event sources are windows, the x position's 0 value is defined as the
     * left edge of the window and positive x values extend to the right edge.
     * <p>
     * If drags outside of the window's bounds are supported, the x value can be
     * negative or larger than the width of the window.
     * 
     * @return The mouse's x position
     */
    public int getX() {
        return x;
    }

    /**
     * <p>
     * Get the y position of the mouse at the time of the event. Given that most
     * event sources are windows, the y position's 0 value is defined as the
     * bottom edge of the window and positive y values extend to the top of the
     * window.
     * <p>
     * If drags outside of the window's bounds are supported, the y value can be
     * negative or larger than the height of the window.
     * 
     * @return The mouse's y position
     */
    public int getY() {
        return y;
    }

    /**
     * Get the amount of scroll increments the scroll wheel has been adjusted
     * by. This value is always 0 if the type is not SCROLL. When the event is a
     * SCROLL event it will be a positive or negative value. A positive value
     * represents a backwards scrolling motion; a negative value is a forwards
     * motion.
     * 
     * @return The scroll delta
     */
    public int getScrollDelta() {
        return scrollDelta;
    }

    /**
     * Get the mouse button that produced the event if the type is PRESS or
     * RELEASE. MOVE and SCROLL will always return NONE. PRESS and RELEASE will
     * always return one of LEFT, RIGHT or CENTER.
     * 
     * @return The mouse button of the event
     */
    public MouseButton getButton() {
        return button;
    }

    @Override
    public MouseEventSource getSource() {
        return source;
    }

    @Override
    public String toString() {
        return "[Mouse " + type + " at (" + x + ", " + y + "), button: " + button + ", scroll: " + scrollDelta + "]";
    }
}
