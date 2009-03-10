package com.ferox.state;

/**
 * An exception to use when problems arise during state creation and update.
 * 
 * @author Michael Ludwig
 *
 */
public class StateException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public StateException() {
		super();
	}

	public StateException(String arg0) {
		super(arg0);
	}

	public StateException(Throwable arg0) {
		super(arg0);
	}

	public StateException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

}
