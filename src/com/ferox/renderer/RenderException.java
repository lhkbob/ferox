package com.ferox.renderer;

/**
 * An exception to use when problems arise during the visiting of a scene, flushing of
 * a RenderQueue or access to a renderer's methods.
 * 
 * @author Michael Ludwig
 *
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
