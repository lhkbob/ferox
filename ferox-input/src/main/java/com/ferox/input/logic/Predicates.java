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
import com.ferox.input.MouseEvent.MouseButton;

/**
 * Predicates provides static factory methods for many of the common predicates needed for input handling,
 * such as mouse movement, button clicks, or key presses.
 *
 * @author Michael Ludwig
 */
public final class Predicates {
    private Predicates() {
    }

    /**
     * Return a Predicate that evaluates to true when the key code transitions from the up to down position.
     *
     * @param code The key that is pressed
     *
     * @return A Predicate for matching key presses
     *
     * @throws NullPointerException if code is null
     */
    public static Predicate keyPress(final KeyCode code) {
        if (code == null) {
            throw new NullPointerException("KeyCode cannot be null");
        }
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return !prev.getKeyboardState().isKeyDown(code) && next.getKeyboardState().isKeyDown(code);
            }
        };
    }

    /**
     * Return a Predicate that evaluates to true when the key code transitions from the down to up position.
     *
     * @param code The key that is released
     *
     * @return A Predicate for matching key releases
     *
     * @throws NullPointerException if code is null
     */
    public static Predicate keyRelease(final KeyCode code) {
        if (code == null) {
            throw new NullPointerException("KeyCode cannot be null");
        }
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return prev.getKeyboardState().isKeyDown(code) && !next.getKeyboardState().isKeyDown(code);
            }
        };
    }

    /**
     * Return a Predicate that evaluates to true when the key code is held down for the duration of the state
     * transition. This will continue to evaluate to true until the key is released.
     *
     * @param code The key that is held
     *
     * @return A Predicate for matching keys held down
     *
     * @throws NullPointerException if code is null
     */
    public static Predicate keyHeld(final KeyCode code) {
        if (code == null) {
            throw new NullPointerException("KeyCode cannot be null");
        }
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                // we use prev as the signal so that we still trigger during
                // the transition of a release (since it was held until the
                // very end), but not the transition for a press (since it was
                // not held until the very end).
                return prev.getKeyboardState().isKeyDown(code);
            }
        };
    }

    /**
     * Return a Predicate that evaluates to true when the key code is pressed and released in under 250
     * milliseconds (i.e. typing that key). This will not evaluate to true until the key is released. If the
     * release occurs too slowly, it will not trigger.
     *
     * @param code The key that is typed
     *
     * @return A Predicate for matching key typing
     *
     * @throws NullPointerException if code is null
     */
    public static Predicate keyTyped(KeyCode code) {
        return keyTyped(code, 250L);
    }

    /**
     * Return a Predicate that evaluates to true when the key code is pressed and released in under
     * <var>delay</var> milliseconds (i.e. typing that key). This will not evaluate to true until the key is
     * released. If the release occurs too slowly, it will not trigger.
     *
     * @param code  The key that is typed
     * @param delay The maximum delay between press and release, in ms
     *
     * @return A Predicate for matching key typing
     *
     * @throws NullPointerException     if code is null
     * @throws IllegalArgumentException if delay is less than or equal to 0
     */
    public static Predicate keyTyped(KeyCode code, long delay) {
        return new KeyTypedPredicate(code, delay);
    }

    /**
     * Return a Predicate that evaluates to true when the mouse button transitions from the up to down
     * position. When the NONE button is used, it evaluates to true once all buttons are released.
     *
     * @param button The button that is pressed
     *
     * @return A Predicate for matching button presses
     *
     * @throws NullPointerException if button is null
     */
    public static Predicate mousePress(final MouseButton button) {
        if (button == null) {
            throw new NullPointerException("MouseButton cannot be null");
        }
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return !prev.getMouseState().isButtonDown(button) &&
                       next.getMouseState().isButtonDown(button);
            }
        };
    }

    /**
     * Return a Predicate that evaluates to true when the mouse button transitions from the down to up
     * position. When the NONE button is used, it evaluates to true when any button is pressed and there were
     * no other buttons pressed before that.
     *
     * @param button The button that is released
     *
     * @return A Predicate for matching button releases
     *
     * @throws NullPointerException if button is null
     */
    public static Predicate mouseRelease(final MouseButton button) {
        if (button == null) {
            throw new NullPointerException("MouseButton cannot be null");
        }
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return prev.getMouseState().isButtonDown(button) &&
                       !next.getMouseState().isButtonDown(button);
            }
        };
    }

    /**
     * Return a Predicate that evaluates to true when the mouse button is held down for the duration of the
     * state transition. This will continue to evaluate to true until the button is released. When the NONE
     * button is used, this evaluates to true so long as no other physical button is held down.
     *
     * @param button The button that is being held
     *
     * @return A Predicate for matching mouse buttons held down
     *
     * @throws NullPointerException if button is null
     */
    public static Predicate mouseHeld(final MouseButton button) {
        if (button == null) {
            throw new NullPointerException("MouseButton cannot be null");
        }
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                // see keyHeld() for why we use prev instead of next
                return prev.getMouseState().isButtonDown(button);
            }
        };
    }

    /**
     * Return a Predicate that evaluates to true when the mouse button is pressed and released once in under
     * 150 milliseconds (i.e. a single click). This will not evaluate to true until the button is released. If
     * the release occurs too slowly, it will not trigger.
     *
     * @param button The button that is clicked
     *
     * @return A Predicate for matching mouse clicking
     *
     * @throws NullPointerException     if button is null
     * @throws IllegalArgumentException if button is NONE
     */
    public static Predicate mouseClick(MouseButton button) {
        return mouseClick(button, 1);
    }

    /**
     * Return a Predicate that evaluates to true when the mouse button is pressed and released
     * <var>numClicks</var> times in under 150 milliseconds (i.e. multiple clicks). This will not evaluate to
     * true until the button is released. If the release occurs too slowly, it will not trigger.
     *
     * @param button The button that is clicked
     *
     * @return A Predicate for matching mouse clicking
     *
     * @throws NullPointerException     if button is null
     * @throws IllegalArgumentException if button is NONE or numClicks is less than or equal to 0
     */
    public static Predicate mouseClick(MouseButton button, int numClicks) {
        return mouseClick(button, numClicks, 150L);
    }

    /**
     * Return a Predicate that evaluates to true when the mouse button is pressed and released
     * <var>numClicks</var> times in under <var>delay</var> milliseconds (i.e. multiple clicks). This will not
     * evaluate to true until the button is released. If the release occurs too slowly, it will not trigger.
     *
     * @param button The button that is clicked
     *
     * @return A Predicate for matching mouse clicking
     *
     * @throws NullPointerException     if button is null
     * @throws IllegalArgumentException if button is NONE, or numClicks or delay are less than or equal to 0
     */
    public static Predicate mouseClick(MouseButton button, int numClicks, long delay) {
        return new MouseClickedPredicate(button, numClicks, delay);
    }

    /**
     * Return a Predicate that evaluates to true when the mouse button is pressed and released twice in under
     * 150 milliseconds (i.e. a double-click). This will not evaluate to true until the button is released. If
     * the release occurs too slowly, it will not trigger.
     *
     * @param button The button that is clicked
     *
     * @return A Predicate for matching double-clicking
     *
     * @throws NullPointerException     if button is null
     * @throws IllegalArgumentException if button is NONE
     */
    public static Predicate doubleClick(MouseButton button) {
        return mouseClick(button, 2);
    }

    /**
     * Return a Predicate that evaluates to true when the mouse's position changes and no button is held
     * down.
     *
     * @return A Predicate for matching mouse movement
     */
    public static Predicate mouseMove() {
        return mouseMove(false);
    }

    /**
     * <p/>
     * Return a Predicate that evaluates to true when the mouse's position changes. If
     * <var>allowAnyButton</var> is true, then the predicate does not depend on the mouse's button state and
     * will return true regardless of which buttons are held down.
     * <p/>
     * If it is false, the predicate will only return true when no buttons are held down.
     *
     * @param allowAnyButton True if any button can be pressed and the predicate will still return true
     *
     * @return A Predicate for matching mouse movement
     */
    public static Predicate mouseMove(boolean allowAnyButton) {
        if (allowAnyButton) {
            // if any button is allowed, we just look for delta movement
            // and don't care about button state
            return new Predicate() {
                @Override
                public boolean apply(InputState prev, InputState next) {
                    MouseState pm = prev.getMouseState();
                    MouseState nm = next.getMouseState();
                    return pm.getX() != nm.getX() || pm.getY() != nm.getY();
                }
            };
        } else {
            return mouseDrag(MouseButton.NONE);
        }
    }

    /**
     * Return a Predicate that evaluates to true when the mouse's position changes and the specified mouse
     * button is held down. If the NONE button is used, this is equivalent to {@link #mouseMove()}.
     *
     * @param button The button that must be held down for the drag
     *
     * @return A Predicate matching mouse dragging
     *
     * @throws NullPointerException if button is null
     */
    public static Predicate mouseDrag(MouseButton button) {
        return and(mouseMove(true), mouseHeld(button));
    }

    /**
     * Return a Predicate that evaluates to true when the amount of wheel scrolling moves in the negative
     * direction, which is synonymous with scrolling the wheel forward.
     *
     * @return A Predicate matching forward wheel motion
     */
    public static Predicate forwardScroll() {
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return next.getMouseState().getScrollCount() < next.getMouseState().getScrollCount();
            }
        };
    }

    /**
     * Return a Predicate that evaluates to true when the amount of wheel scrolling moves in the positive
     * direction, which is synonymous with scrolling the wheel backward.
     *
     * @return A Predicate matching backward wheel motion
     */
    public static Predicate backwardScroll() {
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return next.getMouseState().getScrollCount() > next.getMouseState().getScrollCount();
            }
        };
    }

    /**
     * Return a Predicate that returns true when the logical AND of the left and right predicates returns true
     * (i.e. when both left and right return true). Both predicates are always evaluated, even when one is
     * known to be false before evaluating the second.
     *
     * @param left  The left side of the && operation
     * @param right The right side of the && operation
     *
     * @return A Predicate that performs a logical AND over two predicates
     *
     * @throws NullPointerException if left or right are null
     */
    public static Predicate and(final Predicate left, final Predicate right) {
        if (left == null || right == null) {
            throw new NullPointerException("Predicate arguments to and() cannot be null");
        }
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                // make sure to invoke both predicates
                boolean leftResult = left.apply(prev, next);
                boolean rightResult = right.apply(prev, next);
                return leftResult && rightResult;
            }
        };
    }

    /**
     * Return a Predicate that returns true when the logical OR of the left and right predicates returns true
     * (i.e. when either left or right return true). Both predicates are always evaluated, even when one is
     * known to be true before evaluating the second.
     *
     * @param left  The left side of the || operation
     * @param right The right side of the || operation
     *
     * @return A Predicate that performs a logical OR over two predicates
     *
     * @throws NullPointerException if left or right are null
     */
    public static Predicate or(final Predicate left, final Predicate right) {
        if (left == null || right == null) {
            throw new NullPointerException("Predicate arguments to or() cannot be null");
        }
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                // make sure to invoke both predicates
                boolean leftResult = left.apply(prev, next);
                boolean rightResult = right.apply(prev, next);
                return leftResult || rightResult;
            }
        };
    }

    /**
     * Return a Predicate that returns true when the provided predicate returns false. This performs a logical
     * NOT operation.
     *
     * @param not The predicate that is negated
     *
     * @return A Predicate that performs the logical NOT over another predicate
     *
     * @throws NullPointerException if not is null
     */
    public static Predicate not(final Predicate not) {
        if (not == null) {
            throw new NullPointerException("Predicate argument to not() cannot be null");
        }
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return !not.apply(prev, next);
            }
        };
    }
}
