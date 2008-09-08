package com.ferox.core.states;

public class StateUpdateException extends RuntimeException {
	private static final long serialVersionUID = 1L;

	private StateAtom failedAtom;
	
	public StateUpdateException(StateAtom atom) {
		super();
		this.failedAtom = atom;
	}

	public StateUpdateException(StateAtom atom, String msg) {
		super(msg);
		this.failedAtom = atom;
	}

	public StateUpdateException(StateAtom atom, String msg, Throwable error) {
		super(msg, error);
		this.failedAtom = atom;
	}

	public StateAtom getStateAtom() {
		return this.failedAtom;
	}
}
