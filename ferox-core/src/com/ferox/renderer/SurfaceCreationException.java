package com.ferox.renderer;

/**
 * An exception that should be thrown when there is a problem creating a new
 * Surface.
 * 
 * @author Michael Ludwig
 */
public class SurfaceCreationException extends RenderException {
	private static final long serialVersionUID = 1L;

	public SurfaceCreationException() {
		super();
	}

	public SurfaceCreationException(String arg0) {
		super(arg0);
	}

	public SurfaceCreationException(Throwable arg0) {
		super(arg0);
	}

	public SurfaceCreationException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
}
