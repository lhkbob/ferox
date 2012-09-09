package com.ferox.input.logic;

import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.MouseEvent.MouseButton;

public final class Predicates {
    private Predicates() { }
    
    public static Predicate keyPressed(final KeyCode code) {
        if (code == null)
            throw new NullPointerException("KeyCode cannot be null");
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return !prev.getKeyboardState().isKeyDown(code) && next.getKeyboardState().isKeyDown(code);
            }
        };
    }
    
    public static Predicate keyReleased(final KeyCode code) {
        if (code == null)
            throw new NullPointerException("KeyCode cannot be null");
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return prev.getKeyboardState().isKeyDown(code) && !next.getKeyboardState().isKeyDown(code);
            }
        };
    }
    
    public static Predicate keyHeld(final KeyCode code) {
        if (code == null)
            throw new NullPointerException("KeyCode cannot be null");
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return next.getKeyboardState().isKeyDown(code);
            }
        };
    }
    
    public static Predicate keyTyped(KeyCode code) {
        return keyTyped(code, 250L);
    }
    
    public static Predicate keyTyped(KeyCode code, long delay) {
        return new KeyTypedPredicate(code, delay);
    }
    
    public static Predicate mousePressed(final MouseButton button) {
        if (button == null)
            throw new NullPointerException("MouseButton cannot be null");
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return !prev.getMouseState().isButtonDown(button) && next.getMouseState().isButtonDown(button);
            }
        };
    }
    
    public static Predicate mouseReleased(final MouseButton button) {
        if (button == null)
            throw new NullPointerException("MouseButton cannot be null");
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return prev.getMouseState().isButtonDown(button) && !next.getMouseState().isButtonDown(button);
            }
        };
    }
    
    public static Predicate mouseHeld(final MouseButton button) {
        if (button == null)
            throw new NullPointerException("MouseButton cannot be null");
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return next.getMouseState().isButtonDown(button);
            }
        };
    }
    
    public static Predicate mouseClicked(MouseButton button) {
        return mouseClicked(button, 1);
    }
    
    public static Predicate mouseClicked(MouseButton button, int numClicks) {
        return mouseClicked(button, numClicks, 150L);
    }
    
    public static Predicate mouseClicked(MouseButton button, int numClicks, long delay) {
        return new MouseClickedPredicate(button, numClicks, delay);
    }
    
    public static Predicate doubleClicked(MouseButton button) {
        return mouseClicked(button, 2);
    }
    
    public static Predicate mouseMoved() {
        return mouseMoved(false);
    }
    
    public static Predicate mouseMoved(boolean allowAnyButton) {
        if (allowAnyButton) {
            // if any button is allowed, we just look for delta movement
            // and don't care about button state
            return new Predicate() {
                @Override
                public boolean apply(InputState prev, InputState next) {
                    return next.getMouseState().getDeltaX() != 0 || next.getMouseState().getDeltaY() != 0;
                }
            };
        } else {
            return and(mouseMoved(true), mouseHeld(MouseButton.NONE));
        }
    }
    
    public static Predicate mouseDragged(MouseButton button) {
        return and(mouseMoved(true), mouseHeld(button));
    }
    
    public static Predicate forwardScroll() {
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return next.getMouseState().getScrollDelta() < 0;
            }
        };
    }
    
    public static Predicate backwardScroll() {
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return next.getMouseState().getScrollDelta() > 0;
            }
        };
    }
    
    public static Predicate and(final Predicate left, final Predicate right) {
        if (left == null || right == null)
            throw new NullPointerException("Predicate arguments to and() cannot be null");
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
    
    public static Predicate or(final Predicate left, final Predicate right) {
        if (left == null || right == null)
            throw new NullPointerException("Predicate arguments to or() cannot be null");
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
    
    public static Predicate not(final Predicate not) {
        if (not == null)
            throw new NullPointerException("Predicate argument to not() cannot be null");
        return new Predicate() {
            @Override
            public boolean apply(InputState prev, InputState next) {
                return !not.apply(prev, next);
            }
        };
    }
}
