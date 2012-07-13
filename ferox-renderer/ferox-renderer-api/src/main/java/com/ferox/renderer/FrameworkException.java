package com.ferox.renderer;

/**
 * A RuntimeException that is used by the renderer package for exceptions that
 * are better described by custom exceptions, instead of the basic java.lang
 * ones.
 * 
 * @author Michael Ludwig
 */
public class FrameworkException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public FrameworkException() {
        super();
    }

    public FrameworkException(String arg0) {
        super(arg0);
    }

    public FrameworkException(Throwable arg0) {
        super(arg0);
    }

    public FrameworkException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }
}
