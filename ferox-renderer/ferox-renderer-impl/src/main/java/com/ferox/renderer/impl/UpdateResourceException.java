package com.ferox.renderer.impl;

/**
 * UpdateResourceException is thrown by a {@link ResourceDriver} when a resource
 * update fails because the resource is misconfigured for the current hardware.
 * 
 * @author Michael Ludwig
 */
public class UpdateResourceException extends Exception {
    private static final long serialVersionUID = 1L;

    public UpdateResourceException(String errorMessage) {
        super(errorMessage);
    }
}
