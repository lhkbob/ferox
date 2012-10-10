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
 * KeyListener is the event listener interface for handling keyboard input
 * events. Before a listener can receive events, it must be added to a
 * {@link KeyEventSource}. Its event handler, {@link #handleEvent(KeyEvent)}, is
 * invoked every time the user presses a key on the keyboard while the
 * listener's associated source has focus.
 * <p>
 * Depending on the OS, holding a key down might generate repeated key events of
 * the same key code without any release event until the key is finally
 * released.
 * 
 * @author Michael Ludwig
 */
public interface KeyListener extends EventListener {
    /**
     * <p>
     * Process the specified KeyEvent. This will be invoked as soon as possible
     * after the real-world event occurs but there is obviously some delay.
     * KeyListeners should strive to quickly return to allow other listeners to
     * process the event.
     * <p>
     * This method will be invoked on an internal event-queue thread managed by
     * the KeyEventSource this listener was registered to. Because of this,
     * KeyListener implementations must be thread-safe.
     * 
     * @param event The key event that just occurred
     */
    public void handleEvent(KeyEvent event);
}
