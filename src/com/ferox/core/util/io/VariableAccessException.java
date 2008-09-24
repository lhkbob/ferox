package com.ferox.core.util.io;

public class VariableAccessException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	public VariableAccessException() {
		super();
	}
	
	public VariableAccessException(String msg) {
		super(msg);
	}
	
	public VariableAccessException(String msg, Throwable t) {
		super(msg, t);
	}
}
