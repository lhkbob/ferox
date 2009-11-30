package com.ferox.renderer.impl;

import java.util.concurrent.Future;

import com.ferox.resource.DirtyState;
import com.ferox.resource.Resource;
import com.ferox.resource.Resource.Status;

public interface ResourceManager {
	public Future<Status> scheduleUpdate(Resource resource, DirtyState<?> dirtyDescriptor);
	
	public Future<Object> scheduleDispose(Resource resource);
	
	public ResourceHandle getHandle(Resource resource);
	
	public Status getStatus(Resource resource);
	
	public String getStatusMessage(Resource resource);
	
	public void destroy();
}
