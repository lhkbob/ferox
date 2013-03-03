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

import com.ferox.input.KeyEvent.KeyCode;
import com.ferox.input.MouseEvent.MouseButton;

import java.awt.*;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;

/**
 * AWTEventAdapter is a utility for converting events produced by Java's Abstract
 * Windowing Toolkit to the appropriate events for this framework. An EventSource that
 * wraps an AWT component can attach its component to an AWTEventAdapter and it will
 * automatically dispatch events.
 *
 * @author Michael Ludwig
 */
public class AWTEventAdapter
        implements java.awt.event.KeyListener, java.awt.event.MouseListener,
                   MouseMotionListener, MouseWheelListener {
    private Component component;

    private final MouseKeyEventDispatcher dispatcher;

    /**
     * Create a new AWTEventAdapter that will convert AWT events and dispatch them to the
     * given MouseKeyEventDispatcher.
     *
     * @param dispatcher The dispatcher to use
     */
    public AWTEventAdapter(MouseKeyEventDispatcher dispatcher) {
        if (dispatcher == null) {
            throw new NullPointerException("Dispatcher cannot be null");
        }

        this.dispatcher = dispatcher;
    }

    /**
     * <p/>
     * Attach the adapter to the given component. The adapter can only be attached to a
     * single component at a time and must be detached before listening on another
     * component.
     * <p/>
     * To produce meaningful events, the attached component must be related to the
     * EventSource implementation used by this adapter's dispatcher.
     * <p/>
     * The adapter cannot send events to its dispatcher until its been attached to an
     * alive component that is producing the relevant AWT events.
     *
     * @param component The component to attach to
     *
     * @throws NullPointerException  if component is null
     * @throws IllegalStateException if the adapter is currently attached to another
     *                               component
     */
    public void attach(Component component) {
        if (component == null) {
            throw new NullPointerException("Component cannot be null");
        }

        synchronized (this) {
            if (this.component != null) {
                throw new IllegalStateException(
                        "AWTEventAdapter already attached to another Component");
            }

            component.addKeyListener(this);
            component.addMouseListener(this);
            component.addMouseMotionListener(this);
            component.addMouseWheelListener(this);

            this.component = component;
        }
    }

    /**
     * Detach this adapter from the component it's currently attached to. If the adapter
     * is not attached to a component, nothing happens. After detaching, the adapter will
     * not convert and dispatch AWT events.
     */
    public void detach() {
        synchronized (this) {
            if (component != null) {
                component.removeKeyListener(this);
                component.removeMouseListener(this);
                component.removeMouseMotionListener(this);
                component.removeMouseWheelListener(this);

                component = null;
            }
        }
    }

    /* AWT event listener methods */

    private KeyCode getKeyCode(java.awt.event.KeyEvent e) {
        switch (e.getKeyCode()) {
        case java.awt.event.KeyEvent.VK_ESCAPE:
            return KeyEvent.KeyCode.ESCAPE;
        case java.awt.event.KeyEvent.VK_BACK_QUOTE:
            return KeyEvent.KeyCode.BACK_QUOTE;
        case java.awt.event.KeyEvent.VK_TAB:
            return KeyEvent.KeyCode.TAB;
        case java.awt.event.KeyEvent.VK_OPEN_BRACKET:
            return KeyEvent.KeyCode.LEFT_BRACKET;
        case java.awt.event.KeyEvent.VK_CLOSE_BRACKET:
            return KeyEvent.KeyCode.RIGHT_BRACKET;
        case java.awt.event.KeyEvent.VK_BACK_SLASH:
            return KeyEvent.KeyCode.BACK_SLASH;
        case java.awt.event.KeyEvent.VK_SLASH:
            return KeyEvent.KeyCode.FORWARD_SLASH;
        case java.awt.event.KeyEvent.VK_ENTER:
            return KeyEvent.KeyCode.RETURN;
        case java.awt.event.KeyEvent.VK_SEMICOLON:
            return KeyEvent.KeyCode.SEMICOLON;
        case java.awt.event.KeyEvent.VK_QUOTE:
            return KeyEvent.KeyCode.QUOTE;
        case java.awt.event.KeyEvent.VK_COMMA:
            return KeyEvent.KeyCode.COMMA;
        case java.awt.event.KeyEvent.VK_PERIOD:
            return KeyEvent.KeyCode.PERIOD;
        case java.awt.event.KeyEvent.VK_MINUS:
            return KeyEvent.KeyCode.MINUS;
        case java.awt.event.KeyEvent.VK_EQUALS:
            return KeyEvent.KeyCode.EQUALS;
        case java.awt.event.KeyEvent.VK_BACK_SPACE:
            return KeyEvent.KeyCode.BACK_SPACE;
        case java.awt.event.KeyEvent.VK_DELETE:
            return KeyEvent.KeyCode.DELETE;

        case java.awt.event.KeyEvent.VK_SPACE:
            return KeyEvent.KeyCode.SPACE;

        case java.awt.event.KeyEvent.VK_PAUSE:
            return KeyEvent.KeyCode.PAUSE;
        case java.awt.event.KeyEvent.VK_INSERT:
            return KeyEvent.KeyCode.INSERT;

        case java.awt.event.KeyEvent.VK_HOME:
            return KeyEvent.KeyCode.HOME;
        case java.awt.event.KeyEvent.VK_END:
            return KeyEvent.KeyCode.END;
        case java.awt.event.KeyEvent.VK_PAGE_UP:
            return KeyEvent.KeyCode.PAGE_UP;
        case java.awt.event.KeyEvent.VK_PAGE_DOWN:
            return KeyEvent.KeyCode.PAGE_DOWN;

        case java.awt.event.KeyEvent.VK_UP:
            return KeyEvent.KeyCode.UP;
        case java.awt.event.KeyEvent.VK_LEFT:
            return KeyEvent.KeyCode.LEFT;
        case java.awt.event.KeyEvent.VK_RIGHT:
            return KeyEvent.KeyCode.RIGHT;
        case java.awt.event.KeyEvent.VK_DOWN:
            return KeyEvent.KeyCode.DOWN;

        case java.awt.event.KeyEvent.VK_F1:
            return KeyEvent.KeyCode.F1;
        case java.awt.event.KeyEvent.VK_F2:
            return KeyEvent.KeyCode.F2;
        case java.awt.event.KeyEvent.VK_F3:
            return KeyEvent.KeyCode.F3;
        case java.awt.event.KeyEvent.VK_F4:
            return KeyEvent.KeyCode.F4;
        case java.awt.event.KeyEvent.VK_F5:
            return KeyEvent.KeyCode.F5;
        case java.awt.event.KeyEvent.VK_F6:
            return KeyEvent.KeyCode.F6;
        case java.awt.event.KeyEvent.VK_F7:
            return KeyEvent.KeyCode.F7;
        case java.awt.event.KeyEvent.VK_F8:
            return KeyEvent.KeyCode.F8;
        case java.awt.event.KeyEvent.VK_F9:
            return KeyEvent.KeyCode.F9;
        case java.awt.event.KeyEvent.VK_F10:
            return KeyEvent.KeyCode.F10;
        case java.awt.event.KeyEvent.VK_F11:
            return KeyEvent.KeyCode.F11;
        case java.awt.event.KeyEvent.VK_F12:
            return KeyEvent.KeyCode.F12;

        case java.awt.event.KeyEvent.VK_1:
            return KeyEvent.KeyCode.N1;
        case java.awt.event.KeyEvent.VK_2:
            return KeyEvent.KeyCode.N2;
        case java.awt.event.KeyEvent.VK_3:
            return KeyEvent.KeyCode.N3;
        case java.awt.event.KeyEvent.VK_4:
            return KeyEvent.KeyCode.N4;
        case java.awt.event.KeyEvent.VK_5:
            return KeyEvent.KeyCode.N5;
        case java.awt.event.KeyEvent.VK_6:
            return KeyEvent.KeyCode.N6;
        case java.awt.event.KeyEvent.VK_7:
            return KeyEvent.KeyCode.N7;
        case java.awt.event.KeyEvent.VK_8:
            return KeyEvent.KeyCode.N8;
        case java.awt.event.KeyEvent.VK_9:
            return KeyEvent.KeyCode.N9;
        case java.awt.event.KeyEvent.VK_0:
            return KeyEvent.KeyCode.N0;

        case java.awt.event.KeyEvent.VK_A:
            return KeyEvent.KeyCode.A;
        case java.awt.event.KeyEvent.VK_B:
            return KeyEvent.KeyCode.B;
        case java.awt.event.KeyEvent.VK_C:
            return KeyEvent.KeyCode.C;
        case java.awt.event.KeyEvent.VK_D:
            return KeyEvent.KeyCode.D;
        case java.awt.event.KeyEvent.VK_E:
            return KeyEvent.KeyCode.E;
        case java.awt.event.KeyEvent.VK_F:
            return KeyEvent.KeyCode.F;
        case java.awt.event.KeyEvent.VK_G:
            return KeyEvent.KeyCode.G;
        case java.awt.event.KeyEvent.VK_H:
            return KeyEvent.KeyCode.H;
        case java.awt.event.KeyEvent.VK_I:
            return KeyEvent.KeyCode.I;
        case java.awt.event.KeyEvent.VK_J:
            return KeyEvent.KeyCode.J;
        case java.awt.event.KeyEvent.VK_K:
            return KeyEvent.KeyCode.K;
        case java.awt.event.KeyEvent.VK_L:
            return KeyEvent.KeyCode.L;
        case java.awt.event.KeyEvent.VK_M:
            return KeyEvent.KeyCode.M;
        case java.awt.event.KeyEvent.VK_N:
            return KeyEvent.KeyCode.N;
        case java.awt.event.KeyEvent.VK_O:
            return KeyEvent.KeyCode.O;
        case java.awt.event.KeyEvent.VK_P:
            return KeyEvent.KeyCode.P;
        case java.awt.event.KeyEvent.VK_Q:
            return KeyEvent.KeyCode.Q;
        case java.awt.event.KeyEvent.VK_R:
            return KeyEvent.KeyCode.R;
        case java.awt.event.KeyEvent.VK_S:
            return KeyEvent.KeyCode.S;
        case java.awt.event.KeyEvent.VK_T:
            return KeyEvent.KeyCode.T;
        case java.awt.event.KeyEvent.VK_U:
            return KeyEvent.KeyCode.U;
        case java.awt.event.KeyEvent.VK_V:
            return KeyEvent.KeyCode.V;
        case java.awt.event.KeyEvent.VK_W:
            return KeyEvent.KeyCode.W;
        case java.awt.event.KeyEvent.VK_X:
            return KeyEvent.KeyCode.X;
        case java.awt.event.KeyEvent.VK_Y:
            return KeyEvent.KeyCode.Y;
        case java.awt.event.KeyEvent.VK_Z:
            return KeyEvent.KeyCode.Z;

        case java.awt.event.KeyEvent.VK_NUM_LOCK:
            return KeyEvent.KeyCode.NUM_LOCK;
        case java.awt.event.KeyEvent.VK_SCROLL_LOCK:
            return KeyEvent.KeyCode.SCROLL_LOCK;
        case java.awt.event.KeyEvent.VK_CAPS_LOCK:
            return KeyEvent.KeyCode.CAPS_LOCK;

        case java.awt.event.KeyEvent.VK_NUMPAD1:
            return KeyEvent.KeyCode.NUMPAD_1;
        case java.awt.event.KeyEvent.VK_NUMPAD2:
            return KeyEvent.KeyCode.NUMPAD_2;
        case java.awt.event.KeyEvent.VK_NUMPAD3:
            return KeyEvent.KeyCode.NUMPAD_3;
        case java.awt.event.KeyEvent.VK_NUMPAD4:
            return KeyEvent.KeyCode.NUMPAD_4;
        case java.awt.event.KeyEvent.VK_NUMPAD5:
            return KeyEvent.KeyCode.NUMPAD_5;
        case java.awt.event.KeyEvent.VK_NUMPAD6:
            return KeyEvent.KeyCode.NUMPAD_6;
        case java.awt.event.KeyEvent.VK_NUMPAD7:
            return KeyEvent.KeyCode.NUMPAD_7;
        case java.awt.event.KeyEvent.VK_NUMPAD8:
            return KeyEvent.KeyCode.NUMPAD_8;
        case java.awt.event.KeyEvent.VK_NUMPAD9:
            return KeyEvent.KeyCode.NUMPAD_9;
        case java.awt.event.KeyEvent.VK_NUMPAD0:
            return KeyEvent.KeyCode.NUMPAD_0;
        case java.awt.event.KeyEvent.VK_ADD:
            return KeyEvent.KeyCode.NUMPAD_ADD;
        case java.awt.event.KeyEvent.VK_SUBTRACT:
            return KeyEvent.KeyCode.NUMPAD_SUBTRACT;
        case java.awt.event.KeyEvent.VK_DECIMAL:
            return KeyEvent.KeyCode.NUMPAD_DECIMAL;
        case java.awt.event.KeyEvent.VK_DIVIDE:
            return KeyEvent.KeyCode.NUMPAD_DIVIDE;
        case java.awt.event.KeyEvent.VK_MULTIPLY:
            return KeyEvent.KeyCode.NUMPAD_MULTIPLY;

        case java.awt.event.KeyEvent.VK_ALT:
            if (e.getKeyLocation() == java.awt.event.KeyEvent.KEY_LOCATION_RIGHT) {
                return KeyEvent.KeyCode.RIGHT_ALT;
            } else {
                return KeyEvent.KeyCode.LEFT_ALT;
            }
        case java.awt.event.KeyEvent.VK_CONTROL:
            if (e.getKeyLocation() == java.awt.event.KeyEvent.KEY_LOCATION_RIGHT) {
                return KeyEvent.KeyCode.RIGHT_CONTROL;
            } else {
                return KeyEvent.KeyCode.LEFT_CONTROL;
            }
        case java.awt.event.KeyEvent.VK_SHIFT:
            if (e.getKeyLocation() == java.awt.event.KeyEvent.KEY_LOCATION_RIGHT) {
                return KeyEvent.KeyCode.RIGHT_SHIFT;
            } else {
                return KeyEvent.KeyCode.LEFT_SHIFT;
            }
        case java.awt.event.KeyEvent.VK_META:
            if (e.getKeyLocation() == java.awt.event.KeyEvent.KEY_LOCATION_RIGHT) {
                return KeyEvent.KeyCode.RIGHT_META;
            } else {
                return KeyEvent.KeyCode.LEFT_META;
            }

        default:
            return KeyEvent.KeyCode.UNKNOWN;
        }
    }

    private char getCharacter(java.awt.event.KeyEvent e) {
        if (e.getKeyChar() == java.awt.event.KeyEvent.CHAR_UNDEFINED) {
            return KeyEvent.CHAR_UNKNOWN;
        } else {
            return e.getKeyChar();
        }
    }

    private MouseButton getButton(java.awt.event.MouseEvent e) {
        switch (e.getButton()) {
        case java.awt.event.MouseEvent.NOBUTTON:
            return MouseEvent.MouseButton.NONE;
        case java.awt.event.MouseEvent.BUTTON1:
            return MouseEvent.MouseButton.LEFT;
        case java.awt.event.MouseEvent.BUTTON2:
            return MouseEvent.MouseButton.RIGHT;
        case java.awt.event.MouseEvent.BUTTON3:
            return MouseEvent.MouseButton.CENTER;
        default:
            throw new IllegalArgumentException("Unknown AWT button code");
        }
    }

    private int getY(java.awt.event.MouseEvent e) {
        return component.getHeight() - e.getY();
    }

    @Override
    public void keyPressed(java.awt.event.KeyEvent e) {
        KeyEvent event = new KeyEvent(KeyEvent.Type.PRESS, dispatcher.getSource(),
                                      getKeyCode(e), getCharacter(e));
        dispatcher.dispatchEvent(event);
    }

    @Override
    public void keyReleased(java.awt.event.KeyEvent e) {
        KeyEvent event = new KeyEvent(KeyEvent.Type.RELEASE, dispatcher.getSource(),
                                      getKeyCode(e), getCharacter(e));
        dispatcher.dispatchEvent(event);
    }

    @Override
    public void keyTyped(java.awt.event.KeyEvent e) {
        // ignore this event
    }

    @Override
    public void mouseClicked(java.awt.event.MouseEvent e) {
        // ignore this event
    }

    @Override
    public void mouseEntered(java.awt.event.MouseEvent e) {
        // ignore this event
    }

    @Override
    public void mouseExited(java.awt.event.MouseEvent e) {
        // ignore this event
    }

    @Override
    public void mousePressed(java.awt.event.MouseEvent e) {
        MouseEvent event = new MouseEvent(MouseEvent.Type.PRESS, dispatcher.getSource(),
                                          e.getX(), getY(e), 0, getButton(e));
        dispatcher.dispatchEvent(event);
    }

    @Override
    public void mouseReleased(java.awt.event.MouseEvent e) {
        MouseEvent event = new MouseEvent(MouseEvent.Type.RELEASE, dispatcher.getSource(),
                                          e.getX(), getY(e), 0, getButton(e));
        dispatcher.dispatchEvent(event);
    }

    @Override
    public void mouseDragged(java.awt.event.MouseEvent e) {
        // we treat a drag just like any other mouse move, the mouse
        // down has already been recorded by a mousePressed() event
        mouseMoved(e);
    }

    @Override
    public void mouseMoved(java.awt.event.MouseEvent e) {
        MouseEvent event = new MouseEvent(MouseEvent.Type.MOVE, dispatcher.getSource(),
                                          e.getX(), getY(e), 0, MouseButton.NONE);
        dispatcher.dispatchEvent(event);
    }

    @Override
    public void mouseWheelMoved(MouseWheelEvent e) {
        MouseEvent event = new MouseEvent(MouseEvent.Type.SCROLL, dispatcher.getSource(),
                                          e.getX(), getY(e), e.getWheelRotation(),
                                          MouseButton.NONE);
        dispatcher.dispatchEvent(event);
    }
}
