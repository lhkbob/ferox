package com.ferox.renderer;

/**
 * A RuntimeException that is used by the renderer package for exceptions that
 * are better described by custom exceptions, instead of the basic java.lang
 * ones.
 * 
 * @author Michael Ludwig
 */
public class RenderException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public RenderException() {
		super();
	}

	public RenderException(String arg0) {
		super(arg0);
	}

	public RenderException(Throwable arg0) {
		super(arg0);
	}

	public RenderException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
}
