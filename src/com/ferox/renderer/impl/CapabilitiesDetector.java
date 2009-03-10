package com.ferox.renderer.impl;

import com.ferox.renderer.RenderCapabilities;

/**
 * CapabilitiesDetector queries the computer's hardware for 
 * its RenderCapabilities and returns them when requested.
 * 
 * @author Michael Ludwig
 *
 */
public interface CapabilitiesDetector {
	/** Must somehow detect the system's capabilities and return them.
	 * This will only be called once in the constructor of AbstractRenderer, 
	 * so it can be a slow operation.
	 * 
	 * It cannot return a null capabilities. */
	public RenderCapabilities detect();
}
