package com.ferox.renderer.impl;

import com.ferox.renderer.RenderException;

/**
 * A RuntimeException that can be thrown when a rendering is interrupted. It is
 * very similar in purpose to an {@link InterruptedException} except that it is
 * unchecked so that it can be thrown by Renderer implementations.
 * 
 * @author Michael Ludwig
 */
public class RenderInterruptedException extends RenderException {
    private static final long serialVersionUID = 1L;

    public RenderInterruptedException() { }

    public RenderInterruptedException(String message) {
        super(message);
    }

    public RenderInterruptedException(Throwable cause) {
        super(cause);
    }

    public RenderInterruptedException(String message, Throwable cause) {
        super(message, cause);
    }
}
