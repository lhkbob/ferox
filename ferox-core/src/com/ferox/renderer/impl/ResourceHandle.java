package com.ferox.renderer.impl;

import com.ferox.resource.Resource.Status;

public abstract class ResourceHandle {
	private final int id;
	private Status status;
	private String statusMessage;
	
	public ResourceHandle(int id) {
		this.id = id;
		setStatus(Status.DISPOSED);
		setStatusMessage("");
	}
	
	public final int getId() {
		return id;
	}
	
	public Status getStatus() {
		return status;
	}
	
	public String getStatusMessage() {
		return statusMessage;
	}
	
	public void setStatus(Status status) {
		this.status = (status == null ? Status.DISPOSED : status);
	}
	
	public void setStatusMessage(String message) {
		statusMessage = (message == null ? "" : message);
	}
}
