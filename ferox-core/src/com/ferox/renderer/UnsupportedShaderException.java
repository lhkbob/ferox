package com.ferox.renderer;

/**
 * An exception that should be thrown whenever an Effect is used that the
 * Framework can't handle.
 * 
 * @author Michael Ludwig
 */
public class UnsupportedShaderException extends RenderException {
	private static final long serialVersionUID = 1L;

	public UnsupportedShaderException() {
		super();
	}

	public UnsupportedShaderException(String arg0) {
		super(arg0);
	}

	public UnsupportedShaderException(Throwable arg0) {
		super(arg0);
	}

	public UnsupportedShaderException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
}
