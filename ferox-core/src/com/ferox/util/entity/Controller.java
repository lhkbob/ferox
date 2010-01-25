package com.ferox.util.entity;

public abstract class Controller {
	protected final EntitySystem system;
	boolean invalid;
	
	public Controller(EntitySystem system) {
		if (system == null)
			throw new NullPointerException("EntitySystem cannot be null");
		this.system = system;
		invalid = false;

		system.registerController(this); // can fail
	}
	
	public abstract void process();
	
	protected final void validate() {
		if (invalid)
			throw new IllegalStateException("Controller has been removed from its EntitySystem");
	}
}
