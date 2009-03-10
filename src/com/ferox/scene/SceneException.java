package com.ferox.scene;

/**
 * An exception to use when problems arise during scene creation and update.
 * 
 * @author Michael Ludwig
 *
 */
public class SceneException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public SceneException() {
		super();
	}

	public SceneException(String arg0) {
		super(arg0);
	}

	public SceneException(Throwable arg0) {
		super(arg0);
	}

	public SceneException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
