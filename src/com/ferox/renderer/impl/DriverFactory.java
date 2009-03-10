package com.ferox.renderer.impl;

/** Implementations of DriverFactory provide a low-level
 * driver object T that performs the necessary operations
 * for a high-level type, S.
 * 
 * This is primarily used by AbstractRenderer to get
 * drivers for resources, geometries, and states.  DriverFactories
 * should not be shared between AbstractRenderer instances.
 * 
 * @author Michael Ludwig */
public interface DriverFactory<S, T> {
	/** Return a driver object that can handle the specified
	 * type (S may be a class or other descriptor of "type").
	 * 
	 * DriverFactories should return the same driver instance 
	 * for repeated calls with the same type parameter.
	 * 
	 * Return null if no driver can be determined for the given type. */
	public T getDriver(S type);
}
