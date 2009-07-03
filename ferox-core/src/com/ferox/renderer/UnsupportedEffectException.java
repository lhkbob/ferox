package com.ferox.renderer;

/**
 * An exception that should be thrown whenever an Effect is used that the
 * Framework can't handle.
 * 
 * @author Michael Ludwig
 */
public class UnsupportedEffectException extends RenderException {
	private static final long serialVersionUID = 1L;

	public UnsupportedEffectException() {
		super();
	}

	public UnsupportedEffectException(String arg0) {
		super(arg0);
	}

	public UnsupportedEffectException(Throwable arg0) {
		super(arg0);
	}

	public UnsupportedEffectException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
}
