package com.ferox.util.entity;

public abstract class Controller {
	protected final EntitySystem system;
	
	public Controller(EntitySystem system) {
		if (system == null)
			throw new NullPointerException("EntitySystem cannot be null");
		// FIXME: should throw an exception if this returns non-null - make sure to restore previous controller
		// must fix permissions on register and unregister for controllers
		system.registerController(this);
		this.system = system;
	}
	
	public abstract void process();
}
