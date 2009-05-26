package com.ferox.renderer;

/**
 * <p>
 * An exception that represents the requested action or method call on the
 * Framework can't be executed because the renderer isn't in the appropriate
 * state to handle the request.
 * </p>
 * <p>
 * Most likely renderers have three main states: rendering, idle, and destroyed.
 * </p>
 * 
 * @author Michael Ludwig
 */
public class RenderStateException extends RenderException {
	private static final long serialVersionUID = 1L;

	public RenderStateException() {
		super();
	}

	public RenderStateException(String arg0) {
		super(arg0);
	}

	public RenderStateException(Throwable arg0) {
		super(arg0);
	}

	public RenderStateException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
}
