package com.ferox.renderer;

/**
 * An exception that should be thrown whenever a Resource (or Geometry) is used
 * that the Renderer can't handle.
 * 
 * @author Michael Ludwig
 * 
 */
public class UnsupportedResourceException extends RenderException {
	private static final long serialVersionUID = 1L;

	public UnsupportedResourceException() {
		super();
	}

	public UnsupportedResourceException(String arg0) {
		super(arg0);
	}

	public UnsupportedResourceException(Throwable arg0) {
		super(arg0);
	}

	public UnsupportedResourceException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}
}
