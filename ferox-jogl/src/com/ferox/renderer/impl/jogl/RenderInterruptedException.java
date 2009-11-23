package com.ferox.renderer.impl.jogl;

public class RenderInterruptedException extends RuntimeException {
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
